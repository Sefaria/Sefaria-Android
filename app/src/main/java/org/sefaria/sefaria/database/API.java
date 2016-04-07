package org.sefaria.sefaria.database;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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

    private String jsonString; //if null, no json to send. if not null send this jsonObject along with url request
    final static int READ_TIMEOUT = 10000;
    final static int CONNECT_TIMEOUT = 10000;
    final static int SPIN_TIMEOUT = 8000;
    //TODO determine good times

    public static void makeAPIErrorToast(Context context){
        Toast.makeText(context, "Problem getting data from Internet",Toast.LENGTH_SHORT).show();
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

        long timeForAPI = System.currentTimeMillis() - startTime;
        if(status == STATUS_NONE && data.length() >0) {
            status = STATUS_GOOD;
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

        public static PlaceRef getPlace(String place) throws APIException, Book.BookNotFoundException {
            PlaceRef placeRef = new PlaceRef();
            Log.d("API", "place:" + place);
            placeRef.book = null;// = new Book(spots[0]);
            place = place.replaceAll("_", " ");
            List<Book> books = Book.getAll();
            for (Book tempBook : books) {
                String newPlace = place.replaceFirst("^" + tempBook.title + "\\s*", "");
                if (!newPlace.equals(place)) {
                    placeRef.book = tempBook;
                    place = newPlace;
                    break;
                }
            }
            Log.d("API", "place:" + place);
            String[] spots = place.split("[\\.:]|(,\\s)");

            if (placeRef.book == null)
                throw (new Book()).new BookNotFoundException();


            if (spots.length == 0) {
                return placeRef;
            }

            try {
                placeRef.node = placeRef.book.getTOCroots().get(0);
                for (int i = 0; i < spots.length; i++) {
                    String spot = spots[i];
                    Log.d("API", "spot1:" + spot);
                    if (spot.length() == 0)
                        continue;
                    Node tempNode = placeRef.node.getChild(spot);
                    Log.d("API", "tempNode: " + tempNode);
                    if (tempNode == null) {
                        //it's a most likely a final number (such as a verse number) so that's why there's no Node for it
                        //so, lets get the firstDescendant (which should just be the node itself, but just in case lets do it this way)
                        placeRef.node = placeRef.node.getFirstDescendant(false);
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
                    placeRef.node = placeRef.node.getFirstDescendant(false);
            }
            return placeRef;
        }
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
        API api = new API();
        try{//try to get the data with the current thread.  This will only work if it's on a background thread.
            api.jsonString = jsonString;
            data = api.fetchData(url);
        }catch (NetworkOnMainThreadException e){//if it was running on main thread, create our own background thread to handle it
            api = getDataFromURLAsync(url,jsonString);//creating an instance of api which will fetch data
            api.alreadyDisplayedURL = true;
            data = api.getData();//waiting for data to be returned from internet
        }

        Log.d("api","in getDataFromURL: data length: " + data.length() );

        if(api.status != API.STATUS_GOOD){
            Log.e("api","throwing apiexception");
            throw api.new APIException();
        }

        if(!useCache)
            return data;
        /*
        Cache cache = null;//Cache.getCache(url);
        if(cache != null  &&  !cache.isExpired()){
            data = cache.data;
        }
        else{
            data = api.getData();//waiting for data to be returned from intern

        }
        */

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

    private static String removeEmpty(String str){
        if(str.equals("[]"))
            return "";
        else
            return str;
    }

    public static List<Text> getLinks(Text orgText, LinkFilter linkFilter) throws APIException {
        Log.d("API.Link","got starting LinksAPI");
        List<Text> texts = new ArrayList<>();
        String place = orgText.getURL(false, false);
        String url = LINK_URL +place;
        String data = getDataFromURL(url);
        Log.d("API.Link","got data");
        Book book;
        try {
            book = new Book(orgText.bid);
        } catch (Book.BookNotFoundException e) {
            return texts;
        }
        if(data.length()==0)
            return texts;
        List<Text> commentaries = new ArrayList<>();

        String commentOn = " on " + book.title;
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
                    if(category.equals("Commentary"))
                        commentaries.add(tempText);
                    else
                        texts.add(tempText);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        Collections.sort(commentaries,compareTexts);
        Collections.sort(texts, compareTexts);
        texts.addAll(0,commentaries);

        Log.d("API.Link","finished LinksAPI");
        return texts;
    }

    static Comparator<Text> compareTexts = new Comparator<Text>() {
        @Override
        public int compare(Text a, Text b) {
            //only sorting on bid. Within same book using sable sort to keep order
            return a.bid - b.bid;
        }
    };

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
        protected String doInBackground(String... params) {
            String result = fetchData(params[0]);
            data = result;//put into data so that the static function can pull the data
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

