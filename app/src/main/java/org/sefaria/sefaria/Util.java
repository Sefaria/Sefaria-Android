package org.sefaria.sefaria;

import android.content.Context;

/**
 * Created by nss on 9/8/15.
 */
public class Util {

    public static final int EN = 0;
    public static final int HE = 1;
    public static final int BI = 2;

    public static final float EN_HE_RATIO = 40f/35f; //universal constant

    public static int dp2pixel(int dp, Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp*scale + 0.5f);
    }
}
