package org.sefaria.sefaria.database;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;

import org.sefaria.sefaria.Util;

public class Link implements Parcelable {

    public Link(Cursor cursor){
        lid = cursor.getInt(0);
        connType = cursor.getString(1);
        bid = cursor.getInt(2);
        final int LEVELS_START_NUM = 3;
        for(int i = 0;i<NUM_LEVELS;i++) {
            levels[i] = cursor.getInt(i + LEVELS_START_NUM);
        }
		/*
		bidb = cursor.getInt(9);
		level1b = cursor.getInt(10);
		level2b = cursor.getInt(11);
		level3b = cursor.getInt(12);
		level4b = cursor.getInt(13);
		level5b = cursor.getInt(14);
		level6b = cursor.getInt(15);
		 */
    }

    private static final String KconnType = "connType";
    private static final String Kbida = "bida";
    private static final String Klevel1a = "level1a";
    private static final String Klevel2a = "level2a";
    private static final String Klevel3a = "level3a";
    private static final String Klevel4a = "level4a";
    private static final String Klevel5a = "level5a";
    private static final String Klevel6a = "level6a";
    private static final String Kbidb = "bidb";
    private static final String Klevel1b = "level1b";
    private static final String Klevel2b = "level2b";
    private static final String Klevel3b = "level3b";
    private static final String Klevel4b = "level4b";
    private static final String Klevel5b = "level5b";
    private static final String Klevel6b = "level6b";

    public int lid;
    public String connType;
    public int bid;
    private final static int NUM_LEVELS = 6;
    public int [] levels = new int [NUM_LEVELS];

