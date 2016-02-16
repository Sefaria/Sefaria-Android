package org.sefaria.sefaria.database;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;


import java.util.ArrayList;
import java.util.List;

import org.sefaria.sefaria.Util;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Text implements Parcelable {

    public static final int MAX_LEVELS = 4;
    public static final double BILINGUAL_THRESHOLD = 0.05; //percentage of text that is bilingual for us to default to bilingual

    private static boolean usingHuffman = true;



    public Node parentNode = null; //for SectionAdapter. not null indicates that this obj is actually a placeholder for a perek title (and the node represents that perek)
    public int tid;
    public int bid;
    private String enText;
    private String heText;
    private byte [] enTextCompress;
    private int enTextLength = 0;
    private int heTextLength = 0;
    private byte [] heTextCompress;
    private boolean isChapter = false;
    private int parentNID;

    public String getText(Util.Lang lang)
    {
        if (lang == Util.Lang.EN) {
            if (enText == null) {
                if (enTextCompress == null)
                    enText = "";
                else
                    enText = Huffman.decode(enTextCompress, enTextLength);
            }
            return enText;
        } else if (lang == Util.Lang.HE) {
            if(heText == null){
                if(heTextCompress == null)
                    heText = "";
                else
                    heText = Huffman.decode(heTextCompress,heTextLength);
            }
            return heText;
        } else {
            Log.e("Text","Input wrong lang into Text.getText(Util.lang)");
            return "";
        }
    }

    public void setText(String text, Util.Lang lang){
        if(lang == Util.Lang.HE)
            heText = text;
        else
            enText = text;
    }


    private int numLinks = 0;

    public int getNumLinks(){ return numLinks;}

    public boolean isChapter() { return isChapter;}
    /**
     * Little sections (like verse) to Big (like chap) and the rest zeros
     * For ex. chapter 3, verse 8 would be {8,3,0,0,0,0}
     */
    public int [] levels;
    //public int hid;
    public boolean displayNum;

    public static final String TABLE_TEXTS = "Texts";



    public Text(){
        //empty
    }


    /**
     * this is used as a chapter heading as part of the text list
     * @param node
     */
    public Text(Node node) {
        isChapter = true;
        parentNode = node;
        this.enText = node.getWholeTitle(Util.Lang.EN);
        this.heText = node.getWholeTitle(Util.Lang.HE);
    }

    public Text(Cursor cursor ){
        getFromCursor(cursor);
    }

    // NEW CONSTRUCTOR FOR API:
    public Text(String enText, String heText) {
        this.enText = enText;
        this.heText = heText;
        this.tid = 0;
        this.bid = 0;
        levels = new int [MAX_LEVELS];
        this.displayNum = true;//unless we know otherwise, we'll default to display the verse Number
    }

    public Text(int tid) {
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        try{
            Cursor cursor = db.query(TABLE_TEXTS, null, "_id" + "=?",
                    new String[] { String.valueOf(tid) }, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()){
                getFromCursor(cursor);
            }
            else{
                this.tid = 0;
                this.levels = new int [MAX_LEVELS];
            }
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            //This probably means that it's the API database
            this.tid = 0;
            this.levels = new int [MAX_LEVELS];
        }
    }


    private void getFromCursor(Cursor cursor){
        tid = cursor.getInt(0);
        bid = cursor.getInt(1);
        int flags = cursor.getInt(MAX_LEVELS +4);
        displayNum = ((flags & 0x01) != 0);
        if(!usingHuffman) {
            enText = cursor.getString(2);
            heText = cursor.getString(3);
        }
        else{
            enTextCompress = cursor.getBlob(2);
            heTextCompress = cursor.getBlob(3);

            enTextLength =  (flags & 0x0e)>>1;
            if(enTextCompress != null)
                enTextLength += (enTextCompress.length-((enTextLength != 0)?1:0))*8;
            heTextLength =  (flags & 0x70)>>4;
            if(heTextCompress != null)
                heTextLength += (heTextCompress.length-((heTextLength != 0)?1:0))*8;

        }
        levels = new int [MAX_LEVELS];
        for(int i=0;i<MAX_LEVELS;i++){
            levels[i] = cursor.getInt(i+4);
        }

        numLinks = cursor.getInt(MAX_LEVELS+5);
        parentNID = cursor.getInt(MAX_LEVELS+6);
    }

    public String getLocationString(Util.Lang lang){
        Book book = new Book(bid);
        String str = book.getTitle(lang);
        if(parentNID != 0){ //It's a regular non-Complex text
            str += " <complex> ";
        }
        int sectionNum = book.sectionNamesL2B.length-1;
        boolean useSpace = true; //starting true so has space after book.title
        for(int i=levels.length-1;i>=0;i--){
            int num = levels[i];
            if(num == 0) continue;
            boolean isDaf = false;

            if(book.sectionNamesL2B.length > sectionNum && sectionNum >0) {
                isDaf = (book.sectionNamesL2B[sectionNum].equals("Daf"));
                //str += " " + book.sectionNamesL2B[sectionNum];
            }
            if(useSpace)
                str +=  " " +  Header.getNiceGridNum(lang,num,isDaf);
            else
                str +=  ":" +  Header.getNiceGridNum(lang,num,isDaf);

            sectionNum--;
            useSpace = isDaf && (lang == Util.Lang.HE);
        }



        return str;
    }

    private static List<Text> get(Book book, int[] levels) throws API.APIException {

        if(book.textDepth != levels.length){
            Log.e("Error_sql", "wrong size of levels.");
            //return new ArrayList<Text>();
        }
        //else
        return get(book.bid, levels,0);
    }


	/*
	public static void removeFoundWordsColoring(List<Text> list){
		Text text = null;
		for (int i = 0; i< list.size(); i++){
			text = list.get(i);
			text.enText = text.enText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
			text.heText = text.heText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
		}
		return;
	}
	 */



    private static List<Text> getFromDB(int bid, int[] levels, int parentNID) {
        List<Text> textList = new ArrayList<Text>();
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        String sql = "SELECT DISTINCT * FROM "+ TABLE_TEXTS +" " + fullWhere(bid, levels, parentNID) + " ORDER BY " + orderBy(levels);
        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                textList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }
        return textList;

    }




    /**
     *
     * @param id
     * @param levels
     *
     *
     * @return textList
     * @throws API.APIException
     */

    /**
     *
     * @param bid
     * @param levels
     * @param parentNID
     *  set parentNID == 0 if you don't want to use parentNID at all
     *  Sample usage:
     *  int[] levels = new {0, 12};
     * 	Text.get(1, levels,false); //get book bid 1 everything in chap 12.
     * @return textList
     * @throws API.APIException
     */
    public static List<Text> get(int bid, int[] levels, int parentNID) throws API.APIException {
        List<Text> textList = new ArrayList<>();
        try {
            if(API.useAPI()){
                if(parentNID <=0) //TODO make it work for API with NID
                    textList = API.getTextsFromAPI(Book.getTitle(bid), levels);
            }else{
                textList = getFromDB(bid,levels,parentNID);
            }
        }catch(Exception e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }

        }

        return textList;
    }

    public static List<Text> getWithTids(int startTID,int endTID){
        List<Text> textList = new ArrayList<Text>();
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        String sql = "SELECT * FROM "+ TABLE_TEXTS +" where _id BETWEEN ? AND ? ORDER BY _id";
        Cursor cursor = db.rawQuery(sql, new String [] {"" + startTID,"" + endTID});

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                textList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }
        return textList;
    }


    public static Text makeDummyChapText(Text text){
        int wherePage = (new Book(text.bid)).wherePage;
        Text dummyChapText = deepCopy(text);
        //dummyChapText.log();

        for(int i=0;i<6;i++){
            if(wherePage > i+1)
                dummyChapText.levels[i] = -1;
        }

        //TODO check that it's correct to use ">" for all types of where pages.
        Log.d("sql_dummytext", "wherePage: " + wherePage);
        Log.d("sql", "TODO check that it's correct to use '>' for all types of where pages.");
        dummyChapText.log();

        return dummyChapText;
    }

    public static Text makeDummyChapText0(Text text, int wherePage){
        Text dummyChapText = deepCopy(text);
        //dummyChapText.log();

        for(int i=0;i<6;i++){
            if(wherePage > i+1)
                dummyChapText.levels[i] = 0;
        }

        //TODO check that it's correct to use ">" for all types of where pages.
        Log.d("sql_dummytext", "wherePage: " + wherePage);
        Log.d("sql", "TODO check that it's correct to use '>' for all types of where pages.");
        dummyChapText.log();

        return dummyChapText;
    }


    public static void searchAppLevel(String langType, String word, int how) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    int chunkSize = 5000;
                    Database dbHandler = Database.getInstance();
                    SQLiteDatabase db = dbHandler.getReadableDatabase();

                    List<Text> textList = new ArrayList<Text>();

                    Cursor cursor;
                    Log.d("sql_textFind", "start");
                    String sql = "select heText from Texts "+
                            " WHERE _id >= " + 0  + " AND _id <" + chunkSize ;

                    // +
                    //" where (bid = 1 OR bid = 11 OR bid = 12 OR bid = 13 OR bid = 14 OR bid = 51 OR bid = 61 OR bid = 71 OR bid = 81 OR bid = 91 OR bid = 111 OR bid = 2 OR bid = 22 OR bid = 22 OR bid = 23 OR bid = 24 OR bid = 52 OR bid = 62 OR bid = 72 OR bid = 82 OR bid = 92 OR bid = 222 " +
                    //"OR bid = 3 OR bid = 33 OR bid = 33 OR bid = 33 OR bid = 34 OR bid = 53 OR bid = 63 OR bid = 73 OR bid = 83 OR bid = 93 OR bid = 333 " +
                    ///"OR bid = 4 OR bid = 44 OR bid = 44 OR bid = 43 OR bid = 44 OR bid = 54 OR bid = 64 OR bid = 74 OR bid = 84 OR bid = 94 OR bid = 444 " +
                    //"OR bid = 5 OR bid = 55 OR bid = 55 OR bid = 53 OR bid = 54 OR bid = 55 OR bid = 65 OR bid = 75 OR bid = 85 OR bid = 95 OR bid = 555 " +
                    //"OR bid = 6 OR bid = 66 OR bid = 66 OR bid = 63 OR bid = 64 OR bid = 56 OR bid = 66 OR bid = 76 OR bid = 86 OR bid = 96 OR bid = 666 " +
                    //"OR bid = 7 OR bid = 77 OR bid = 77 OR bid = 73 OR bid = 74 OR bid = 57 OR bid = 67 OR bid = 77 OR bid = 87 OR bid = 97 OR bid = 777 " +
                    //"OR bid = 8 OR bid = 88 OR bid = 88 OR bid = 83 OR bid = 84 OR bid = 58 OR bid = 68 OR bid = 78 OR bid = 88 OR bid = 98 OR bid = 888 " +
                    //")"
                    ;// AND enText like '%gems%' LIMIT 10";
                    cursor = db.rawQuery(sql, null);
                    // looping through all rows and adding to list
                    int count = 0;
                    int foundCount = 0;
                    String yo = null;
                    if (cursor.moveToFirst()) {
                        do {

                            yo = cursor.getString(0);
                            if(yo == null)
                                continue;
                            count++;
                            if(Util.getRemovedNikudString(yo).contains("\u05d1\u05d2"))
                                //if(yo.replaceAll("[\u0591-\u05C7]", "").contains("\u05d1\u05d2"))
                                foundCount++;

                            //textList.add(new Text(cursor));

                        } while (cursor.moveToNext());
                    }
                    Log.d("sql_textFind", "end.." + "finished!!! " + foundCount + " ..."  + count);
                    //LOGING:
                    //for(int i = 0; i < textList.size(); i++)
                    //	textList.get(i).log();
                }catch(Exception e){
                    GoogleTracker.sendException(e, "moving index.json");
                }
            }
        }).start();
        return;
    }


    public static int getNonZeroLevel(int[] levels) {
        int nonZeroLevel;
        for(nonZeroLevel = 0; nonZeroLevel < levels.length; nonZeroLevel++){
            if(levels[nonZeroLevel] != 0)
                break;
        }
        return nonZeroLevel;
    }


    /**
     * //TODO maybe add usingNID to function and generalize it
     * @param bid
     * @param levels
     * @return
     * @throws API.APIException
     */
    public static ArrayList<Integer> getChaps(int bid, int[] levels) throws API.APIException {
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        ArrayList<Integer> chapList = new ArrayList<Integer>();

        int nonZeroLevel = getNonZeroLevel(levels);
        String sql = "SELECT DISTINCT level" + nonZeroLevel + " FROM " + TABLE_TEXTS + " " + fullWhere(bid, levels,0) + " ORDER BY " + "level" + nonZeroLevel;

        try {
            Cursor cursor = db.rawQuery(sql, null);

            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    // Adding  to list
                    chapList.add(Integer.valueOf((cursor.getInt(0))));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            chapList = API.getChaps(Book.getTitle(bid), levels);

        }

		/*//LOGING:
	        for(int i = 0; i < chapList.size(); i++)
	            Log.d("sql_chapList" , chapList.get(i).toString());
		 */
        return chapList;
    }


    /**
     *  makes a where statement for getting the texts from a book with a id (as bid) or with text that have a parentNode with nid of id
     *
     * @param bid
     * @param levels
     * @return whereStatement
     */
    private static String fullWhere(int bid, int[] levels, int parentNID){
        String fullWhere;

        fullWhere = " WHERE " + Kbid + "= " + String.valueOf(bid);
        if(parentNID >0)
            fullWhere += " AND parentNode = " + String.valueOf(parentNID);
        for(int i = 0; i < levels.length; i++){
            if(!(levels[i] == 0)){
                fullWhere +=  " AND level" + String.valueOf(i + 1) + "= " + String.valueOf(levels[i]);
            }
        }
        return fullWhere;
    }

    private static String orderBy(int[] levels){
        String orderBy = Klevel1;
        for(int i = 0; i < levels.length - 2; i++){
            orderBy = "level"+ String.valueOf(i + 2) + ", " + orderBy;
        }
        return orderBy;
    }


    public void log() {
        Log.d("text", toString());
    }

    private static String convertLangToLangText(String lang){
        if(lang.equals("en"))
            return KenText;
        else if(lang.equals("he"))
            return  KheText;
        Log.e( "sql_text_convertLang", "Unknown lang");
        return "";
    }

    @Override
    public int hashCode() {
        return tid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Text))
            return false;

        Text text = (Text) o;
        return text.tid == this.tid;
    }

    public static Text deepCopy(Text text) {
        Text newText = new Text();
        newText.bid = text.bid;
        newText.enText = text.enText;
        newText.heText = text.heText;
        newText.levels = text.levels.clone();
        newText.tid    = text.tid;
        newText.displayNum = text.displayNum;
        return newText;
    }

    @Override
    public String toString() {
        String string =  tid + "-" + bid ;
        for(int i=0;i<levels.length;i++)
            string+= "." + levels[i];
        string += " " + enText + " " + heText;
        return string;
    }


    private static final String Kbid = "bid";
    private static final String KenText = "enText";
    private static final String KheText = "heText";
    protected static final String Klevel1 = "level1";
    protected static final String Klevel2 = "level2";
    protected static final String Klevel3 = "level3";
    protected static final String Klevel4 = "level4";
    //public static final String Khid = Header.Khid;


    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Text> CREATOR
            = new Parcelable.Creator<Text>() {
        public Text createFromParcel(Parcel in) {
            return new Text(in);
        }

        public Text[] newArray(int size) {
            return new Text[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(tid);
        dest.writeInt(bid);
        dest.writeString(enText);
        dest.writeString(heText);
        dest.writeIntArray(levels);
        dest.writeInt(displayNum ? 1 : 0);
    }

    private Text(Parcel in) {
        tid = in.readInt();
        bid = in.readInt();
        enText = in.readString();
        heText = in.readString();
        levels = in.createIntArray();
        displayNum = in.readInt() != 0;
    }
}