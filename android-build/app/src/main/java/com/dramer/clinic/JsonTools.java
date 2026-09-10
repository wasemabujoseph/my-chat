package com.dramer.clinic;

public final class JsonTools {
    private JsonTools(){}
    public static String extractMarkedJson(String text){
        if(text==null)return null;
        String a="CLINIC_JSON_START", b="CLINIC_JSON_END";
        int end=text.lastIndexOf(b); if(end<0)return null;
        int start=text.lastIndexOf(a,end); if(start<0)return null;
        String json=text.substring(start+a.length(),end).trim();
        return json.startsWith("{")&&json.endsWith("}")?json:null;
    }
}
