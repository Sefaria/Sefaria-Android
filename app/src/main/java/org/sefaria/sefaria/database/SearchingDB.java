package org.sefaria.sefaria.database;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

public class SearchingDB {


    final private static int CHUNK_SIZE = Database.getDBSetting("blockSize",false);
    final private static int BITS_PER_PACKET = 24;
    private static final long WAITING_TIME = 2000; //2 seconds


    private int currSearchIndex = -1;
    private ArrayList<Pair<Integer,Integer>> searchableTids = null;
    private boolean isDoneSearching = false;
    private boolean middleOfSearching = false;
    private int currResultNumber = 0;

    private String query;
    private ArrayList<ArrayList<Segment>> resultsLists;

    //changed for testing
    private boolean usePureSearchEvenHe = false;
    private int returnResultsSize; //see constructor for default
    private int returnResultsLongTimeSize;



    public SearchingDB(String query,List<String> filterList) throws SQLException {
        this(query, filterList, false, 6, 3);
    }

    /**
     * This is used for searching everything, a particular category, or book.
     * (Book will be implemented differently in the future).
     *
     * @param query the word to search in Hebrew or English (currently only supporting Hebrew).
     * @throws SQLException
     */
    private SearchingDB(String query, List<String> filterList, boolean usePureSearchEvenHe, int returnResultsSize, int returnResultsLongTimeSize) throws SQLException {
        this.query = query;
        resultsLists = new ArrayList<>();
        this.usePureSearchEvenHe = usePureSearchEvenHe;
        this.returnResultsSize = returnResultsSize;
        this.returnResultsLongTimeSize = returnResultsLongTimeSize;
        //this should run only after the other variables are set
        searchableTids = getSearchableTids(filterList, query);
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

    private ArrayList<Integer> bytesToNums(byte [] bytes){//{}, int maxNumber){
        ArrayList<Integer> chunkList = new ArrayList<>();
        int maxNumber = returnResultsSize*200;
        int maxBytes = bytes.length;
        maxBytes = Math.min(maxNumber * 4, bytes.length);

        byte [] bArray = new byte[4];
        for(int i = 0; i< maxBytes; i+=4){
            for(int j = 0; j < 4; j++){
                bArray[j] = bytes[i + j];
            }
            ByteBuffer bb = ByteBuffer.wrap(bArray);
            chunkList.add(bb.getInt());
        }
        return chunkList;
    }

    private static ArrayList<Integer> JHpacketToNums (byte [] bytes) {
        ArrayList<Integer> chunkList = new ArrayList<>();
        for(int i = 0;i< bytes.length/4;i++){
            int a = (Integer.valueOf(bytes[i*4]) & 0xff)*BITS_PER_PACKET; //0xff should remove the negative from byte
            if(a < 0){
                Log.e("searching", "bad byte conversion..");
            }
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

    private ArrayList<Integer> getSearchingChunks(String query) throws SQLException{
        SQLiteDatabase db = Database.getDB();
        ArrayList<Integer> list = new ArrayList<>();
        if(query.length() == 0)
            return list;
        String [] words = getWords(query, Util.Lang.HE);
        String likeStatement = "_id LIKE ? ";
        String[] testWords;
        for(int i =0; i< words.length; i++){
            ArrayList<Integer> unionlist = null;
            if(words[i].charAt(0) == '_'){//you need to also get the chunks for when this first letter is a vav
                likeStatement = likeStatement + " OR " + likeStatement;
                testWords = new String[] {words[i], words[i].substring(1)};
            }
            else
                testWords = new String[] {words[i] };
            String tableName;
            if(CHUNK_SIZE == 1){
                tableName = "SearchingFull";
            }else{
                tableName = "Searching";
            }
            Cursor cursor = db.query(tableName, new String[] {"chunks"}, likeStatement,
                    testWords, null, null, null, null);
            byte [] bytes = null;
            if(cursor.moveToFirst()){
                do{
                    bytes = cursor.getBlob(0);
                    ArrayList<Integer> list1;
                    if(CHUNK_SIZE != 1){
                        list1 = JHpacketToNums(bytes);
                    }else{

                        list1 = bytesToNums(bytes);
                    }
                    if(unionlist == null){
                        unionlist = list1;
                    }
                    else{
                        unionlist = findUnion(unionlist, list1);
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
        if(CHUNK_SIZE == 1){
            // since it's full Index, we can get just the first x results and that will be enough
            ;//list = new ArrayList<>(list.subList(0, Math.min(returnResultsSize, list.size())));
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
        ArrayList<String> tempArray = new ArrayList<>();
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

    private static String [] getWords(String text, Util.Lang lang){
        //String orgTetx = ""+ segment;
        text = text.replaceAll("[\u0591-\u05C7\u05f3\u05f4\'\"]", ""); //all nikkudot and ", ' marks are removed
        if(lang == Util.Lang.HE){
            text = text.replaceAll("([^\\u05d0-\\u05ea_%])", " "); //anything not part of the hebrew set (or _ and % for special searching), will be removed
        }


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
        //else return ".*?"; //you might want this for not whole words... but then it basically gets everything


    }
    private static Pattern nikkudlessRegEx(String word, boolean wholeWord, Util.Lang lang){
        String nikkuds = "[\u0591-\u05bd\u05bf-\u05C7\u05f3\u05f4\'\"]*";//all nikkuds but - mark
        if(lang == Util.Lang.EN){
            nikkuds = "";
        }
        String regEx = "";
        if(wholeWord) {
            if(lang == Util.Lang.EN){
                regEx += "\\b"; //leading vavs is added
            }else {
                regEx += "\\b\u05d5*" + nikkuds; //leading vavs is added
            }
        }
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


    public static List<Segment> findWordsInList(List<Segment> list, String word, boolean enDBOverride, boolean removeLongText){
        List<Segment> foundItems = new ArrayList<>();
        Segment segment = null;

        Pattern hePattern = nikkudlessRegEx(word,false, Util.Lang.HE);
        Pattern enPattern;
        try{
            if(enDBOverride)
                enPattern  = Pattern.compile(word.replaceAll("_", ".").replaceAll("%", ".*"),Pattern.CASE_INSENSITIVE); //make case insensitive
            else
                enPattern  = Pattern.compile(word.replaceAll("_", replace_(false)).replaceAll("%", replacePer(false)),Pattern.CASE_INSENSITIVE); //make case insensitive
        }catch(Exception e){//maybe try escaping the regex
            enPattern = Pattern.compile("");
            Toast.makeText(MyApp.getContext(), MyApp.getContext().getString(R.string.error_parsing_query), Toast.LENGTH_SHORT).show();
        }
        //String wordLower = Util.getRemovedNikudString(word.toLowerCase(Locale.US));
        for (int i = 0; i< list.size(); i++){
            boolean foundEn = false;
            segment = list.get(i);
            //Test english words
            Matcher m = enPattern.matcher(segment.getText(Util.Lang.EN));
            if(m.find()){
                segment.setText(addRedToFoundWord(m, segment.getText(Util.Lang.EN), removeLongText, false), Util.Lang.EN);
                foundEn = true;
                foundItems.add(segment);
            }

            ///TEST Hebrew words
            Util.Lang lang = Util.Lang.HE;
            m = hePattern.matcher(segment.getText(lang));

            if(m.find()){
                segment.setText(addRedToFoundWord(m, segment.getText(lang), removeLongText, false), lang);
                if(!foundEn){//didn't already add this to the list for found English
                    foundItems.add(segment);
                }
            }
        }

        return foundItems;
    }

    public static void removeRed(List<Segment> segmentList){
        for(Segment segment : segmentList){
            segment.setText(segment.getText(Util.Lang.EN).replace(FONT_RED_END,"").replace(FONT_RED_START,""), Util.Lang.EN);
            segment.setText(segment.getText(Util.Lang.HE).replace(FONT_RED_END, "").replace(FONT_RED_START,""), Util.Lang.HE);
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
        if(orgText.length() < 300)//you could get the segment in there, so let it be.
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


    /*
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
    }*/


    private ArrayList<Pair<Integer,Integer>> getSearchableTids(List<String> filterList, String query) throws SQLException {
        //chunklist is [) (inclusive, exclusive), and tid is (now) also [) (inclusive,exclusive)
        Log.d("searching","getsearchableTids started");
        ArrayList<Pair<Integer,Integer>> searchableTids = new ArrayList<>();
        ArrayList<Integer> searchableChunks = getSearchingChunks(query);
        if(searchableChunks.size() == 0) return searchableTids;

        if(filterList == null || filterList.size() == 0 ){
            for(int i=0;i<searchableChunks.size();i++) {
                searchableTids.add(
                        new Pair<>(CHUNK_SIZE*searchableChunks.get(i),CHUNK_SIZE *(searchableChunks.get(i)+1))
                );
            }
            Log.d("searching","getsearchableTid finished");
            return searchableTids;
        }else {
            ArrayList<Pair<Integer, Integer>> tidMinMax = createFilteredTidMinMaxList(filterList);
            int tidIndex = 0;
            for (int i = 0; i < searchableChunks.size(); i++) {
                int chunkStart = CHUNK_SIZE * (searchableChunks.get(i));
                int chunkEnd = chunkStart + CHUNK_SIZE - 1;//now both lists are inclusive
                while (chunkStart > tidMinMax.get(tidIndex).second) {
                    tidIndex++;
                    if (tidIndex >= tidMinMax.size()) {
                        Log.d("bid", "finished searchableTidLoc");
                        return searchableTids;
                    }
                }
                //chunkStart <= tidMinMax.get(tidIndex).second must be true
                if (chunkEnd >= tidMinMax.get(tidIndex).first) {
                    searchableTids.add(
                            new Pair<>(Math.max(chunkStart, tidMinMax.get(tidIndex).first), Math.min(chunkEnd, tidMinMax.get(tidIndex).second) + 1)
                    );
                } else
                    continue;
            }
            Log.d("searching","getsearchableTids finished");
            return searchableTids;
        }
    }


    private static ArrayList<Pair<Integer,Integer>> createFilteredTidMinMaxList(List<String> filterList){
        StringBuilder likeStatement = new StringBuilder();
        String [] filterArray = new String [filterList.size()];

        for(int i = 0; i < filterList.size(); ++i){
            likeStatement.append( " path LIKE ? OR ");
            filterArray[i] = filterList.get(i) + "%";
        }
        likeStatement.append(" 0=1");
        return createFilteredTidMinMaxList(likeStatement.toString(), filterArray);
    }

    private static ArrayList<Pair<Integer,Integer>> createFilteredTidMinMaxList(String likeStatement, String [] filterArray){
        SQLiteDatabase db = Database.getDB();
        ArrayList<Pair<Integer,Integer>> tidMinMax = new ArrayList<>();

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

    private static ArrayList<Pair<Integer,Integer>> createFilteredTidMinMaxList(LinkFilter linkFilter, boolean alsoSearchCommentary){
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
        return createFilteredTidMinMaxList(likeStatement, filterArray);
    }

    boolean didFullIndex = false;
    private ArrayList<Segment> searchDBheTextsFullIndex() {
        Log.d("Searching", "searchDBheTextsFullIndex started");
        ArrayList<Segment> results = new ArrayList<>();
        if(didFullIndex){
            isDoneSearching = true;
            return results;
        }
        try {
            SQLiteDatabase db = Database.getDB();

            String[] words = getWords(query, Util.Lang.HE);
            Pattern[] patterns = new Pattern[words.length];
            for (int i = 0; i < patterns.length; i++) {
                patterns[i] = nikkudlessRegEx(words[i], true, Util.Lang.HE);
            }

            Cursor cursor = null;
            String sql = "SELECT * " +
                    "FROM Texts WHERE  ";
            sql += "  _id in (";
            StringBuilder tids = new StringBuilder();
            tids.append(searchableTids.get(0).first.toString());
            for(int i = 1; i< searchableTids.size(); i++) {
                tids.append("," + searchableTids.get(i).first);
            }
            sql += tids.toString() + ")";
            cursor = db.rawQuery(sql, null);//filterArray
            if (cursor.moveToFirst()) {
                do {
                    Segment segment = new Segment(cursor);
                    /* this finds the number of times the item occurs.
                    Matcher m1 = patterns[0].matcher(segment.getText(Util.Lang.HE));
                    while (m1.find())
                        foundCount++;
                        Log.d("counter", "" + foundCount);
                    */


                    results.add(segment);
                }while (cursor.moveToNext());
            }
        }catch (Exception e){

        }
        didFullIndex = true;
        Log.d("Searching", "searchDBheTextsFullIndex finished");
        return results;
    }

    private ArrayList<Segment> searchDBheTexts() {
        if(CHUNK_SIZE == 1){
            return  searchDBheTextsFullIndex();
        }
        Log.d("Searching", "searchDBheTextsFullIndex NOT run");
        int startingChunk = currSearchIndex;
        ArrayList<Segment> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        try {
            SQLiteDatabase db = Database.getDB();

            String [] words = getWords(query, Util.Lang.HE);
            Pattern [] patterns = new Pattern [words.length];
            for(int i = 0; i< patterns.length; i++){
                patterns[i] = nikkudlessRegEx(words[i],true, Util.Lang.HE);
            }

            Cursor cursor = null;
            for(int i=startingChunk+1; i < searchableTids.size();i++){
                if(shouldReturnResults(results.size(),startTime)){
                    return results;
                }

                String sql = "SELECT * " +
                        "FROM Texts WHERE  ";
                sql += "  _id >= " + searchableTids.get(i).first + " AND _id < "  + searchableTids.get(i).second + " AND heTextCompress NOT NULL"; //used to <=
                cursor = db.rawQuery(sql, null);//filterArray
                if (cursor.moveToFirst()) {
                    do {
                        Segment segment = new Segment(cursor);

                        //if(verse.length() > 4000) continue;//TODO this is a major hack!!, but we aren't searching huge things

                        String verse = segment.getText(Util.Lang.HE);
                        boolean foundAllWords = true;
                        for(int j=0; j<words.length;j++) {
                            Matcher m = patterns[j].matcher(verse);
                            if (!m.find()) {
                                foundAllWords = false;
                                break;
                            }
                        }
                        if(foundAllWords) { //divideed into different parts so the addingHighlighting doesn't need to run each time
                            for (int j = 0; j < words.length; j++) {
                                verse = segment.getText(Util.Lang.HE);
                                Matcher m = patterns[j].matcher(verse);
                                m.find();
                                segment.setText(addRedToFoundWord(m, verse, true, true), Util.Lang.HE);
                            }
                            results.add(segment);
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

    private boolean shouldReturnResults(int resultsSize, long startTime){
        return (resultsSize >= returnResultsLongTimeSize || (resultsSize >= returnResultsLongTimeSize  && System.currentTimeMillis() > startTime + WAITING_TIME));
    }

    private ArrayList<Segment> searchPureText(Util.Lang lang){
        Log.d("searching","pure_search");
        ArrayList<Segment> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int maxTid = -1;
        try {
            SQLiteDatabase db = Database.getDB();

            String [] words = getWords(query, lang);
            Log.d("searching","words length:" + words.length + "___" + query);
            Pattern [] patterns = new Pattern [words.length];
            for(int i = 0; i< patterns.length; i++){
                patterns[i] = nikkudlessRegEx(words[i], true, lang);
            }

            String textType;
            if(lang == Util.Lang.EN){
                textType = "enTextCompress";
            }else{
                textType = "heTextCompress";
            }
            Cursor cursor = null;
            if(currSearchIndex < 0){
                currSearchIndex = 0;
            }
            while (!shouldReturnResults(results.size(),startTime)){
                String sql = "SELECT * " + " FROM Texts WHERE  ";
                sql += "  _id >= " + CHUNK_SIZE*(currSearchIndex) + " AND _id < "  + CHUNK_SIZE*(++currSearchIndex) + " AND " + textType + " NOT NULL"; //used to <=
                //Log.d("searching", "sql:" + sql);
                cursor = db.rawQuery(sql, null);//filterArray
                if (cursor.moveToFirst()) {
                    do {
                        Segment segment = new Segment(cursor);
                        String verse = segment.getText(lang);
                        for(int j=0; j<words.length;j++){
                            Matcher m = patterns[j].matcher(verse);
                            if(!m.find()) {
                                break;
                            }

                            segment.setText(addRedToFoundWord(m, verse, true, true), lang);
                            if(j == words.length -1){
                                results.add(segment);
                            }
                        }
                    }
                    while (cursor.moveToNext());
                }else{
                    if(maxTid == -1){
                        sql = "SELECT MAX(_id) " + " FROM Texts";
                        Cursor cursor2 = db.rawQuery(sql, null);//filterArray
                        if (cursor2.moveToFirst()) {
                            maxTid = cursor2.getInt(0);
                        }
                        cursor2.close();
                    }
                    if(maxTid < CHUNK_SIZE*(currSearchIndex)){
                        cursor.close();
                        break;
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            GoogleTracker.sendException(e, "DB_search_pure");
        }

        if (results.size() == 0) isDoneSearching = true;

        return results;
    }

    public static List<Segment> findOnPage(Node node, String term){
        List<Segment> list;
        try {
            list = findWordsInList(node.getTexts(), term, false, false);
        }catch(Exception e){
            list = new ArrayList<>();
        }
        return list;
    }





    /**
     *  waits (in spin loop) until there is data to send. It will also tell a Async task to refill the search buffer if it's within it's limits
     * @return the next batch of search results
     */
    public ArrayList<Segment> getResults(){
        while(true){
            Log.d("SearchingDB", "getResults: currResultNumber:" + currResultNumber + "... resultsLists.size():"+ resultsLists.size());
            if (isDoneSearching && currResultNumber >= resultsLists.size()) {
                Log.d("SearchingDB", "getResults: done searching");
                return new ArrayList<>(); // this means we got nothing else
            }
            else if(isBufferFilled()) {
                return resultsLists.get(currResultNumber++);
            }else if(middleOfSearching) { //this will happen with background task trying to fill it up
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue; //this is why there's a while loop
            }else{
                Log.d("SearchingDB", "filing buffer");
                fillSearchBuffer(false);
                return resultsLists.get(currResultNumber++);
            }
        }
    }

    private String fillSearchBufferTask(){
        if(middleOfSearching) return null;
        if(isDoneSearching) return null;
        middleOfSearching = true;
        while(true){
            ArrayList<Segment> results;
            try {
                if (query.length() == 0) {
                    results = new ArrayList<>();
                } else if (Util.hasHebrew(query)) {
                    if (!usePureSearchEvenHe) {
                        results = searchDBheTexts();
                    } else {
                        results = searchPureText(Util.Lang.HE);
                    }
                } else {
                    results = searchPureText(Util.Lang.EN);
                }

                resultsLists.add(results);
                Log.d("SearchingDB", "fillSearchBufferTask: currResultNumber:" + currResultNumber + "... resultsLists.size():" + resultsLists.size());
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
    private void fillSearchBuffer(boolean async){
        if(!async)
            fillSearchBufferTask();
        else
            this.new FillSearchBufferAsync().execute();
    }

    private boolean isBufferFilled(){
        return (resultsLists.size() > currResultNumber);
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
        return (Database.hasOfflineDB() && CHUNK_SIZE != Database.BAD_SETTING_GET);
    }

    public static String [] createPhrase(String [] queries){
        String [] queries_phrases = new String[queries.length];

        for(int i = 0; i < queries.length; i++){
            String phrase = "";
            for(int j = 0; j < queries.length; j++){
                if(i != j){
                    if(phrase.length() > 0){
                        phrase += " ";
                    }
                    phrase += queries[j];
                }
            }
            queries_phrases[i] = phrase;
        }
        return queries_phrases;
    }

    public static class AsyncRunTests extends AsyncTask<Void,Void,String> {

        private Context context;
        public AsyncRunTests(Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            String [] queries350k = new String[] {"לו", "את", "כי"};
            String [] queries6k = new String[] {"ישב", "נפשו", "ברית"};
            String [] queries55 = new String[] {"מפספס", "אורבי", "מפנהו"};
            String [] queries10 = new String[] {"איפוק", "בשאיני", "קיללו"};
            String [] querySizes = new String[] {"350k", "6k", "55", "10"}; // {"350k", "6k", "55", "10"};
            boolean USE_PHRASE = true;
            if(USE_PHRASE) {
                queries350k = createPhrase(queries350k);
                queries6k = createPhrase(queries6k);
                queries55 = createPhrase(queries55);
                queries10 = createPhrase(queries10);
            }
            List<String []> queryTypes = new ArrayList<>();
            queryTypes.add(queries350k);
            queryTypes.add(queries6k);
            queryTypes.add(queries55); queryTypes.add(queries10);
            StringBuilder testingResults = new StringBuilder();
            testingResults.append(" CHUNK SIZE: " + CHUNK_SIZE + " DB#: " + Database.getVersionInDB(false) + " USE_PHRASE: " + USE_PHRASE + "__DONOT_200_unloadAllInts\n\n");
            final int RETURN_RESULTS_REG = 6; //6;
            final int LARGE_INT = 10000000;
            int[] returnResultAmounts = {RETURN_RESULTS_REG};
            boolean[] usePureNoIndexSearches = {false};
            for (boolean usePureNoIndexSearch : usePureNoIndexSearches) {
                for (int returnResultAmount : returnResultAmounts) {
                    String searchMethod = "[" + (usePureNoIndexSearch ? "NoIndex" : "compressedIndex")
                            + (returnResultAmount == LARGE_INT ? ", ReturnAll" : ", First" + returnResultAmount)
                            + "]\n";
                    Log.d("searching", searchMethod);
                    testingResults.append("\n------------------------------------------\n" + searchMethod);
                    int querySizeIndex = 0;
                    for(String [] queries: queryTypes) {
                        testingResults.append("\n-----------------\nqueries" + querySizes[querySizeIndex++] + ":\n");
                        long sumTime = 0;
                        long sumResults = 0;
                        for (String query : queries) {
                            long startTime = System.currentTimeMillis();
                            SearchingDB searchingDB = null;
                            try {
                                searchingDB = new SearchingDB(query, null, usePureNoIndexSearch, returnResultAmount, returnResultAmount);
                                List<Segment> results = searchingDB.getResults();
                                long totalTime = System.currentTimeMillis() - startTime;
                                sumTime += totalTime;
                                sumResults += results.size();
                                String timing = " (Q: >>" + query + "<<) " +
                                        " {results: " + results.size() + ". took: "
                                        + totalTime + "ms."
                                        + " blockIndex:" + (searchingDB.currSearchIndex + (searchingDB.usePureSearchEvenHe ? 0 : 1))
                                        //+ (searchingDB.usePureSearchEvenHe ? "" : ". blockNum:" +  searchingDB.searchableTids.get(searchingDB.currSearchIndex).second/CHUNK_SIZE)
                                        + "}"
                                        ;
                                testingResults.append("\n" + timing + "\n");
                                Log.d("searching", timing);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        testingResults.append("\nAVG TIME: " + sumTime/queries.length + ". AVG RESULTS: " + sumResults/queries.length);
                    }
                }
            }

            return testingResults.toString();
        }

        @Override
        protected void onPostExecute(String timings) {
            super.onPostExecute(timings);
            String email = "jherzberg@sefaria.org";
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", email, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Test Search results");
            emailIntent.putExtra(Intent.EXTRA_TEXT, timings);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String [] {email});
            context.startActivity(Intent.createChooser(emailIntent, "Send email"));
        }
    };



}