package com.dramer.clinic;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.*;
import android.view.accessibility.*;
import java.util.*;

public class ClinicAccessibilityService extends AccessibilityService {
    private final Handler h=new Handler(Looper.getMainLooper());
    private long lastInject=0,lastCapture=0;
    private final Runnable poller=new Runnable(){@Override public void run(){try{tick();}catch(Exception ignored){}h.postDelayed(this,650);}};

    @Override protected void onServiceConnected(){super.onServiceConnected();h.removeCallbacks(poller);h.post(poller);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){tick();}

    private void tick(){
        AutomationCoordinator.Job j=AutomationCoordinator.current();
        if(j==null||j.future.isDone())return;
        AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return;
        CharSequence pkg=root.getPackageName(); if(pkg==null||!pkg.toString().contains("chrome"))return;
        long now=System.currentTimeMillis();
        if(!j.submitted && !j.sendAttemptInProgress && now-lastInject>900){lastInject=now;injectOrSend(j,root);}
        else if(j.submitted && now-lastCapture>850){lastCapture=now;capture(j,root);}
    }

    private void injectOrSend(AutomationCoordinator.Job j,AccessibilityNodeInfo root){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        AccessibilityNodeInfo editor=findBestEditor(root);
        if(editor==null){j.phase="WAITING_FOR_AI_COMPOSER";return;}
        String existing=safeText(editor);
        if(!existing.contains(j.startMarker)){
            j.phase="COMPOSER_FOUND";
            boolean ok=setEditorText(editor,j.prompt);
            if(!ok){j.phase="COMPOSER_SET_TEXT_FAILED";return;}
            j.phase="PROMPT_INSERTED";
            h.postDelayed(()->attemptSend(j),550);
        }else{
            j.phase="PROMPT_READY_TO_SEND";
            attemptSend(j);
        }
    }

    private void attemptSend(AutomationCoordinator.Job j){
        if(AutomationCoordinator.current()!=j||j.future.isDone()||j.submitted||j.sendAttemptInProgress)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
        AccessibilityNodeInfo editor=findBestEditor(root);if(editor==null)return;
        j.sendAttemptInProgress=true;j.sendAttempts++;j.phase="SEND_ATTEMPT_"+j.sendAttempts;

        boolean attempted=false;
        AccessibilityNodeInfo send=findBestSend(root,editor,j.provider);
        if(send!=null) attempted=clickNodeOrAncestor(send);
        if(!attempted) attempted=dispatchFallbackSendTap(editor,j.provider);

        final boolean didAttempt=attempted;
        h.postDelayed(()->verifySubmitted(j,didAttempt),900);
    }

