package org.sefaria.sefaria.database;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.Toast;

public class API {
    final static String TEXT_URL = "http://www.sefaria.org/api/texts/";
    final static String COUNT_URL = "http://www.sefaria.org/api/counts/";
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


    private boolean useCache = Cache.USE_CACHE_DEFAULT;

    private String jsonString; //if null, no json to send. if not null send this jsonObject along with url request
    final static int READ_TIMEOUT = 3000;
    final static int CONNECT_TIMEOUT = 3000;
    final static int SPIN_TIMEOUT = 5000;

    final static int READ_TIMEOUT_LONG = 10000;
    final static int CONNECT_TIMEOUT_LONG = 10000;
    final static int READ_TIMEOUT_SHORT = 1500;
    final static int CONNECT_TIMEOUT_SHORT = 1500;
    private TimeoutType timeoutType;
    public enum TimeoutType {
        REG,LONG,SHORT
    }
    //TODO determine good times

    public static void makeAPIErrorToast(Context context) {
        String extraString = MyApp.getRString(R.string.consider_downloading);
        makeAPIErrorToast(context,extraString);
    }

    public static void makeAPIErrorToast(Context context, String extraString){
        String message = MyApp.getRString(R.string.problem_internet);
        if(extraString != null && extraString.length() >0)
            message  += " - " + extraString;
        try {
            Toast.makeText(context,message, Toast.LENGTH_LONG).show();
        }catch (Exception e){
            GoogleTracker.sendException(e,"API toast");
            e.printStackTrace();
        }
    }


