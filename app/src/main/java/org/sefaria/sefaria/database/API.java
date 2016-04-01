package org.sefaria.sefaria.database;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.Util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.Toast;

public class API {
    final static String TEXT_URL = "http://www.sefaria.org/api/texts/";
    final static String COUNT_URL = "http://www.sefaria.org/api/counts/";
    public final static String SEARCH_URL = "http://search.sefaria.org:788/sefaria/_search/";
    final static String LINK_URL = "http://www.sefaria.org/api/links/";
    final static String LINK_ZERO_TEXT = "?with_text=0";
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
    private boolean alreadyDisplayedURL = false;

    private String jsonString; //if null, no json to send. if not null send this jsonObject along with url request
    final static int READ_TIMEOUT = 10000;
    final static int CONNECT_TIMEOUT = 10000;
    final static int SPIN_TIMEOUT = 8000;
    //TODO determine good times
    public static int useAPI = -1;

    public static void makeAPIErrorToast(Context context){
        Toast.makeText(context, "Problem getting data from Internet",Toast.LENGTH_SHORT).show();
    }


    //non-static methods
    private String fetchData(String urlString){
        String data = "";
        this.url = urlString;
        if(!alreadyDisplayedURL)
            Log.d("api","URL: " + url);
        try {
            if(jsonString == null) {//!use JSON post
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.connect();
                InputStream stream = conn.getInputStream();
                data = convertStreamToString(stream);
            }else{
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                //connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setUseCaches (false);

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
                //wr.writeBytes(otherParametersUrServiceNeed);

                byte[] buf = jsonString.getBytes("UTF-8");
                wr.write(buf, 0, buf.length);
                wr.flush();
                wr.close();
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.connect();
                InputStream stream = connection.getInputStream();
                data = convertStreamToString(stream);
            }


            //TODO handle timeouts ... messages, or maybe increase timeout time, etc.
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d("API", "malformed url");
            status = STATUS_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("API", "io exception");
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
    public static API getDataFromURLAsync(String url, String jsonString){
        API api = new API();
        api.jsonString = jsonString;
        api.new GetDataTask().execute(url);
        return api;
    }

    /**
     * Waits for async task to finish.
     * Returns when api.data and api.status is available to use.
     */
    public void waitForComplete(){
        try {
            long startTime = System.currentTimeMillis();
            while(!isDone){
                if(System.currentTimeMillis() - startTime > SPIN_TIMEOUT){
                    Log.e("api","Spin time out");
                    isDone = true;
                    status = STATUS_ERROR;
                }
                Thread.sleep(20);
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

    public static String getDataFromURL(String url) throws APIException{
        return getDataFromURL(url,true);
    }



    public static String getDataFromURL(String url, boolean useCache) throws APIException {
        return getDataFromURL(url,null,useCache);
    }

    /**
     * This function will wait until it gets the data from the Internet to return.
     * It is possible that it will take a while if you are asking for lots of data or bad connection.
     * Read timeout is {@value #READ_TIMEOUT}ms and connection timeout is {@value #CONNECT_TIMEOUT}ms.
     *
     * @param url
     * @return data as String from url request
     * @throws APIException
     */
    public static String getDataFromURL(String url, String jsonString, boolean useCache) throws APIException{
       String data;
        try{//try to get the data with the current thread.  This will only work if it's on a background thread.
            API api = new API();
            api.jsonString = jsonString;
            data = api.fetchData(url);
        }catch (NetworkOnMainThreadException e){//if it was running on main thread, create our own background thread to handle it
            API api = getDataFromURLAsync(url,jsonString);//creating an instance of api which will fetch data
            api.alreadyDisplayedURL = true;
            data = api.getData();//waiting for data to be returned from internet
        }

        Log.d("api","in getDataFromURL: data length: " + data.length() );
        if(!useCache)
            return data;
        /*
        Cache cache = null;//Cache.getCache(url);
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
        */

        return data;
    }


    private static List<Text> parseJSON(String in,int [] levels, int bid) {
        List<Text> textList = new ArrayList<>();
        if(in.length()==0)
            return textList;

        try {
            JSONObject jsonData = new JSONObject(in);
            //Log.d("api", "jsonData:" + jsonData.toString());

            //TODO make work for 1 and 3 (or more) levels of depth (exs. Hadran, Arbaah Turim)
            JSONArray textArrayBig = jsonData.getJSONArray("text");
            JSONArray heArrayBig = jsonData.getJSONArray("he");


            int stop = Math.max(textArrayBig.length(), heArrayBig.length());

            int startLevel1 = levels[0];
            if(startLevel1 == 0)
                startLevel1 = 1;

            for (int k = 0; k < stop; k++) {
                JSONArray textArray;
                JSONArray heArray;
                try {
                    textArray = textArrayBig.getJSONArray(k);
                    heArray = heArrayBig.getJSONArray(k);
                } catch (JSONException e1) {
                    Log.d("API","didn't find sub arrays in text");
                    textArray = textArrayBig;
                    heArray = heArrayBig;
                    stop = 0;
                }

                int maxLength = Math.max(textArray.length(), heArray.length());
                //Log.d("api",textArray.toString() + " " + heArray.toString());
                for (int i = 0; i < maxLength; i++) {
                    //get the texts if i is less it's within the length (otherwise use "")
                    String enText = "";
                    try {
                        enText = textArray.getString(i);
                    } catch (JSONException e) {
                        Log.d("api", e.toString());
                    }
                    String heText = "";
                    try {
                        heText = heArray.getString(i);
                    } catch (JSONException e) {
                        Log.d("api", e.toString());
                    }
                    Text text = new Text(enText, heText,bid,null);
                    for (int j = 0; j < levels.length; j++) {
                        text.levels[j] = levels[j]; //TODO get full level info in there
                    }

                    //only do it at the 2nd level, but currently this can only haddle at this level, but can't handle 3 levels of depth in a ref.
                    text.levels[1] += k;

                    text.levels[0] = i + startLevel1;


                    textList.add(text);
                }
                startLevel1 = 1;
            }
        }catch(JSONException e){
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
                    new String[]{String.valueOf(1)}, null, null, null, null);
            Log.d("api", "got here without problems" + cursor);
            useAPI = 0;
            return false;
        }catch(Exception e){
            useAPI = 1;
            return true;
        }
    }

/*
    public static ArrayList<Text> getSearchResults(String query,String[] filterArray, int from, int offset) throws APIException {
        Log.d("Searching", "starting api");
        ArrayList<Text> texts = new ArrayList<>();
        String url = SEARCH_URL ;//+ "?" + "&from=" +from + "&offset=" + offset + "q=" + Uri.encode(query) ;
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

        Log.d("Searching", "finishing api");
        return texts;

    }

*/
    /**
     * Get links that are tied to the whole chapter, but not to a specific verse.
     * @param dummyChapText
     * @param limit
     * @param offset
     * @return
     * @throws APIException
     */
    public static List<Text> getChapLinks(Text dummyChapText, int limit, int offset) {
        List<Text> texts = new ArrayList<>();
        String place = createPlace(Book.getTitle(dummyChapText.bid), dummyChapText.levels);
        String url = LINK_URL + place + LINK_ZERO_TEXT;

        //String data = getDataFromURL(url);
        //TODO parse
        //TODO make async getting links

        return texts;

    }

    private static String removeEmpty(String str){
        if(str.equals("[]"))
            return "";
        else
            return str;
    }

    public static List<Text> getLinks(Text text, LinkFilter linkFilter) throws APIException {
        Log.d("API.Link","got starting LinksAPI");
        List<Text> texts = new ArrayList<>();
        String place = text.getURL(false, false);
        String url = LINK_URL +place;
        String data = getDataFromURL(url);
        Log.d("API.Link","got data");
        Book book = new Book(text.bid);
        List<Text> textList = new ArrayList<>();
        if(data.length()==0)
            return textList;

        try {
            JSONArray linksArray = new JSONArray(data);
            //Log.d("api", "jsonData:" + jsonData.toString());
            for(int i=0;i<linksArray.length();i++){
                JSONObject jsonLink = linksArray.getJSONObject(i);
                String enTitle = jsonLink.getString("index_title");
                String category = jsonLink.getString("category");
                String ref = jsonLink.getString("ref");
                if(     linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL ||
                        (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT && category.equals(linkFilter.enTitle))||
                        (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK && enTitle.equals(linkFilter.enTitle))
                         ){
                    Text tempText = new Text(removeEmpty(jsonLink.getString("text")),removeEmpty(jsonLink.getString("he")),Book.getBid(enTitle),ref);
                    texts.add(tempText);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d("API.Link","finished LinksAPI");
        return texts;
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


    /*
     * Will only return after response from web is complete.
     * @param bookTitle
     * @param levels
     * @return textList
     * @throws APIException

    static public List<Text> getTextsFromAPI1(String bookTitle, int[] levels) throws APIException{ //(String booktitle, int []levels)
        Log.d("API","getTextsFromAPI called");
        String place = createPlace(bookTitle, levels);
        String completeUrl = TEXT_URL + place + "?" + ZERO_CONTEXT + ZERO_COMMENTARY;
        String data = getDataFromURL(completeUrl);
        Log.d("API","getTextsFromAPI got data.size:" + data.length());
        List<Text> textList = parseJSON(data,levels,Book.getBid(bookTitle));
        //for(int i=0;i<levels.length;i++)
          //  Log.d("api", "in getTextsFromAPI: levels" + i + ". "  + levels[i] );
        Log.d("api", "in getTextsFromAPI: api.textlist:" + textList.size());
        return textList;
    }
    */

    static public List<Text> getTextsFromAPI2(Node node) throws APIException{ //(String booktitle, int []levels)
        Log.d("API","getTextsFromAPI2 called");
        String completeUrl = TEXT_URL + node.getPath(Util.Lang.EN,true,true,true) + "?" + ZERO_CONTEXT + ZERO_COMMENTARY;

        String data = getDataFromURL(completeUrl);
        Log.d("API","getTextsFromAPI got data.size:" + data.length());
        Log.d("API","Node.levels:" + node.getLevels());
        List<Text> textList = parseJSON(data,node.getLevels(),node.getBid());
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

