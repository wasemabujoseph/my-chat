package com.dramer.clinic;

public final class JsonTools {
    private JsonTools(){}
    public static String extractMarkedJson(String text,String startMarker,String endMarker){
        if(text==null||startMarker==null||endMarker==null)return null;
        int end=text.lastIndexOf(endMarker); if(end<0)return null;
        int start=text.lastIndexOf(startMarker,end); if(start<0)return null;
        String json=text.substring(start+startMarker.length(),end).trim();
        return json.startsWith("{")&&json.endsWith("}")?json:null;
    }
}
