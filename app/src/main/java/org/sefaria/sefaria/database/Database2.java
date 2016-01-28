package org.sefaria.sefaria.database;
import org.sefaria.sefaria.MyApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;



public class Database2 extends SQLiteOpenHelper{

    //private static final int DATABASE_VERSION = 2;
    //private static final String DATABASE_NAME = "BetaMidrashDB";
    private static Database2 sInstance;




    public static String DB_NAME = "UpdateForSefariaMobileDatabase";
    static int DB_VERSION = 1;

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public Database2(Context context) {
        super(context, getDbPath() + DB_NAME + ".db", null, DB_VERSION);
        this.myContext = context;
    }

    static public String getDbPath(){
        //The Android's default system path of your application database.
        String DB_PATH = getInternalFolder() + "databases/";
        mkDirs(DB_PATH);
        return DB_PATH;
    }
    static private void mkDirs(String path){
        File folder = new File(path);
        folder.mkdirs();
    }


    static public String getInternalFolder(){
        String path = MyApp.getContext().getExternalFilesDir(null).toString() + "/";
        //String path = Environment.getExternalStorageDirectory() + "/" +  MyApp.getAppPackageName() + "/data/";
        Log.d("Database2", "State:" + Environment.getExternalStorageState());
        mkDirs(path);
        Log.d("Database2", path);
        Log.d("Database2", "_" + Environment.getExternalStorageDirectory().toString());
        Log.d("Database2", "_" + Environment.getDataDirectory().toString());
        /*
        //old regular code
         path = "/data/data/" + MyApp.getAppPackageName() + "/";
         */
        return path;

        //return INTERNAL_FOLDER;
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
    }

    public static Database2 getInstance() {
        Context context = MyApp.getContext();
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new Database2(context.getApplicationContext());
        }
        return sInstance;
    }

    public static boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = getDbPath() + DB_NAME + ".db";
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(Exception e){
            MyApp.sendException(e, "database does't exist");
            //database does't exist yet.

        }

        if(checkDB != null){
            checkDB.close();
        }

        return checkDB != null ? true : false;
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

    public static void createAPIdb(){
        Database2 myDbHelper = new Database2(MyApp.currActivityContext);
        Log.d("api", "trying to create db");
        myDbHelper.getReadableDatabase();
        try {
            myDbHelper.unzipDatabase("UpdateForSefariaMobileDatabase.zip.jar", Database2.getDbPath(),true);
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

    public static int getVersion(){
        int versionNum = -1;
        try{
            Database2 dbHandler = Database2.getInstance();
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            Cursor cursor = db.query("Settings", null, "_id" + "=?",
                    new String[] { "version" }, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()){
                versionNum = cursor.getInt(1);
            }
        }catch(Exception e){
            versionNum = -1;
        }
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
