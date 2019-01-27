package org.sefaria.sefaria.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class LinkFilter {

    protected String enTitle;
    protected String heTitle;
    protected String groupEnTitle; //the group titles are mostly for commentaries. e.g. all books of rashi have the group title "Rashi"
    protected String groupHeTitle;
    protected DEPTH_TYPE depth_type;
    protected int count;
    protected List<LinkFilter> children;
    protected LinkFilter parent = null;

    public enum DEPTH_TYPE {
        ALL, CAT, BOOK, ERR
    }

    public LinkFilter(String enTitle, int count, String heTitle, DEPTH_TYPE depth_type, Book parentBook){
        this(enTitle, count, heTitle, enTitle, heTitle, depth_type);
        if(parentBook != null) {
            try {
                Book linkBook = new Book(this.enTitle);
                this.groupEnTitle = linkBook.getCleanedCommentaryTitle(Util.Lang.EN, parentBook);
                this.groupHeTitle = linkBook.getCleanedCommentaryTitle(Util.Lang.HE, parentBook);
            } catch (Book.BookNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //category is passed in only if DEPTH_TYPE == DEPTH_TYPE.BOOK
    public LinkFilter(String enTitle, int count, String heTitle, String groupEnTitle, String groupHeTitle, DEPTH_TYPE depth_type){
        this.enTitle = enTitle;
        this.heTitle = heTitle;
        this.groupEnTitle = groupEnTitle;
        this.groupHeTitle = groupHeTitle;
        this.count = count;
        this.depth_type = depth_type;
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

    public String getGroupTitle(Util.Lang lang){
        if(lang == Util.Lang.HE)
            return groupHeTitle;
        else
            return groupEnTitle;
    }

    public int getCount(){ return count; }

    public DEPTH_TYPE getDepthType() { return depth_type; }

    public String getCategory() {
        if(parent == null)
            return "";
        if(DEPTH_TYPE.BOOK == depth_type)
            return parent.getRealTitle(Util.Lang.EN);
            //return parent.getSlimmedTitle(book, Util.Lang.EN);
        else// if(DEPTH_TYPE.CAT == depth_type) || ALL
            return getRealTitle(Util.Lang.EN);
            //return getSlimmedTitle(book, Util.Lang.EN);

    }

    public List<LinkFilter> getChildren(){
        if(children == null)
            children = new ArrayList<>();
        return children;
    }

    protected void addChild(LinkFilter child){
        addChild(child,false);
    }

    protected void addChild(LinkFilter child, boolean front){
        if(children == null)
            children = new ArrayList<>();
        count += child.count;
        if(front)
            children.add(0,child);
        else
            children.add(child);
        child.parent = this;
        if(this.parent != null){
            parent.count += child.count;
        }
    }

    public static String getStringTree(LinkFilter lc,int tabs, boolean printToLog){
        String tabStr = "";
        for(int i=0;i<tabs;i++)
            tabStr += "-";
        String str = tabStr + lc.toString() + '\n';
        if(printToLog)
            Log.d("Link", str);
        for(int i=0;i<lc.getChildren().size();i++) {
            str += getStringTree(lc.children.get(i),tabs + 1, printToLog);
        }
        return str;
    }

    public static List<LinkFilter> getList(LinkFilter lc) {
        List<LinkFilter> linkList = new ArrayList<>();

        if(lc.getDepthType() != DEPTH_TYPE.ALL)
            linkList.add(lc);
        for (int i = 0; i < lc.getChildren().size(); i++) {
            linkList.addAll(getList(lc.children.get(i)));
        }
        if(lc.getDepthType() == DEPTH_TYPE.ALL)
            linkList.add(lc);

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

    final public static String ALL_CONNECTIONS = "All";
    final public static String COMMENTARY = "Commentary";
    final public static String QUOTING_COMMENTARY = "Quoting Commentary";
    final public static String TARGUM = "Targum";

    private static LinkFilter getCommentaryOnChap(int chapStart, int chapEnd, int bid){

        LinkFilter commentaryGroup = new LinkFilter(COMMENTARY, 0, "מפרשים", DEPTH_TYPE.CAT, null);

        SQLiteDatabase db = Database.getDB();
        String sql;
        /*
        if(Build.VERSION.SDK_INT >= 21) {
            sql = "SELECT B.title, B.heTitle FROM Books B, Links_small L, Texts T WHERE (" +
                    "((L.tid1 BETWEEN " + chapStart + " AND " + chapEnd + ") AND L.tid2=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"
                    + " OR ((L.tid2 BETWEEN " + chapStart + " AND " + chapEnd + ") AND L.tid1=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"  //TODO make work for jeli's phone
                    + ") GROUP BY B._id ORDER BY B._id"
            ;
        }else {*/
        /*
        sql = " SELECT B.title, B.heTitle, B._id FROM Books B, Texts T WHERE tid=T._id AND T.bid= B._id AND B.commentsOn=" + bid + " AND  T._id in (" +
                "SELECT L.tid2 as tid FROM Links_small L WHERE (" +
                "L.tid1 BETWEEN " + chapStart + " AND " + chapEnd
                + ") UNION " +
                "SELECT L.tid1 as tid FROM Links_small L WHERE (" +
                " L2.tid2 BETWEEN " + chapStart + " AND " + chapEnd
                + ") GROUP BY B._id ORDER BY B._id"
        ;*/
        String endOfSql = "", connTypeCheck = "";
        if(Database.isNewCommentaryVersionWithConnType()){
            connTypeCheck = Link.connTypeCheck("L", true);
        }
        if(Database.isNewCommentaryVersion()){
            endOfSql = "  WHERE tmp.tid=T._id AND T.bid= B._id AND B.commentsOnMultiple like '%(" + bid + ")%' GROUP BY B._id ORDER BY B._id";
        }else{
            endOfSql = "  WHERE tmp.tid=T._id AND T.bid= B._id AND B.commentsOn=" + bid + " GROUP BY B._id ORDER BY B._id";
        }

        sql = "SELECT B.title, B.heTitle, B._id FROM Books B, Texts T, (" +
                "SELECT L.tid2 as tid FROM Links_small L WHERE L.tid1 BETWEEN " + chapStart + " AND " + chapEnd + connTypeCheck
                + " UNION SELECT L.tid1 as tid FROM Links_small L WHERE  L.tid2 BETWEEN " + chapStart + " AND " + chapEnd + connTypeCheck + ") as tmp"
                + endOfSql;

        Cursor cursor = db.rawQuery(sql, null);
        Book parentBook;
        try {
            parentBook = Book.getByBid(bid);
        }catch (Book.BookNotFoundException e){
            parentBook = null;
        }
        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                LinkFilter linkCount = new LinkFilter(cursor.getString(0), 0, cursor.getString(1), DEPTH_TYPE.BOOK, parentBook);
                commentaryGroup.addChild(linkCount);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return commentaryGroup;
    }

    /**
     * linearly loop through all children (commentary books on the chapter), and update the count for the specific book (using the title as the key)
     * @param title
     * @param count
     */
    private void addCommentaryCount(String title, int count, String heTitle, Book parentBook){
        if(count == 0) return; //it's not worth trying to go through the whole list testing a match, if this is just zero anyway. (It should never happen that this is 0).
        for(LinkFilter child: children){
            if(child.enTitle.equals(title)){
                child.count = count;
                this.count += count;
                return;
            }
        }
        Log.e("LinkFilter", "I couldn't find the book, even though the list is supposed to contain all books that comment on the chap... I'll create a new LinkFilter if I hae to");
        LinkFilter linkCount = new LinkFilter(title, count, heTitle, DEPTH_TYPE.BOOK, parentBook);
        this.addChild(linkCount);
    }

    private void addCount(){
        count += 1;
        if(parent != null){
            parent.count +=1;
            if(parent.parent != null){
                parent.parent.count +=1;
            }
        }
    }

    private static LinkFilter getFromLinks_API(Segment segment) throws API.APIException, Book.BookNotFoundException {
        LinkFilter allLinkCounts = makeAllLinkCounts();


        Log.d("API.Link","got starting LinksAPI");
        String place = segment.getURL(false);
        String url = API.LINK_URL + place + API.LINK_ZERO_TEXT;
        String data = API.getDataFromURL(url);
        if(data.length()==0)
            return allLinkCounts;

        try {
            JSONArray linksArray = new JSONArray(data);
            //Log.d("api", "jsonData:" + jsonData.toString());

            Map<String,LinkFilter> booksMap = new HashMap<>();

            for(int i=0;i<linksArray.length();i++){


                JSONObject jsonLink = linksArray.getJSONObject(i);
                String enTitle = jsonLink.getString("index_title");

                LinkFilter bookFilter = booksMap.get(enTitle);
                if(bookFilter != null){
                    bookFilter.addCount();
                    continue;
                }
                String category = jsonLink.getString("category");

                //it seems that some books dont have heTitles
                String heTitle;
                try {
                    heTitle = jsonLink.getString("heTitle");
                } catch (JSONException e) {
                    heTitle = enTitle;//continue; //jetison!!!
                }
                String enGroupTitle, heGroupTitle;
                try{
                    JSONObject groupTitles = jsonLink.getJSONObject("collectiveTitle");
                    enGroupTitle= groupTitles.getString("en");
                    heGroupTitle = groupTitles.getString("he");
                }catch (JSONException e){
                    enGroupTitle = enTitle;
                    heGroupTitle = heTitle;
                }

                LinkFilter linkCount = new LinkFilter(enTitle, 1, heTitle, enGroupTitle, heGroupTitle, DEPTH_TYPE.BOOK);
                booksMap.put(enTitle,linkCount);
                String heCategory;
                LinkFilter groupFilter = null;
                for(LinkFilter groupFilterTemp:allLinkCounts.getChildren()){
                    if(category.equals(groupFilterTemp.enTitle)){
                        groupFilter = groupFilterTemp;
                        break;
                    }
                }
                if(groupFilter == null){//didn't find the group already
                    if(category.equals(QUOTING_COMMENTARY)) {
                        heCategory = "מפרשים מצטטים";
                    } else if (category.equals(COMMENTARY)) {
                        heCategory = "מפרשים";
                    } else if (category.equals(TARGUM)) {
                        heCategory = "תרגום";
                    }
                    else {
                        heCategory = category;
                        //the real he titles will be added in sortLinkCountCategories() if it finds match
                    }
                    groupFilter = new LinkFilter(category, 0, heCategory, DEPTH_TYPE.CAT, null);
                    allLinkCounts.addChild(groupFilter);
                }

                groupFilter.addChild(linkCount);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        allLinkCounts = allLinkCounts.sortLinkCountCategories();


        return allLinkCounts;
    }

    public static LinkFilter makeAllLinkCounts(){
        return new LinkFilter(ALL_CONNECTIONS, 0, "הכל",DEPTH_TYPE.ALL, null);
    }

    public static LinkFilter makeAllLinkCountsError(String shortMessage){
        return new LinkFilter(shortMessage, 0, shortMessage,DEPTH_TYPE.ERR, null);
    }

    public static LinkFilter getLinkFilters(Segment segment){
        try {
            if (segment.tid == 0 || Settings.getUseAPI()) { //segment.tid == 0 when
                return getFromLinks_API(segment);
            } else {
                return getFromLinks_small(segment);
            }
        }catch (API.APIException e){
            e.printStackTrace();
            return LinkFilter.makeAllLinkCountsError(MyApp.getRString(R.string.error_getting_links) + ": " + "Internet Connection");
        }catch (Book.BookNotFoundException e){
            e.printStackTrace();
            GoogleTracker.sendException(e,"linkFilter,book error");
            return LinkFilter.makeAllLinkCountsError(MyApp.getRString(R.string.error_getting_links) + ": " + "Book");
        }catch (Exception e){
            e.printStackTrace();
            GoogleTracker.sendException(e,"linkFilter,unknown error");
            return LinkFilter.makeAllLinkCountsError(MyApp.getRString(R.string.error_getting_links) + ": " + "Unknown");
        }
    }

    private static boolean categoryIsCommentary(String [] categories){
        return (categories.length > 1 && categories[1].equals(COMMENTARY))
                || (categories.length > 0 && categories[0].equals(COMMENTARY));
    }

    private static LinkFilter getFromLinks_small(Segment segment) throws API.APIException {
        LinkFilter allLinkCounts = makeAllLinkCounts();

        final int commentaryAddonAmount = 15;
        LinkFilter commentaryGroup = getCommentaryOnChap(
                segment.tid - commentaryAddonAmount,
                segment.tid + commentaryAddonAmount,
                segment.bid);//getting all commentaries +-commentaryAddonAmount of the current segment

        LinkFilter quotingCommentaryGroup = new LinkFilter(QUOTING_COMMENTARY, 0, "מפרשים מצטטים", DEPTH_TYPE.CAT, null);

        if(segment.getNumLinks() == 0){
            if(commentaryGroup.getChildren().size() > 0)
                allLinkCounts.addChild(commentaryGroup, true);
            return allLinkCounts;
        }

        SQLiteDatabase db = Database.getDB();
        String sql;

        /*
        if(Build.VERSION.SDK_INT >= 21 && true) {//this works for newer androids
            sql = "SELECT B.title, Count(*) as booksCount, B.heTitle, B.commentsOn, B.categories FROM Books B, Links_small L, Texts T WHERE (" +
                    "(L.tid1 = " + segment.tid + " AND L.tid2=T._id AND T.bid= B._id)" +
                    " OR (L.tid2 = " + segment.tid + " AND L.tid1=T._id AND T.bid= B._id)" +
                    " ) GROUP BY B._id ORDER BY B.categories, B._id"
            ;
        }
        */
        sql = "select B2.title, Count(*) as booksCount, B2.heTitle, B2.commentsOn, B2.categories";
        if(Database.isNewCommentaryVersion()){
            sql += ", B2.commentsOnMultiple ";
        }// else don't need more
        sql +=  " FROM" +
                " (SELECT ALL B._id FROM Books B, Links_small L, Texts T WHERE ((L.tid1 = " + segment.tid + " AND L.tid2=T._id AND T.bid= B._id) )" +
                " UNION ALL SELECT ALL B._id FROM Books B, Links_small L, Texts T WHERE ( (L.tid2 = " + segment.tid + " AND L.tid1=T._id AND T.bid= B._id) )) as tmp, Books B2" +
                " where tmp._id= B2._id GROUP BY tmp._id ORDER BY B2.categories, B2._id";

        Cursor cursor = db.rawQuery(sql, null);

        LinkFilter countGroups = null;
        String lastCategory = "";
        Book parentBook;
        try {
            parentBook = Book.getByBid(segment.bid);
        }catch (Book.BookNotFoundException e){
            parentBook = null;
        }
        if (cursor.moveToFirst()) {
            do {

                //Log.d("LinkFilter", ""+ segment.levels[0] + ": " + cursor.getString(4));
                String [] categories = Util.str2strArray(cursor.getString(4));
                if(categories.length == 0) continue;
                boolean isCommentary = categoryIsCommentary(categories);
                if(!isCommentary && (countGroups == null || !categories[0].equals(lastCategory))){
                    if(countGroups != null && countGroups.count >0) {
                        //add the last group
                        allLinkCounts.addChild(countGroups);
                    }
                    //create a new group
                    String category = categories[0];
                    lastCategory = category;
                    //the real heCategory will be added in sortLinkCountCategories() if it finds match
                    countGroups = new LinkFilter(category, 0, category, DEPTH_TYPE.CAT, null);
                }
                String childEnTitle = cursor.getString(0);
                int childCount = cursor.getInt(1);
                String childHeTitle = cursor.getString(2);
                String commentsOnMulti = null;
                if(Database.isNewCommentaryVersion()){
                    commentsOnMulti = cursor.getString(5); //can be null
                }
                if(cursor.getInt(3) == segment.bid ||
                        (commentsOnMulti != null && commentsOnMulti.contains("(" + segment.bid + ")"))) {
                    //Comments on this book
                    commentaryGroup.addCommentaryCount(childEnTitle, childCount, childHeTitle, parentBook);
                }else{
                    LinkFilter linkCount = new LinkFilter(childEnTitle, childCount, childHeTitle, DEPTH_TYPE.BOOK, null);
                    if(categoryIsCommentary(categories)) {
                        //It's not commenting on this book,
                        // but it's a commentary. So that means it's a quotingCommentary
                        quotingCommentaryGroup.addChild(linkCount);
                    }else if (countGroups != null ){
                        //it's a regular cat group
                        countGroups.addChild(linkCount);
                    }
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        if(countGroups != null && countGroups.count >0 && countGroups != quotingCommentaryGroup)
            allLinkCounts.addChild(countGroups);

        allLinkCounts = allLinkCounts.sortLinkCountCategories();

        if(commentaryGroup.count >0 || commentaryGroup.getChildren().size()>0)
            allLinkCounts.addChild(commentaryGroup,true);

        if(quotingCommentaryGroup.count >0 || quotingCommentaryGroup.getChildren().size() > 0)
            allLinkCounts.addChild(quotingCommentaryGroup);

        //Log.d("LinkFilter", "...finished: " + allLinkCounts.count);
        return allLinkCounts;
    }

    private LinkFilter sortLinkCountCategories(){
        LinkFilter newLinkCount = new LinkFilter(enTitle, 0, heTitle, depth_type, null);
        MenuNode menuNode = MenuState.getRootNode(MenuState.IndexType.MAIN);
        String [] titles = menuNode.getChildrenTitles(Util.Lang.EN);
        String [] heTitles = menuNode.getChildrenTitles(Util.Lang.HE);
        for(int i=0;i<titles.length;i++){
            String title = titles[i];
            for(int j=0;j<getChildren().size();j++){
                LinkFilter child = getChildren().get(j);
                Log.d("hetitle",child.enTitle + " == " + title + ": " + child.enTitle.equals(title));
                if(child.enTitle.equals(title)){
                    child.heTitle = heTitles[i];
                    child.groupHeTitle = heTitles[i];
                    newLinkCount.addChild(child);
                    getChildren().remove(j);
                    break;
                }
            }
        }



        //if there's any categories left out them back in
        for(LinkFilter child:getChildren()){
            newLinkCount.addChild(child,child.enTitle.equals(COMMENTARY));
        }


        return newLinkCount;
    }

    //this is not a fool-proof equals() implementation. hence 'pseudo'
    public static boolean pseudoEquals(LinkFilter lc1, LinkFilter lc2) {
        if (lc1 == null || lc2 == null) return false;
        return (lc1.getRealTitle(Util.Lang.EN).equals(lc2.getRealTitle(Util.Lang.EN)) &&
                lc1.depth_type == lc2.depth_type);
    }
}
