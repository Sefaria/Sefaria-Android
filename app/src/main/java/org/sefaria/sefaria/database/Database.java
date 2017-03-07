package org.sefaria.sefaria.database;
import org.sefaria.sefaria.Dialog.DialogCallable;
import org.sefaria.sefaria.Dialog.DialogManager2;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;


public class Database extends SQLiteOpenHelper{

    private static Database APIInstance;
    private static Database offlineInstance;
    public static String DB_NAME = "UpdateForSefariaMobileDatabase";
    public static String API_DB_NAME = "API_UpdateForSefariaMobileDatabase";
    static int DB_VERSION = 1;


    public static boolean isDownloadingDatabase = false; //used for showing "snackbar" on top of actionbar
    private static final int MIN_DB_VERSION = 151;

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public Database(Context context){
        super(new DatabaseContext(context, getDbPath(context)), DB_NAME , null, DB_VERSION);
        this.myContext = context;
    }



    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public Database(Context context, int useAPI) {
        super(new DatabaseContext(context, getDbPath(context)), API_DB_NAME, null, DB_VERSION);
        this.myContext = context;
    }

    /**
     *
     * @param context
     * @param getNonMainPath - true if you want to get the path associated with !Settings.getUseSDCard(). this effectively returns the other possible db storage path. Note, if the SD card doesn't exist, this will return the internal path anyway
     * @return
     */
    static public String getDbPath(Context context, boolean getNonMainPath) {
        String dbPath;
        if (Settings.getUseSDCard() || getNonMainPath) {
            File sdcard = context.getExternalFilesDir(null);
            try {
                dbPath = sdcard.getAbsolutePath() + File.separator;
            } catch (NullPointerException e) {
                Toast.makeText(context, "SDCard is Null?", Toast.LENGTH_SHORT).show();
                Log.d("DbError","SDCARD PRBLEMDSDJKQ!!!");
                dbPath = getInternalFolder() + "databases/";
            }
        } else {
            //The Android's default system path of your application database.
            dbPath = getInternalFolder() + "databases/";
        }


        Log.d("databasepath", dbPath + " mkdirs: " + mkDirs(dbPath));
        //File[] files = new File(DB_PATH).listFiles();
        return dbPath;
    }

    static public String getDbPath(Context context){
        return getDbPath(context, false);
    }

    public class SDCardNotFoundException extends Exception{
        public SDCardNotFoundException() {
            super("SD card not found exception");
        }
        public SDCardNotFoundException(String message){
            super(message);
        }
        private static final long serialVersionUID = 613L;
    }

    static private boolean mkDirs(String path){
        File folder = new File(path);
        return (folder.mkdirs() ||  folder.isDirectory());
    }

    static public boolean isValidOfflineDB(){
        return  getVersionInDB(false)>= MIN_DB_VERSION;
    }

    public static void dealWithStartupDatabaseStuff(Activity activity){
        Log.d("MyApp", "dealWithDatabaseStuff");
        long time = Settings.getDownloadSuccess(true);
        if(time >0) {
            GoogleTracker.sendEvent("Download", "Update Finished", time);
            if(hasOfflineDB()){
                Settings.setUseAPI(false);
            }
        }

        checkAndSwitchToNeededDB(activity);

        Util.deleteNonRecursiveDir(Downloader.FULL_DOWNLOAD_PATH); //remove any old temp downloads
        Cache.clearExpiredCache(); //TODO I suspect this line is taking a lot of time to run. not sure
        Database.getOfflineDBIfNeeded(activity,false);
        if(!Settings.getUseAPI() && Settings.getIfShouldDoUpdateCheck() ) {
            new UpdateService.silentlyCheckForUpdates().execute(activity);
        }
    }

