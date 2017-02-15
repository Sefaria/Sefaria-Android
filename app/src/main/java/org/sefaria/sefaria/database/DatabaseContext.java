package org.sefaria.sefaria.database;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

import java.io.File;

/**
 * Created by nss on 2/9/17.
 */

class DatabaseContext extends ContextWrapper {

    private static final String DEBUG_CONTEXT = "DatabaseContext";
    private String internalDBPath;

    public DatabaseContext(Context base, String internalDBPath) {
        super(base);
        this.internalDBPath = internalDBPath;
    }

    @Override
    public File getDatabasePath(String name)  {
        String dbfile;
        if (Settings.getUseSDCard()) {
            File sdcard = Environment.getExternalStorageDirectory();
            dbfile = sdcard.getAbsolutePath() + File.separator + "databases" + File.separator + name;
        } else {
            dbfile = this.internalDBPath + name + ".db";
        }
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db" ;
        }

        File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }

        if (Log.isLoggable(DEBUG_CONTEXT, Log.WARN)) {
            Log.w(DEBUG_CONTEXT, "getDatabasePath(" + name + ") = " + result.getAbsolutePath());
        }

        return result;
    }


    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name,mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        if (Log.isLoggable(DEBUG_CONTEXT, Log.WARN)) {
            Log.w(DEBUG_CONTEXT, "openOrCreateDatabase(" + name + ",,) = " + result.getPath());
        }
        return result;
    }
}
