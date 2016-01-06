package org.sefaria.sefaria;

import android.app.Activity;
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

    //private static final String GOOGLE_AN_ID = "UA-59002633-2"; ///THIS IS IS ONLY FOR TESTING!!!
    private static final String GOOGLE_AN_ID = "UA-60342564-2"; //OLD: "UA-60342564-1"; //new: -2
    private static Context context;
    public static Activity currActivityContext;
    public static final String APP_NAME = "Sefaria";//TODO get rid of variable
    public static final String CATEGORY_NEW_TEXT = "Opened Text Page";
    public static final String BUTTON_PRESS = "Button Press";
    public static final String SETTING_CHANGE = "Setting Change";
    public static String randomID = null;
    public static final int KILL_SWITCH_NUM = -247;
    private static String appPackageName = "org.sefaria.sefaria";
    public static String INTERNAL_FOLDER = "/data/data/" + getAppPackageName() + "/";
    public static boolean askedForUpgradeThisTime = false;
    //public static int MIN_WORKING_DB_VERSION = 1; //this doesn't yet need to be made, b/c we don't have a db that will break it...


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        appPackageName = context.getPackageName();

        initFonts();
        getTracker();
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


    /**
     * returns the default lang that should be used different types of things
     * //TODO make this changable in some user level thing and save them in settings.
     */
    public static Util.Lang getDefaultLang(Util.SETTING_LANG_TYPE setting_lang_type){

        if(setting_lang_type == Util.SETTING_LANG_TYPE.TEXTS){
            return Util.Lang.HE; //TODO change to BI
        }else if(setting_lang_type == Util.SETTING_LANG_TYPE.MENU){
            return Util.Lang.EN;
        }
        else {//if(setting_lang_type == Util.SETTING_LANG_TYPE.SYSTEM) {
            return Util.Lang.EN;//TODO check if it's a hebrew phone
        }
    }

    public static Context getContext() { return context; }
    public static String getAppPackageName() { return appPackageName; }

    public static void killSwitch(){return; }//TODO remove function


	///*
	////////////////////////////////////////////////////////////////////////////////////
	//THESE FAKE FUNCTIONS SHOULD BE UNCOMMENETED WHEN NOT USING ANYLITICS
	public static void setTrackerID(){}
	synchronized void getTracker() {}
	static public void sendEvent(String cat, String act, long value){}
	static public void sendEvent(String cat, String act){}
	static public void sendScreen(String screenName){}
	static public void sendException(Exception e){}
	static public void sendException(Exception e, String addedText){e.printStackTrace();}
	/////////////////////////////////////////////////////////////////////////////////////
	//*/


    ////////////////////////////////////////////////////////////////////////////
    // THESE FUNCTIONS SHOULD BE COMMENETED WHEN NOT USING ANYLITICS
    // ALSO COMMENT IMPORTS
    ////////////////////////////////////////////////////////////////////////////
    /*
    public static Tracker tracker = null;

    public static void setTrackerID(){
        try{
            tracker.set("randomID", randomID);
            tracker.set("01", randomID);
        } catch (Exception e){
            e.printStackTrace();
            sendException(e);
        }
    }

    synchronized void getTracker() {
        if(tracker != null)
            return;
        try{

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(GOOGLE_AN_ID);
            t.enableAdvertisingIdCollection(true);
            t.enableExceptionReporting(true);
            t.enableAutoActivityTracking(true);
            tracker = t;

            sendScreen("Started App");
            //Toast.makeText(context, "Made tracker.", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            e.printStackTrace();
            sendException(e);
        }
        return;
    }

    static public void sendEvent(String cat, String act, long value){
        try{
            if(tracker == null){
                return;
            }
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(cat).setAction(act).setLabel(randomID)
                    .setValue(value)
                    .build());
        }catch(Exception e){
            e.printStackTrace();
            sendException(e);
        }
    }

    static public void sendEvent(String cat, String act){
        try{
            if(tracker == null){
                return;
            }
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(cat).setAction(act).setLabel(randomID)
                            //.setValue(value)
                    .build());
        }catch(Exception e){
            e.printStackTrace();
            sendException(e);
        }
    }

    static public void sendScreen(String screenName){
        sendEvent("Screen Change", screenName);
        // Set screen name.
        tracker.setScreenName(screenName);

        // Send a screen view.
        tracker.send(new HitBuilders.AppViewBuilder().build());

    }


    static public void sendException(Exception e){
        sendException(e,"");
    }

    static public void sendException(Exception e, String addedText){
        String reportText = "_" + addedText + ";" + e + ";" + e.getStackTrace()[0].toString();
        e.printStackTrace();
        //Sending toast might break the app if run from other thread
        //Toast.makeText(MyApp.currActivityContext,"" + reportText.length() + ". " + reportText , Toast.LENGTH_SHORT).show(); //TODO comment out b/f release

        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(reportText)
                .setFatal(false)
                .build());

    }
*/


}
