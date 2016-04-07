package org.sefaria.sefaria.database;


import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Util;

public class Cache{

    private static final File path = MyApp.getContext().getCacheDir();
    private static final int MAX_FILE_SIZE = 307200;//300KB

    public static boolean add(String url,String json, String data) {
        long time = System.currentTimeMillis();
        if(data.length()<1 || data.length() > MAX_FILE_SIZE){
            Log.d("cache", "bad filesize: " + data.length());
            return false;
        }

        FileOutputStream outputStream;
        try {
            Util.writeFile(urlToHashFile(url,json),data);
            Util.writeFile(urlToTimeFile(url,json),""+ time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    public static String getCache(String url,String json){
        try {
            long currTime = System.currentTimeMillis();
            String time = Util.readFile(urlToTimeFile(url,json));
            if(time.length() == 0)
                return null;
            Log.d("cache", "url:" + url + "  --- time:" + time);
            long cachedTime = Long.valueOf(time);
            if(currTime - cachedTime > 1.21e+9){//GREATER THAN 2 WEEKS
                Log.d("cache", "expired:" + currTime + " - " + cachedTime);
                return null;
            }else if(currTime - cachedTime > 604800){//Greater than 1 week
                Log.d("cache", "week old:" + currTime + " - " + cachedTime);
                String filename = urlToHashFile(url,json);
                String data = Util.readFile(filename);
                new File(filename).delete();
                new File(urlToTimeFile(url,json)).delete();
                return data;
            }
            else{
                String filename = urlToHashFile(url,json);
                return Util.readFile(filename);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
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
        return path + "/" + "time_" + hashcode;
    }


}
