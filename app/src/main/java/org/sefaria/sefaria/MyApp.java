package org.sefaria.sefaria;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.widget.Toast;

import org.sefaria.sefaria.activities.HomeActivity;
import org.sefaria.sefaria.database.LinkFilter;

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
            "Apocrypha","More >", LinkFilter.QUOTING_COMMENTARY,
            "Modern Works", LinkFilter.COMMENTARY, LinkFilter.ALL_CONNECTIONS
            };
    public static final int[] CAT_COLORS = {R.color.tanach, R.color.mishnah,R.color.talmud,
            R.color.tosefta,R.color.liturgy,R.color.liturgy,
            R.color.philosophy,R.color.chasidut,R.color.musar,
            R.color.system_color,R.color.halkhah,R.color.midrash,
            R.color.kabbalah,R.color.responsa,R.color.parshanut,
            R.color.apocrypha,R.color.system_color,R.color.quoting_commentary,
            R.color.modern_works,R.color.commentary, R.color.system_color};

    public enum Font {
        MONTSERRAT,TAAMEY_FRANK,OPEN_SANS_EN,OPEN_SANS_HE,GARAMOND,NEW_ATHENA,CRIMSON,QUATTROCENTO
    }

    private static Typeface monserrat_tf;
    private static Typeface taamey_frank_tf;
    private static Typeface open_sans_en_tf;
    private static Typeface open_sans_he_tf;
    private static Typeface garamond_tf;
    private static Typeface new_athena_tf;
    private static Typeface crimson_tf;
    private static Typeface quattrocento_tf;

    private static Context context;
    public static Activity currActivityContext;
    public static final String APP_NAME = "Sefaria";//TODO get rid of variable
    public static final int REQUEST_WRITE_STORAGE = 112;

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
        open_sans_en_tf = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSans-Regular.ttf");
        open_sans_he_tf = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSansHebrew-Regular.ttf");
        garamond_tf = Typeface.createFromAsset(context.getAssets(), "fonts/EBGaramond12-Regular.ttf");
        new_athena_tf = Typeface.createFromAsset(context.getAssets(), "fonts/new_athena_unicode.ttf");
        crimson_tf = Typeface.createFromAsset(context.getAssets(), "fonts/CrimsonText-Regular.ttf");
        quattrocento_tf = Typeface.createFromAsset(context.getAssets(), "fonts/Quattrocento-Regular.otf");
    }

    public static Typeface getFont(Font font) {
        switch (font) {
            case MONTSERRAT:
                return monserrat_tf;
            case TAAMEY_FRANK:
                return taamey_frank_tf;
            case OPEN_SANS_EN:
                return open_sans_en_tf;
            case OPEN_SANS_HE:
                return open_sans_he_tf;
            case GARAMOND:
                return garamond_tf;
            case NEW_ATHENA:
                return new_athena_tf;
            case CRIMSON:
                return crimson_tf;
            case QUATTROCENTO:
                return quattrocento_tf;
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

    public static void homeClick(Activity activity, boolean openNewTab){
        Intent intent = new Intent(activity, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Clear Activity stack
        if(openNewTab)
            intent = startNewTab(intent);
        intent.putExtra("homeClicked",true);
        activity.startActivity(intent);
        //TODO if(openNewTab) start animation
    }

    public static Intent startNewTab(Intent intent){
        Toast.makeText(context, context.getString(R.string.opening_new_task), Toast.LENGTH_SHORT).show();
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        //ActivityCompat.startActivity(this, intent, options)
        return intent;
    }

}
