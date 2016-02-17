package org.sefaria.sefaria.database;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class API {
    final static String TEXT_URL = "http://www.sefaria.org/api/texts/";
    final static String COUNT_URL = "http://www.sefaria.org/api/counts/";
    final static String SEARCH_URL = "http://search.sefaria.org:788/sefaria/_search/";
    final static String LINK_URL = "http://staging.sefaria.org/api/links/";
    final static String LINK_ZERO_TEXT = "?text=0";
    final static String ZERO_CONTEXT = "&context=0";
    final static String ZERO_COMMENTARY = "&commentary=0";

    public final static String NO_TEXT_MESSAGE = "no such table: Texts";
    //TODO possibly add reference userID so sefaria can get some user data


    final static int STATUS_NONE = 0;
    final static int STATUS_GOOD = 1;
    final static int STATUS_ERROR = 2;

    private String data = "";
    private String url = "";
    private int status = STATUS_NONE;
    private boolean isDone = false;
    String sefariaData = null;
    final static int READ_TIMEOUT = 3000;
    final static int CONNECT_TIMEOUT = 3000;
    //TODO determine good times
    private static int useAPI = -1;

    public static void makeAPIErrorToast(Context context){
        Toast.makeText(context, "Problem getting data from Internet",Toast.LENGTH_SHORT).show();
    }


    //non-static methods
    private String fetchData(String urlString){
        String data = "";
        this.url = urlString;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.connect();
            InputStream stream = conn.getInputStream();
            data = convertStreamToString(stream);
            //TODO handle timeouts ... messages, or maybe increase timeout time, etc.
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d("ERROR", "malformed url");
            status = STATUS_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ERROR", "io exception");
            status = STATUS_ERROR;
        }
        return data;
    }


    public class APIException extends Exception{
        public APIException(){
            super();
        }
        private static final long serialVersionUID = 1L;
    }

    /**
     * //add ability to not wait for task (and return  so that api.data can be used later)
     * When looking for data you can call api.getData() which will only return after complete.
     * You can also manually check is done with api.isDone().
     * Then retrieve the data and status with api.getData() and api.getStatus().
     *
     * @param url
     * @return api;
     */
    public static API getDataFromURLAsync(String url){
        Log.d("api","URL:" + url);
        API api = new API();
        api.new GetDataTask().execute(url);
        return api;
    }

    /**
     * Waits for async task to finish.
     * Returns when api.data and api.status is available to use.
     */
    public void waitForComplete(){
        try {
            while(!isDone){
                //TODO maybe use something smarter to do this - make a timeout just in case
                Thread.sleep(10);
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
            isDone = true;
            //maybe change status
        }
    }

    /**
     *  Waits for data from Internet then returns data
     * @return data
     */
    public String getData(){
        waitForComplete();
        //TODO maybe check status to make sure it's ok
        return data;
    }

    /**
     *  Waits for data from Internet then returns status
     * @return status
     */
    public int getStatus(){
        waitForComplete();
        return status;
    }

    /**
     * true if it finished it's request from the web and false if it's still getting data.
     * @return isDone
     */
    public boolean isDone(){
        return isDone;
    }


    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }


    //static methods

    /**
     * This function will wait until it gets the data from the Internet to return.
     * It is possible that it will take a while if you are asking for lots of data or bad connection.
     * Read timeout is {@value #READ_TIMEOUT}ms and connection timeout is {@value #CONNECT_TIMEOUT}ms.
     *
     * @param url
     * @return data as String from url request
     * @throws APIException
     */
    public static String getDataFromURL(String url) throws APIException{
        API api = getDataFromURLAsync(url);//creating an instance of api which will fetch data
        Cache cache = null;//Cache.getCache(url);
        String data;

        data = api.getData();//waiting for data to be returned from internet
        Log.d("api","in getDataFromURL: data length: " + data.length() );
        if(true)
            return data;

        if(cache != null  &&  !cache.isExpired()){
            data = cache.data;
        }
        else{
            data = api.getData();//waiting for data to be returned from intern
            if(api.status != API.STATUS_GOOD){
                if(cache != null)
                    data  = cache.data;
                else{
                    Log.e("api","throwing apiexception");
                    throw api.new APIException();
                }
            }
        }


        Log.d("api","in getDataFromURL: data length: " + data.length() );
        return data;
    }


    private static List<Text> parseJSON(String in,int [] levels, int bid) {
        List<Text> textList = new ArrayList<Text>();

        try {
            JSONObject jsonData = new JSONObject(in);
            //Log.d("api", "jsonData:" + jsonData.toString());

            //TODO make work for 1 and 3 (or more) levels of depth (exs. Hadran, Arbaah Turim)
            JSONArray textArray = jsonData.getJSONArray("text");
            JSONArray heArray = jsonData.getJSONArray("he");


            int maxLength = Math.max(textArray.length(),heArray.length());
            //Log.d("api",textArray.toString() + " " + heArray.toString());
            for (int i = 0; i < maxLength; i++) {
                //get the texts if i is less it's within the length (otherwise use "")
                String enText = "";
                try{
                    enText = textArray.getString(i);
                }catch(JSONException e){
                    Log.d("api",e.toString());
                }
                String heText = "";
                try{
                    heText = heArray.getString(i);
                }catch(JSONException e){
                    Log.d("api",e.toString());
                }
                Text text = new Text(enText, heText);

                text.bid = bid;
                for(int j=0;j<levels.length;j++){
                    text.levels[j] = levels[j]; //TODO get full level info in there
                }
                text.levels[0] = i+1;


                textList.add(text);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("api", "error processing json data");
        }
        return textList;

    }

    /**
     *
     * @return false if there's a Text table in the db. true if not (and should be using API)
     */
    public static boolean useAPI(){
        if(useAPI == 1) return true;
        if(useAPI == 0) return false;
        //TODO maybe check the settings table instead (api should be 1)
        try{
            Database dbHandler = Database.getInstance();
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            Cursor cursor = db.query(Text.TABLE_TEXTS, null, "_id" + "=?",
                    new String[] { String.valueOf(1) }, null, null, null, null);
            Log.d("api", "got here without problems" + cursor);
            useAPI = 0;
            return false;
        }catch(Exception e){
            useAPI = 1;
            return true;
        }
    }


    public static ArrayList<Text> getSearchResults(String query,String[] filterArray, int from, int offset) throws APIException {
        ArrayList<Text> texts = new ArrayList<Text>();
        String url = SEARCH_URL + "?" + "&from=" +from + "&offset=" + offset + "q=" + Uri.encode(query) ;
        String data = getDataFromURL(url);
        try {
            JSONObject jsonData = new JSONObject(data);
            JSONArray hits = jsonData.getJSONObject("hits").getJSONArray("hits");
            for(int i=0;i<hits.length();i++){
                JSONObject hit = hits.getJSONObject(i);
                if(!hit.getString("_type").equals("text"))
                    continue;//TODO make it such that this won't prevent further searches
                JSONObject source = hit.getJSONObject("_source");
                String content = source.getString("content");
                if(content.length() > 103)
                    content = content.substring(0, 100) + "...";
                content = "<big><b>" + source.getString("ref") + "</b></big> " + content;
                String lang = source.getString("lang");
                Text text = null;
                if(lang.equals("he"))
                    text = new Text("", content);
                else //lang is en
                    text = new Text(content,"");
                text.bid = 2; //TODO this needs more real info
                text.levels[0] = 1;
                text.levels[1] = 1;
                //z2Log.d("api",text.toString());
                texts.add(text);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return texts;

    }


    /**
     * Get links that are tied to the whole chapter, but not to a specific verse.
     * @param dummyChapText
     * @param limit
     * @param offset
     * @return
     * @throws APIException
     */
    public static List<Text> getChapLinks(Text dummyChapText, int limit, int offset) {
        List<Text> texts = new ArrayList<Text>();
        String place = createPlace(Book.getTitle(dummyChapText.bid), dummyChapText.levels);
        String url = LINK_URL + place + LINK_ZERO_TEXT;

        //String data = getDataFromURL(url);
        //TODO parse
        //TODO make async getting links

        return texts;

    }

    public static List<Text> getLinks(Text text, LinkFilter linkFilter) {
        List<Text> texts = new ArrayList<Text>();

        return texts;
    }

    /**
     *
     * @param bookTitle
     * @param levels
     * @return chapList (a list of all the chapter numbers)
     * @throws APIException
     */
    public static ArrayList<Integer> getChaps(String bookTitle, int [] levels) throws APIException{
        String place = bookTitle.replace(" ", "_");
        String url = COUNT_URL + place;
        String data = getDataFromURL(url);
        Log.d("api", "getChaps data.len: " + data.length());
        ArrayList<Integer> chapList = new ArrayList<Integer>();
        try {
            JSONObject jsonData = new JSONObject(data);
            JSONArray counts = jsonData.getJSONObject("_all").getJSONArray("availableTexts");
            for(int i=levels.length-1;i>=0;i--){
                if(levels[i] == 0)
                    continue;
                counts = counts.getJSONArray(levels[i]-1);//-1 b/c the first chap of levels is 1, the array is zero indexed
            }
            int totalChaps = counts.length();
            for(int i=0;i<totalChaps;i++){
                try{
                    if(counts.getJSONArray(i).length()>0)
                        chapList.add(i+1);
                }catch(JSONException e){//most likely it's b/c it only has one level
                    chapList.add(i+1);
                }
            }
        }catch(Exception e){
            Log.e("api","Error: " + e.toString());
        }
        return chapList;

    }


    static private String createPlace(String bookTitle, int[] levels){
        String place = bookTitle.replace(" ", "_"); //the api call doesn't have spaces

        for(int i= levels.length-1;i>=0;i--){
            //TODO error check on bad input (like [1,0,1] which doesn't make any sense)
            if(levels[i]== 0)
                continue;
            place += "." + levels[i];
        }
        return place;
    }

    /**
     * Will only return after response from web is complete.
     * @param bookTitle
     * @param levels
     * @return textList
     * @throws APIException
     */
    static public List<Text> getTextsFromAPI(String bookTitle, int[] levels) throws APIException{ //(String booktitle, int []levels)
        Log.d("API","getTextsFromAPI called");
        String place = createPlace(bookTitle, levels);
        String completeUrl = TEXT_URL + place + "?" + ZERO_CONTEXT + ZERO_COMMENTARY;
        String data = getDataFromURL(completeUrl);
        Log.d("API","getTextsFromAPI got data.size)" + data.length());
        List<Text> textList = parseJSON(data,levels,Book.getBid(bookTitle));
        //for(int i=0;i<levels.length;i++)
          //  Log.d("api", "in getTextsFromAPI: levels" + i + ". "  + levels[i] );
        Log.d("api", "in getTextsFromAPI: api.textlist:" + textList.size());
        return textList;
    }

    private class GetDataTask extends AsyncTask <String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = fetchData(params[0]);
            data = result;//put into data so that the static function can pull the data
            if(status == STATUS_NONE && data.length() >0)
                status = STATUS_GOOD;
            isDone = true;

            if(status == STATUS_GOOD && data.length() >0){
                ;//Cache.add(url, data); //cache data for later
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            //TODO: FILL IN:
            //Log.d("api", "in onPostExecute: data length: " + result.length());
            //How about using intent to push the List<Text> to Text.java using Parcelable, as Text class already implements it? (ES)
            //Intent intent = new Intent();
        }
    }


}

