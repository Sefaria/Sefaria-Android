package org.sefaria.sefaria.database;
import org.sefaria.sefaria.GoogleTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sefaria.sefaria.Util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Segment implements Parcelable {

    public static final int MAX_LEVELS = 4;
    public static final double BILINGUAL_THRESHOLD = 0.05; //percentage of segment that is bilingual for us to default to bilingual

    private static final boolean usingHuffman = true;



    public Node parentNode = null; //for SepTextAdapter. not null indicates that this obj is actually a placeholder for a perek title (and the node represents that perek)
    public int tid;
    public int bid;
    private String enText;
    private String heText;
    private byte [] enTextCompress;
    private int enTextLength = 0;
    private int heTextLength = 0;
    private byte [] heTextCompress;
    private boolean isChapter = false;
    private boolean isLoader = false; //true when this is a filler segment which indicates that the next/prev section is loading
    private boolean chapterHasTexts = true;
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

    public void setChapterHasTexts(boolean chapterHasTexts){
        this.chapterHasTexts = chapterHasTexts;
    }

    public boolean getChapterHasTexts(){
        return chapterHasTexts;
    }

    private int numLinks = 0;

    public int getNumLinks(){ return numLinks;}

    public String getRef(){ return ref; }
    public boolean isChapter() { return isChapter;}
    public boolean isLoader() { return isLoader;}
    /**
     * Little sections (like verse) to Big (like chap) and the rest zeros
     * For ex. chapter 3, verse 8 would be {8,3,0,0,0,0}
     */
    public int [] levels;
    //public int hid;
    public boolean displayNum;

    public static final String TABLE_TEXTS = "Texts";





    /**
     * this is used as a chapter heading as part of the segment list
     * @param node
     */
    public Segment(Node node) {
        isChapter = true;
        parentNode = node;
        levels = new int [MAX_LEVELS];

        if (node != null) {
            boolean doSectionName = true;
            try {
                String category  = node.getBook().categories[0];
                if ((category.equals("Tanach") || category.equals("Talmud") || category.equals("Mishnah") || category.equals("Tosefta")) && (node.getTocRootNum() == 0))
                    doSectionName = false;
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.enText = node.getWholeTitle(Util.Lang.EN, doSectionName, false);
            this.heText = node.getWholeTitle(Util.Lang.HE, doSectionName, false);
        }
    }

    /**
     * Filler constructor to make a Segment object which indicates that the next/prev section is loading
     * @param isLoader
     */
    public Segment(boolean isLoader) {
        this.isLoader = isLoader;
    }

    public Segment(Cursor cursor ){
        getFromCursor(cursor);
    }

    // NEW CONSTRUCTOR FOR API:
    public Segment(String enText, String heText, int bid, String ref) {
        this.enText = enText;
        this.heText = heText;
        this.tid = 0;
        this.bid = bid;
        this.ref = ref;
        levels = new int [MAX_LEVELS];
        this.displayNum = true;//unless we know otherwise, we'll default to display the verse Number
    }

    public Segment(int tid) {
        SQLiteDatabase db = Database.getDB();
        try{
            Cursor cursor = db.query(TABLE_TEXTS, null, "_id" + "=?",
                    new String[] { String.valueOf(tid) }, null, null, null, null);

            if (cursor.moveToFirst()){
                getFromCursor(cursor);
            }
            else{
                this.tid = 0;
                this.levels = new int [MAX_LEVELS];
            }
            cursor.close();
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
     * @return "" if there's an error
     */
    public String getURL(boolean prependDomain) throws Book.BookNotFoundException {

        StringBuilder str = new StringBuilder();
        if(prependDomain) {
            str.append("https://www.sefaria.org/");
        }
        if(parentNode != null && (!parentNode.isRef())){
            String path = parentNode.getPath(Util.Lang.EN,true, true, true)
                    + "." + levels[0];
            return str + path;
        }

        Book book = Book.getByBid(bid);
        try {
            str.append(book.getTitle(Util.Lang.EN));
            int sectionNum = book.sectionNamesL2B.length - 1;
            for (int i = levels.length - 1; i >= 0; i--) {
                int num = levels[i];
                if (num == 0) continue;
                boolean isDaf = false;
                if (book.sectionNamesL2B.length > sectionNum && sectionNum > 0) {
                    isDaf = (book.sectionNamesL2B[sectionNum].equals("Daf"));
                }
                str.append("." + Header.getNiceGridNum(Util.Lang.EN, num, isDaf));
                sectionNum--;
            }
        }catch (Exception e){
            throw new Book.BookNotFoundException();
        }
        return str.toString().replace(" ","_");
    }

    public String getLocationString(Util.Lang lang){
        if(ref != null)
            return ref;
        Book book = null;
        try {
            book = Book.getByBid(bid);
        } catch (Book.BookNotFoundException e) {
            return "";
        }
        String str = book.getTitle(lang);
        if(parentNode != null && !parentNode.isRef()){ //It's a complex segment... I Don't think it's always complex segment... it could also be just from the Popupmenu for example

            str = parentNode.getPath(lang,false, true, false);
            if(str.charAt(str.length()-1) == '.' || str.charAt(str.length()-1) == ':')// it ends in a daf
                str += " " + Header.getNiceGridNum(lang,levels[0],false);
            else
                str += ":" + Header.getNiceGridNum(lang,levels[0],false);
            Log.d("Segment", "getLocationStri using getPath()" + str);
            return str;
        }
        Log.d("Segment", "getLocationStri using levels");
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
     * @return the Node that is the parent of the current Segment or null if it has problems doing that
     * @throws API.APIException
     * @throws Book.BookNotFoundException
     */
    public Node getNodeFromText(Book book) throws API.APIException, Book.BookNotFoundException {
        Segment segment = this;
        if(segment.ref != null && segment.ref.length() > 0){
            API.PlaceRef placeRef = API.PlaceRef.getPlace(segment.ref,book);
            segment.parentNode = placeRef.node;
            if(placeRef.segment != null){
                segment.levels = placeRef.segment.levels.clone();
                segment.tid = placeRef.segment.tid;
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
                for (int i = segment.levels.length-1; i > 0; i--) {
                    if (segment.levels[i] == 0)
                        continue;
                    int num = segment.levels[i];
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
                        segment.parentNode = node.getFirstDescendant();
                        return segment.parentNode;
                    }

                }
            }else{
                Log.d("Node","not getting complex node yet");
                segment.parentNode = node.getFirstDescendant();
                return segment.parentNode;
            }
        }catch (Exception e){
            Log.d("Node",e.toString());
            return null;
        }

        segment.parentNode = node.getFirstDescendant();
        return parentNode;
    }


    private static List<Segment> getFromDB(Book book, int[] levels) throws API.APIException {
        if(book.textDepth != levels.length){
            Log.e("Error_sql", "wrong size of levels.");
            return new ArrayList<>();
        }
        return getFromDB(book.bid, levels,0);
    }


	/*
	public static void removeFoundWordsColoring(List<Segment> list){
		Segment segment = null;
		for (int i = 0; i< list.size(); i++){
			segment = list.get(i);
			segment.enText = segment.enText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
			segment.heText = segment.heText.replaceAll("<font color='#ff5566'>",  "").replaceAll("</font>", "");
		}
		return;
	}
	 */



    public static List<Segment> getFromDB(int bid, int[] levels, int parentNID) {
        List<Segment> segmentList = new ArrayList<>();
        SQLiteDatabase db = Database.getDB();

        String sql = "SELECT DISTINCT * FROM "+ TABLE_TEXTS +" " + fullWhere(bid, levels, parentNID) + " ORDER BY " + orderBy(levels);
        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                segmentList.add(new Segment(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return segmentList;

    }





    /*
     *
     * @param bid
     * @param levels
     * @param parentNID
     *  set parentNID == 0 if you don't want to use parentNID at all
     *  Sample usage:
     *  int[] levels = new {0, 12};
     * 	Segment.get(1, levels,false); //get book bid 1 everything in chap 12.
     * @return textList
     * @throws API.APIException

    public static List<Segment> get(int bid, int[] levels, int parentNID) throws API.APIException {
        List<Segment> textList = new ArrayList<>();
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

    public static List<Segment> getWithTids(int startTID, int endTID){
        List<Segment> segmentList = new ArrayList<Segment>();
        SQLiteDatabase db = Database.getDB();

        String sql = "SELECT * FROM "+ TABLE_TEXTS +" where _id BETWEEN ? AND ? ORDER BY _id";
        Cursor cursor = db.rawQuery(sql, new String [] {"" + startTID,"" + endTID});

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                segmentList.add(new Segment(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return segmentList;
    }


    /*
    public static Segment makeDummyChapText(Segment segment){
        int wherePage = (new Book(segment.bid)).wherePage;
        Segment dummyChapText = deepCopy(segment);
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
    public static Segment makeDummyChapText0(Segment segment, int wherePage){
        Segment dummyChapSegment = deepCopy(segment);
        //dummyChapSegment.log();

        for(int i=0;i<6;i++){
            if(wherePage > i+1)
                dummyChapSegment.levels[i] = 0;
        }

        //TODO check that it's correct to use ">" for all types of where pages.
        Log.d("sql_dummytext", "wherePage: " + wherePage);
        Log.d("sql", "TODO check that it's correct to use '>' for all types of where pages.");
        dummyChapSegment.log();

        return dummyChapSegment;
    }


    public static void searchAppLevel(String langType, String word, int how) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    int chunkSize = 5000;
                    SQLiteDatabase db = Database.getDB();

                    List<Segment> segmentList = new ArrayList<Segment>();

                    Cursor cursor;
                    Log.d("sql_textFind", "start");
                    String sql = "select heText from Texts "+
                            " WHERE _id >= " + 0  + " AND _id <" + chunkSize ;

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

                            //segmentList.add(new Segment(cursor));

                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                    Log.d("sql_textFind", "end.." + "finished!!! " + foundCount + " ..."  + count);
                    //LOGGING:
                    //for(int i = 0; i < segmentList.size(); i++)
                    //	segmentList.get(i).log();
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
     *  makes a where statement for getting the texts from a book with a id (as bid) or with segment that have a parentNode with nid of id
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
        Log.d("segment", toString());
    }

    @Override
    public int hashCode() {
        if(tid == 0)
            return super.hashCode();
        return tid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Segment))
            return false;

        Segment segment = (Segment) o;
        if(segment.tid != 0 && this.tid != 0)
            return segment.tid == this.tid;

        if(super.equals(o))
            return true;

        boolean isEqual =  (
                Arrays.equals(segment.levels,this.levels)
                &&
                this.bid == segment.bid
                //&& this.parentNode.equals(segment.parentNode)
                //TODO maybe needs stricter def... but for now this is fine
        );
        if(this.parentNode != null && segment.parentNode != null)
            return isEqual && this.parentNode.pseudoEquals(segment.parentNode);
        return isEqual;
        /*
        if((segment.tid == 0 && this.tid ==0) || (segment.ref != null || this.ref != null)){
            //return super.equals(o);
        }
        */

    }

    private static Segment deepCopy(Segment segment) {
        Segment newSegment = new Segment(segment.getText(Util.Lang.EN), segment.getText(Util.Lang.HE), segment.bid, segment.ref);
        newSegment.levels = segment.levels.clone();
        newSegment.tid    = segment.tid;
        newSegment.displayNum = segment.displayNum;
        newSegment.parentNode = segment.parentNode;
        return newSegment;
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
    protected static final String Klevel1 = "level1";
    protected static final String Klevel2 = "level2";
    protected static final String Klevel3 = "level3";
    protected static final String Klevel4 = "level4";


    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Segment> CREATOR
            = new Parcelable.Creator<Segment>() {
        public Segment createFromParcel(Parcel in) {
            return new Segment(in);
        }

        public Segment[] newArray(int size) {
            return new Segment[size];
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

    private Segment(Parcel in) {
        tid = in.readInt();
        bid = in.readInt();
        enText = in.readString();
        heText = in.readString();
        levels = in.createIntArray();
        displayNum = in.readInt() != 0;
        ref = in.readString();
    }
}