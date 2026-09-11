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
    private final Runnable poller=new Runnable(){@Override public void run(){try{tick();}catch(Exception ignored){}h.postDelayed(this,550);}};

    @Override protected void onServiceConnected(){super.onServiceConnected();h.removeCallbacks(poller);h.post(poller);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){tick();}

    private void tick(){
        AutomationCoordinator.Job j=AutomationCoordinator.current();
        if(j==null||j.future.isDone())return;
        AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return;
        CharSequence pkg=root.getPackageName(); if(pkg==null||!pkg.toString().contains("chrome"))return;
        long now=System.currentTimeMillis();
        if(!j.submitted && !j.sendAttemptInProgress && now-lastInject>700){lastInject=now;injectOrSend(j,root);}
        else if(j.submitted && now-lastCapture>700){lastCapture=now;capture(j,root);}
    }

    private void injectOrSend(AutomationCoordinator.Job j,AccessibilityNodeInfo root){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        AccessibilityNodeInfo editor=findBestEditor(root);
        if(editor==null){j.phase="WAITING_FOR_AI_COMPOSER";return;}
        boolean alreadyInserted=containsMarker(editor,j.startMarker,0);
        if(!alreadyInserted){
            j.phase="COMPOSER_FOUND";
            boolean ok=setEditorText(editor,j.prompt);
            if(!ok){j.phase="COMPOSER_SET_TEXT_FAILED";return;}
            j.phase="PROMPT_INSERTED";
            h.postDelayed(()->attemptSend(j),450);
        }else{
            j.phase="PROMPT_READY_TO_SEND";
            attemptSend(j);
        }
    }

    private void attemptSend(AutomationCoordinator.Job j){
        if(AutomationCoordinator.current()!=j||j.future.isDone()||j.submitted||j.sendAttemptInProgress)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
        AccessibilityNodeInfo editor=findBestEditor(root);if(editor==null)return;
        j.sendAttemptInProgress=true;
        int stage=j.sendStage%4;
        j.sendStage++;
        j.sendAttempts++;
        boolean attempted=false;
        String detail="";

        if(stage==0){
            AccessibilityNodeInfo explicit=findExplicitSendAnyNode(root,editor,j.provider);
            if(explicit!=null){
                Rect r=new Rect();explicit.getBoundsInScreen(r);
                attempted=clickNodeOrAncestor(explicit);
                if(!attempted && !r.isEmpty())attempted=gestureTap(r.centerX(),r.centerY());
                detail="EXPLICIT_"+r.centerX()+"x"+r.centerY();
            }else detail="EXPLICIT_NOT_FOUND";
        }else if(stage==1){
            AccessibilityNodeInfo visual=findVisualSendCandidate(root,editor,j.provider);
            if(visual!=null){
                Rect r=new Rect();visual.getBoundsInScreen(r);
                attempted=!r.isEmpty()&&gestureTap(r.centerX(),r.centerY());
                detail="VISUAL_"+r.centerX()+"x"+r.centerY();
            }else detail="VISUAL_NOT_FOUND";
        }else if(stage==2){
            int[] pt=calibratedSendPoint(root,editor,j.provider,j.sendAttempts);
            attempted=gestureTap(pt[0],pt[1]);
            detail="CALIBRATED_"+pt[0]+"x"+pt[1];
        }else{
            attempted=performImeEnter(editor);
            detail="IME_ENTER";
            if(!attempted){
                int[] pt=calibratedSendPoint(root,editor,j.provider,j.sendAttempts+1);
                attempted=gestureTap(pt[0],pt[1]);
                detail="IME_THEN_TAP_"+pt[0]+"x"+pt[1];
            }
        }

        j.lastSendDetail=detail;
        j.phase="SEND_"+j.sendAttempts+"_"+detail;
        final boolean didAttempt=attempted;
        h.postDelayed(()->verifySubmitted(j,didAttempt),1350);
    }

    private void verifySubmitted(AutomationCoordinator.Job j,boolean didAttempt){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){j.sendAttemptInProgress=false;return;}
        if(isGenerationActive(root)){
            j.submitted=true;j.sendAttemptInProgress=false;j.phase="PROMPT_SENT_GENERATING";h.postDelayed(this::tick,700);return;
        }
        AccessibilityNodeInfo editor=findBestEditor(root);
        boolean promptStillPresent=editor!=null && containsMarker(editor,j.startMarker,0);
        if(didAttempt && !promptStillPresent){
            j.submitted=true;j.sendAttemptInProgress=false;j.phase="PROMPT_SENT_CONFIRMED";h.postDelayed(this::tick,700);
        }else{
            j.sendAttemptInProgress=false;
            j.phase="SEND_NOT_CONFIRMED_"+j.lastSendDetail;
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
            if(cls.contains("edittext"))score+=180;
            if(n.isFocused())score+=120;
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

    private AccessibilityNodeInfo findExplicitSendAnyNode(AccessibilityNodeInfo root,AccessibilityNodeInfo editor,String provider){
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectAll(root,all,0);
        Rect er=new Rect();editor.getBoundsInScreen(er);
        int[] target=calibratedSendPoint(root,editor,provider,0);
        AccessibilityNodeInfo best=null;double bestScore=Double.MAX_VALUE;
        for(AccessibilityNodeInfo n:all){
            Rect r=new Rect();n.getBoundsInScreen(r);if(r.isEmpty())continue;
            String m=meta(n).toLowerCase(Locale.ROOT);String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            if(isChromeAddressUi(m,id))continue;
            boolean semantic=hasAny(m,"send message","send prompt","send","submit","إرسال","ارسال","إرسال الرسالة","send arrow")||hasAny(id,"send","submit");
            if(!semantic)continue;
            int cx=r.centerX(),cy=r.centerY();
            if(cy<er.top-120||cy>er.bottom+140)continue;
            double dist=Math.hypot(cx-target[0],cy-target[1]);
            if(dist<bestScore){bestScore=dist;best=n;}
        }
        return best;
    }

    private AccessibilityNodeInfo findVisualSendCandidate(AccessibilityNodeInfo root,AccessibilityNodeInfo editor,String provider){
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectAll(root,all,0);
        Rect er=new Rect();editor.getBoundsInScreen(er);
        int[] target=calibratedSendPoint(root,editor,provider,0);
        AccessibilityNodeInfo best=null;double bestScore=Double.MAX_VALUE;
        for(AccessibilityNodeInfo n:all){
            Rect r=new Rect();n.getBoundsInScreen(r);if(r.isEmpty())continue;
            String m=meta(n).toLowerCase(Locale.ROOT);String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            if(isChromeAddressUi(m,id))continue;
            if(hasAny(m,"microphone","voice","attach","attachment","add","plus","tools","model","dropdown","camera","photo","mic","ميكروفون")||hasAny(id,"mic","attach","plus","tool","model"))continue;
            int cx=r.centerX(),cy=r.centerY();
            if(cy<er.top-100||cy>er.bottom+120)continue;
            if(r.width()>240||r.height()>240)continue;
            double dist=Math.hypot(cx-target[0],cy-target[1]);
            if(dist<bestScore){bestScore=dist;best=n;}
        }
        return bestScore<180?best:null;
    }

    private int[] calibratedSendPoint(AccessibilityNodeInfo root,AccessibilityNodeInfo editor,String provider,int attempt){
        Rect rb=new Rect();root.getBoundsInScreen(rb);Rect er=new Rect();editor.getBoundsInScreen(er);
        return SendGeometry.calibrated(rb.left,rb.top,rb.right,rb.bottom,er.bottom,provider,attempt);
    }

    private boolean clickNodeOrAncestor(AccessibilityNodeInfo n){
        AccessibilityNodeInfo cur=n;
        for(int i=0;i<6&&cur!=null;i++){
            try{if((cur.isClickable()||has(cur,AccessibilityNodeInfo.ACTION_CLICK))&&cur.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;}catch(Exception ignored){}
            cur=cur.getParent();
        }
        return false;
    }

    private boolean gestureTap(float x,float y){
        try{
            Path p=new Path();p.moveTo(x,y);
            GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(p,0,90);
            return dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
        }catch(Exception ignored){return false;}
    }

    private boolean performImeEnter(AccessibilityNodeInfo editor){
        try{
            editor.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                int id=AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId();
                if(has(editor,id) && editor.performAction(id))return true;
                if(editor.performAction(id))return true;
            }
        }catch(Exception ignored){}
        return false;
    }

    private boolean isGenerationActive(AccessibilityNodeInfo root){
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectAll(root,all,0);
        for(AccessibilityNodeInfo n:all){
            String m=meta(n).toLowerCase(Locale.ROOT);
            if(hasAny(m,"stop generating","stop response","stop responding","cancel response","إيقاف الإنشاء","إيقاف الرد","إيقاف الاستجابة"))return true;
        }
        return false;
    }

    private boolean containsMarker(AccessibilityNodeInfo n,String marker,int d){
        if(n==null||d>90)return false;
        if(n.getText()!=null&&n.getText().toString().contains(marker))return true;
        if(n.getContentDescription()!=null&&n.getContentDescription().toString().contains(marker))return true;
        for(int i=0;i<n.getChildCount();i++)if(containsMarker(n.getChild(i),marker,d+1))return true;
        return false;
    }

    private void collectAll(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int d){
        if(n==null||d>90||out.size()>6000)return;
        out.add(n);
        for(int i=0;i<n.getChildCount();i++)collectAll(n.getChild(i),out,d+1);
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
