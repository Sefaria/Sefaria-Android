package org.sefaria.sefaria.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.sefaria.sefaria.Dialog.DialogCallable;
import org.sefaria.sefaria.Dialog.DialogManager2;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuState;

public class UpdateService extends Service {
    public static final int UPDATE_STAGE_2_COMPLETE = 1;
    public static final int UPDATE_STAGE_3_COMPLETE = 2;

    public static final int NOTIFICATION_ID = 613267;

    public static final String DATABASE_ZIP_DOWNLOAD_LOC = Downloader.FULL_DOWNLOAD_PATH + Database.DB_NAME + ".zip";
    public static final String INDEX_DOWNLOAD_LOC = Downloader.FULL_DOWNLOAD_PATH + Downloader.INDEX_JSON_NAME;

    private static final String LAST_ASK_TO_UPGRADE_TIME = "LAST_ASK_TIME_UPGRADE_TIME";

    //these two vars are stupid, but work. they are persistent vars which I need for checking version num. before and after this, they serve no purpose
    public static int updatedVersionNum;
    public static int currentVersionNum;
    public static boolean userInitiated;
    public static boolean evenOverWriteOldDatabase = false;
    public static boolean inUpdateStage3 = false;
    private static long startedUpdateTime = 0;

    private static Intent intentYo;
    private static Service serviceYo;
    private static PowerManager.WakeLock powerLock;
    private static WifiManager.WifiLock wifiLock;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Downloader.init(this);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock= wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifiTag");
        wifiLock.acquire();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        powerLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "powerTag");
        powerLock.acquire();

        intentYo = intent;
        serviceYo = this;

        boolean isPre = intent.getBooleanExtra("isPre", false);
        boolean userInit = intent.getBooleanExtra("userInit", false);

        if (isPre) {
            preUpdateLibrary(userInit);
        } else {
            updateLibrary(userInit);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Downloader.unregisterDownloader(this);
        endService();
    }

    //check internet status before update, and if necessary, inform user of problems
    private static void preUpdateLibrary(boolean userInit) {
        Downloader.ConnectionType netStat = Downloader.getNetworkStatus();
        if (netStat == Downloader.ConnectionType.NONE) {
            DialogManager2.showDialog(Downloader.getActivity(), DialogManager2.DialogPreset.NO_INTERNET);
        } else if (netStat == Downloader.ConnectionType.DATA) {
            DialogManager2.showDialog(Downloader.getActivity(), DialogManager2.DialogPreset.DATA_CONNECTED);
        } else if (netStat == Downloader.ConnectionType.WIFI) {
            updateLibrary(userInit);
        }
    }

    //suppressing because I handle sdk check myself
    @SuppressLint("NewApi")
    private static void updateLibrary(boolean userInit) {

        //Toast.makeText(context, "Start background update", Toast.LENGTH_SHORT).show();
        //Toast.makeText(context, "background update only headers.", Toast.LENGTH_SHORT).show();
        GoogleTracker.sendEvent("Download", "getting_update_csv");
        startedUpdateTime = System.currentTimeMillis();
        //lockOrientation(context);

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        Notification.Builder notBuild = new Notification.Builder(serviceYo)
                .setTicker("Updating Sefaria")
                .setSmallIcon(R.drawable.sefaria_icon_noti)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Updating Sefaria")
                .setContentText("Library downloading and installing");


        Notification notification;
        if (SDK_INT >= 11 && SDK_INT <= 15) notification = notBuild.getNotification();
        else notification = notBuild.build();

        Intent notificationIntent = new Intent(serviceYo, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(serviceYo, 0, notificationIntent, 0);
        serviceYo.startForeground(NOTIFICATION_ID, notification);
        //save last check time
        SharedPreferences settings = Settings.getGeneralSettings();
        Editor editor = settings.edit();
        editor.putLong("lastUpdateTime", System.currentTimeMillis());
        editor.apply();

        userInitiated = userInit;
        currentVersionNum = Database.getVersionInDB(false);

        postUpdateStage1();

    }

    private static class UpdateCSVData{
        int dbVersion;
        String zipUrl;
        String indexURL;
        int newestAppVersionNum;
        public UpdateCSVData() throws Exception{
            String csvData = null;
            csvData = API.getDataFromURL(Downloader.getCSVurl(),null,false, API.TimeoutType.LONG);
            Log.d("Downloader", "postUpdateStage1 CSV: " + csvData);
            String[] firstLine = csvData.split(",");
            dbVersion = Integer.parseInt(firstLine[0]);
            zipUrl = firstLine[1];
            indexURL = firstLine[2];
            String newestAppVersion = firstLine[3];
            Log.d("Downloader","postUpdateStage1: +" + newestAppVersion + "+");
            newestAppVersionNum  = Integer.parseInt(newestAppVersion.replace("[^0-9]","").replace("\n",""));
        }
    }


    static public class silentlyCheckForUpdates extends AsyncTask<Activity, Void, UpdateCSVData> {
        Activity activity;
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected UpdateCSVData doInBackground(Activity... params) {
            activity = params[0];
            try {
                UpdateCSVData updateCSVData = new UpdateCSVData();
                return updateCSVData;
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(UpdateCSVData updateCSVData) {
            if(updateCSVData != null){
                Settings.setLastUpdateCheckToNow();
                if(updateCSVData.dbVersion > Database.getVersionInDB(false)){
                    DialogManager2.showDialog(activity, DialogManager2.DialogPreset.NEW_UPDATE_FROM_SILENT_CHECK);
                }
            }
        }
    }

    private static void postUpdateStage1() {
        try {
            UpdateCSVData updateCSVData = new UpdateCSVData();
            updatedVersionNum = updateCSVData.dbVersion; //save this for later

            if((updateCSVData.newestAppVersionNum > MyApp.getContext().getPackageManager().getPackageInfo(MyApp.getAppPackageName(), 0).versionCode)
                    && !MyApp.askedForUpgradeThisTime){
                Toast.makeText(MyApp.getContext(), MyApp.getContext().getString(R.string.upgrade_to_newest) + " " + MyApp.APP_NAME, Toast.LENGTH_SHORT).show();
                try {
                    MyApp.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MyApp.getAppPackageName())));
                } catch (android.content.ActivityNotFoundException anfe) {
                    MyApp.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + MyApp.getAppPackageName())));
                }
                MyApp.askedForUpgradeThisTime = true;
                if(userInitiated)
                    DialogManager2.dismissCurrentDialog(); //dismiss progressDialog
                unlockOrientation((Activity)MyApp.getContext());
                return;

            }//else just continue to check if there's an update

            //check versions before continuing
            if ((updatedVersionNum > currentVersionNum && userInitiated) || evenOverWriteOldDatabase ) {
                //DialogManager2.dismissCurrentDialog();
                //DialogManager2.showDialog((Activity)MyApp.getContext(), DialogManager2.DialogPreset.UPDATE_STARTED);
                updateStage2(updateCSVData.zipUrl, updateCSVData.indexURL);
            } else if (updatedVersionNum > currentVersionNum && !userInitiated) {
                if (currentVersionNum == -1) {
                    //click yes very quickly...
                    Intent intent = new Intent(MyApp.getContext(),UpdateReceiver.class);
                    intent.putExtra("isPre",true);
                    intent.putExtra("userInit",true);
                    Downloader.getActivity().sendBroadcast(intent);
                    DialogManager2.showDialog((Activity)MyApp.getContext(), DialogManager2.DialogPreset.CHECKING_FOR_UPDATE);
                } else {
                    DialogManager2.showDialog((Activity)MyApp.getContext(), DialogManager2.DialogPreset.NEW_UPDATE);
                }
            } else if (updatedVersionNum <= currentVersionNum && userInitiated) {
                DialogManager2.dismissCurrentDialog(); //dismiss progressDialog
                DialogManager2.showDialog((Activity)MyApp.getContext(),DialogManager2.DialogPreset.NO_NEW_UPDATE);
                //UpdateService.endService(); //PROBABLY NOT NECESSARY, BUT ADDED IN CASE OF FUTURE BUG (ES)
            } else {
                //no new update and not user initiated
                UpdateService.endService(); //ADDED TO STOP THE SERVICE, SINCE THERE IS NO UPDATE (ES)
                unlockOrientation((Activity)MyApp.getContext());

            }

        }catch (Exception e){
            e.printStackTrace();
            GoogleTracker.sendException(e, "postUpdateStage1");
            if(userInitiated) DialogManager2.dismissCurrentDialog();
            unlockOrientation((Activity)MyApp.getContext());
            return;
        }
    }

    //...which uses it download the zip. Once done, it goes to updateStage3()
    private static void updateStage2(String zipUrl, String indexURL) {
        GoogleTracker.sendEvent("Download", "updateStage2 - Starting DB download");

        //delete any file with the same name to avoid confusion
        File testFile = new File(DATABASE_ZIP_DOWNLOAD_LOC);
        if (testFile.exists()) testFile.delete();

        File testIndexFile = new File(INDEX_DOWNLOAD_LOC);
        if (testIndexFile.exists()) testIndexFile.delete();
        Downloader.eitherDBorIndexFinished = false;
        Downloader.download(indexURL, Downloader.JSON_INDEX_TITLE, Downloader.DB_DOWNLOAD_PATH, Downloader.INDEX_JSON_NAME, false);
        Downloader.download(zipUrl,Downloader.DB_DOWNLOAD_TITLE,Downloader.DB_DOWNLOAD_PATH, Database.DB_NAME + ".zip",false);
        //this guy calls a complete handler in Downloader to inform us we're down and move on to stage2
    }

    //...which unzips update into the database location.
    private static void updateStage3() {
        Log.d("up", "stage 3 started");
        //if (!inUpdateStage3) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                inUpdateStage3 = true;
                try {
                    Database myDbHelper;
                    myDbHelper = new Database(MyApp.getContext());

                    myDbHelper.getReadableDatabase();
                    try {
                        Database.deleteDatabase();
                        myDbHelper.unzipDatabase(DATABASE_ZIP_DOWNLOAD_LOC, Database.getDbPath(), false);

                        //move index.json file into right location
                        Util.moveFile(Downloader.FULL_DOWNLOAD_PATH, Downloader.INDEX_JSON_NAME, Database.getInternalFolder(), MenuState.jsonIndexFileName);

                        long timeToCompleteUpdate = System.currentTimeMillis() - startedUpdateTime;
                        if(startedUpdateTime != 0 && timeToCompleteUpdate > 0){
                            Settings.setDownloadSuccess(timeToCompleteUpdate);
                        }
                        Thread.sleep(200);


                    } catch (IOException ioe) { //maybe add more exception handling
                        GoogleTracker.sendException(ioe, "updateStage3. THROWING");
                        throw ioe;
                    } /*catch (Exception	e){
							Toast.makeText(contextYo,e.getMessage(),Toast.LENGTH_LONG).show();
							MyApp.sendException(e, "updateStage3. THROWING");
							throw new Error("Unable to create database");
						}*/
                    //testing...
                    //Segment.getAllTextsFromDB2();
                    myDbHelper.close();

                    //clean up by deleting zip that you downloaded...
                    //File updateFile = new File(DATABASE_ZIP_DOWNLOAD_LOC);
                    //if (updateFile.exists()) updateFile.delete();
                    //Util.deleteNonRecursiveDir(Downloader.FULL_DOWNLOAD_PATH);


                    handler.sendEmptyMessage(UPDATE_STAGE_3_COMPLETE);


                } catch (Exception e) {
                    GoogleTracker.sendException(e,"updateStage3. THROWING");
                    throw new Error(e);
                }

            }
        }).start();

        //}

    }

    private static Map<Integer,String> messageMap = new HashMap<>();


    private static void sendMessage(String string){
        messageMap.put(string.hashCode(),string);
        handler.sendEmptyMessage(string.hashCode());
    }

    public static Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case UPDATE_STAGE_2_COMPLETE:
                    //let's move on to stage 2
                    //Toast.makeText(contextYo, "Download complete. Installing...", Toast.LENGTH_SHORT).show();
                    updateStage3();
                    break;
                case UPDATE_STAGE_3_COMPLETE:
                    endService();

                    restart();


                    break;
                default:
                    endService();
                    DialogManager2.showDialog((Activity) MyApp.getContext(), new DialogCallable("Download Error",
                            messageMap.get(msg.what),MyApp.getRString(R.string.OK),null,null, DialogCallable.DialogType.ALERT) {
                        @Override
                        public void positiveClick() {
                            DialogManager2.dismissCurrentDialog();
                        }
                    });
                    break;
            }
        }
    };


    public static void lockOrientation(Activity ac) {

        if(ac.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static void unlockOrientation(Activity ac) {
        ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private static void restart() {
        inUpdateStage3 = false;

        DialogManager2.dismissCurrentDialog();
        //Toast.makeText(MyApp.currActivityContext, "Installation complete. Enjoy the Torah!", Toast.LENGTH_SHORT).show();
        unlockOrientation((Activity)MyApp.getContext());
        //total restart. To be safe, restart so the database is readable.
        MyApp.restart();
    }

    public static void endService() {
        Database.setIsDownloadingDatabase(false);

        try {
            DialogNoahSnackbar.dismissCurrentDialog();
            serviceYo.stopForeground(true);
            UpdateReceiver.completeWakefulIntent(intentYo);
            wifiLock.release();
            powerLock.release();
        } catch( Exception e) {
            GoogleTracker.sendException(e);
            e.printStackTrace();
        }

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) MyApp.getContext().getSystemService(ns);
        nMgr.cancel(UpdateService.NOTIFICATION_ID);

    }



}


