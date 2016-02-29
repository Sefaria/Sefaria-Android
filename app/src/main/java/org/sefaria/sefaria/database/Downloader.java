package org.sefaria.sefaria.database;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.sefaria.sefaria.DialogManager;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;

public class Downloader {

    //public static final String GOOGLE_DRIVE_PATH = "https://googledrive.com/host/0B42RTqGcyx8kbjgtaVJLRlFBSlE/";
    public static final String CSV_FILE_NAME = "sefaria_mobile_updating_csv.csv";
    private static final String CSV_DEBUG_URL = "http://betamidrash.com/other/app/v2/dev/" + CSV_FILE_NAME; //developing version
    private static final String CSV_REAL_URL  = "http://betamidrash.com/other/app/v2/" + CSV_FILE_NAME;
    private static boolean useDebugCSV = false;
    public static final String CSV_DOWNLOAD_TITLE = "Sefaria Pre Update";
    public static final String DB_DOWNLOAD_TITLE = "Sefaria Library Update";
    public static final String JSON_INDEX_TITLE = "Sefaria Index";
    public static final String DB_DOWNLOAD_PATH = "sefariaTempDownld/" ; // + "/";
    public static final String FULL_DOWNLOAD_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + DB_DOWNLOAD_PATH; //Environment.getExternalStorageDirectory()
    public static final String INDEX_JSON_NAME = "sefaria_mobile_updating_index.json";

    public static final int INTERNET_LOST = 56;
    public static final int NOT_ENOUGH_SPACE = 57;
    public static final int UNKNOWN_ERROR = 58;


    public static final int DATA_CONNECTED = 0;
    public static final int WIFI_CONNECTED = 1;
    public static final int NO_INTERNET = 2;

    public static DownloadManager manager;
    public static ArrayList<Long> downloadIdList;

    private static IntentFilter downloadCompleteIntentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    private static Context context;

    public static boolean eitherDBorIndexFinished = false;
    private static boolean receiverRegistered = false;
    private static Context registeredContext;

    public static int downloadErrorNum;

    public static String getCSVurl(){
        if(useDebugCSV)
            return CSV_DEBUG_URL;
        else
            return CSV_REAL_URL;
    }

    public static void updateLibrary(Activity activity) {
        UpdateService.lockOrientation(activity);
        Intent intent = new Intent(activity,UpdateReceiver.class);
        intent.putExtra("isPre", true);
        intent.putExtra("userInit",true);
        activity.sendBroadcast(intent);
        DialogManager.showDialog(DialogManager.CHECKING_FOR_UPDATE);
    }

    //even though this is really long, it's just a property of the SettingsActivity
    private static BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            if (downloadIdList.indexOf(id) == -1) {
                Log.v("download", "Ingnoring unrelated download " + id);
                return;
            } else {
                //Toast.makeText(context, "did: " + downloadId, Toast.LENGTH_SHORT).show();
                int ind = downloadIdList.indexOf(id);
                downloadIdList.remove(ind); //retire this id. this is due to a bug in downloadmanager downloading twice
            }
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor cursor = downloadManager.query(query);

