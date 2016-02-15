package org.sefaria.sefaria.database;

import java.util.BitSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;

public class Searching extends Text {


    final static int CHUNK_SIZE = 500;
    final static int VERSE_COUNT = 1000000;
    final static int CHUNK_COUNT = VERSE_COUNT/CHUNK_SIZE;
    final static int WORD_COUNT = 25000000;

    private static int currChunkIndex;
    private static ArrayList<Integer> searchableChunks = null;
    private static ArrayList<Pair<Integer,Integer>> searchableTids = null;
    public static boolean doneSearching;
    public static volatile boolean interrupted;
    private static final long WAITING_TIME = 2*1000; //2 seconds
    private static int APIStart = 0;

    public static void init() {
        currChunkIndex = -1;
        searchableTids = null;
        doneSearching = false;
        interrupted = false;
        searchableChunks = null;
    }
    public static int getCurrChunkIndex(){
        return currChunkIndex;
    }
    public static void setCurrChunkIndex(int num){
        currChunkIndex = num;
        if(num == -1)
            init();
    }

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


    final static int BITS_PER_PACKET = 24;
    final static int PACKET_SIZE = 32;
    final static int MAX_PACKET_COUNT = (int) Math.ceil(CHUNK_SIZE/BITS_PER_PACKET);

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
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
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


    public static List<Integer> findWordsInList(List<Text> list, String word, boolean enDBOverride, boolean removeLongText){
        List<Integer> foundPositions = new ArrayList<Integer>();
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
            Toast.makeText(MyApp.currActivityContext, MyApp.getContext().getString(R.string.error_parsing_query), Toast.LENGTH_SHORT).show();
        }
        //String wordLower = Util.getRemovedNikudString(word.toLowerCase(Locale.US));
        for (int i = 0; i< list.size(); i++){
            boolean foundEn = false;
            text = list.get(i);
            //Test english words
            Matcher m = enPattern.matcher(text.enText);
            if(m.find()){
                text.enText = addRedToFoundWord(m, text.enText, removeLongText);
                foundEn = true;
                foundPositions.add(i);
            }

            ///TEST Hebrew words
            m = hePattern.matcher(text.heText);
            if(m.find()){
                text.heText = addRedToFoundWord(m, text.heText, removeLongText);
                if(!foundEn){//didn't already add this to the list for found English
                    foundPositions.add(i);
                }
            }
        }

