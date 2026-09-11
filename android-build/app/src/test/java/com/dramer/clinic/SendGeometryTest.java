package com.dramer.clinic;

import org.junit.Test;
import static org.junit.Assert.*;

public class SendGeometryTest {
    @Test public void chatGptPointMatchesSamsungScreenshotRegion(){
        int[] p=SendGeometry.calibrated(0,0,710,1536,1402,"chatgpt",4);
        assertTrue("ChatGPT send x",p[0]>=610 && p[0]<=650);
        assertTrue("ChatGPT send y",p[1]>=1335 && p[1]<=1375);
    }
    @Test public void geminiPointMatchesSamsungScreenshotRegion(){
        int[] p=SendGeometry.calibrated(0,0,710,1536,1425,"gemini",4);
        assertTrue("Gemini send x",p[0]>=88 && p[0]<=120);
        assertTrue("Gemini send y",p[1]>=1335 && p[1]<=1375);
    }
    @Test public void pointsStayOnCorrectSidesAcrossCommonPhoneWidth(){
        int[] c=SendGeometry.calibrated(0,0,1080,2400,2200,"chatgpt",4);
        int[] g=SendGeometry.calibrated(0,0,1080,2400,2200,"gemini",4);
        assertTrue(c[0]>810);
        assertTrue(g[0]<270);
    }
}