    //non-static methods
    private String fetchData(String urlString){
        String data = "";
        this.url = urlString;
        long startTime = System.currentTimeMillis();
        if(!alreadyDisplayedURL)
            Log.d("api","URL: " + url);

        if(Downloader.getNetworkStatus() == Downloader.ConnectionType.NONE){
            this.status = STATUS_ERROR;
            return data;
        }
        int readTimeout = READ_TIMEOUT;
        int connectionTimeout = CONNECT_TIMEOUT;
        if(timeoutType == TimeoutType.LONG){
            readTimeout = READ_TIMEOUT_LONG;
            connectionTimeout = CONNECT_TIMEOUT_LONG;
        }else if(timeoutType == TimeoutType.SHORT){
            readTimeout = READ_TIMEOUT_SHORT;
            connectionTimeout = CONNECT_TIMEOUT_SHORT;
        }

        try {
            if(jsonString == null) {//!use JSON post
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(readTimeout);
                conn.setConnectTimeout(connectionTimeout);
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
                connection.setReadTimeout(readTimeout);
                connection.setConnectTimeout(connectionTimeout);
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

        long timeForAPI = System.currentTimeMillis() - startTime;
        if(status == STATUS_NONE && data.length() >0) {
            status = STATUS_GOOD;
            if(useCache)
                Cache.add(url,jsonString,data); //cache data for later
            GoogleTracker.sendEvent(GoogleTracker.CATEGORY_API_REQUEST, "good", timeForAPI);
        }else{
            GoogleTracker.sendEvent(GoogleTracker.CATEGORY_API_REQUEST, "error", timeForAPI);
        }

        return data;
    }

    public static class PlaceRef {
        public Book book;
        public Node node;
        public Text text;

        PlaceRef() {
        }

        public static PlaceRef getPlace(String place, Book book) throws APIException, Book.BookNotFoundException {
            PlaceRef placeRef = new PlaceRef();
            Log.d("API", "place:" + place);
            place = place.replace("_", " ");
            if (book == null) {
                String title = place.replaceAll("[\\s\\.][0-9]+.*$", "");
                Log.d("api", "title:" + title);
                if (title.contains(",")) {
                    book = null;
                } else {
                    try {
                        book = new Book(title);
                        Log.d("api","Found book");
                    } catch (Book.BookNotFoundException e) {
                        book = null;
                    }
                }
            }

            if (book != null){
                placeRef.book = book;
                place = place.replaceFirst("^" + book.title + "\\s*", "");
            } else{
                List<Book> books = Book.getAll();
                for (Book tempBook : books) {
                    String newPlace = place.replaceFirst("^" + tempBook.title + "\\s*", "");
                    if (!newPlace.equals(place)) {
                        placeRef.book = tempBook;
                        place = newPlace;
                        break;
                    }
                }
            }


            //Log.d("API", "place:" + place);
            String[] spots = place.split("[\\.:]|(,\\s)");

            if (placeRef.book == null)
                throw new Book.BookNotFoundException();


            if (spots.length == 0) {
                return placeRef;
            }

            try {
                placeRef.node = placeRef.book.getTOCroots().get(0);
                for (int i = 0; i < spots.length; i++) {
                    String spot = spots[i];
                    //Log.d("API", "spot1:" + spot);
                    if (spot.length() == 0)
                        continue;
                    Node tempNode = placeRef.node.getChild(spot);
                    //Log.d("API", "tempNode: " + tempNode);
                    if (tempNode == null) {
                        //it's a most likely a final number (such as a verse number) so that's why there's no Node for it
                        //so, lets get the firstDescendant (which should just be the node itself, but just in case lets do it this way)
                        placeRef.node = placeRef.node.getFirstDescendant();
                        List<Text> texts = placeRef.node.getTexts();
                        int num = Util.convertDafOrIntegerToNum(spot);
                        for (Text tempText : texts) {
                            if (tempText.levels[0] == num) {
                                placeRef.text = tempText;
                                //Log.d("API","textLevel: " + num);
                                break;
                            }
                        }
                        break;
                    }
                    placeRef.node = tempNode;
                }
            }catch (Exception e){
                if(placeRef.node != null)
                    placeRef.node = placeRef.node.getFirstDescendant();
            }
            return placeRef;
        }
    }





    public class APIException extends Exception{
        public APIException() {
            super("API exception");
        }
        public APIException(String message){
            super(message);
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
        return getDataFromURL(url,null,Cache.USE_CACHE_DEFAULT,TimeoutType.REG);
    }

    public static String getDataFromURL(String url, TimeoutType timeoutType) throws APIException{
        return getDataFromURL(url,null,Cache.USE_CACHE_DEFAULT,timeoutType);
    }


    public static String getDataFromURL(String url, boolean useCache) throws APIException{
        return getDataFromURL(url,null,useCache,TimeoutType.REG);
    }



    /**

     * @param url
     * @return data as String from url request
     * @throws APIException
     */

    /**
     *
     * This function will wait until it gets the data from the Internet to return.
     * It is possible that it will take a while if you are asking for lots of data or bad connection.
     * Read timeout is {@value #READ_TIMEOUT}ms and connection timeout is {@value #CONNECT_TIMEOUT}ms.
     *
     *
     * @param url the URL to get the data from
     * @param jsonString the jsonString to send or null if you don't need to send JSON
     * @param useCache if the cache is available
     * @param timeoutType if you really want to wait a while if it's a bad connection, or short if you barley want to try
     * @return string data
     * @throws APIException
     */
    public static String getDataFromURL(String url, String jsonString, boolean useCache, TimeoutType timeoutType) throws APIException{
        String data;
        if(useCache){
            data = Cache.getCache(url,jsonString);
            if(data != null && data.length()>0)
                return data;
        }

        API api = new API();
        try{//try to get the data with the current thread.  This will only work if it's on a background thread.
            api.timeoutType = timeoutType;
            api.jsonString = jsonString;
            api.useCache = useCache;
            data = api.fetchData(url);
        }catch (NetworkOnMainThreadException e){//if it was running on main thread, create our own background thread to handle it
            api = getDataFromURLAsync(url,jsonString);//creating an instance of api which will fetch data
            api.alreadyDisplayedURL = true;
            api.useCache = useCache;
            data = api.getData();//waiting for data to be returned from internet
        }

        Log.d("api","in getDataFromURL: data length: " + data.length() );

        if(api.status != API.STATUS_GOOD){
            Log.e("api","throwing apiexception");
            throw api.new APIException();
        }
        return data;
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



    private class GetDataTask extends AsyncTask <String, Void, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            String result = fetchData(params[0]);
            data = result;//put into data so that the static function can pull the data
            isDone = true;

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }


}