    private void verifySubmitted(AutomationCoordinator.Job j,boolean didAttempt){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){j.sendAttemptInProgress=false;return;}
        AccessibilityNodeInfo editor=findBestEditor(root);
        boolean promptStillPresent=editor!=null && safeText(editor).contains(j.startMarker);
        if(didAttempt && !promptStillPresent){
            j.submitted=true;j.sendAttemptInProgress=false;j.phase="PROMPT_SENT";h.postDelayed(this::tick,900);
        }else{
            j.sendAttemptInProgress=false;j.phase="SEND_NOT_CONFIRMED_RETRY";
        }
    }

    private boolean setEditorText(AccessibilityNodeInfo editor,String text){
        try{
            editor.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle b=new Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
            if(editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b))return true;
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm!=null){
                cm.setPrimaryClip(ClipData.newPlainText("clinic_prompt",text));
                if(editor.performAction(AccessibilityNodeInfo.ACTION_PASTE))return true;
            }
        }catch(Exception ignored){}
        return false;
    }

    private AccessibilityNodeInfo findBestEditor(AccessibilityNodeInfo root){
        Rect rb=new Rect();root.getBoundsInScreen(rb);int screenBottom=Math.max(rb.bottom,1);
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectEditors(root,all,0);
        AccessibilityNodeInfo best=null;int bestScore=Integer.MIN_VALUE;
        for(AccessibilityNodeInfo n:all){
            String meta=meta(n).toLowerCase(Locale.ROOT);
            String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            if(isChromeAddressUi(meta,id))continue;
            Rect r=new Rect();n.getBoundsInScreen(r);int cy=(r.top+r.bottom)/2;
            if(cy < screenBottom*0.22)continue;
            int score=cy/3;
            if(hasAny(meta,"ask gemini","gemini","message chatgpt","message","prompt","composer","ask","اسأل","اكتب","رسالة","ادخل","أدخل"))score+=900;
            if(hasAny(id,"prompt","composer","textarea","input"))score+=700;
            String cls=String.valueOf(n.getClassName()).toLowerCase(Locale.ROOT);
            if(cls.contains("edittext"))score+=150;
            if(n.isFocused())score+=100;
            if(score>bestScore){bestScore=score;best=n;}
        }
        return best;
    }

    private void collectEditors(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int d){
        if(n==null||d>90)return;
        if(n.isEditable()||has(n,AccessibilityNodeInfo.ACTION_SET_TEXT)||has(n,AccessibilityNodeInfo.ACTION_PASTE))out.add(n);
        for(int i=0;i<n.getChildCount();i++)collectEditors(n.getChild(i),out,d+1);
    }

    private boolean isChromeAddressUi(String meta,String id){
        return hasAny(id,"url_bar","location_bar","omnibox","search_box","toolbar","fakebox","address_bar") ||
               hasAny(meta,"search or type web address","search or type url","address and search bar","بحث أو كتابة عنوان الويب","شريط العنوان والبحث");
    }

    private AccessibilityNodeInfo findBestSend(AccessibilityNodeInfo root,AccessibilityNodeInfo editor,String provider){
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectClickable(root,all,0);
        Rect er=new Rect();editor.getBoundsInScreen(er);
        AccessibilityNodeInfo explicit=null;int explicitScore=Integer.MIN_VALUE;
        AccessibilityNodeInfo fallback=null;double fallbackScore=Double.MAX_VALUE;
        int targetX="gemini".equalsIgnoreCase(provider)?er.left+Math.max(45,er.width()/12):er.right-Math.max(45,er.width()/12);
        int targetY=er.bottom-Math.max(42,Math.min(90,er.height()/6));
        for(AccessibilityNodeInfo n:all){
            Rect r=new Rect();n.getBoundsInScreen(r);if(r.isEmpty())continue;
            String m=meta(n).toLowerCase(Locale.ROOT);String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            if(isChromeAddressUi(m,id))continue;
            int cx=(r.left+r.right)/2,cy=(r.top+r.bottom)/2;
            if(hasAny(m,"send message","send prompt","send","submit","إرسال","ارسال","إرسال الرسالة")||hasAny(id,"send","submit")){
                int s=2000-Math.abs(cy-targetY)-Math.abs(cx-targetX)/2;if(s>explicitScore){explicitScore=s;explicit=n;}
            }
            boolean nearComposer=cy>=er.top-60 && cy<=er.bottom+70;
            boolean saneSize=r.width()<=Math.max(220,er.width()/2) && r.height()<=Math.max(220,er.height());
            boolean excluded=hasAny(m,"microphone","voice","attach","attachment","add","plus","tools","model","dropdown","camera","photo","mic","ميكروفون")||hasAny(id,"mic","attach","plus","tool","model");
            if(nearComposer&&saneSize&&!excluded){
                double dist=Math.hypot(cx-targetX,cy-targetY);
                if(dist<fallbackScore){fallbackScore=dist;fallback=n;}
            }
        }
        return explicit!=null?explicit:fallback;
    }

    private boolean clickNodeOrAncestor(AccessibilityNodeInfo n){
        AccessibilityNodeInfo cur=n;
        for(int i=0;i<5&&cur!=null;i++){
            try{if((cur.isClickable()||has(cur,AccessibilityNodeInfo.ACTION_CLICK))&&cur.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;}catch(Exception ignored){}
            cur=cur.getParent();
        }
        return false;
    }

    private boolean dispatchFallbackSendTap(AccessibilityNodeInfo editor,String provider){
        try{
            Rect er=new Rect();editor.getBoundsInScreen(er);if(er.isEmpty())return false;
            float x="gemini".equalsIgnoreCase(provider)?er.left+Math.max(55,er.width()*0.08f):er.right-Math.max(55,er.width()*0.08f);
            float y=er.bottom-Math.max(48,Math.min(90,er.height()*0.17f));
            Path p=new Path();p.moveTo(x,y);
            GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(p,0,70);
            return dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
        }catch(Exception ignored){return false;}
    }

    private void collectClickable(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int d){
        if(n==null||d>90)return;
        if(n.isClickable()||has(n,AccessibilityNodeInfo.ACTION_CLICK))out.add(n);
        for(int i=0;i<n.getChildCount();i++)collectClickable(n.getChild(i),out,d+1);
    }

    private void capture(AutomationCoordinator.Job j,AccessibilityNodeInfo root){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        j.phase="WAITING_FOR_AI_RESULT";
        StringBuilder sb=new StringBuilder();collectText(root,sb,0);
        String json=JsonTools.extractMarkedJson(sb.toString(),j.startMarker,j.endMarker);if(json==null)return;
        try{
            org.json.JSONObject o=new org.json.JSONObject(json);
            if(!j.requestId.equals(o.optString("request_id")))return;
            AutomationCoordinator.complete(j,json);
            Intent i=new Intent(this,MainActivity.class);i.putExtra("captured_json",json);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);
        }catch(Exception ignored){}
    }

    private void collectText(AccessibilityNodeInfo n,StringBuilder sb,int d){
        if(n==null||d>90||sb.length()>400000)return;
        if(n.getText()!=null)sb.append('\n').append(n.getText());
        if(n.getContentDescription()!=null)sb.append('\n').append(n.getContentDescription());
        if(Build.VERSION.SDK_INT>=26 && n.getHintText()!=null)sb.append('\n').append(n.getHintText());
        for(int i=0;i<n.getChildCount();i++)collectText(n.getChild(i),sb,d+1);
    }

    private String safeText(AccessibilityNodeInfo n){return n!=null&&n.getText()!=null?n.getText().toString():"";}
    private String meta(AccessibilityNodeInfo n){
        StringBuilder s=new StringBuilder();
        if(n.getText()!=null)s.append(' ').append(n.getText());
        if(n.getContentDescription()!=null)s.append(' ').append(n.getContentDescription());
        if(Build.VERSION.SDK_INT>=26 && n.getHintText()!=null)s.append(' ').append(n.getHintText());
        if(n.getClassName()!=null)s.append(' ').append(n.getClassName());
        return s.toString();
    }
    private boolean hasAny(String s,String... xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private boolean has(AccessibilityNodeInfo n,int id){for(AccessibilityNodeInfo.AccessibilityAction a:n.getActionList())if(a.getId()==id)return true;return false;}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){h.removeCallbacks(poller);super.onDestroy();}
}
