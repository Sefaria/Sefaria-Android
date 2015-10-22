package org.sefaria.sefaria;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by nss on 9/16/15.
 */
public class MyApp extends Application {

    public static final int MONTSERRAT_FONT = 123;
    public static final int TAAMEY_FRANK_FONT = 124;

    private static Typeface monserrat_tf;
    private static Typeface taamey_frank_tf;

    private static Context context;
    private static String appPackageName;



    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        appPackageName = context.getPackageName();

        initFonts();
    }

    private static void initFonts() {
        monserrat_tf = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.otf");
        taamey_frank_tf = Typeface.createFromAsset(context.getAssets(), "fonts/TaameyFrankCLM-Medium.ttf");
    }

    public static Typeface getFont(int which) {
        switch ( which) {
            case MONTSERRAT_FONT:
                return monserrat_tf;
            case TAAMEY_FRANK_FONT:
                return taamey_frank_tf;
        }
        return null;
    }

    public static Context getContext() { return context; }
    public static String getAppPackageName() { return appPackageName; }
}
