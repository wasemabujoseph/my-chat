package com.dramer.clinic;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.UUID;

public class MainActivity extends Activity {
    private TextView status,result;
    private EditText input;
    private Spinner provider;
    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();handleIntent(getIntent());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    @Override protected void onResume(){super.onResume();if(status!=null)status.setText("Accessibility: "+(AutomationCoordinator.isEnabled(this)?"ENABLED":"DISABLED"));}
    private TextView tv(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.rgb(20,45,52));v.setPadding(8,8,8,8);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    private void buildUi(){
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,28,28,28);sc.addView(root);
        TextView title=tv("✚ Dr Amer Fowzn Khallaf Clinic",24);title.setTypeface(null,1);root.addView(title);
        root.addView(tv("Android v5.0 Standalone Automation Test",15));
        status=tv("Accessibility: CHECK",14);root.addView(status);
        Button access=btn("1 · تفعيل خدمة الأتمتة");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(access);
        Button login=btn("2 · فتح ChatGPT / Gemini للتسجيل");login.setOnClickListener(v->AutomationCoordinator.openProvider(this,provider.getSelectedItem().toString()));root.addView(login);
        provider=new Spinner(this);String[] p={"chatgpt","gemini"};provider.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,p));root.addView(provider);
        input=new EditText(this);input.setHint("اكتب حالة تجريبية هنا");input.setMinLines(7);input.setGravity(Gravity.TOP|Gravity.RIGHT);root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        Button analyze=btn("Analyze · إرسال إلى Chrome");analyze.setOnClickListener(v->runAnalysis());root.addView(analyze);
        root.addView(tv("Captured JSON",17));result=tv("لا توجد نتيجة بعد",12);result.setTextIsSelectable(true);root.addView(result);
        root.addView(tv("هذه نسخة اختبار هندسية للتحقق من أتمتة المتصفح فقط. راجع كل نتيجة طبيًا قبل استخدامها.",11));setContentView(sc);
    }
    private void runAnalysis(){
        String history=input.getText().toString().trim();if(history.isEmpty()){Toast.makeText(this,"اكتب الحالة أولاً",Toast.LENGTH_SHORT).show();return;}
        String rid="A5-"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        String prompt="Dr Amer Clinic Android v5 test. Return ONLY marked valid JSON.\nCLINIC_JSON_START\n{\"request_id\":\""+rid+"\",\"working_diagnosis\":\"string\",\"red_flags\":[\"string\"],\"next_steps\":[\"string\"],\"medications\":[{\"generic\":\"string\",\"dose\":\"string|null\",\"route\":\"string|null\",\"frequency\":\"string|null\",\"duration\":\"string|null\"}],\"safety_alerts\":[\"string\"]}\nCLINIC_JSON_END\nReplace placeholders with case-specific content and keep request_id exactly "+rid+". This is clinician decision support; do not invent missing patient facts. CASE:\n"+history;
        try{AutomationCoordinator.Job j=AutomationCoordinator.start(this,provider.getSelectedItem().toString(),prompt);status.setText("Automation: RUNNING");j.future.whenComplete((json,e)->runOnUiThread(()->{if(e!=null){status.setText("Automation: ERROR");result.setText(e.getMessage());}else{status.setText("Automation: CAPTURED");result.setText(json);}}));}catch(Exception e){status.setText("Automation: ERROR");result.setText(e.getMessage());}
    }
    private void handleIntent(Intent i){if(i!=null&&i.hasExtra("captured_json")&&result!=null){result.setText(i.getStringExtra("captured_json"));status.setText("Automation: CAPTURED");}}
}
