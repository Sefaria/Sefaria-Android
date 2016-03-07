package org.sefaria.sefaria.database;


import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Log;

public class Link {//implements Parcelable {

    public Link(Cursor cursor){
        lid = cursor.getInt(0);
        connType = cursor.getString(1);
        bid = cursor.getInt(2);
        final int LEVELS_START_NUM = 3;
        for(int i = 0;i<NUM_LEVELS;i++) {
            levels[i] = cursor.getInt(i + LEVELS_START_NUM);
        }
    }

    public int lid;
    public String connType;
    public int bid;
    private final static int NUM_LEVELS = 4;
    public int [] levels = new int [NUM_LEVELS];

    @Override
    public String toString(){
        String str =  String.valueOf(lid) + " " + connType + " bid: " + String.valueOf(bid);
        for(int i=0;i<NUM_LEVELS;i++){
            str += ","  + String.valueOf(levels[i]);
        }
        return str;
    }
     /*
    private static final String TABLE_LINKS = "Links" ;

    private static String addLevelWhere(String levelName, int levelNum, String linkTableName){
        return addLevelWhere(levelName, levelNum, linkTableName, -1);
    }
    private static String addLevelWhere(String levelName, int levelNum, String linkTableName, int wherePage){
        if(levelNum == 0)//it means this level doesn't exist (it's too high), so don't display anything.
            return " ";
        else if(levelNum == -1){//it means that we want to display everything from this level
            levelNum =  0;
        }
        return " AND (" + linkTableName + "." + levelName  + "=" + String.valueOf(levelNum)// + " OR " + linkTableName +"." + levelName + "=0"
                + ") ";
    }

    private static String makeWhereStatement(Text text){
        String str = " L." + Kbida + "=" + text.bid;
        for(int i=0;i<text.levels.length;i++)
            str += addLevelWhere(Klevel1a, text.levels[0],"L");
        str += " AND    L.bidb=T.bid "
                + " AND (T." + Text.Klevel1 + "=L." + Klevel1b + ")"// + " OR L." + Klevel1b + "=0)"
                + " AND (T." + Text.Klevel2 + "=L." + Klevel2b + ")"//+ " OR L." + Klevel2b + "=0)"
                + " AND (T." + Text.Klevel3 + "=L." + Klevel3b + ")"//+ " OR L." + Klevel3b + "=0)"
                + " AND (T." + Text.Klevel4 + "=L." + Klevel4b + ")"//+ " OR L." + Klevel4b + "=0)"
                //+ " AND (T." + Text.Klevel5 + "=L." + Klevel5b + ")"//+ " OR L." + Klevel5b + "=0)"
                //+ " AND (T." + Text.Klevel6 + "=L." + Klevel6b + ")"//+ " OR L." + Klevel6b + "=0)"
        ;
        return str;
    }

    private static String makeWhereStatement2(Text text){
        String str = " L2." + Kbidb + "=" + text.bid ;
        for(int i=0;i<text.levels.length;i++)
            str += addLevelWhere(Klevel1b, text.levels[i], "L2");
        str += " AND    L2.bida=T3.bid "
                + " AND (T3." + Text.Klevel1 + "=L2." + Klevel1a + ")"//+ " OR L2." + Klevel1a + "=0)"
                + " AND (T3." + Text.Klevel2 + "=L2." + Klevel2a + ")"//+ " OR L2." + Klevel2a + "=0)"
                + " AND (T3." + Text.Klevel3 + "=L2." + Klevel3a + ")"//+ " OR L2." + Klevel3a + "=0)"
                + " AND (T3." + Text.Klevel4 + "=L2." + Klevel4a + ")"//+ " OR L2." + Klevel4a + "=0)"
                //+ " AND (T3." + Text.Klevel5 + "=L2." + Klevel5a + ")"//+ " OR L2." + Klevel5a + "=0)"
                //+ " AND (T3." + Text.Klevel6 + "=L2." + Klevel6a + ")" ; //+ " OR L2." + Klevel6a + "=0)";
        return str;
    }
    */



    /**
     * Get links for specific text (ex. verse).
     * @param text
     * @param limit
     * @param offset
     * @return linkList
     */

    /**
     *
     * @param text
     * @param linkFilter null if no filter or linkCount containing anything you want included in the filter (including LinkFilter linkfiler's children)
     * @return List<Text> for texts links to the input text
     */
    public static List<Text> getLinkedTexts(Text text, LinkFilter linkFilter) {
        List<Text> linkList = new ArrayList<Text>();
        try{
            linkList = getLinkedTextsFromDB(text, linkFilter);
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            linkList = API.getLinks(text,linkFilter);
        }catch(Exception e){
            e.printStackTrace();
        }

        return linkList;
    }


