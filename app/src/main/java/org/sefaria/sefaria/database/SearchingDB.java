package org.sefaria.sefaria.database;

import java.util.BitSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

public class SearchingDB {


    final private static int CHUNK_SIZE = 500;
    final private static int VERSE_COUNT = 1000000;
    final private static int CHUNK_COUNT = VERSE_COUNT/CHUNK_SIZE;
    final private static int WORD_COUNT = 25000000;
    final private static int BITS_PER_PACKET = 24;
    final private static int PACKET_SIZE = 32;
    final private static int MAX_PACKET_COUNT = (int) Math.ceil(CHUNK_SIZE/BITS_PER_PACKET);
    private static final long WAITING_TIME = 2000; //2 seconds



    private int currSearchIndex = -1;
    private ArrayList<Pair<Integer,Integer>> searchableTids = null;
    private boolean isDoneSearching = false;
    private boolean middleOfSearching = false;
    private int currResultNumber = 0;

    private static int APIStart = 0;

    private LinkFilter linkFilter;
    private String query;
    private ArrayList<ArrayList<Text>> resultsLists;

    /**
     * This is used for searching everything, a particular category, or book.
     * (Book will be implemented differently in the future).
     *
     * @param query the word to search in Hebrew or English (currently only supporting Hebrew).
     * @param linkFilter the enTitle of the category or book name that you're searching
     * @param alsoSearchCommentary used for when linkFilter.depth_type == CAT and you want to search for the commentary in addition to the category (like Tanach and the Commentaries on Tanach).
     * @throws SQLException
     */
    public SearchingDB(String query, LinkFilter linkFilter, boolean alsoSearchCommentary) throws SQLException {
        this.linkFilter = linkFilter;
        this.query = query;
        searchableTids = getSearchableTids(linkFilter,alsoSearchCommentary);
        resultsLists = new ArrayList<>();
        fillSearchBuffer();
    }

    public boolean isDoneSearching(){ return isDoneSearching; }

