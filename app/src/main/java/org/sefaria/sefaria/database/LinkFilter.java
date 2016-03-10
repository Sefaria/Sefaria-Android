package org.sefaria.sefaria.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.List;


public class LinkFilter {

    protected String enTitle;
    protected String heTitle;
    protected DEPTH_TYPE depth_type;
    protected int count;
    protected List<LinkFilter> children;
    protected LinkFilter parent = null;

    public enum DEPTH_TYPE {
        ALL,CAT,BOOK
    }

    //category is passed in only if DEPTH_TYPE == DEPTH_TYPE.BOOK
    public LinkFilter(String enTitle, int count, String heTitle, DEPTH_TYPE depth_type){
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

    final public static String ALL_CONNECTIONS = "All";
    final public static String COMMENTARY = "Commentary";
    final public static String QUOTING_COMMENTARY = "Quoting Commentary";

    private static LinkFilter getCommentaryOnChap(int chapStart, int chapEnd, int bid){

        LinkFilter commentaryGroup = new LinkFilter(COMMENTARY,0, "מפרשים",DEPTH_TYPE.CAT);

        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        String sql;
        /*
        if(Build.VERSION.SDK_INT >= 21) {
            sql = "SELECT B.title, B.heTitle FROM Books B, Links_small L, Texts T WHERE (" +
                    "((L.tid1 BETWEEN " + chapStart + " AND " + chapEnd + ") AND L.tid2=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"
                    + " OR ((L.tid2 BETWEEN " + chapStart + " AND " + chapEnd + ") AND L.tid1=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"  //TODO make work for jeli's phone
                    + ") GROUP BY B._id ORDER BY B._id"
            ;
        }else {*/
        sql = " SELECT B.title, B.heTitle, B._id FROM Books B, Texts T WHERE tid=T._id AND T.bid= B._id AND B.commentsOn=" + bid + " AND  T._id in (" +
                "SELECT L.tid2 as tid FROM Links_small L WHERE (" +
                "L.tid1 BETWEEN " + chapStart + " AND " + chapEnd
                + ") UNION " +
                "SELECT L.tid1 as tid FROM Links_small L WHERE (" +
                " L2.tid2 BETWEEN " + chapStart + " AND " + chapEnd
                + ") GROUP BY B._id ORDER BY B._id"
        ;
        sql = "SELECT B.title, B.heTitle, B._id FROM Books B, Texts T, (" +
                "SELECT L.tid2 as tid FROM Links_small L WHERE L.tid1 BETWEEN " + chapStart + " AND " + chapEnd
                + " UNION SELECT L.tid1 as tid FROM Links_small L WHERE  L.tid2 BETWEEN " + chapStart + " AND " + chapEnd + ") as tmp" +
                "  WHERE tmp.tid=T._id AND T.bid= B._id AND B.commentsOn=" + bid + " GROUP BY B._id ORDER BY B._id";

        Cursor cursor = db.rawQuery(sql, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                LinkFilter linkCount = new LinkFilter(cursor.getString(0),0,cursor.getString(1),DEPTH_TYPE.BOOK);
                commentaryGroup.addChild(linkCount);
            } while (cursor.moveToNext());
        }
        return commentaryGroup;
    }

    /**
     * linearly loop through all children (commentary books on the chapter), and update the count for the specific book (using the title as the key)
     * @param title
     * @param count
     */
    private void addCommentaryCount(String title, int count, String heTitle){
        if(count == 0) return; //it's not worth trying to go through the whole list testing a match, if this is just zero anyways. (It should never happen that this is 0).
        for(LinkFilter child: children){
            if(child.enTitle.equals(title)){
                child.count = count;
                this.count += count;
                return;
            }
        }
        Log.e("LinkFilter", "I couldn't find the book, even though the list is supposed to contain all books that comment on the chap... I'll create a new LinkFilter if I hae to");
        LinkFilter linkCount = new LinkFilter(title,count,heTitle,DEPTH_TYPE.BOOK);
        this.addChild(linkCount);
    }


