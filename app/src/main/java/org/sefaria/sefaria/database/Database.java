package org.sefaria.sefaria.database;
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
import java.util.Set;
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


    private static boolean isDownloadingDatabase = false; //used for showing "snackbar" on top of actionbar
    private static final int MIN_DB_VERSION = 151;

    private SQLiteDatabase myDataBase;


    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public Database(Context context){
        super(context,getDbPath() + DB_NAME + ".db" , null, DB_VERSION);
        this.myContext = context;
    }


    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public Database(Context context, int useAPI) {
        super(context, getDbPath() + API_DB_NAME + ".db", null, DB_VERSION);
        this.myContext = context;
    }

    static public String getDbPath(){
        //The Android's default system path of your application database.
        String DB_PATH = getInternalFolder() + "databases/";
        Log.d("databasepath", DB_PATH + " mkdirs: " + mkDirs(DB_PATH));
        File[] files = new File(DB_PATH).listFiles();
        return DB_PATH;
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

    public static boolean isNewCommentaryVersion(){
        return Database.getVersionInDB(Settings.getUseAPI()) >= 266;
    }

    public static boolean isNewCommentaryVersionWithConnType(){
        return Database.getVersionInDB(Settings.getUseAPI()) >= 274;
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

        if(!Database.hasOfflineDB() && !Settings.getUseAPI()){ //There's no DB
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
        Log.d("databasepath", path + " makedirs:" + mkDirs(path));
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

    public static void deleteDatabase() {
        File oldDB = new File(getDbPath() + DB_NAME + ".db");
        if (oldDB.exists()) {
            Log.d("db","deleting");
            oldDB.delete();
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
                Database.createAPIdb();
                APIInstance = new Database(context.getApplicationContext(), 1);
                return APIInstance;
            }
        }
        return instance;
    }


    public static SQLiteDatabase getDB(){
        return getInstance(null).getReadableDatabase();
    }

    public static boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = getDbPath() + DB_NAME + ".db";
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(Exception e){
            GoogleTracker.sendException(e, "database doesn't exist");
            //database doesn't exist yet.

        }

        if(checkDB != null){
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transferring bytestream.
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
            cursor.close();
        }catch(Exception e){
            ;
        }
        return value;
    }

    public static void createAPIdb(){
        Log.d("api", "trying to create db");
        Database myDbHelper = new Database(MyApp.getContext());
        myDbHelper.getReadableDatabase();
        try {
            myDbHelper.unzipDatabase("API_UpdateForSefariaMobileDatabase.zip", Database.getDbPath(),true);
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

    public static void setIsDownloadingDatabase(boolean isDownloading){
        isDownloadingDatabase = isDownloading;
        versionNums[0] = null; //only need to change offline version
    }

    public static boolean getIsDownloadingDatabase(){
        return isDownloadingDatabase;
    }

    private static Integer [] versionNums = new Integer[] {null, null};
    public static int getVersionInDB(Boolean forAPI){
        if(forAPI == null){
            forAPI = Settings.getUseAPI();
        }
        int index = forAPI ? 1 : 0;
        if(versionNums[index] != null && !isDownloadingDatabase)
            return versionNums[index];

        int versionNum = getDBSetting("version", forAPI);
        if(versionNum == BAD_SETTING_GET)
            versionNum = -1;
        versionNums[index] = versionNum;
        return versionNum;
    }


    public void openDataBase() throws SQLException{

        //Open the database
        String myPath = getDbPath() + DB_NAME + ".db";
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
