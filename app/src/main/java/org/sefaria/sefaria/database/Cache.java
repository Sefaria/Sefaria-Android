package org.sefaria.sefaria.database;


import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Util;

public class Cache{


    public static final boolean USE_CACHE_DEFAULT = true;

    private static final File path = MyApp.getContext().getCacheDir();
    private static final int MAX_FILE_SIZE = 307200;// ~300KB

    private static int maxCache = -1;
    private static final int MB = 1048576;

    private static int getMaxCache(){
        if(maxCache == -1){
            long totalSpace = Util.getInternalAvailableSpace();
            Log.d("cache", "totalSpace: " + totalSpace);
            if(totalSpace >5000l*MB){
                maxCache = 40*MB;
            }else if(totalSpace >512*MB){
                maxCache = 20*MB;
            }else if(totalSpace >256*MB){
                maxCache = 10*MB;
            }else if(totalSpace >128*MB){
                maxCache = 3*MB;
            }else {
                maxCache = MB;
            }
            Log.d("cache","maxCache: " + maxCache);
        }
        return maxCache;
    }

    public static boolean add(String url,String json, String data) {
        long time = System.currentTimeMillis();
        if(data.length()<1 || data.length() > MAX_FILE_SIZE){
            Log.d("cache", "bad filesize: " + data.length());
            return false;
        }

        try {
            Util.writeFile(urlToHashFile(url, json), data);
            Util.writeFile(urlToTimeFile(url,json),"" + time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    public static String getCache(String url,String json){
        try {
            Log.d("cache", "url:" + url);
            String filename = urlToHashFile(url,json);
            String data = Util.readFile(filename);
            if(data != null && data.length() >0){
                Log.d("cache", "Good Data api");
                return data;
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    private static String timeFile(String hashName){
        return path + "/" + hashName + ".time";
    }

    private static String jsonFile(String hashName){
        return path + "/" + hashName + ".json";
    }

    private static String urlToHashFile(String url,String json){
        String hashcode;
        if(json != null)
            hashcode = "" + url.hashCode() + json.hashCode();
        else
            hashcode = "" + url.hashCode();
        return path + "/" + hashcode + ".json";
    }

    private static String urlToTimeFile(String url,String json){
        String hashcode;
        if(json != null)
            hashcode = "" + url.hashCode() + json.hashCode();
        else
            hashcode = "" + url.hashCode();
        return path + "/" + hashcode + ".time";
    }


    private static void clearTooManyFiles(){
        for(int i=0;i<10;i++){//10 times just so it doesn't accidentally run for too long
            long size = 0;
            int fileCount = 0;
            for (File file : path.listFiles()) {
                if (file.isFile()) {
                    size += file.length();
                    fileCount++;
                }
            }
            if(size < getMaxCache())
                return;
            double maxSize = size/fileCount*.8;
            if(i == 9)
                maxSize = -1;
            clearBigFiles(maxSize);
        }
    }

    private static void clearBigFiles(double maxSize){
        for (File file : path.listFiles()) {
            if (file.isFile() && file.length() > maxSize) {
                String timeFile = file.getAbsolutePath().replaceAll("\\.json", ".time");
                file.delete();
                new File(timeFile).delete();
            }
        }
    }



    private static boolean clearExpiredCacheFile(String filename){
        filename = filename.replaceAll("\\.time", "").replaceAll("\\.json", "");
        //Log.d("cache", "filename cleared:" + filename);
        try {
            String time = Util.readFile(timeFile(filename));
            if (time.length() == 0)
                return false;
            //Log.d("cache", "filename:" + filename + "  time: " + time);
            long cachedTime = Long.valueOf(time);
            long currTime = System.currentTimeMillis();
            if (currTime - cachedTime > 6.048e+8) {//Greater than 1 week
                Log.d("cache", "deleting expired: " + filename + "...." + currTime + " - " + cachedTime);
                new File(jsonFile(filename)).delete();
                new File(timeFile(filename)).delete();
                return true;
            }
        }catch (IOException e1){
            //Log.w("cache", "unable to read:" + filename + "...." + e1.getMessage());
            new File(jsonFile(filename)).delete();
            return true;
        }
        return false;
    }

    public static void clearExpiredCache(){
        Cache cache = new Cache();
        cache.new ClearCache().execute();
    }

    private static void clearExpiredCacheTask(){
        Log.d("cache", "starting clearExpiredCacheTask");
        int size = 0;
        for (File file : path.listFiles()) {
            if (file.isFile()) {
                //Log.d("cache", "file: " + file.getName() + " " + file.length());
                if(file.getName().matches(".*\\.time")) {
                    continue;
                }
                clearExpiredCacheFile(file.getName());
            }
            else{
                Log.d("cache", "dir: " + file.getName());
            }
        }

        Log.d("cache", "total size: " + size + " currTime:" + System.currentTimeMillis());
        clearTooManyFiles();
        Log.d("cache", "finished clearExpiredCacheTask");
    }

    private class ClearCache extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            clearExpiredCacheTask();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }


}