    public static LinkFilter getFromLinks_small(Text text){
        //Log.d("LinkFilter", text.levels[0] + " starting...");
        LinkFilter allLinkCounts = new LinkFilter(ALL_CONNECTIONS, 0, "הכל",DEPTH_TYPE.ALL);
        LinkFilter commentaryGroup = getCommentaryOnChap(text.tid - 11, text.tid + 11, text.bid);//getting all commentaries +-11 of the current text


        if(text.getNumLinks() == 0){
            if(commentaryGroup.getChildren().size()>0)
                allLinkCounts.addChild(commentaryGroup,true);
            return allLinkCounts;
        }

        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        String sql;

        /*
        if(Build.VERSION.SDK_INT >= 21 && true) {//this works for newer androids
            sql = "SELECT B.title, Count(*) as booksCount, B.heTitle, B.commentsOn, B.categories FROM Books B, Links_small L, Texts T WHERE (" +
                    "(L.tid1 = " + text.tid + " AND L.tid2=T._id AND T.bid= B._id)" +
                    " OR (L.tid2 = " + text.tid + " AND L.tid1=T._id AND T.bid= B._id)" +
                    " ) GROUP BY B._id ORDER BY B.categories, B._id"
            ;
        }
        */
        sql = "select B2.title, Count(*) as booksCount, B2.heTitle, B2.commentsOn, B2.categories FROM" +
                " (SELECT ALL B._id FROM Books B, Links_small L, Texts T WHERE ((L.tid1 = " + text.tid + " AND L.tid2=T._id AND T.bid= B._id) )" +
                " UNION ALL SELECT ALL B._id FROM Books B, Links_small L, Texts T WHERE ( (L.tid2 = " + text.tid + " AND L.tid1=T._id AND T.bid= B._id) )) as tmp, Books B2" +
                " where tmp._id= B2._id GROUP BY tmp._id ORDER BY B2.categories, B2._id";

        Cursor cursor = db.rawQuery(sql, null);

        LinkFilter countGroups = null;
        String lastCategory = "";
        if (cursor.moveToFirst()) {
            do {
                //Log.d("LinkFilter", ""+ text.levels[0] + ": " + cursor.getString(4));
                String [] categories = Util.str2strArray(cursor.getString(4));
                if(categories.length == 0) continue;
                if(countGroups == null || !categories[0].equals(lastCategory)){
                    if(countGroups != null && countGroups.count >0) {
                        allLinkCounts.addChild(countGroups);
                    }
                    lastCategory = categories[0];
                    String category,heCategory;
                    if(categories[0].equals("Commentary")) {
                        category = QUOTING_COMMENTARY;
                        heCategory = "מפרשים מצטטים";
                    }else {
                        category = categories[0];
                        heCategory = category;
                        //the real he titles will be added in sortLinkCountCategories() if it finds match
                    }

                    countGroups = new LinkFilter(category,0,heCategory ,DEPTH_TYPE.CAT);
                }
                String childEnTitle = cursor.getString(0);
                int childCount = cursor.getInt(1);
                String childHeTitle = cursor.getString(2);
                if(cursor.getInt(3) == text.bid) {//Comments on this book
                    commentaryGroup.addCommentaryCount(childEnTitle,childCount,childHeTitle);
                }else{ //non commentary book
                    LinkFilter linkCount = new LinkFilter(childEnTitle,childCount,childHeTitle,DEPTH_TYPE.BOOK);
                    countGroups.addChild(linkCount);
                }

            } while (cursor.moveToNext());
        }
        if(countGroups.count >0)
            allLinkCounts.addChild(countGroups);

        allLinkCounts = allLinkCounts.sortLinkCountCategories();

        if(commentaryGroup.count >0 || commentaryGroup.getChildren().size()>0)
            allLinkCounts.addChild(commentaryGroup,true);

        //Log.d("LinkFilter", "...finished: " + allLinkCounts.count);
        return allLinkCounts;
    }

    private LinkFilter sortLinkCountCategories(){
        LinkFilter newLinkCount = new LinkFilter(enTitle,0,heTitle,depth_type);
        MenuNode menuNode = MenuState.getRootNode();
        String [] titles = menuNode.getChildrenTitles(Util.Lang.EN);
        String [] heTitles = menuNode.getChildrenTitles(Util.Lang.HE);
        for(int i=0;i<titles.length;i++){
            String title = titles[i];
            for(int j=0;j<getChildren().size();j++){
                LinkFilter child = getChildren().get(j);
                if(child.enTitle.equals(title)){
                    child.heTitle = heTitles[i];
                    newLinkCount.addChild(child);
                    getChildren().remove(j);
                    break;
                }
            }
        }
        //if there's any categories left out them back in
        for(LinkFilter child:getChildren()){
            newLinkCount.addChild(child);
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
