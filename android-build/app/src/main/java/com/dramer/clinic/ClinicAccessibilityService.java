package com.dramer.clinic;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
        if(!j.submitted && now-lastInject>1000){lastInject=now;inject(j,root);}
        else if(j.submitted && now-lastCapture>900){lastCapture=now;capture(j,root);}
    }

    private void inject(AutomationCoordinator.Job j,AccessibilityNodeInfo root){
        if(AutomationCoordinator.current()!=j||j.future.isDone())return;
        AccessibilityNodeInfo editor=findBestEditor(root);
        if(editor==null){j.phase="WAITING_FOR_AI_COMPOSER";return;}
        j.phase="COMPOSER_FOUND";
        boolean ok=setEditorText(editor,j.prompt);
        if(!ok){j.phase="COMPOSER_SET_TEXT_FAILED";return;}
        j.phase="PROMPT_INSERTED";
        h.postDelayed(()->{
            if(AutomationCoordinator.current()!=j||j.future.isDone())return;
            AccessibilityNodeInfo r=getRootInActiveWindow();if(r==null)return;
            AccessibilityNodeInfo send=findBestSend(r);
            if(send!=null && send.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                j.submitted=true;j.phase="PROMPT_SENT";h.postDelayed(()->tick(),1200);
            }else{
                j.phase="WAITING_FOR_SEND_BUTTON";
            }
        },900);
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

    private AccessibilityNodeInfo findBestSend(AccessibilityNodeInfo root){
        Rect rb=new Rect();root.getBoundsInScreen(rb);int screenBottom=Math.max(rb.bottom,1);
        ArrayList<AccessibilityNodeInfo> all=new ArrayList<>();collectClickable(root,all,0);
        AccessibilityNodeInfo best=null;int bestScore=Integer.MIN_VALUE;
        for(AccessibilityNodeInfo n:all){
            String m=meta(n).toLowerCase(Locale.ROOT);String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            Rect r=new Rect();n.getBoundsInScreen(r);int cy=(r.top+r.bottom)/2;
            if(cy < screenBottom*0.25)continue;
            int score=cy/4;
            if(hasAny(m,"send message","send","submit","إرسال","ارسال","إرسال الرسالة"))score+=1600;
            if(hasAny(id,"send","submit"))score+=1400;
            if(score>bestScore && (hasAny(m,"send","submit","إرسال","ارسال")||hasAny(id,"send","submit"))){bestScore=score;best=n;}
        }
        return best;
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
