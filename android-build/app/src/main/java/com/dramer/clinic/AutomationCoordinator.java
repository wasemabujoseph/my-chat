package com.dramer.clinic;

import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import java.util.concurrent.*;

public final class AutomationCoordinator {
    public static final class Job {
        final String provider, prompt, requestId, startMarker, endMarker;
        final long createdAt=System.currentTimeMillis();
        final CompletableFuture<String> future = new CompletableFuture<>();
        volatile boolean submitted=false;
        volatile boolean sendAttemptInProgress=false;
        volatile int sendAttempts=0;
        volatile int sendStage=0;
        volatile String phase="CREATED";
        volatile String lastSendDetail="";
        Job(String provider,String prompt,String requestId,String startMarker,String endMarker){
            this.provider=provider;this.prompt=prompt;this.requestId=requestId;this.startMarker=startMarker;this.endMarker=endMarker;
        }
    }
    private static volatile Job current;
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor();
    private AutomationCoordinator(){}
    public static synchronized Job current(){return current;}
    public static synchronized Job start(Context c,String provider,String prompt,String requestId,String startMarker,String endMarker){
        if(!isEnabled(c)) throw new IllegalStateException("فعّل خدمة Accessibility الخاصة بالتطبيق أولاً");
        if(current!=null && !current.future.isDone()) current.future.completeExceptionally(new CancellationException("تم إلغاء التحليل السابق وبدء تحليل جديد"));
        Job j=new Job(provider,prompt,requestId,startMarker,endMarker); current=j;
        TIMER.schedule(() -> {
            if(!j.future.isDone()){
                j.phase="TIMEOUT_"+j.lastSendDetail;
                j.future.completeExceptionally(new TimeoutException("لم يتم إرسال/التقاط جواب AI خلال 150 ثانية. المرحلة الأخيرة: "+j.phase));
            }
        },150,TimeUnit.SECONDS);
        openProvider(c,provider);
        j.phase="OPENING_"+provider.toUpperCase();
        return j;
    }
    public static synchronized void cancelCurrent(){
        if(current!=null && !current.future.isDone()) current.future.completeExceptionally(new CancellationException("تم إلغاء التحليل"));
        current=null;
    }
    public static synchronized void complete(Job j,String json){
        if(current==j && !j.future.isDone()){
            j.phase="CAPTURED";
            j.future.complete(json);
        }
    }
    public static boolean isEnabled(Context c){
        ComponentName cn=new ComponentName(c,ClinicAccessibilityService.class);
        String enabled=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null)return false;
        for(String s:enabled.split(":")) if(s.equalsIgnoreCase(cn.flattenToString())||s.equalsIgnoreCase(cn.flattenToShortString())) return true;
        return false;
    }
    public static void openProvider(Context c,String provider){
        String url="gemini".equalsIgnoreCase(provider)?"https://gemini.google.com/app":"https://chatgpt.com/";
        Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url)); i.setPackage("com.android.chrome"); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try{c.startActivity(i);}catch(Exception e){Intent f=new Intent(Intent.ACTION_VIEW,Uri.parse(url));f.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(f);}
    }
}
