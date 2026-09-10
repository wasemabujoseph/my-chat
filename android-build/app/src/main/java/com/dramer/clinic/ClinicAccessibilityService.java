package com.dramer.clinic;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.*;
import android.view.accessibility.*;
import java.util.*;

public class ClinicAccessibilityService extends AccessibilityService {
    private final Handler h=new Handler(Looper.getMainLooper());
    private long lastInject=0,lastCapture=0;
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        CharSequence pkg=event.getPackageName(); if(pkg==null||!pkg.toString().contains("chrome"))return;
        AutomationCoordinator.Job j=AutomationCoordinator.current(); if(j==null||j.future.isDone())return;
        long now=System.currentTimeMillis();
        if(!j.submitted&&now-lastInject>900){lastInject=now;h.postDelayed(()->inject(j),500);}
        if(j.submitted&&now-lastCapture>700){lastCapture=now;h.postDelayed(()->capture(j),500);}
    }
    private void inject(AutomationCoordinator.Job j){
        AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return;
        AccessibilityNodeInfo editor=findEditor(root); if(editor==null)return;
        Bundle b=new Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,j.prompt);
        boolean ok=editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b); if(!ok)return;
        h.postDelayed(()->{AccessibilityNodeInfo r=getRootInActiveWindow();AccessibilityNodeInfo send=findSend(r);if(send!=null&&send.performAction(AccessibilityNodeInfo.ACTION_CLICK)){j.submitted=true;h.postDelayed(()->capture(j),1500);}},700);
    }
    private AccessibilityNodeInfo findEditor(AccessibilityNodeInfo n){
        if(n==null)return null; String t=text(n).toLowerCase(Locale.ROOT); String id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
        if((n.isEditable()||has(n,AccessibilityNodeInfo.ACTION_SET_TEXT))&&!t.contains("search"))return n;
        if((id.contains("prompt")||id.contains("composer"))&&has(n,AccessibilityNodeInfo.ACTION_SET_TEXT))return n;
        for(int i=n.getChildCount()-1;i>=0;i--){AccessibilityNodeInfo x=findEditor(n.getChild(i));if(x!=null)return x;}return null;
    }
    private AccessibilityNodeInfo findSend(AccessibilityNodeInfo n){
        if(n==null)return null; String t=text(n).toLowerCase(Locale.ROOT),id=String.valueOf(n.getViewIdResourceName()).toLowerCase(Locale.ROOT);
        if((t.equals("send")||t.contains("send prompt")||t.contains("إرسال")||id.contains("send")||id.contains("submit"))&&(n.isClickable()||has(n,AccessibilityNodeInfo.ACTION_CLICK)))return n;
        for(int i=n.getChildCount()-1;i>=0;i--){AccessibilityNodeInfo x=findSend(n.getChild(i));if(x!=null)return x;}return null;
    }
    private void capture(AutomationCoordinator.Job j){
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;StringBuilder sb=new StringBuilder();collect(root,sb,0);
        String json=JsonTools.extractMarkedJson(sb.toString());if(json==null)return;
        try{new org.json.JSONObject(json);AutomationCoordinator.complete(j,json);Intent i=new Intent(this,MainActivity.class);i.putExtra("captured_json",json);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);}catch(Exception ignored){}
    }
    private void collect(AccessibilityNodeInfo n,StringBuilder sb,int d){if(n==null||d>80||sb.length()>250000)return;if(n.getText()!=null)sb.append('\n').append(n.getText());if(n.getContentDescription()!=null)sb.append('\n').append(n.getContentDescription());for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),sb,d+1);}
    private String text(AccessibilityNodeInfo n){return String.valueOf(n.getText())+" "+String.valueOf(n.getContentDescription());}
    private boolean has(AccessibilityNodeInfo n,int id){for(AccessibilityNodeInfo.AccessibilityAction a:n.getActionList())if(a.getId()==id)return true;return false;}
    @Override public void onInterrupt(){}
}
