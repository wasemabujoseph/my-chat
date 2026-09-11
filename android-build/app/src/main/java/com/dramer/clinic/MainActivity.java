package com.dramer.clinic;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.UUID;

public class MainActivity extends Activity {
    private TextView status,result;
    private EditText input;
    private Spinner provider;
    private final Handler uiHandler=new Handler(Looper.getMainLooper());
    private final Runnable statusPoller=new Runnable(){@Override public void run(){refreshStatus();uiHandler.postDelayed(this,500);}};
    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();handleIntent(getIntent());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    @Override protected void onResume(){super.onResume();uiHandler.removeCallbacks(statusPoller);uiHandler.post(statusPoller);}
    @Override protected void onPause(){uiHandler.removeCallbacks(statusPoller);super.onPause();}
    private void refreshStatus(){
        if(status==null)return;
        if(!AutomationCoordinator.isEnabled(this)){status.setText("Accessibility: DISABLED");return;}
        AutomationCoordinator.Job j=AutomationCoordinator.current();
        if(j!=null&&!j.future.isDone()){
            String d=j.lastSendDetail==null||j.lastSendDetail.isEmpty()?"":"\nSend: "+j.lastSendDetail;
            status.setText("Automation: "+j.phase+d);
        } else status.setText("Accessibility: ENABLED");
    }
    private TextView tv(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.rgb(20,45,52));v.setPadding(8,8,8,8);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    private void buildUi(){
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,28,28,28);sc.addView(root);
        TextView title=tv("✚ Dr Amer Fowzn Khallaf Clinic",24);title.setTypeface(null,1);root.addView(title);
        root.addView(tv("Android v5.3 Verified Send Test",15));
        status=tv("Accessibility: CHECK",14);root.addView(status);
        Button access=btn("1 · تفعيل خدمة الأتمتة");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(access);
        Button login=btn("2 · فتح ChatGPT / Gemini للتسجيل");login.setOnClickListener(v->AutomationCoordinator.openProvider(this,provider.getSelectedItem().toString()));root.addView(login);
        provider=new Spinner(this);String[] p={"chatgpt","gemini"};provider.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,p));root.addView(provider);
        input=new EditText(this);input.setHint("اكتب حالة تجريبية هنا");input.setMinLines(7);input.setGravity(Gravity.TOP|Gravity.RIGHT);root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        Button analyze=btn("Analyze · إرسال تلقائي إلى Chrome");analyze.setOnClickListener(v->runAnalysis());root.addView(analyze);
        Button reset=btn("إلغاء التحليل المعلّق / Reset");reset.setOnClickListener(v->{AutomationCoordinator.cancelCurrent();result.setText("تمت إعادة الضبط");refreshStatus();});root.addView(reset);
        root.addView(tv("Captured JSON",17));result=tv("لا توجد نتيجة بعد",12);result.setTextIsSelectable(true);root.addView(result);
        root.addView(tv("v5.3 يجرب: زر Accessibility → clickable parent → tap فعلي لموضع السهم → IME Enter، ولا يقبل النجاح قبل تأكيد الإرسال.",11));setContentView(sc);
    }
    private void runAnalysis(){
        String history=input.getText().toString().trim();if(history.isEmpty()){Toast.makeText(this,"اكتب الحالة أولاً",Toast.LENGTH_SHORT).show();return;}
        if(!AutomationCoordinator.isEnabled(this)){result.setText("فعّل خدمة Accessibility الخاصة بالتطبيق أولاً");refreshStatus();return;}
        String rid="A5-"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        String startMarker="CLINIC_RESULT_START_"+rid;
        String endMarker="CLINIC_RESULT_END_"+rid;
        String prompt="Dr Amer Clinic Android automation test. Analyze this case conservatively as clinician decision support. Your response MUST start with the exact line "+startMarker+" and MUST end with the exact line "+endMarker+". Between those two marker lines output exactly one valid JSON object and no markdown fences. The JSON must contain request_id with the exact value "+rid+", working_diagnosis as a string, red_flags as an array of strings, next_steps as an array of strings, medications as an array of objects with generic, dose, route, frequency, duration, and safety_alerts as an array of strings. Do not invent missing patient facts. CASE:\n"+history;
        try{
            result.setText("جاري فتح "+provider.getSelectedItem().toString()+" وإرسال الحالة تلقائيًا...");
            AutomationCoordinator.Job j=AutomationCoordinator.start(this,provider.getSelectedItem().toString(),prompt,rid,startMarker,endMarker);
            status.setText("Automation: OPENING");
            j.future.whenComplete((json,e)->runOnUiThread(()->{
                if(e!=null){status.setText("Automation: ERROR");result.setText(String.valueOf(e.getMessage())+"\nLast send: "+j.lastSendDetail);}
                else{status.setText("Automation: CAPTURED");result.setText(json);}
            }));
        }catch(Exception e){status.setText("Automation: ERROR");result.setText(e.getMessage());}
    }
    private void handleIntent(Intent i){if(i!=null&&i.hasExtra("captured_json")&&result!=null){result.setText(i.getStringExtra("captured_json"));status.setText("Automation: CAPTURED");}}
}
