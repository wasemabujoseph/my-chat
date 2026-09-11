package com.dramer.clinic;

public final class SendGeometry {
    private SendGeometry(){}
    public static int[] calibrated(int rootLeft,int rootTop,int rootRight,int rootBottom,int editorBottom,String provider,int attempt){
        int width=Math.max(rootRight-rootLeft,1),height=Math.max(rootBottom-rootTop,1);
        boolean gemini="gemini".equalsIgnoreCase(provider);
        int x=(int)(rootLeft+(gemini?0.145:0.890)*width);
        int y=editorBottom-(gemini?70:46);
        int minY=rootTop+(int)(height*0.62), maxY=rootBottom-70;
        y=Math.max(minY,Math.min(maxY,y));
        int variation=((attempt/4)%3)-1;
        x+=variation*16;
        if(((attempt/8)&1)==1)y-=14;
        return new int[]{x,y};
    }
}
