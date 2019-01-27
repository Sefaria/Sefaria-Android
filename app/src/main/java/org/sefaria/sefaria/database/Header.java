package org.sefaria.sefaria.database;
import org.sefaria.sefaria.Util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;



public class Header implements Parcelable {

    public Header()
    {
        displayNum = true;//1 is good default value
        displayLevelType = true; //1 is good default value
        heHeader = "";
        enHeader = "";
    }



    private static final String Kbid = "bid";
    private static final String Klevel1 = "level1";
    private static final String Klevel2 = "level2";
    private static final String Klevel3 = "level3";
    private static final String Klevel4 = "level4";
    private static final String Klevel5 = "level5";
    private static final String Klevel6 = "level6";
    private static final String KdisplayNum = "displayNum";
    private static final String KdisplayLevelType = "displayLevelType";




    public static final String TABLE_HEADERS = "Headers";



    public int hid;
    public int bid;
    private boolean displayNum;
    private boolean displayLevelType;
    public int chapNum;
    public String enHeader;
    public String heHeader;
    private int [] levels;
    public String enNumString;
    public String heNumString;


    private void addFromCursor(Cursor cursor){
        hid = cursor.getInt(0);
        bid = cursor.getInt(1);
        heHeader = cursor.getString(2);
        levels = new int [6];
        for(int i = 0; i<6; i++)
            levels[i] = cursor.getInt(i +3);
        displayNum = (cursor.getInt(9) != 0);
        displayLevelType = (cursor.getInt(10) != 0);
        chapNum = levels[getNonZeroLevel(levels)];
        enHeader = cursor.getString(11);
        if(enHeader.equals(""))
            enHeader = heHeader;
        if(heHeader.equals(""))
            heHeader = enHeader;
        enNumString = String.valueOf(chapNum);
        heNumString = Util.int2heb(chapNum);

    }


    /***
     *
     * @param levels
     * @param book
     * @return {textLocationEn, textLocationHe, enchapNumTotalString, heChapNumTotalString}
     */

    public static String [] getTextLocationString(int [] levels, Book book) {
        String textLocationEn = "";
        String textLocationHe = "";
        String enChapNumTotal = "";
        String heChapNumTotal = "";


        int[] tempLevels = Arrays.copyOfRange(levels, 0,book.textDepth);

        ArrayList<Header> headers = Header.getAllSectionDepthsHeaders(book, tempLevels);
        boolean isFirst = true;
        for (Header head : headers) {
            if (!isFirst){
                textLocationEn = " " + textLocationEn;
                textLocationHe = " " + textLocationHe;
                enChapNumTotal = ":" + enChapNumTotal;
                heChapNumTotal = ":" + heChapNumTotal;

            }
            else isFirst = false;

            textLocationEn = head.enHeader + textLocationEn;
            textLocationHe = head.heHeader + textLocationHe;
            enChapNumTotal = head.enNumString + enChapNumTotal;
            heChapNumTotal = head.heNumString + heChapNumTotal;

            //else return new Pair<String, String>(textLocationEn, textLocationHe);//don't this needed line
        }
        heChapNumTotal = heChapNumTotal.replace(".:", ". ").replace("::", ": "); //fix weird daf thing
        return new String [] {textLocationEn, textLocationHe, enChapNumTotal, heChapNumTotal};
    }


    /**
     * will get the grid number in the right language and all converted if it's a daf
     * @param lang
     * @return
     */
    public static String getNiceGridNum(Util.Lang lang, int num, boolean isDaf){
        if(!isDaf){
            if(Util.Lang.HE == lang)
                return Util.int2heb(num);
            else
                return ""+ num;

        }else{
            if(Util.Lang.HE == lang)
                return Header.num2heDaf(num);
            else
                return Header.num2enDaf(num);
        }
    }