    /**
     *
     * @param activity
     * @param evenIfUsingAPI
     * @return - true if downloading, false otherwise
     */
    static public boolean getOfflineDBIfNeeded(Activity activity, boolean evenIfUsingAPI){
        if(!isDownloadingDatabase && (evenIfUsingAPI || !Settings.getUseAPI()) && (!Database.isValidOfflineDB()|| !Database.hasOfflineDB())) {
            Toast.makeText(activity, MyApp.getRString(R.string.starting_download), Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(activity,false);
            return true;
        }
        return false;
    }

    private static Boolean hasOfflineDB;
    /**
     *
     * @return false if there's a Segment table in the db. true if not (and should be using API)
     */
    public static boolean hasOfflineDB(){
        if(hasOfflineDB != null)
            return hasOfflineDB;
        //TODO maybe check the settings table instead (api should be 1)
        try{
            Database dbHandler = Database.getInstance(false);
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            Cursor cursor = db.query(Segment.TABLE_TEXTS, null, "_id" + "=?",
                    new String[]{String.valueOf(1)}, null, null, null, null);
            //Log.d("api", "got here without problems" + cursor);
            hasOfflineDB = true;
        }catch(Exception e){
            hasOfflineDB = false;
        }
        return hasOfflineDB;
    }

    public static void checkAndSwitchToNeededDB(Activity activity){
        boolean hasInternet = (Downloader.getNetworkStatus() != Downloader.ConnectionType.NONE);

        if (Settings.getUseSDCard() && !hasSDCard(activity)) {
            //there's an issue with your SD card db. switch to online made

            DialogManager2.showDialog(activity, new DialogCallable("SD Card Error",
                    "Couldn't find library on SD card. Switching to online mode.",MyApp.getRString(R.string.OK),null,null, DialogCallable.DialogType.ALERT) {
                @Override
                public void positiveClick() {
                    DialogManager2.dismissCurrentDialog();
                }
            });
            //Settings.setUseSDCard(false); actually, probably better to keep this set and then if they reinsert their card, they get their library back
        } else if (Settings.getUseSDCard() && hasSDCard(activity)) {
            Settings.setUseAPI(false);
        } else if(!Database.hasOfflineDB() && !Settings.getUseAPI()){ //There's no DB
            Toast.makeText(activity,MyApp.getRString(R.string.switching_to_api),Toast.LENGTH_LONG).show();
            //DialogManager2.showDialog(activity, DialogManager2.DialogPreset.SWITCHING_TO_API);


            Settings.setUseAPI(true);
        } else if(Settings.getUseAPI() && !hasInternet && Database.hasOfflineDB()){
            Toast.makeText(activity,MyApp.getRString(R.string.NO_INTERNET_TITLE) + " - " + MyApp.getRString(R.string.switching_to_offline),Toast.LENGTH_SHORT).show();
            Settings.setUseAPI(false);
        }


    }

    public static void onRequestPermissionsResult(Activity activity,int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MyApp.REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Downloader.updateLibrary(activity, false);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(activity, MyApp.getRString(R.string.without_permission), Toast.LENGTH_LONG).show();
                    Settings.setUseAPI(true);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    static public String getInternalFolder(){
        String path = getStorageDir(true);
        Log.d("databasepath", path + " makdirs:" + mkDirs(path));
        return path;
    }

    static private String getStorageDir(boolean tryExternal){
        String [] dirs = Util.getStorageDirectories();
        final String androidPath = "/Android/data/" + MyApp.getAppPackageName() + "/files/";
        final String regularPath = "/data/data/" + MyApp.getAppPackageName() + "/"; //this is the old way of doing it which worked fine for non-SD cards
        if(!tryExternal)
            return regularPath;

        //trying to get SD card path
        for(String dir:dirs){
            if(dir.contains("ext")) {
                String tempPath = dir + androidPath;
                if(mkDirs(tempPath))
                    return tempPath;
            }
        }
        for(String dir:dirs){
            if(!dir.contains("emulated")){
                String tempPath = dir + androidPath;
                if(mkDirs(tempPath))
                    return tempPath;
            }
        }

        if(dirs.length > 0){
            String tempPath = dirs[0] + androidPath;
            if(mkDirs(tempPath))
                return tempPath;
        }
        return regularPath;
    }

    static public boolean hasSDCard(Context context) {
        File sdcard = context.getExternalFilesDir(null);
        try {
            return sdcard.exists();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDatabase() throws IOException{
        //just copy, no one cares what you overwrite
        //copyDatabase(DB_PATH);
        //unzipDatabase(DB_PATH);
        //String path = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        //copyDatabase(path,"testReal.db");


        //unzipDatabase(DB_PATH,"test.zip");

        //Log.d("db","now, to decide it once and for all...");
        //boolean result = FileUtils.contentEquals(new File(path + "testReal.db"), new File (path + "test.db"));
        //Log.d("db", "the files are the same (T/F)? " + result);
    }

    public static void deleteDatabase(Context context) {
        File oldMainDB = new File(getDbPath(context) + DB_NAME + ".db");
        if (oldMainDB.exists()) {
            Log.d("db","deleting main db");
            oldMainDB.delete();
        }
        File oldOtherDB = new File(getDbPath(context, true) + DB_NAME + ".db");
        if (oldOtherDB.exists()) {
            Log.d("db","deleting other db");
            oldOtherDB.delete();
        }
        hasOfflineDB = null;
        Settings.setUseAPI(true);
    }

    private static Database getInstance(Boolean forAPI) {
        Context context = MyApp.getContext();
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        Database instance;
        if(forAPI == null){
            forAPI = Settings.getUseAPI();
        }
        if(!forAPI)
            instance = offlineInstance;
        else {
            instance = APIInstance;
        }

        if (instance == null) {
            if(!forAPI) {
                offlineInstance = new Database(context.getApplicationContext());
                return offlineInstance;
            }else {
                Database.createAPIdb(context);
                APIInstance = new Database(context.getApplicationContext(), 1);
                return APIInstance;
            }
        }
        return instance;
    }


    public static SQLiteDatabase getDB(){
        return getInstance(null).getReadableDatabase();
    }

    public static boolean checkDataBase(Context context){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = getDbPath(context) + DB_NAME + ".db";
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(Exception e){
            GoogleTracker.sendException(e, "database doesn't exist");
            //database does't exist yet.

        }

        if(checkDB != null){
            checkDB.close();
        }

        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDatabase(String path, String name) throws IOException{
        //the following ensures all directories and files that we want will exist
        File testFile = new File(path + name);
        if (!testFile.exists()) {
            File testParent = new File(path);
            if (!testParent.exists()) {
                Log.d("yo","creating pars");
                testParent.mkdirs();
            }
            Log.d("yo","creating");
            testFile.createNewFile();
        }

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(name);

        // Path to the just created empty db
        String outFileName = path + name;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();



    }

    public final static int BAD_SETTING_GET = -999999991;
    public static int getDBSetting(String key, Boolean forAPI){
        int value = BAD_SETTING_GET;
        try {
            Database dbHandler = Database.getInstance(forAPI);
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            Cursor cursor = db.query("Settings", null, "_id" + "=?",
                    new String[]{key}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                value = cursor.getInt(1);
            }
        }catch(Exception e){
            ;
        }
        return value;
    }

    public static void createAPIdb(Context context){
        Log.d("api", "trying to create db");
        Database myDbHelper = new Database(MyApp.getContext());
        myDbHelper.getReadableDatabase();
        try {
            myDbHelper.unzipDatabase("API_UpdateForSefariaMobileDatabase.zip", Database.getDbPath(context),true);
        } catch (IOException e) {
            Log.e("api",e.toString());
        }
    }

    public  void unzipDatabase(String oldPath, String newPath, boolean fromAssets) throws IOException
    {
        Log.d("zip","let's unzip this bad boy...");
        InputStream is;
        ZipInputStream zis;
        String filename;
        if(fromAssets)
            is = myContext.getAssets().open(oldPath);
        else
            is =  new FileInputStream(new File(oldPath));
        zis = new ZipInputStream(is);
        ZipEntry ze;
        byte[] buffer = new byte[1024];
        int count;

        while ((ze = zis.getNextEntry()) != null)
        {
            // zapis do souboru - Czech for "write to a file"
            filename = ze.getName();

            // Need to create directories if not exists, or
            // it will generate an Exception...
            if (ze.isDirectory()) {
                //Log.d("yo",newPath + filename);
                File fmd = new File(newPath + filename);
                fmd.mkdirs();
                continue;
            }

            OutputStream fout = new FileOutputStream(newPath + filename);

            // cteni zipu a zapis - Czech for "reading and writing zip"
            while ((count = zis.read(buffer)) != -1)
            {
                fout.write(buffer, 0, count);
            }

            fout.flush();
            fout.close();
            zis.closeEntry();
        }

        zis.close();
    }

    public static int getVersionInDB(Boolean forAPI){
        int versionNum = getDBSetting("version",forAPI);
        if(versionNum == BAD_SETTING_GET)
            versionNum = -1;
        return versionNum;
    }


    public void openDataBase(Context context) throws SQLException{

        //Open the database
        String myPath = getDbPath(context) + DB_NAME + ".db";
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {

        if(myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Add your public helper methods to access and get content from the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
    // to you to create adapters for your views.

}