        return foundPositions;
    }

    private static String addRedToFoundWord(Matcher m, String orgText, boolean removeLongText){
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
            newText += orgText.substring(lastSpot, m.start()) + "<font color='#ff5566'>"
                    + orgText.substring(m.start(),m.end()) + "</font>";
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


    private static String makeFilterStatement(String[] filterArray, String tableAbr){
        String likeStatement = "";
        if (filterArray.length != 0) {
            likeStatement = " " + tableAbr + "categories LIKE ? ";
            for (int j = 1; j < filterArray.length; j++) {
                likeStatement += " OR " + tableAbr + "categories LIKE ? ";
            }
        }
        return likeStatement;
    }

    private static String [] convertfilterArray(String[] filterArray){
        for (int j = 0; j < filterArray.length; j++) {
            filterArray[j] = "[\"" + filterArray[j] + "\"%";
        }
        return filterArray;
    }

    private static String[] concat(String [] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c= new String[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private static ArrayList<Pair<Integer,Integer>> setSearchableTids(ArrayList<Pair<Integer,Integer>> tidMinMax){
        //chunklist is [) (inclusive, exclusive), while tid is [] (inclusive,inclusive)
        ArrayList<Pair<Integer,Integer>> searchableTidLoc = new ArrayList<Pair<Integer,Integer>>();
        int tidIndex = 0;
        for(int i = 0; i<searchableChunks.size();i++){
            int chunkStart = CHUNK_SIZE*(searchableChunks.get(i));
            int chunkEnd = chunkStart + CHUNK_SIZE - 1;//now both lists are inclusive
            while(chunkStart > tidMinMax.get(tidIndex).second){
                tidIndex++;
                if(tidIndex >= tidMinMax.size()){
                    Log.d("bid", "finished searchableTidLoc");
                    return searchableTidLoc;
                }
            }
            //chunkStart <= tidMinMax.get(tidIndex).second must be true
            if(chunkEnd >= tidMinMax.get(tidIndex).first){
                searchableTidLoc.add(new Pair<Integer, Integer>(
                        Math.max(chunkStart, tidMinMax.get(tidIndex).first), Math.min(chunkEnd, tidMinMax.get(tidIndex).second)));
            }else
                continue;
        }
        Log.d("bid", "finished searchableTidLoc");
        return searchableTidLoc;
    }


    private static ArrayList<Pair<Integer,Integer>> createTidList(String likeStatement, String [] filterArray){
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        ArrayList<Pair<Integer,Integer>> tidMinMax = new ArrayList<Pair<Integer,Integer>>();
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
                Pair<Integer, Integer> tempPair = new Pair<Integer, Integer>(first, second);
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
        return tidMinMax;//it was doing stuff on the local tidList
    }


    private static ArrayList<Text> APISearch(String query, String[] filterArray) throws API.APIException{
        int offset = 10;
        ArrayList<Text> resultsList =  API.getSearchResults(query, filterArray, APIStart,offset);
        APIStart += offset;
        if(resultsList.size() == 0)
            doneSearching = true;
        return resultsList;
    }

    public static ArrayList<Text> searchDBheTexts(String query, String[] filterArray) throws InterruptedException, API.APIException {
        if(API.useAPI()){
            return APISearch(query, filterArray);
        }


        int startingChunk = currChunkIndex;
        ArrayList<Text> resultsList = new ArrayList<Text>();
        int endSearchingLoop;
        long startTime = System.currentTimeMillis();
        try {
            Database dbHandler = Database.getInstance();
            SQLiteDatabase db = dbHandler.getReadableDatabase();

            if(searchableChunks == null)//it's the first time searching this word
                searchableChunks = getSearchingChunks(query);

            if(filterArray.length >0){//use filter
                String likeStatement = makeFilterStatement(filterArray, "");
                filterArray = convertfilterArray(filterArray);
                if(searchableTids == null){//first time, so make tidList
                    if(searchableChunks.size() != 0)
                        searchableTids = setSearchableTids(createTidList(likeStatement, filterArray));
                    else
                        searchableTids = new ArrayList<Pair<Integer,Integer>>();
                }
                endSearchingLoop = searchableTids.size();
            }
            else
                endSearchingLoop = searchableChunks.size();

            String [] words = getWords(query);
            Pattern [] patterns = new Pattern [words.length];
            for(int i = 0; i< patterns.length; i++){
                patterns[i] = nikkudlessRegEx(words[i],true);
            }

            Cursor cursor = null;
            for(int i=startingChunk + 1; i<endSearchingLoop;i++){
                if (interrupted) {
                    throw new InterruptedException();
                }

                if(resultsList.size() >= 6 || (resultsList.size() >= 3  && System.currentTimeMillis() > startTime + WAITING_TIME)){
                    return resultsList;
                }
                String sql;
                if(filterArray.length > 0){//use filter
                    sql = "SELECT _id, heText FROM Texts WHERE "
                            + " _id >= " + searchableTids.get(i).first + " AND _id <= "  + searchableTids.get(i).second;
                }
                else{
                    sql = "select _id, heText from Texts "+
                            " WHERE _id >= " + CHUNK_SIZE*searchableChunks.get(i) + " AND _id < "  + CHUNK_SIZE*(searchableChunks.get(i)+1) ;	//	+ " AND heText NOT NULL "
                }
                cursor = db.rawQuery(sql, null);//filterArray

                String verse = null;
                if (cursor.moveToFirst()) { //TODO CursorWIndowAllocationException
                    do {

                        verse = cursor.getString(1);
                        if(verse == null)
                            continue;//might not be needed if doing at DB level
                        //String cleacursorredVerse = verse.replaceAll("[\u0591-\u05C7]", "");
                        //if(verse.length() > 4000) continue;//TODO this is a major hackk!!, but we aren't searching huge things

                        for(int j=0; j<words.length;j++){


                            Matcher m = patterns[j].matcher(verse);
                            if(m.find()){
                                //if(clearedVerse.contains(words[j])){ //if(verse.replaceFirst(wordsRegEx[j], "ABC").hashCode() != verse.hashCode()){ //more detailed //	if(clearedVerse.replaceAll("\\b\\u05d5","").replaceFirst("\\b" + words[j] + "\\b", "ABC").hashCode() == clearedVerse.hashCode()) break;

                                verse = addRedToFoundWord(m,verse, true);
                                if(j == words.length -1){
                                    resultsList.add(new Text(cursor.getInt(0)));//maybe create a list of tids and do it as one big search
                                    resultsList.get(resultsList.size()-1).heText = verse;
                                }
                            }
                            else
                                break;
                        }
                    }
                    while (cursor.moveToNext());
                }
                cursor.close();
                currChunkIndex = i;
            }


        } catch (SQLException e) {
            GoogleTracker.sendException(e, "DB_search");
        }

        if (resultsList.size() == 0) doneSearching = true;

        return resultsList;
    }

    public static ArrayList<Text> searchEnTexts(String word, String [] filterArray) throws API.APIException {
        if(API.useAPI()){
            return APISearch(word, filterArray);
        }

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

}