    /***
     *
     * levels should be as long as the full length of textDepth...
     * @param book
     * @param levels (could be segment.levels) ex. {0, 2, 3} would return headers.size() == 2, with headers.get(0) refering to the middle level (2), and headers.get(1) would refer to the highest level (3)
     * @return empty list if there's a problem. And normally a list of headers for each nonZero level of levels (headers.get(0) is the lowest non zero level (ex. verse))
     */
    private static ArrayList<Header> getAllSectionDepthsHeaders(Book book, int [] levels){
        SQLiteDatabase db = Database.getDB();
        ArrayList <Header> headers = new ArrayList<Header> ();
        ArrayList <Header> finalHeaders = new ArrayList<Header> ();
        if(book.textDepth != levels.length)
            return finalHeaders;
        int nonZeroLevel = getNonZeroLevel(levels);
        for(int i = nonZeroLevel; i<levels.length; i++){
            if(levels[i] == 0)
                return finalHeaders;
        }
        int numOfRequestedLevels = levels.length - nonZeroLevel;

        String sql = "SELECT * FROM Headers WHERE bid = " + book.bid;
        for(int  i = 0; i < levels.length; i++){
            if(levels[i] ==  0)
                ;//it's not completely defined
            sql += " AND (level" + (i+1) + " = " + 0 + " OR " + "level" + (i+1) + " = " + levels[i] + ") ";
        }
        sql += " ORDER BY ";
        for(int i = levels.length; i>0; i--){
            sql += " level" + i;
            if(i>1)
                sql += ", ";
        }
        Log.d("header","1." + sql);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                Header header = new Header();
                header.addFromCursor(cursor);
                headers.add(header);
            } while (cursor.moveToNext());
        }

        if(headers.size() == 0){
            for(int i = nonZeroLevel; i< levels.length; i++){
                finalHeaders.add(createHeader(book.sectionNamesL2B[i],  book.heSectionNamesL2B[i], levels[i]));
            }
        }
        else if(headers.size() == levels.length){
            for(int i = 0; i< levels.length; i++){
                finalHeaders.add(completeHeader(headers.get(i), book.sectionNamesL2B[i],  book.heSectionNamesL2B[i]));
            }
        }
        else if(headers.size()< levels.length){//find the missing ones
            int headNum = 0;

            for(int i = levels.length - 1; i>=0; i--){
                if(headNum + 1 > headers.size()){
                    if(headNum + 1 > numOfRequestedLevels){
                        break;
                    }
                    finalHeaders.add(createHeader(book.sectionNamesL2B[i],  book.heSectionNamesL2B[i], levels[i]));
                    headNum++;
                    continue;
                }
                int nonZero = getNonZeroLevel(headers.get(headNum).levels);
                if(nonZero == i){
                    finalHeaders.add(completeHeader(headers.get(headNum++), book.sectionNamesL2B[i],  book.heSectionNamesL2B[i]));
                }
                else if(nonZero < i){
                    finalHeaders.add(createHeader(book.sectionNamesL2B[i],  book.heSectionNamesL2B[i], levels[i]));
                    headNum++;
                }else if(nonZero > i){//problem
                    Log.e("sql_headers", "weird list");
                    return  new ArrayList<Header> ();

                }

            }
            ///this is a backwards loop, so in it, b/f returning you want to flip it
            Collections.reverse(finalHeaders);
        }
        else if(headers.size()> levels.length){//that's a problem
            Log.e("sql_headers", "List too long");
            return finalHeaders;
            //finalHeaders = headers;
        }


        return finalHeaders;
    }

    private static int num2DafNum(int num){
        return (num+1)/2;
    }

    public static String num2enDaf(int num){
        String daf = String.valueOf(num2DafNum(num));
        if(num % 2 == 1)
            daf += "a";
        else
            daf += "b";
        return daf;
    }

    public static String num2heDaf(int num){
        String daf = Util.int2heb(num2DafNum(num));
        if(num % 2 == 1)
            daf += ".";//"\u05f4\u05d0";
        else
            daf += ":";//"\u05f4\u05d1";
        return daf;
    }

    private static Header completeHeader(Header tempHeader, String sectionName, String heSectionName){
        String tempenHeader = "";
        String tempheHeader = "";
        if(tempHeader.displayLevelType){
            tempenHeader += sectionName;
            tempheHeader += heSectionName;
        }
        if(tempHeader.displayNum){
            tempenHeader += " " + tempHeader.chapNum;
            tempheHeader +=  " " +  Util.int2heb(tempHeader.chapNum);
        }

        tempHeader.heHeader = tempheHeader + " " + tempHeader.heHeader;
        tempHeader.enHeader = tempenHeader + " " + tempHeader.enHeader;
        return tempHeader;
    }

    private static Header createHeader(String sectionName, String heSectionName, int chapNum){
        Header tempHeader = new Header();
        if(sectionName.equals("Daf")){
            tempHeader.hid = 0;//there was no header for this.
            tempHeader.enNumString = num2enDaf(chapNum);
            tempHeader.heNumString = num2heDaf(chapNum);
            tempHeader.enHeader = sectionName + " " +  tempHeader.enNumString;
            tempHeader.heHeader = heSectionName + " " + tempHeader.heNumString;
            tempHeader.chapNum = chapNum;
        }else {

            tempHeader.hid = 0;//there was no header for this.
            tempHeader.enNumString = String.valueOf(chapNum);
            tempHeader.heNumString = Util.int2heb(chapNum);
            tempHeader.enHeader = sectionName + " " +  tempHeader.enNumString;
            tempHeader.heHeader = heSectionName + " " + tempHeader.heNumString;
            tempHeader.chapNum = chapNum;


        }
        return tempHeader;
    }

    /*
    public static ArrayList<Header> getHeaderChaps(Book book, int[] levels) throws API.APIException {
        ArrayList<Header> headerList = getChapHeaders(book.bid, levels);
        ArrayList <Integer> chapList = Segment.getChaps(book.bid, levels);
        ArrayList <Header> combinedList = new ArrayList<Header> ();
        String sectionName = book.sectionNamesL2B[getNonZeroLevel(levels) -1];
        String heSectionName = book.heSectionNamesL2B[getNonZeroLevel(levels) -1];
        int headerSpot = 0;
        Header tempHeader = null;
        for(int i = 0; i< chapList.size(); i++){
            boolean addedToComList = false;
            for(;headerSpot<headerList.size();headerSpot++){
                if((int)chapList.get(i) < headerList.get(headerSpot).chapNum)
                    break;

                if((int) chapList.get(i) == headerList.get(headerSpot).chapNum){ //you found a header - chap match
                    tempHeader = headerList.get(headerSpot);
                    tempHeader = completeHeader(tempHeader, sectionName, heSectionName);
                    combinedList.add(tempHeader);
                    addedToComList = true;
                    break;
                }
            }
            if(!addedToComList){//make a new header that is just using the chapter numbers (nothing special from headers DB).
                tempHeader = createHeader(sectionName, heSectionName, chapList.get(i));
                combinedList.add(tempHeader);
            }
        }

        return combinedList;
    }
    */

    private static ArrayList<Header> getChapHeaders(int bid, int[] levels) {

        SQLiteDatabase db = Database.getDB();

        ArrayList<Header> chapList = new ArrayList<Header>();


        Cursor cursor = db.rawQuery("SELECT DISTINCT " + "_id, heHeader, enHeader," + KdisplayNum + ", " + KdisplayLevelType + ", level" + getNonZeroLevel(levels) + " FROM "+ TABLE_HEADERS +" " + fullWhere(bid, levels) + " ORDER BY " + orderBy(bid, levels), null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Header header = new Header();
                header.hid = cursor.getInt(0);
                header.heHeader = cursor.getString(1);
                header.enHeader = cursor.getString(2);
                if(header.enHeader.equals(""))
                    header.enHeader = header.heHeader;
                if(header.heHeader.equals(""))
                    header.heHeader = header.enHeader;
                header.displayNum = (cursor.getInt(3) != 0);
                header.displayLevelType = (cursor.getInt(4) != 0);
                header.chapNum = cursor.getInt(5);
                // Adding  to list
                chapList.add(header);
            } while (cursor.moveToNext());
        }

        return chapList;
    }


    /**
     * zero indexed
     * @param levels
     * @return
     */
    private static int getNonZeroLevel(int[] levels){
        int nonZeroLevel;
        for(nonZeroLevel = 0; nonZeroLevel < levels.length; nonZeroLevel++){
            if(levels[nonZeroLevel] != 0)
                return nonZeroLevel;
        }
        return nonZeroLevel;
    }

    private static String fullWhere(int bid, int[] levels){
        String fullWhere =  " WHERE " + Kbid + "= " + String.valueOf(bid);
        for(int i = 0; i < levels.length; i++){
            if(!(levels[i] == 0)){
                fullWhere +=  " AND level" + String.valueOf(i + 1) + "= " + String.valueOf(levels[i]);
            }
        }
        return fullWhere;
    }

    private static String orderBy(int bid, int[] levels){
        String orderBy = Klevel1;
        for(int i = 0; i < levels.length - 2; i++){
            orderBy = "level"+ String.valueOf(i + 2) + ", " + orderBy;
        }
        return orderBy;
    }


    /*
        public static void addHeaders(Context context){
            Database dbHandler = Database.getInstance(MyApp.context);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HEADERS); //TODO be able to update only a bit at a time.
            db.execSQL(CREATE_HEADES_TABLE /* headers? heads? */);

            Log.d("sql_headers_add", "about to add headers");

            final int headerVer = 2;
            int headerFileNum = -1;
            while(true){
                headerFileNum++;
                String path = "headers/" + headerVer + "header"+ headerFileNum +  ".csv";
                try{
                    CSVReader reader = new CSVReader(new InputStreamReader(context.getResources().getAssets().open(path)));
                    addHeaderFile(db, reader);
                    Log.d("sql_header_adding","(I think) added " + headerVer + "header" + headerFileNum + ".csv to database");
                }catch(Exception e){
                    MyApp.sendException(e);
                    break;
                }
            }
            db.close();

        }

        private static void addHeaderFile(SQLiteDatabase db, CSVReader reader){

            String next[] = {};

            try {
                Book book = new Book();
                db.beginTransaction();
                while(true) {
                    next = reader.readNext();
                    if(next != null) {
                        if(!next[0].equals(book.title))
                            book = new Book(next[0],db);
                        try{
                            if(db.insert(TABLE_HEADERS, null, putValues(next, book)) != -1)
                                ;//Log.d("sql_header_add", "Added new header: " + title  + " " + langString);
                            else
                                Log.e("sql_header_add", "Couldn't add header for " + next[0]);
                        } catch (Exception e){
                            MyApp.sendException(e);
                            Log.e("sql_header_add", "Couldn't add header. " + e);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                MyApp.sendException(e);
                e.printStackTrace();
            }finally{
                db.setTransactionSuccessful();
                db.endTransaction();
            }
            return;
        }

        private static ContentValues putValues(String [] row, Book book){
            ContentValues values = new ContentValues();

            values.put(Kbid, book.bid);
            values.put(Klevel1, row[1]);
            values.put(Klevel2, row[2]);
            values.put(Klevel3, row[3]);
            values.put(Klevel4, row[4]);
            values.put(Klevel5, row[5]);
            values.put(Klevel6, row[6]);
            values.put(Kheader, row[7]);
            values.put(KdisplayNum, row[8]);
            values.put(KdisplayLevelType, row[9]);
            return values;
        }
    */
    @Override
    public String  toString(){
        return  "hid: " + hid + " " + "heHeader: " + heHeader + "enHeader: " + enHeader + " displayNum: " + displayNum
                + " displayLevelType: " + displayLevelType + " chapNum: " + String.valueOf(chapNum)
                + " enNumString: " + enNumString + " heNumString: " + heNumString
                ;
    }

    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Header> CREATOR
            = new Parcelable.Creator<Header>() {
        public Header createFromParcel(Parcel in) {
            return new Header(in);
        }

        public Header[] newArray(int size) {
            return new Header[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(hid);
        dest.writeInt(bid);
        dest.writeString(heHeader);
        dest.writeString(enHeader);
        dest.writeInt(displayNum ? 1 : 0); //ridiculous boolean conversion
        dest.writeInt(displayLevelType ? 1 : 0);
        dest.writeInt(chapNum);
        dest.writeString(enHeader);
        dest.writeString(heHeader);
        dest.writeIntArray(levels);
        dest.writeString(enNumString);
        dest.writeString(heNumString);



    }

    private Header(Parcel in) {
        hid = in.readInt();
        bid = in.readInt();
        heHeader = in.readString();
        enHeader = in.readString();
        displayNum =  in.readInt() != 0;
        displayLevelType = in.readInt() != 0;
        chapNum = in.readInt();
        enHeader = in.readString();
        heHeader = in.readString();
        levels = in.createIntArray();
        enNumString = in.readString();
        heNumString = in.readString();

    }

}
