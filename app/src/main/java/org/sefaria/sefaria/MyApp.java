package org.sefaria.sefaria;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.sefaria.sefaria.activities.SepTextActivity;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.LinkFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nss on 9/16/15.
 */
public class MyApp extends Application {


    /*@Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }*/

    public static final int DEFAULT_TEXT_SIZE = 17;

    public static final boolean DEBUGGING = false;

    public static final String[] CAT_NAMES = {"Tanakh","Mishnah","Talmud",
            "Tosefta","Liturgy","Tefillah",
            "Philosophy","Chasidut","Musar",
            "Other","Halakhah","Midrash",
            "Kabbalah","Responsa","Parshanut",
            "Apocrypha","More", LinkFilter.QUOTING_COMMENTARY,
            "Modern Works", LinkFilter.COMMENTARY, LinkFilter.ALL_CONNECTIONS,
            "Targum", "Tanach", "ERROR", "Tanaitic"
            };
    public static final int[] CAT_COLORS = {R.color.tanach, R.color.mishnah,R.color.talmud,
            R.color.tosefta,R.color.liturgy,R.color.liturgy,
            R.color.philosophy,R.color.chasidut,R.color.musar,
            R.color.system_color,R.color.halakhah,R.color.midrash,
            R.color.kabbalah,R.color.responsa,R.color.parshanut,
            R.color.apocrypha,R.color.system_color,R.color.quoting_commentary,
            R.color.modern_works,R.color.commentary, R.color.system_color,
            R.color.commentary,R.color.tanach,R.color.error, R.color.tosefta
            };

    public enum Font {
        MONTSERRAT,TAAMEY_FRANK,OPEN_SANS_EN,OPEN_SANS_HE,GARAMOND,NEW_ATHENA,CRIMSON,QUATTROCENTO,CARDO
    }

    private static Typeface montserrat_tf;
    private static Typeface taamey_frank_tf;
    private static Typeface open_sans_en_tf;
    private static Typeface open_sans_he_tf;
    private static Typeface garamond_tf;
    private static Typeface new_athena_tf;
    private static Typeface crimson_tf;
    private static Typeface quattrocento_tf;
    private static Typeface cardo_tf;

    private static Context context;
    public static final String APP_NAME = "Sefaria";//TODO get rid of variable
    public static final int REQUEST_WRITE_STORAGE = 112;


    public static final int KILL_SWITCH_NUM = -247;
    private static String appPackageName = "org.sefaria.sefaria";
    public static boolean askedForUpgradeThisTime = false;
    public static boolean isFirstTimeOpened = false;
    //public static int MIN_WORKING_DB_VERSION = 1; //this doesn't yet need to be made, b/c we don't have a db that will break it...


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        appPackageName = context.getPackageName();