            // it shouldn't be empty, but just in case
            if (!cursor.moveToFirst()) {
                Log.e("download", "Empty row");
                return;
            }
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(statusIndex);
            if (DownloadManager.STATUS_FAILED == status) {

                int reason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                GoogleTracker.sendEvent("DOWNLOAD_ERROR", "Error : " + reason);
                Log.d("status","Download Status " + status);
                if (reason == DownloadManager.ERROR_CANNOT_RESUME) {
                    downloadErrorNum = DownloadManager.ERROR_CANNOT_RESUME;
                    UpdateService.handler.sendEmptyMessage(Downloader.INTERNET_LOST);
                } else if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                    UpdateService.handler.sendEmptyMessage(Downloader.NOT_ENOUGH_SPACE);
                    downloadErrorNum = DownloadManager.ERROR_INSUFFICIENT_SPACE;
                } else { //unknown error
                    UpdateService.handler.sendEmptyMessage(Downloader.UNKNOWN_ERROR);
                    downloadErrorNum = reason;
                }
                return;
            }

            int colIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            String downloadTitle = cursor.getString(colIndex);

            Log.d("update",downloadTitle);

            if (downloadTitle.equals(DB_DOWNLOAD_TITLE)) {
                if (eitherDBorIndexFinished) UpdateService.handler.sendEmptyMessage(UpdateService.UPDATE_STAGE_2_COMPLETE);
                else eitherDBorIndexFinished = true;
            } else if (downloadTitle.equals(JSON_INDEX_TITLE)) {
                if (eitherDBorIndexFinished) UpdateService.handler.sendEmptyMessage(UpdateService.UPDATE_STAGE_2_COMPLETE);
                else eitherDBorIndexFinished = true;
            }
        }


    };

    protected static void init(Context con) {
        context = con;
        context.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
        receiverRegistered = true;
        registeredContext = context;
        //Toast.makeText(context, "registered", Toast.LENGTH_SHORT).show();
        downloadIdList = new ArrayList<Long>();
    }

    protected static void download(String url, String description, String destPath, String destName, boolean isHidden) {
        if (!receiverRegistered) {
            context.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
            receiverRegistered = true;
            registeredContext = context;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(description);
        request.setTitle(description);
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            if (isHidden) request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            else request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE); //used to be visible
        }

        //get the data dir...
        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (NameNotFoundException e) {
            GoogleTracker.sendException(e);
            Log.w("yourtag", "Error Package name not found ", e);
        }catch (Exception e1) { //JH added
            GoogleTracker.sendException(e1);
            Log.e("downloader","" + e1);
        }

        //i would use the var s, but it doesn't seem to work...

        if (canWrite()) {
            //Log.d("yo","all good");
        } else {
            Toast.makeText(context, context.getString(R.string.problem_writting_file), Toast.LENGTH_LONG).show();
            return;
        }

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destPath + destName);
        // get download service and enqueue file
        manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadIdList.add(manager.enqueue(request));

        //progressUpdater();

    }

    protected static int getNetworkStatus()
    {
        final ConnectivityManager connMgr = (ConnectivityManager)
                MyApp.currActivityContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if( wifi.isConnected()) {
            return WIFI_CONNECTED;
        } else if( mobile.isConnected()) {
            return DATA_CONNECTED;
        } else {
            return NO_INTERNET;
        }
    }

    protected static void unregisterDownloader(Context context) {
        if (receiverRegistered && registeredContext.equals(context) && downloadIdList.size() == 0) {
            context.unregisterReceiver(downloadCompleteReceiver);
            receiverRegistered = false;
            registeredContext = null;
            //Toast.makeText(context,"unregistered",Toast.LENGTH_SHORT).show();
        }
    }

    //simple func that just tells you if you have permission to write on the users system
    private static boolean canWrite() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        Log.d("stor","Avail " + mExternalStorageAvailable + " Write " + mExternalStorageWriteable);
        return mExternalStorageAvailable && mExternalStorageWriteable;
    }

    private static void progressUpdater() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int nullCount = 0; //keep track of times when there is no progress update
                    double progress = 0.0;
                    double oldprogress = 0.0;
                    while (progress < 95.0 && nullCount < 20) {

                        if (downloadIdList.size() > 0) {

                            Query query = new Query();
                            query.setFilterById(downloadIdList.get(downloadIdList.size()-1));
                            Cursor c = manager.query(query);
                            if (c.moveToFirst()) {
                                int sizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                                int downloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                                long size = c.getInt(sizeIndex);
                                long downloaded = c.getInt(downloadedIndex);

                                if (size != -1) {

                                    double newprogress = downloaded*100.0/size;

                                    int diff = (int) (0.4*(Math.floor(newprogress) - Math.floor(progress)));
                                    if (DialogManager.isShowingDialog && oldprogress != progress) {

                                        ((ProgressDialog)DialogManager.dialog).incrementProgressBy(diff);
                                    }
                                    Log.d("dm","Diff: "+diff);
                                    Log.d("dm","Prog: " + progress);
                                    Log.d("dm","NPro: " + newprogress);
                                    oldprogress = progress;
                                    progress = newprogress;

                                    nullCount = 0;
                                } else nullCount++;

                            }
                        } else nullCount++;

                        Thread.sleep(1500);
                    }
                } catch (InterruptedException e) {
                    //boohoo
                }
            }
        }).start();
    }
}
