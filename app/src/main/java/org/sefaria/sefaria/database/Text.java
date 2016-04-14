package org.sefaria.sefaria.database;
import org.sefaria.sefaria.GoogleTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

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
    //protected int parentNID;
    private String ref = null;

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
        } else{// if(lang == Util.Lang.BI) {
            return getText(Util.Lang.HE) + "<br>\n" + getText(Util.Lang.EN);
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

    public String getRef(){ return ref; }
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
    public Text(String enText, String heText, int bid, String ref) {
        this.enText = enText;
        this.heText = heText;
        this.tid = 0;
        this.bid = bid;
        this.ref = ref;
        levels = new int [MAX_LEVELS];
        this.displayNum = true;//unless we know otherwise, we'll default to display the verse Number
    }

    public Text(int tid) {
        SQLiteDatabase db = Database.getDB();
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
        //parentNID = cursor.getInt(MAX_LEVELS+6);
    }


    /**
     *
     * @param prependDomain if you want to go to the GUI site
     * @param useHTTPS if you want to bypass the Intent calling the app again
     * @return
     */
    public String getURL(boolean prependDomain, boolean useHTTPS){

        StringBuilder str = new StringBuilder();
        if(prependDomain) {
            if (useHTTPS)
                str.append("https://www.sefaria.org/");
            else
                str.append("http://www.sefaria.org/");
        }
        if(parentNode != null && (!parentNode.isRef())){
            String path = parentNode.getPath(Util.Lang.EN,true, true, true) + "." + levels[0];
            return str + path;
        }

        Book book = null;
        try {
            book = new Book(bid);
        } catch (Book.BookNotFoundException e) {
            return "";
        }
        str.append(book.getTitle(Util.Lang.EN));
        int sectionNum = book.sectionNamesL2B.length-1;
        for(int i=levels.length-1;i>=0;i--){
            int num = levels[i];
            if(num == 0) continue;
            boolean isDaf = false;
            if(book.sectionNamesL2B.length > sectionNum && sectionNum >0) {
                isDaf = (book.sectionNamesL2B[sectionNum].equals("Daf"));
            }
            str.append("." +  Header.getNiceGridNum(Util.Lang.EN,num,isDaf));
            sectionNum--;
        }
        return str.toString().replace(" ","_");
    }

    public String getLocationString(Util.Lang lang){
        if(ref != null)
            return ref;
        Book book = null;
        try {
            book = new Book(bid);
        } catch (Book.BookNotFoundException e) {
            return "";
        }
        String str = book.getTitle(lang);
        if(parentNode != null && !parentNode.isRef()){ //It's a complex text... I Don't think it's always complex text... it could also be just from the Popupmenu for example

            str = parentNode.getPath(lang,false, true, false);
            if(str.charAt(str.length()-1) == '.' || str.charAt(str.length()-1) == ':')// it ends in a daf
                str += " " + Header.getNiceGridNum(lang,levels[0],false);
            else
                str += ":" + Header.getNiceGridNum(lang,levels[0],false);
            Log.d("Text", "getLocationStri using getPath()" + str);
            return str;
        }
        Log.d("Text", "getLocationStri using levels");
        int sectionNum = book.sectionNamesL2B.length-1;
        boolean useSpace = true; //starting true so has space after book.title
        for(int i=levels.length-1;i>=0;i--){
            int num = levels[i];
            if(num == 0) continue;
            boolean isDaf = false;

            if(book.sectionNamesL2B.length > sectionNum && sectionNum >0) {
                isDaf = (book.sectionNamesL2B[sectionNum].equals("Daf"));
                //str += " " + book.psectionNamesL2B[sectionNum];
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

    /**
     *
     * @param book
     * @return the Node that is the parent of the current Text or null if it has problems doing that
     * @throws API.APIException
     * @throws Book.BookNotFoundException
     */
    public Node getNodeFromText(Book book) throws API.APIException, Book.BookNotFoundException {
        Text text = this;
        if(text.ref != null && text.ref.length() > 0){
            API.PlaceRef placeRef = API.PlaceRef.getPlace(text.ref,book);
            text.parentNode = placeRef.node;
            if(placeRef.text != null){
                text.levels = placeRef.text.levels.clone();
                text.tid = placeRef.text.tid;
            }
            return parentNode;
        }

        List<Node> roots = Node.getRoots(book);
        if(roots.size() == 0){
            return null; //TODO deal with if can't find TOCRoots
        }
        Node node = roots.get(0);
        try {
            if (!node.isComplex()) {
                for (int i = text.levels.length-1; i > 0; i--) {
                    if (text.levels[i] == 0)
                        continue;
                    int num = text.levels[i];
                    boolean foundChild = false;
                    for(Node child:node.getChildren()) {
                        if(num == child.gridNum) {
                            node = child;
                            foundChild = true;
                            break;
                        }
                    }
                    if(!foundChild){
                        Log.e("Node","Problem finding getNodeFromLink child. node" + node);
                        text.parentNode = node.getFirstDescendant();
                        return text.parentNode;
                    }

                }
            }else{
                Log.d("Node","not getting complex node yet");
                text.parentNode = node.getFirstDescendant();
                return text.parentNode;
            }
        }catch (Exception e){
            Log.d("Node",e.toString());
            return null;
        }

        text.parentNode = node.getFirstDescendant();
        return parentNode;
    }


    private static List<Text> getFromDB(Book book, int[] levels) throws API.APIException {
        if(book.textDepth != levels.length){
            Log.e("Error_sql", "wrong size of levels.");
            return new ArrayList<>();
        }
        return getFromDB(book.bid, levels,0);
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



    public static List<Text> getFromDB(int bid, int[] levels, int parentNID) {
        List<Text> textList = new ArrayList<>();
        SQLiteDatabase db = Database.getDB();

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





    /*
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

    public static List<Text> get(int bid, int[] levels, int parentNID) throws API.APIException {
        List<Text> textList = new ArrayList<>();
        try {
            if(Settings.getUseAPI()){
                if(parentNID <=0) //TODO make it work for API with NID
                    textList = API.getTextsFromAPI1(Book.getTitle(bid), levels);
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
    */

    public static List<Text> getWithTids(int startTID,int endTID){
        List<Text> textList = new ArrayList<Text>();
        SQLiteDatabase db = Database.getDB();

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


    /*
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
    */
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
                    SQLiteDatabase db = Database.getDB();

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

        return chapList;
    }
    */

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
        Log.e("sql_text_convertLang", "Unknown lang");
        return "";
    }

    @Override
    public int hashCode() {
        if(tid == 0)
            return super.hashCode();
        return tid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Text))
            return false;

        Text text = (Text) o;
        if(text.tid != 0 && this.tid != 0)
            return text.tid == this.tid;

        if(super.equals(o))
            return true;

        boolean isEqual =  (
                Arrays.equals(text.levels,this.levels)
                &&
                this.bid == text.bid
                //&& this.parentNode.equals(text.parentNode)
                //TODO maybe needs stricter def... but for now this is fine
        );
        if(this.parentNode != null && text.parentNode != null)
            return isEqual && this.parentNode.pseudoEquals(text.parentNode);
        return isEqual;
        /*
        if((text.tid == 0 && this.tid ==0) || (text.ref != null || this.ref != null)){
            //return super.equals(o);
        }
        */

    }

    private static Text deepCopy(Text text) {
        Text newText = new Text();
        newText.bid = text.bid;
        newText.enText = text.getText(Util.Lang.EN);
        newText.heText = text.getText(Util.Lang.HE);
        newText.levels = text.levels.clone();
        newText.tid    = text.tid;
        newText.displayNum = text.displayNum;
        newText.parentNode = text.parentNode;
        return newText;
    }

    @Override
    public String toString() {
        String string =  tid + "-" + bid ;
        try {
            for (int i = 0; i < levels.length; i++)
                string += "." + levels[i];
            string += " " + getText(Util.Lang.BI);
        }catch (Exception e){
            ;
        }
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
        dest.writeString(ref);
    }

    private Text(Parcel in) {
        tid = in.readInt();
        bid = in.readInt();
        enText = in.readString();
        heText = in.readString();
        levels = in.createIntArray();
        displayNum = in.readInt() != 0;
        ref = in.readString();
    }
}