package org.sefaria.sefaria.database;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sefaria.sefaria.Settings;

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

    private int lid;
    private String connType;
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

    /**
     *
     * @param segment
     * @param linkFilter null if no filter or linkCount containing anything you want included in the filter (including LinkFilter linkfiler's children)
     * @return List<Segment> for texts links to the input segment
     */
    public static List<Segment> getLinkedTexts(Segment segment, LinkFilter linkFilter) throws API.APIException, Book.BookNotFoundException  {
        List<Segment> linkList;
        if(segment.tid == 0 || Settings.getUseAPI()){ //tid might be 0 if it was gotten using API (So for example with alternate segment versions)
            linkList = getLinkedTextsFromAPI(segment,linkFilter);
        }else{
            linkList = getLinkedTextsFromDB(segment, linkFilter);
        }
        return linkList;
    }


    public static List<Segment> getLinkedTextsFromAPI(Segment orgSegment, LinkFilter linkFilter) throws API.APIException, Book.BookNotFoundException {
        List<Segment> segments = new ArrayList<>();
        String place = orgSegment.getURL(false);
        String url = API.LINK_URL + place;
        String data = API.getDataFromURL(url);
        try {
            Book.getByBid(orgSegment.bid);
        } catch (Book.BookNotFoundException e) {
            return segments;
        }
        if(data.length() == 0)
            return segments;
        List<Segment> commentaries = new ArrayList<>();
        try {
            JSONArray linksArray = new JSONArray(data);
            //Log.d("api", "jsonData:" + jsonData.toString());
            for(int i=0;i<linksArray.length();i++){
                try {
                    JSONObject jsonLink = linksArray.getJSONObject(i);
                    String enTitle = jsonLink.getString("index_title");
                    String category = jsonLink.getString("category");
                    String ref = jsonLink.getString("ref");
                    if (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL ||
                            (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT && category.equals(linkFilter.enTitle)) ||
                            (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK && enTitle.equals(linkFilter.enTitle))
                            ) {
                        Segment tempSegment = new Segment(removeEmpty(jsonLink.getString("text")), removeEmpty(jsonLink.getString("he")), Book.getBid(enTitle), ref);
                        if (category.equals("Commentary"))
                            commentaries.add(tempSegment);
                        else
                            segments.add(tempSegment);
                    }
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        Collections.sort(commentaries,compareTexts);
        Collections.sort(segments, compareTexts);
        segments.addAll(0,commentaries);

        return segments;
    }

    static Comparator<Segment> compareTexts = new Comparator<Segment>() {
        @Override
        public int compare(Segment a, Segment b) {
            //only sorting on bid. Within same book using stable sort to keep order
            return a.bid - b.bid;
        }
    };

    private static String removeEmpty(String str){
        if(str.equals("[]"))
            return "";
        else
            return str;
    }

    //TODO NEEDS WORK!!
    private static List<Segment> getLinkedTextsFromDB(Segment segment, LinkFilter linkFilter) {
        SQLiteDatabase db = Database.getDB();
        List<Segment> linkList = new ArrayList<>();

        //Log.d("getLinksTextsFromDB", "Started ... linkFiler:" + linkFilter);

        StringBuilder sql = new StringBuilder("SELECT T.* FROM " + Segment.TABLE_TEXTS + " T, Books B WHERE T.bid = B._id AND T._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 = " + segment.tid
                + " UNION "
                + " SELECT L2.tid1 FROM Links_small L2 WHERE L2.tid2 = " + segment.tid
                + ")");

        List<String> args = new ArrayList<>();
        if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT){
            if(linkFilter.enTitle.equals(LinkFilter.COMMENTARY)){
                if(Database.isNewCommentaryVersion()){
                    sql.append(" and B.commentsOnMultiple like '%(" + segment.bid + ")%'");
                }else {
                    sql.append(" AND B.commentsOn = " + segment.bid);
                }
            }else{
                String category;
                if(linkFilter.enTitle.equals(LinkFilter.QUOTING_COMMENTARY)) {

                    //don't include the commentary that is directly for this book (like "Rashi on Genesis" for "Genesis")
                    if(Database.isNewCommentaryVersion()){
                        sql.append(" AND B.commentsOnMultiple not like '%(" + segment.bid + ")%'");
                    }else {
                        sql.append(" AND B.commentsOn <> " + segment.bid);
                    }
                    //the category in the database is simply "Commentary"
                    sql.append(" AND B.categories like '[\"%\",\"Commentary\",%' ");
                }else {

                    category = linkFilter.enTitle;
                    // I think || line concat strings in the sql statement
                    sql.append(" AND B.categories like '[\"' || ? || '%' ");
                    args.add(category);
                }

                //get the string for categories start with the selected category
                //if(Build.VERSION.SDK_INT >= 21) sql += " AND B.categories like printf('%s%s%s','[\"',?,'%') "; else
            }
        }else if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK){
            sql.append(" AND B.title = ?");
            args.add(linkFilter.enTitle);
        }

        ///more more more
        if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL) {
            if (Database.isNewCommentaryVersion()) {
                sql.append(" ORDER BY (case when B.commentsOnMultiple like '%(" + segment.bid + ")%' then 0 else 1 end), T.bid");
            } else {
                sql.append(" ORDER BY (case when B.commentsOn=" + segment.bid + " then 0 else 1 end), T.bid");
            }
        } else {
            sql.append(" ORDER BY T._id");
        }

        String [] argsArray = args.toArray((new String [args.size()]));
        Cursor cursor = db.rawQuery(sql.toString(), argsArray);
        // Populate list of texts that are linked
        if (cursor.moveToFirst()) {
            do {
                linkList.add(new Segment(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        /*
        //Log.d("getLinksTextsFromDB", "Finished ... linkList.size():" + linkList.size());
        if(linkList.size()!= linkFilter.count && linkList.size() < 7){
            for(LinkFilter lc: linkFilter.getChildren()){
                Log.d("Link", lc.toString());
            }
            for(Segment link:linkList){
                link.log();
            }
        }
        */
        return linkList;
    }

    /*

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


