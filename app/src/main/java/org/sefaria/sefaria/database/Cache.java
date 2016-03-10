package org.sefaria.sefaria.database;


import java.io.File;

import java.io.FileOutputStream;

import android.content.Context;

import org.sefaria.sefaria.MyApp;

public class Cache{

    public Cache() {
    }

    public String url;
    public String data;
    public int time;

    public static boolean add(String url, String data) {
        if(true) return false;
        int time = 123;

        File path = MyApp.getContext().getCacheDir();
        FileOutputStream outputStream;
        try {
            outputStream = MyApp.getContext().openFileOutput(path+"/" + urlToHashFile(url), Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.flush();
            outputStream.close();

            outputStream = MyApp.getContext().openFileOutput(path+"/" + urlToTimeFile(url), Context.MODE_PRIVATE);
            outputStream.write((""+ time).getBytes());
            outputStream.flush();
            outputStream.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public static Cache getCache(String url){
        Cache cache = new Cache();
        cache.url = url;
        cache.data = "";
        cache.time = 0;
        return null;//TOdo change
        //return cache;
    }

    public boolean isExpired(){
        if(time <10){
            return false;
        }
        else
            return true;
    }

    private static String urlToHashFile(String url){
        return ""+url.hashCode()+ ".json";
    }
    private static String urlToTimeFile(String url){
        return "time_"+url.hashCode();
    }


}
