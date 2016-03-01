package org.sefaria.sefaria;

/**
 * Created by LenJ on 2/4/2016.
 */

import android.content.SharedPreferences;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Random;
import java.util.Set;

public class GoogleTracker extends MyApp {

    private static final String GOOGLE_AN_ID = "UA-73355210-1";
    public static String randomID = null;
    private static Tracker tracker = null;

    public static final String CATEGORY_NEW_TEXT = "Opened Text Page";
    public static final String BUTTON_PRESS = "Button Press";
    public static final String SETTING_CHANGE = "Setting Change";
    public static final String CATEGORY_OPEN_MENU = "Opened Menu page";
    public static final String CATEGORY_SCREEN_CHANGE = "Screen Change";
    public static final String CATEGORY_STATUS_INFO = "Status Info";
    public static final String CATEGORY_OPEN_NEW_BOOK_ACTION = "Open new book action";

    public GoogleTracker(){
        getTracker();
        setTrackerID();
        sendEvent(CATEGORY_SCREEN_CHANGE, "Started App");
        sendEvent("Menu lang",Settings.lang2Str(Settings.getDefaultTextLang()));
        int theme = Settings.getTheme();
        String themeName = "";
        if(theme == R.style.SefariaTheme_White)
            themeName = "SefariaTheme_White";
        else if(theme == R.style.SefariaTheme_Grey)
            themeName = "SefariaTheme_Grey";
        else if(theme == R.style.SefariaTheme_Black)
            themeName = "SefariaTheme_Black";
        sendEvent("Theme",themeName);
        Boolean sideBySide = Settings.getIsSideBySide();
        sendEvent("sideBySide", sideBySide.toString());
        sendEvent("Text lang",Settings.lang2Str(Settings.getDefaultTextLang()));
    }


    private static void setTrackerID(){
        if(randomID == null){
            SharedPreferences settings = Settings.getGeneralSettings();
            randomID = settings.getString("randomID","");
            if(randomID.equals("")){
                Random random = new Random();
                Long longID = random.nextLong(); //there's a really small chance that it's 0... we're going to ignore that.
                randomID = "" + longID;
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("randomID",randomID);
                editor.apply();
            }
        }

        try{
            tracker.set("randomID", randomID);

            // You only need to set User ID on a tracker once. By setting it on the tracker, the ID will be
            // sent with all subsequent hits.
            tracker.set("&uid", randomID);

            // This hit will be sent with the User ID value and be visible in User-ID-enabled views (profiles).
            //tracker.send(new HitBuilders.EventBuilder().setCategory("Scr").setAction("User Sign In").build());
        } catch (Exception e){
            e.printStackTrace();
            sendException(e);
        }
    }

    synchronized void getTracker() {
        if(tracker != null)
            return;
        try{

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(MyApp.getContext());
            Tracker t = analytics.newTracker(GOOGLE_AN_ID);
            t.enableAdvertisingIdCollection(true);
            t.enableExceptionReporting(true);
            t.enableAutoActivityTracking(true);
            tracker = t;
            //Toast.makeText(context, "Made tracker.", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            e.printStackTrace();
            //sendException(e);
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
        sendEvent(CATEGORY_SCREEN_CHANGE, screenName);
        // Set screen name.
        tracker.setScreenName(screenName); //when you do this, it overrides the

        // Send a screen view.
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

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



}