    private static byte[] toByteArray(BitSet bits, int byteCount) {
        if(byteCount <= 0)
            byteCount = bits.length()/8+1;
        byte[] bytes = new byte[byteCount];
        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8); //or
            }
        }
        return bytes;
    }



    private static ArrayList<Integer> JHpacketToNums (byte [] bytes) {
        ArrayList<Integer> chunkList = new ArrayList<Integer>();
        for(int i = 0;i< bytes.length/4;i++){
            int a = Integer.valueOf(bytes[i*4])*BITS_PER_PACKET;
            int bitNum = 0;
            for(int j = 3; j >=1;j--){
                byte b = bytes[i*4 +j];
                byte mask = (byte) 0x01;
                for (int k = 0; k < 8; k++)
                {
                    if((b & mask) != 0){
                        chunkList.add(bitNum + a);
                    }
                    bitNum++;
                    mask = (byte) (mask << 1);

                }
            }
        }
        return chunkList;
    }

    private static ArrayList<Integer> findIntersect(ArrayList<Integer> l1, ArrayList<Integer> l2){
        ArrayList<Integer> list = new ArrayList<Integer>(l1.size());
        int j =0;
        int i = 0;
        while(i < l1.size() && j < l2.size()){
            int num1 = l1.get(i);
            int num2 = l2.get(j);
            if(num1 == num2){
                i++;
                j++;
                list.add(num1);
            }
            else if(num1 < num2)
                i++;
            else //num1 > num2
                j++;
        }
        return list;
    }

    private static ArrayList<Integer> findUnion(ArrayList<Integer> l1, ArrayList<Integer> l2){
        ArrayList<Integer> list = new ArrayList<Integer>(l1.size() + l2.size());
        int j = 0;
        int i = 0;
        while(i < l1.size() || j < l2.size()){
            int num1, num2;
            if(i < l1.size())
                num1 = l1.get(i);
            else
                num1 = Integer.MAX_VALUE;
            if(j < l2.size())
                num2 = l2.get(j);
            else
                num2 = Integer.MAX_VALUE;
            if(num1 == num2){
                i++;
                j++;
                list.add(num1);
            }
            else if(num1 < num2){
                i++;
                list.add(num1);
            }
            else{ //num1 > num2
                j++;
                list.add(num2);
            }
        }
        return list;
    }

    private static ArrayList<Integer> getSearchingChunks(String query) throws SQLException{
        SQLiteDatabase db = Database.getDB();
        String [] words =  getWords(query);
        ArrayList<Integer> list = new ArrayList<Integer>();
        String likeStatement = "_id LIKE ? ";
        String[] testWords;
        for(int i =0; i< words.length;i++){
            ArrayList<Integer> unionlist = null;
            if(words[i].charAt(0) == '_'){//you need to also get the chunks for when this first letter is a vav
                likeStatement = likeStatement + " OR " + likeStatement;
                testWords = new String[] {words[i], words[i].substring(1)};
            }
            else
                testWords = new String[] {words[i] };
            Cursor cursor = db.query("Searching", new String[] {"chunks"},likeStatement,
                    testWords, null, null, null, null);
            byte [] bytes = null;
            if(cursor.moveToFirst()){
                do{
                    bytes = cursor.getBlob(0);
                    if(unionlist == null){
                        unionlist = JHpacketToNums(bytes);
                    }
                    else{
                        unionlist = findUnion(unionlist,JHpacketToNums(bytes));
                    }

                }while (cursor.moveToNext());
            }
            else //the word doesn't exist in the db
                return list;
            if(i == 0)
                list = unionlist;
            else
                list = findIntersect(list, unionlist); //only find intersect with previous list
        }

        return list;
    }

    /***
     * removes junk words (like blank words etc) from search
     * @param array
     * @param badWords
     * @return
     */
    private static String [] removeElements(String [] array, ArrayList<Integer> badWords){
        if(badWords == null) // no need to do anything
            return array;
        ArrayList<String> tempArray = new ArrayList<String>();
        boolean copyEl = true;
        for(int i = 0; i < array.length; i++){
            for(int j = 0; j< badWords.size(); j++){
                if(badWords.get(j) == i){
                    copyEl = false;
                    break;
                }
            }
            if(copyEl)
                tempArray.add(array[i]);
        }
        return tempArray.toArray(new String[tempArray.size()]);
    }

    private static String [] getWords(String text){
        //String orgTetx = ""+ text;
        text = text.replaceAll("[\u0591-\u05C7\u05f3\u05f4\'\"]", ""); //all nikkudot and ", ' marks are removed
        text = text.replaceAll("([^\\u05d0-\\u05ea_%])", " "); //anything not part of the hebrew set (or _ and % for special searching), will be removed

        String [] words = text.split(" ");
        ArrayList<Integer> badWords = null;
        for(int i =0 ;i<words.length; i++){
            words[i] = words[i].replaceAll("\\s", "").replaceAll("\\b\\u05d5",""); ///remove white space and starting vavs
            if(words[i].length() < 1){
                if(badWords == null)
                    badWords = new ArrayList<Integer>();
                badWords.add(i);
            }
        }

        return removeElements(words, badWords);
    }


    private static String replace_(boolean wholeWord){
        if(wholeWord)
            return "[^\\u0591-\\u05C7\\s]{1}";
        else
            return "[^\u0591-\u05C7]{1}";

    }

    private static String replacePer(boolean wholeWord){
        //if(wholeWord)
        return "[^\\s\u05be]*";//it's not perfect, but basically it's saying as long as there's no space in between it's good
        //else return ".*?"; //you might want this for not whole words... but then it basically gets eberything


    }
    private static Pattern nikkudlessRegEx(String word, boolean wholeWord){
        final String nikkuds = "[\u0591-\u05bd\u05bf-\u05C7\u05f3\u05f4\'\"]*";//all nukkids but - mark
        String regEx = "";
        if(wholeWord)
            regEx += "\\b\u05d5*" + nikkuds; //leading vavs is added
        String letter;
        for(int i = 0; i <word.length();i++){
            if(word.charAt(i) == '_')
                letter = replace_(wholeWord);
            else if(word.charAt(i) == '%')
                letter = replacePer(wholeWord);
            else
                letter = ""+ word.charAt(i);
            regEx = regEx +  letter + nikkuds;
        }

        if(wholeWord)
            regEx += "\\b";
        Pattern p;
        try{
            p = Pattern.compile(regEx);
        }catch(Exception e){
            try{
                p = Pattern.compile(word);
                Toast.makeText(MyApp.getContext(),MyApp.getContext().getString(R.string.error_parsing_query_n), Toast.LENGTH_SHORT).show();
            }
            catch(Exception e1)
            {
                p = Pattern.compile("");
                Toast.makeText(MyApp.getContext(), MyApp.getContext().getString(R.string.error_parsing_query), Toast.LENGTH_SHORT).show();
            }
        }

        return p;
    }


    public static List<Text> findWordsInList(List<Text> list, String word, boolean enDBOverride, boolean removeLongText){
        List<Text> foundItems = new ArrayList<>();
        Text text = null;

        Pattern hePattern = nikkudlessRegEx(word,false);
        Pattern enPattern;
        try{
            if(enDBOverride)
                enPattern  = Pattern.compile(word.replaceAll("_", ".").replaceAll("%", ".*"),Pattern.CASE_INSENSITIVE); //make case insensitive
            else
                enPattern  = Pattern.compile(word.replaceAll("_", replace_(false)).replaceAll("%", replacePer(false)),Pattern.CASE_INSENSITIVE); //make case insensitive
        }catch(Exception e){//maybe try excaping the regex
            enPattern = Pattern.compile("");
            Toast.makeText(MyApp.getContext(), MyApp.getContext().getString(R.string.error_parsing_query), Toast.LENGTH_SHORT).show();
        }
        //String wordLower = Util.getRemovedNikudString(word.toLowerCase(Locale.US));
        for (int i = 0; i< list.size(); i++){
            boolean foundEn = false;
            text = list.get(i);
            //Test english words
            Matcher m = enPattern.matcher(text.getText(Util.Lang.EN));
            if(m.find()){
                text.setText(addRedToFoundWord(m, text.getText(Util.Lang.EN), removeLongText, false), Util.Lang.EN);
                foundEn = true;
                foundItems.add(text);
            }

            ///TEST Hebrew words
            Util.Lang lang = Util.Lang.HE;
            m = hePattern.matcher(text.getText(lang));
            if(m.find()){
                text.setText(addRedToFoundWord(m, text.getText(lang), removeLongText, false), lang);
                if(!foundEn){//didn't already add this to the list for found English
                    foundItems.add(text);
                }
            }
        }

        return foundItems;
    }

    public static void removeRed(List<Text> textList){
        for(Text text:textList){
            text.setText(text.getText(Util.Lang.EN).replace(FONT_RED_END,"").replace(FONT_RED_START,""), Util.Lang.EN);
            text.setText(text.getText(Util.Lang.HE).replace(FONT_RED_END, "").replace(FONT_RED_START,""), Util.Lang.HE);
        }
    }

    private static final String FONT_RED_START = "<font color='#ff5566'>";
    private static final String FONT_RED_END = "</font>";
    protected static final String BIG_BOLD_START = "<big><b>";
    protected static final String BIG_BOLD_END = "</b></big>";
    private static String addRedToFoundWord(Matcher m, String orgText, boolean removeLongText, boolean useBold){
        final String addStart;
        final String addEnd;
        if(useBold){
            addStart = BIG_BOLD_START;
            addEnd = BIG_BOLD_END;
        }else{
            addStart = FONT_RED_START;
            addEnd = FONT_RED_END;
        }
        final int MAX_CHAR_NUM = 100;
        if(orgText.length() < 300)//you could get the text in there, so let it be.
            removeLongText = false;
        int lastSpot = 0;

        String newText = "";
        do{
            if(removeLongText &&  lastSpot < m.start() - MAX_CHAR_NUM*2){
                int newSpot = lastSpot + MAX_CHAR_NUM;
                while(newSpot < orgText.length() -1  && orgText.charAt(newSpot) != ' '){
                    newSpot++;
                }
                if(newSpot < m.start() - MAX_CHAR_NUM){//else just do a regular thing
                    if(lastSpot != 0)
                        newText += orgText.substring(lastSpot, newSpot) + " ...";
                    else
                        newText += "...";
                    lastSpot =  m.start() - MAX_CHAR_NUM;
                    while(lastSpot < orgText.length() -1  && orgText.charAt(lastSpot) != ' '){
                        lastSpot--;
                    }

                }
            }
            newText += orgText.substring(lastSpot, m.start()) + addStart
                    + orgText.substring(m.start(),m.end()) + addEnd;
            lastSpot = m.end();
        }
        while(m.find());
        if(removeLongText && lastSpot + MAX_CHAR_NUM < orgText.length()){
            int newSpot = lastSpot + MAX_CHAR_NUM;
            while(newSpot < orgText.length() -1  && orgText.charAt(newSpot) != ' '){
                newSpot++;
            }
            newText += orgText.substring(lastSpot,newSpot) + " ...";
        }else
            newText += orgText.substring(lastSpot);
        return newText;
    }


    private static String[] concat(String [] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c= new String[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }


    private ArrayList<Pair<Integer,Integer>> getSearchableTids(LinkFilter linkFilter, boolean alsoSearchCommentary) throws SQLException {
        //chunklist is [) (inclusive, exclusive), and tid is (now) also [) (inclusive,exclusive)
        ArrayList<Pair<Integer,Integer>> searchableTids = new ArrayList<>();
        ArrayList<Integer> searchableChunks = getSearchingChunks(query);
        if(searchableChunks.size() == 0) return searchableTids;

        if(linkFilter == null || linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL){
            for(int i=0;i<searchableChunks.size();i++) {
                searchableTids.add(
                        new Pair<>(CHUNK_SIZE*searchableChunks.get(i),CHUNK_SIZE *(searchableChunks.get(i)+1))
                );
            }
            return searchableTids;
        }

        ArrayList<Pair<Integer,Integer>> tidMinMax = createFilteredTidMinMaxList(linkFilter,alsoSearchCommentary);
        int tidIndex = 0;
        for(int i = 0; i<searchableChunks.size();i++){
            int chunkStart = CHUNK_SIZE*(searchableChunks.get(i));
            int chunkEnd = chunkStart + CHUNK_SIZE - 1;//now both lists are inclusive
            while(chunkStart > tidMinMax.get(tidIndex).second){
                tidIndex++;
                if(tidIndex >= tidMinMax.size()){
                    Log.d("bid", "finished searchableTidLoc");
                    return searchableTids;
                }
            }
            //chunkStart <= tidMinMax.get(tidIndex).second must be true
            if(chunkEnd >= tidMinMax.get(tidIndex).first){
                searchableTids.add(
                    new Pair<>(Math.max(chunkStart, tidMinMax.get(tidIndex).first), Math.min(chunkEnd, tidMinMax.get(tidIndex).second)+1)
                );
            }else
                continue;
        }
        Log.d("bid", "finished searchableTids");
        return searchableTids;
    }


    private static ArrayList<Pair<Integer,Integer>> createFilteredTidMinMaxList(LinkFilter linkFilter, boolean alsoSearchCommentary){
        SQLiteDatabase db = Database.getDB();
        ArrayList<Pair<Integer,Integer>> tidMinMax = new ArrayList<>();

        String likeStatement;
        String [] filterArray;
        if (linkFilter == null || linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL) {
            Log.e("SearchingDB", "This function shouldn't have been called if your looking for everything");
            likeStatement = " 1=1 ";
            filterArray = new String[0];
        }else{
            if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT) {
                //TODO make Mishneh Torah (which is a subcategory) work
                if(alsoSearchCommentary){
                    likeStatement = " categories LIKE ? OR ? ";
                    filterArray = new String[]{
                            "[\"" + linkFilter.enTitle + "\"%",
                            "[\"Commentary\",\"" + linkFilter.enTitle + "\"%"
                    };
                }else{
                    likeStatement = " categories LIKE ? ";
                    filterArray = new String[]{"[\"" + linkFilter.enTitle + "\"%"};
                }
            }else{ // if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK) {
                likeStatement = " title LIKE ? ";
                filterArray = new String[]{linkFilter.enTitle};
            }
        }
        String bookSQL = "Select minTid, maxTid FROM Books WHERE " +
                likeStatement + " ORDER BY _id";
        Cursor cursorB = db.rawQuery(bookSQL, filterArray);
        //query("Books", new String[] {"_id"}, likeStatement,	filterArray, null, null, null, null);
        Pair<Integer, Integer> lastPair = null;
        if (cursorB.moveToFirst()) { //TODO CursorWIndowAllocationException
            do {
                int first = cursorB.getInt(0);
                int second = cursorB.getInt(1);
                if(first == -1 || second == -1)//it doesn't have any useful tids
                    continue;
                Pair<Integer, Integer> tempPair = new Pair<>(first, second);
                if(tidMinMax.isEmpty())
                    tidMinMax.add(tempPair);
                else{
                    if(tempPair.first == lastPair.second + 1){//it's a continuation, so just add to the maxMin Num
                        tempPair = new Pair<Integer, Integer>(tidMinMax.get(tidMinMax.size()-1).first, tempPair.second);
                        tidMinMax.set(tidMinMax.size()-1, tempPair);
                    }
                    else
                        tidMinMax.add(tempPair);
                }
                lastPair = tempPair;
            }while (cursorB.moveToNext());
        }
        //Log.d("bid", "finished createTidList");
        return tidMinMax;
    }

    private static final int SEARCH_BUFFER_SIZE = 1;



    private ArrayList<Text> searchDBheTexts() {
        int startingChunk = currSearchIndex;
        ArrayList<Text> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        try {
            SQLiteDatabase db = Database.getDB();

            String [] words = getWords(query);
            Pattern [] patterns = new Pattern [words.length];
            for(int i = 0; i< patterns.length; i++){
                patterns[i] = nikkudlessRegEx(words[i],true);
            }

            Cursor cursor = null;
            for(int i=startingChunk+1; i < searchableTids.size();i++){
                if(results.size() >= 6 || (results.size() >= 3  && System.currentTimeMillis() > startTime + WAITING_TIME)){
                    return results;
                }

                String sql = "SELECT * " +
                        "FROM Texts WHERE  ";
                sql += "  _id >= " + searchableTids.get(i).first + " AND _id < "  + searchableTids.get(i).second + " AND heTextCompress NOT NULL"; //used to <=
                cursor = db.rawQuery(sql, null);//filterArray
                if (cursor.moveToFirst()) {
                    do {
                        Text text = new Text(cursor);
                        String verse = text.getText(Util.Lang.HE);

                        //if(verse == null) continue;//might not be needed if doing at DB level
                        //Log.d("searching", "verse Length: " + verse.length());
                        //String cleacursorredVerse = verse.replaceAll("[\u0591-\u05C7]", "");
                        //if(verse.length() > 4000) continue;//TODO this is a major hackk!!, but we aren't searching huge things

                        for(int j=0; j<words.length;j++){
                            Matcher m = patterns[j].matcher(verse);
                            if(!m.find()) {
                                break;
                            }
                            //if(clearedVerse.contains(words[j])){ //if(verse.replaceFirst(wordsRegEx[j], "ABC").hashCode() != verse.hashCode()){ //more detailed //	if(clearedVerse.replaceAll("\\b\\u05d5","").replaceFirst("\\b" + words[j] + "\\b", "ABC").hashCode() == clearedVerse.hashCode()) break;

                            text.setText(addRedToFoundWord(m, verse, true, true), Util.Lang.HE);
                            if(j == words.length -1){
                                results.add(text);
                                //resultsList.add(new Text(cursor.getInt(0)));//maybe create a list of tids and do it as one big search
                                //resultsList.get(resultsList.size()-1).setText(verse, Util.Lang.HE);
                            }
                        }
                    }
                    while (cursor.moveToNext());
                }
                cursor.close();
                currSearchIndex = i;
            }
        } catch (Exception e) {
            GoogleTracker.sendException(e, "DB_search");
        }

        if (results.size() == 0) isDoneSearching = true;

        return results;
    }

    /*
    private ArrayList<Text> searchEnTexts(String word, String [] filterArray) {

        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        int lastTID = currChunkIndex;

        ArrayList<Text> textList = new ArrayList<Text>();

        String [] bindValues =  new String[] {String.valueOf(lastTID), "%" + word  + "%"};
        int limit = 20;
        Cursor cursor = null;
        if(filterArray.length != 0){
            String sql = "SELECT * FROM Texts WHERE  _id > ? AND  enText LIKE ? AND bid in (SELECT B._id FROM Books B WHERE " +  makeFilterStatement(filterArray, "B.") +  " )";
            sql += " LIMIT " + limit;
            filterArray = convertfilterArray(filterArray);
            cursor= db.rawQuery(sql, concat(bindValues,filterArray ));
        }
        else{
            cursor= db.query("Texts", null, " _id > ? AND  enText LIKE ? ",	bindValues, null, null, null, String.valueOf(limit));
        }

        //
        // looping through all rows and adding to list
        if (cursor != null && cursor.moveToFirst()) {
            do {
                textList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }
        if(textList.size() > 0)
            currChunkIndex = textList.get(textList.size() -1).tid;

        findWordsInList(textList, word, true, true);

        return textList;
    }
    */

    public static List<Text> findOnPage(Node node, String term){
        List<Text> list;
        try {
            list = findWordsInList(node.getTexts(), term, false, false);
        }catch(Exception e){
            list = new ArrayList<>();
        }
        return list;
    }

    private String fillSearchBufferTask(){
        Log.d("SearchingDB", "Async task seeing if it should start... not async");
        if(middleOfSearching) return null;
        if(isDoneSearching) return null;
        Log.d("SearchingDB", "Async task started!.. not async");
        middleOfSearching = true;
        while(true){
            ArrayList<Text> results;
            try {
                if(true) {//TODO check if it's hebrew
                    results = searchDBheTexts();
                }else{
                    ;//results = searchEnTexts(query,filterArray);
                }
                resultsLists.add(results);
                Log.d("SearchingDB", "ASYNC: currResultNumber:" + currResultNumber + "... resultsLists.size():" + resultsLists.size());
                if(results.size()==0){
                    isDoneSearching = true;
                    break;
                }
                if(isBufferFilled())
                    break;
            } catch (Exception e) {
                isDoneSearching = true;
                e.printStackTrace();
                GoogleTracker.sendException(e,"in fillSearchBuffer");
            }
        }
        middleOfSearching = false;
        return null;
    }
    private void fillSearchBuffer(){
        fillSearchBufferTask();
        //this.new FillSearchBufferAsync().execute();
    }

    private boolean isBufferFilled(){
        return (resultsLists.size() > currResultNumber + SEARCH_BUFFER_SIZE);
    }



    /**
     *  waits (in spin loop) until there is data to send. It will also tell a Async task to refill the search buffer if it's within it's limits
     * @return the next batch of search results
     */
    public ArrayList<Text> getResults(){
        while(true) {
            Log.d("SearchingDB", "currResultNumber:" + currResultNumber + "... resultsLists.size():"+ resultsLists.size());
            if (currResultNumber < resultsLists.size()) {
                if(!isBufferFilled())
                    fillSearchBuffer();
                return resultsLists.get(currResultNumber++);
            } else if (isDoneSearching && currResultNumber >= resultsLists.size()) {
                return new ArrayList<>();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return new ArrayList<>();
            }
        }
    }

    private class FillSearchBufferAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return fillSearchBufferTask();
        }

        @Override
        protected void onPostExecute(String result) {}
    }


    public static boolean hasSearchTable(){
        return (Database.hasOfflineDB() && Database.getVersionInDB(false) >= 215);
    }

}