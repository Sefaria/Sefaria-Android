package org.sefaria.sefaria;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

import java.util.Arrays;

/**
 * Created by nss on 9/16/15.
 */
public class MyApp extends Application {

    public static final String[] CAT_NAMES = {"Tanach","Mishnah","Talmud",
            "Tosefta","Liturgy","Tefillah",
            "Philosophy","Chasidut","Musar",
            "Other","Halakhah","Midrash",
            "Kabbalah","Responsa","Parshanut",
            "Apocrypha","More >","Quoting Commentary",
            "Modern Works","Commentary","All Connections"
            };
    public static final int[] CAT_COLORS = {R.color.tanach, R.color.mishnah,R.color.talmud,
            R.color.tosefta,R.color.liturgy,R.color.liturgy,
            R.color.philosophy,R.color.chasidut,R.color.musar,
            R.color.system_color,R.color.halkhah,R.color.midrash,
            R.color.kabbalah,R.color.responsa,R.color.parshanut,
            R.color.apocrypha,R.color.system_color,R.color.quoting_commentary,
            R.color.modern_works,R.color.commentary, R.color.system_color};

    public static final int MONTSERRAT_FONT = 123;
    public static final int TAAMEY_FRANK_FONT = 124;

    private static Typeface monserrat_tf;
    private static Typeface taamey_frank_tf;


    private static Context context;
    public static Activity currActivityContext;
    public static final String APP_NAME = "Sefaria";//TODO get rid of variable

    public static final int KILL_SWITCH_NUM = -247;
    private static String appPackageName = "org.sefaria.sefaria";
    public static boolean askedForUpgradeThisTime = false;
    //public static int MIN_WORKING_DB_VERSION = 1; //this doesn't yet need to be made, b/c we don't have a db that will break it...


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        appPackageName = context.getPackageName();

        initFonts();
        GoogleTracker googleTracker = new GoogleTracker();
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

    public static int getCatColor(String catName) {
        int color;
        int homeInd = Arrays.asList(CAT_NAMES).indexOf(catName);
        if (homeInd != -1) color = CAT_COLORS[homeInd];
        else color = -1;

        return color;
    }


    public static Context getContext() { return context; }
    public static String getAppPackageName() { return appPackageName; }

    public static void killSwitch(){return; }//TODO remove function


}