    private static List<Text> getLinkedTextsFromDB(Text text, LinkFilter linkFilter) {
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        List<Text> linkList = new ArrayList<Text>();

        //Log.d("getLinksTextsFromDB", "Started ... linkFiler:" + linkFilter);

        String sql = "SELECT T.* FROM " + Text.TABLE_TEXTS + " T, Books B WHERE T.bid = B._id AND T._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 = " + text.tid
                + " UNION "
                + " SELECT L2.tid1 FROM Links_small L2 WHERE L2.tid2 = " + text.tid
                + ")";

        String [] args = null;
        if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT){
            if(linkFilter.enTitle.equals(LinkFilter.COMMENTARY)){
                sql += " AND B.commentsOn = ? ";
                args = new String[] {""+text.bid};
            }else{
                String category;
                if(linkFilter.enTitle.equals(LinkFilter.QUOTING_COMMENTARY)) {
                    //the category in the database is simply "Commentary"
                    category = "Commentary";
                    //don't include the commentary that is directly for this book (like "Rashi on Genesis" for "Genesis")
                    sql += " AND B.commentsOn <> " + text.bid;
                }else {
                    category = linkFilter.enTitle;
                }
                //get the string for categories start with the selected category
                sql += " AND B.categories like printf('%s%s%s','[\"',?,'%') ";
                args = new String[]{category};

            }
        }else if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK){
            sql += " AND B.title = ?";
            args = new String[]{linkFilter.enTitle};
        }

        sql += " ORDER BY ";
        if(Build.VERSION.SDK_INT >=21)
            sql += " (case when B.commentsOn=" + text.bid  + " then 0 else 1 end), ";
        sql += " T.bid";


        Cursor cursor = db.rawQuery(sql, args);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }

        //Log.d("getLinksTextsFromDB", "Finished ... linkList.size():" + linkList.size());
        if(linkList.size()!= linkFilter.count && linkList.size() < 7){
            for(LinkFilter lc: linkFilter.getChildren()){
                Log.d("Link", lc.toString());
            }
            for(Text link:linkList){
                link.log();
            }
        }
        return linkList;
    }



    /**
     * gets links to a particular level other than the last level
     * @param text
     * @param limit
     * @param offset
     * @return
     */
    /*
    public static List<Text> getLinkedChapTexts(Text text, int limit, int offset) {
        List<Text> texts = new ArrayList<Text>();
        Text dummyChapText = Text.makeDummyChapText(text);
        try{
            texts = getLinkedChapTextsFromDB(dummyChapText, limit, offset);
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            texts = API.getChapLinks(dummyChapText,limit,offset);
        }catch(Exception e){
            e.printStackTrace();
        }

        return texts;

    }


    private static List<Text> getLinkedChapTextsFromDB(Text text, int limit, int offset) {
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Text> linkList = new ArrayList<Text>();
        String whereStatement = makeWhereStatement(text);
        String whereStatement2 = makeWhereStatement2(text);

        String select = "SELECT T2.* FROM " + Text.TABLE_TEXTS + " T2 WHERE T2._id"
                + " IN (  SELECT T._id"
                + " FROM " + TABLE_LINKS +" L, " + Text.TABLE_TEXTS + " T "
                + " WHERE " + whereStatement
                + ")";

        String select2 = "SELECT T4.* FROM " + Text.TABLE_TEXTS + " T4 WHERE T4._id"
                + " IN (  SELECT T3._id"
                + " FROM " + TABLE_LINKS +" L2, " + Text.TABLE_TEXTS + " T3 "
                + " WHERE " + whereStatement2
                + ")";

        String sql = select + " UNION " + select2 //"( " + select + " )";// + " UNION " + "( " + select2 + " )"
                + "ORDER BY bid"
                + " LIMIT " + String.valueOf(limit)
                + " OFFSET " + String.valueOf(offset)
                ;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }
        return linkList;
    }


    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Link> CREATOR
            = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(lid);
        dest.writeString(connType);
        dest.writeInt(bid);
        for(int i =0;i<NUM_LEVELS;i++)
            dest.writeInt(levels[i]);


    }

    private Link(Parcel in) {
        lid = in.readInt();
        connType = in.readString();
        bid = in.readInt();
        for(int i =0;i<NUM_LEVELS;i++)
            levels[i] = in.readInt();
    }
    */
}