    /*
    public int level1;
    public int level2;
    public int level3;
    public int level4;
    public int level5;
    public int level6;

    public int bidb;
    public int level1b;
    public int level2b;
    public int level3b;
    public int level4b;
    public int level5b;
    public int level6b;

     */
    @Override
    public String toString(){
        String str =  String.valueOf(lid) + " " + connType + " bid: " + String.valueOf(bid);
        for(int i=0;i<NUM_LEVELS;i++){
            str += ","  + String.valueOf(levels[i]);
        }
        return str;
    }

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
                + " AND (T." + Text.Klevel5 + "=L." + Klevel5b + ")"//+ " OR L." + Klevel5b + "=0)"
                + " AND (T." + Text.Klevel6 + "=L." + Klevel6b + ")"//+ " OR L." + Klevel6b + "=0)"
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
                + " AND (T3." + Text.Klevel5 + "=L2." + Klevel5a + ")"//+ " OR L2." + Klevel5a + "=0)"
                + " AND (T3." + Text.Klevel6 + "=L2." + Klevel6a + ")" ; //+ " OR L2." + Klevel6a + "=0)";
        return str;
    }


    public static class LinkCount {
        protected String enTitle;
        protected String heTitle;
        protected DEPTH_TYPE depth_type;
        protected int count;
        protected List<LinkCount> children;

        public enum DEPTH_TYPE {
            ALL,CAT,BOOK
        }

        public LinkCount(String enTitle,int count,String heTitle, DEPTH_TYPE depth_type){
            this.enTitle = enTitle;
            this.heTitle = heTitle;
            this.count = count;
            this.depth_type = depth_type;
        }

        /**
         * This will convert the commentary name to remove the name of the book that it is commenting on.
         * For example, it will transform "Rashi on Genesis" to "Rashi"
         * @param book the book that it is commenting on (for example, Genesis)
         * @param menuLang the lang for the displayed title
         * @return the name of the commentary without the " on xyzBook" (for example, Rashi)
         */
        public String getSlimmedTitle(Book book, Util.Lang menuLang){
            return Book.removeOnMainBookFromTitle(this.getRealTitle(menuLang),book);
        }

        /**
         * This is the real book title.
         * use getSlimmedTitle() if you want to remove word like " on Genesis" from a title like "Rashi on Genesis"
         * @param lang
         * @return
         */
        public String getRealTitle(Util.Lang lang){
            if(lang == Util.Lang.HE)
                return heTitle;
            else
                return enTitle;
        }

        public int getCount(){ return count; }

        public DEPTH_TYPE getDepthType() { return depth_type; }

        public List<LinkCount> getChildren(){
            if(children == null)
                children = new ArrayList<>();
            return children;
        }

        protected void addChild(LinkCount child){
            if(children == null)
                children = new ArrayList<>();
            count += child.count;
            children.add(child);
        }

        public static String getStringTree(LinkCount lc,int tabs){
            String tabStr = "";
            for(int i=0;i<tabs;i++)
                tabStr += "-";
            String str = tabStr + lc.toString() + '\n';
            for(int i=0;i<lc.getChildren().size();i++) {
                str += getStringTree(lc.children.get(i),tabs + 1);
            }
            return str;
        }

        public static List<LinkCount> getList(LinkCount lc) {
            List<LinkCount> linkList = new ArrayList<>();

            linkList.add(lc);
            for (int i = 0; i < lc.getChildren().size(); i++) {
                linkList.addAll(getList(lc.children.get(i)));
            }
            return linkList;
        }


        @Override
        public String toString() {
            String type;
            if (depth_type == DEPTH_TYPE.BOOK) type = "BOOK";
            else if (depth_type == DEPTH_TYPE.CAT) type = "CAT";
            else if (depth_type == DEPTH_TYPE.ALL) type = "ALL";
            else type = "NONE";

            String str = enTitle + " " + heTitle + " " + count + " " + type + "\n";

            return str;
        }

        final private static String ALL_CONNECTIONS = "All Connections";
        final private static String COMMENTARY = "Commentary";
        final private static String QUOTING_COMMENTARY = "Quoting Commentary";

        public static LinkCount getFromLinks_small(Text text){
            LinkCount allLinkCounts = new LinkCount(ALL_CONNECTIONS, 0, "All Connections (He)",DEPTH_TYPE.ALL);
            if(text.getNumLinks() == 0)  return allLinkCounts;
            Database2 dbHandler = Database2.getInstance();
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            //Log.d("Link", "starting getCountsTitlesFromLinks_small");
            String sql = "SELECT B.title, Count(*) as booksCount, B.heTitle, B.commentsOn, B.categories FROM Books B, Links_small L, Texts T WHERE (" +
                    "(L.tid1 = " + text.tid + " AND L.tid2=T._id AND T.bid= B._id) OR " +
                    "(L.tid2 = " + text.tid + " AND L.tid1=T._id AND T.bid= B._id) ) GROUP BY B._id ORDER BY B.categories, B._id"
                    ;

            Cursor cursor = db.rawQuery(sql, null);

            LinkCount commentaryGroup = new LinkCount(COMMENTARY,0, "מפרשים",DEPTH_TYPE.CAT);
            LinkCount countGroups = null;
            String lastCategory = "";
            if (cursor.moveToFirst()) {
                do {
                    // Adding  to list
                    String [] categories = Util.str2strArray(cursor.getString(4));
                    //TODO test length
                    if(countGroups == null || !categories[0].equals(lastCategory)){
                        if(countGroups != null && countGroups.count >0) {
                            allLinkCounts.addChild(countGroups);
                        }
                        lastCategory = categories[0];
                        String category;
                        if(categories[0].equals("Commentary"))
                            category = QUOTING_COMMENTARY;
                        else
                            category = categories[0];
                        countGroups = new LinkCount(category,0, category + " (he)",DEPTH_TYPE.CAT);
                    }
                    LinkCount linkCount = new LinkCount(cursor.getString(0),cursor.getInt(1),cursor.getString(2),DEPTH_TYPE.BOOK);
                    if(cursor.getInt(3) == text.bid){//Comments on this book
                        commentaryGroup.addChild(linkCount);
                    }else{
                        countGroups.addChild(linkCount);
                    }

                } while (cursor.moveToNext());
            }
            allLinkCounts.addChild(countGroups);
            allLinkCounts.addChild(commentaryGroup);
            //Log.d("Link", "finished getCountsTitlesFromLinks_small");
            //getStringTree(allLinkCounts, 0);

            return allLinkCounts;
        }


    }


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
     * @param linkFilter null if no filter or linkCount containing anything you want included in the filter (including LinkCount linkfiler's children)
     * @return List<Text> for texts links to the input text
     */
    public static List<Text> getLinkedTexts(Text text, LinkCount linkFilter) {
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


    private static List<Text> getLinkedTextsFromDB(Text text, LinkCount linkFilter) {
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        List<Text> linkList = new ArrayList<Text>();

        Log.d("getLinksTextsFromDB", "Started ... linkFiler:" + linkFilter);

        String sql = "SELECT T.* FROM " + Text.TABLE_TEXTS + " T, Books B WHERE T.bid = B._id AND T._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 = " + text.tid
                + " UNION "
                + " SELECT L2.tid1 FROM Links_small L2 WHERE L2.tid2 = " + text.tid
                + ")";

        String [] args = null;
        if(linkFilter.depth_type == LinkCount.DEPTH_TYPE.CAT){
            if(linkFilter.enTitle.equals(LinkCount.COMMENTARY)){
                sql += " AND B.commentsOn = ? ";
                args = new String[] {""+text.bid};
            }else{
                String category;
                if(linkFilter.enTitle.equals(LinkCount.QUOTING_COMMENTARY)) {
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
        }else if(linkFilter.depth_type == LinkCount.DEPTH_TYPE.BOOK){
            sql += " AND B.title = ?";
            args = new String[]{linkFilter.enTitle};
        }

        sql += " ORDER BY (case when B.commentsOn=" + text.bid  + " then 0 else 1 end), T.bid";


        Cursor cursor = db.rawQuery(sql, args);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }

        Log.d("getLinksTextsFromDB", "Finished ... linkList.size():" + linkList.size());
        if(linkList.size()!= linkFilter.count && linkList.size() < 7){
            for(LinkCount lc: linkFilter.getChildren()){
                Log.d("Link", lc.toString());
            }
            for(Text link:linkList){
                link.log();
            }
        }
        return linkList;
    }


    private static String createWhere3(Text text, String type){
        String whereStatement =  " WHERE L.bid" + type + " = " + text.bid;
        for(int i = 0; i < 6; i++){
            if (text.levels[i] == 0)
                return whereStatement;
            whereStatement+= " AND L.level" + (i +1) + type + " = " + text.levels[i];
        }
        return whereStatement;


    }

    public static List<Link> getLinks(Text text) {
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Link> linkList = new ArrayList<Link>();

        String sql = "SELECT L._id, L.connType, L.bidb as bid, L.level1b, L.level2b, L.level3b, L.level4b, L.level5b, L.level6b FROM Links L"
                + createWhere3(text,"a")
                //+ " UNION"
                //+ " SELECT L._id, L.connType, L.bida as bid, L.level1a, L.level2a, L.level3a, L.level4a, L.level5a, L.level6a FROM Links L"
                //+  createWhere3(text,"b")
                //+ " WHERE L.bidb = ? AND L.level1b = ? AND L.level2b = ? AND L.level3b = ? AND L.level4b = ?  AND L.level5b = ? AND L.level6b = ?"
                + " ORDER BY bid";

        Log.d("sql", sql);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Link(cursor));
            } while (cursor.moveToNext());
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
        Database2 dbHandler = Database2.getInstance();
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

}