        initFonts();
        GoogleTracker googleTracker = new GoogleTracker();
    }

    public static int getVersionCode(){
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(appPackageName, 0);
            //String versionName = pinfo.versionName;
            return pinfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }


    private static void initFonts() {
        montserrat_tf = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.otf");
        taamey_frank_tf = Typeface.createFromAsset(context.getAssets(), "fonts/TaameyFrankCLM-Medium.ttf");
        open_sans_en_tf = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSans-Regular.ttf");
        open_sans_he_tf = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSansHebrew-Regular.ttf");
        garamond_tf = Typeface.createFromAsset(context.getAssets(), "fonts/EBGaramond12-Regular.ttf");
        new_athena_tf = Typeface.createFromAsset(context.getAssets(), "fonts/new_athena_unicode.ttf");
        crimson_tf = Typeface.createFromAsset(context.getAssets(), "fonts/CrimsonText-Regular.ttf");
        quattrocento_tf = Typeface.createFromAsset(context.getAssets(), "fonts/Quattrocento-Regular.otf");
        cardo_tf = Typeface.createFromAsset(context.getAssets(),"fonts/Cardo-Regular.ttf");
    }

    public static Typeface getFont(Font font) {
        switch (font) {
            case MONTSERRAT:
                return montserrat_tf;
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
            case CARDO:
                return cardo_tf;
        }
        return null;
    }

    public static Point getScreenSizePixels(){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        //int width = size.x;
        //int height = size.y;
        return size;
    }

    public static Point getScreenSize(){
        Configuration configuration;
        try{
            configuration = getContext().getResources().getConfiguration();
        }catch (NullPointerException e){
            GoogleTracker.sendException(e, "getScreenSize");
            return new Point(300, 600);
        }

        Point size = new Point(configuration.screenWidthDp, configuration.screenHeightDp);
        //Log.d("screenSize","X:"+ size.x);
        return size;
    }



    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }



    public static int getCatColor(String catName) {
        int color;
        int homeInd = Arrays.asList(CAT_NAMES).indexOf(catName);
        if (homeInd != -1) color = CAT_COLORS[homeInd];
        else color = -1; //default

        return color;
    }

    public static boolean validSDKVersion(int requiredVersion){
        return (Build.VERSION.SDK_INT >= requiredVersion);
    }

    public static String getEmailHeader(){
        return  "App Version: " + BuildConfig.VERSION_NAME + " ("  + BuildConfig.VERSION_CODE + ")" + "\n"
                + "Online Mode Version: " + Util.convertDBnum(Database.getVersionInDB(true)) + "\n"
                + "Offline Library Version: " + Util.convertDBnum(Database.getVersionInDB(false)) + "\n"
                + "Using " + (Settings.getUseAPI()? "Online Mode":"Offline Library") + "\n"
                + GoogleTracker.randomID + "\n"
                + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")" + "\n"
                +"\n\n\n";
    }

    public static String getRString(int R_string){
        return getContext().getString(R_string);
    }
    public static float getRDimen(int R_dimen) { return getContext().getResources().getDimension(R_dimen); }

    public static Context getContext() { return context; }
    public static void setContext(Context newContext) { context = newContext; }
    public static String getAppPackageName() { return appPackageName; }

    public static void killSwitch(){return; }//TODO remove function

    /*
    public static void homeClick(Activity activity, boolean openNewTab,boolean hideOpening){
        Intent intent = new Intent(activity, HomeActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Clear Activity stack and put the Home screen as the only activity
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //bring the old Home menu to the front

        //intent.putExtra("homeClicked",true);
        intent.putExtra("isPopup",!openNewTab);
        intent.putExtra("hideOpening",hideOpening);

        if (openNewTab) {
            intent = startNewTab(intent);
            Bundle activityOptionsBundle = ActivityOptionsCompat.makeCustomAnimation(context,R.animator.slide_up,R.animator.stay).toBundle();
            ActivityCompat.startActivity(activity, intent, activityOptionsBundle);
        } else {
            activity.startActivity(intent);
            activity.overridePendingTransition(R.animator.slide_right, R.animator.stay);
        }
    }*/

    public static Intent startNewTab(Intent intent){
        Toast.makeText(context, context.getString(R.string.opening_new_task), Toast.LENGTH_SHORT).show();
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static void openWithInstalledAppExceptCurrentApp(final Activity activity, String url) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        //i.setData(Uri.parse(url));
        Toast.makeText(activity, "Unable to open with Sefaria App. Open in Browser.", Toast.LENGTH_SHORT).show();
        i.putExtra(Intent.EXTRA_SUBJECT, "Open in Browser");
        i.putExtra(Intent.EXTRA_TEXT, url);
        activity.startActivity(Intent.createChooser(i, "Open in Browser"));

        if(true) return;


        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);




        intent.setData(Uri.parse(url));
        intent.setType("text/plain");
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            Log.d("packages", packageName);
            if (!appPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(Intent.ACTION_VIEW);
                targetIntent.setData(Uri.parse(url));
                targetIntent.setPackage(packageName);
                targetIntents.add(targetIntent);
            }
        }
        if(targetIntents.size() > 0) {
            Intent chooserIntent = Intent.createChooser(targetIntents.remove(0), "Open with");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[] {}));
            activity.startActivity(chooserIntent);
        }
        else {
            Toast.makeText(activity, "No app found", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openURLInBrowser(Activity activity, String url){
        if (!url.startsWith("https://") && !url.startsWith("http://")){
            url = "https://" + url;
        }
        openWithInstalledAppExceptCurrentApp(activity, url);
    }

    public static boolean handleIncomingURL(Activity activity, Intent intent){
        try {
            if (intent != null){
                String url = intent.getStringExtra("url");
                if(url != null || intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)){ //&&  intent.getCategories().contains(Intent.CATEGORY_BROWSABLE
                    //url to go to  www.sefaria.org
                    if(url == null) {
                        url = intent.getDataString(); //got here from web click
                    }// otherwise got here from shortcut
                    Log.d("HomeActivity", "Sefaria URL:" + url);
                    GoogleTracker.sendEvent(GoogleTracker.CATEGORY_OPENED_URL, url);
                    String place = url.replaceAll("(?i).*sefaria\\.org/?(s2/)?", "");
                    try {
                        API.PlaceRef placeRef = API.PlaceRef.getPlace(place,null);
                        SuperTextActivity.startNewTextActivityIntent(activity, placeRef.book, placeRef.segment, placeRef.node, true, null,-1);
                    }catch (API.APIException e){
                        openURLInBrowser(activity,url);
                    }catch (Book.BookNotFoundException  e2){
                        openURLInBrowser(activity,url);
                    }
                    activity.finish();
                    return true;
                }
            }
        }catch (Exception e){
            //Toast.makeText(activity, MyApp.getRString(R.string.cannot_parse_link), Toast.LENGTH_SHORT).show();
            Log.e("HomeActivity","not able to open intent for URL parse " + e.getMessage());
        }
        return false;
    }

    public static void restart(){
        Intent mStartActivity = new Intent(MyApp.getContext(), SepTextActivity.class);
        int mPendingIntentId = 31415;
        PendingIntent mPendingIntent = PendingIntent.getActivity(MyApp.getContext(), mPendingIntentId,  mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)MyApp.getContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }




}
