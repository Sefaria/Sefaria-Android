package org.sefaria.sefaria.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.List;


public class LinkCount {

    protected String enTitle;
    protected String heTitle;
    protected DEPTH_TYPE depth_type;
    protected int count;
    protected List<LinkCount> children;
    protected LinkCount parent = null;

    public enum DEPTH_TYPE {
        ALL,CAT,BOOK
    }

    //category is passed in only if DEPTH_TYPE == DEPTH_TYPE.BOOK
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

    public String getCategory(Book book) {
        if(parent == null)
            return "";
        if(DEPTH_TYPE.BOOK == depth_type)
            return parent.getSlimmedTitle(book, Util.Lang.EN);
        else// if(DEPTH_TYPE.CAT == depth_type) || ALL
            return getSlimmedTitle(book, Util.Lang.EN);

    }

    public List<LinkCount> getChildren(){
        if(children == null)
            children = new ArrayList<>();
        return children;
    }

    protected void addChild(LinkCount child){
        addChild(child,false);
    }

    protected void addChild(LinkCount child, boolean front){
        if(children == null)
            children = new ArrayList<>();
        count += child.count;
        if(front)
            children.add(0,child);
        else
            children.add(child);
        child.parent = this;
    }

    public static String getStringTree(LinkCount lc,int tabs, boolean printToLog){
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

    final protected static String ALL_CONNECTIONS = "All Connections";
    final protected static String COMMENTARY = "Commentary";
    final protected static String QUOTING_COMMENTARY = "Quoting Commentary";

    private static LinkCount getCommentaryOnChap(int chapStart, int chapEnd, int bid){
        LinkCount commentaryGroup = new LinkCount(COMMENTARY,0, "מפרשים",DEPTH_TYPE.CAT);
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Log.d("Link", "starting getCommentaryOnChap");

        String sql = "SELECT B.title, B.heTitle FROM Books B, Links_small L, Texts T WHERE (" +
                "((L.tid1 BETWEEN " + chapStart + " AND " + chapEnd +  ") AND L.tid2=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"
                + " OR " +
                "((L.tid2 BETWEEN " + chapStart + " AND " + chapEnd +  ") AND L.tid1=T._id AND T.bid= B._id AND B.commentsOn=" + bid + ")"
                + ") GROUP BY B._id ORDER BY B._id"
                ;

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                LinkCount linkCount = new LinkCount(cursor.getString(0),0,cursor.getString(1),DEPTH_TYPE.BOOK);
                commentaryGroup.addChild(linkCount);
            } while (cursor.moveToNext());
        }
        Log.d("Link", "finsihing getCommentaryOnChap");
        return commentaryGroup;
    }

    /**
     * linearly loop through all children (commentary books on the chapter), and update the count for the specific book (using the title as the key)
     * @param title
     * @param count
     */
    private void addCommentaryCount(String title, int count, String heTitle){
        if(count == 0) return; //it's not worth trying to go through the whole list testing a match, if this is just zero anyways. (It should never happen that this is 0).
        for(LinkCount child: children){
            if(child.enTitle.equals(title)){
                child.count = count;
                this.count += count;
                return;
            }
        }
        Log.e("LinkCount", "I couldn't find the book, even though the list is supposed to contain all books that comment on the chap... I'll create a new LinkCount if I hae to");
        LinkCount linkCount = new LinkCount(title,count,heTitle,DEPTH_TYPE.BOOK);
        this.addChild(linkCount);
    }


    public static LinkCount getFromLinks_small(Text text){
        LinkCount allLinkCounts = new LinkCount(ALL_CONNECTIONS, 0, "All Connections (He)",DEPTH_TYPE.ALL);
        if(text.getNumLinks() == 0)  return allLinkCounts;
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Log.d("Link", "starting getCountsTitlesFromLinks_small");

        LinkCount commentaryGroup;
        //commentaryGroup = new LinkCount(COMMENTARY,0, "מפרשים",DEPTH_TYPE.CAT);
        commentaryGroup = getCommentaryOnChap(text.tid -10,text.tid +10,text.bid);//TODO this actually needs to get from begin and end of chap (not randomly)

        String sql = "SELECT B.title, Count(*) as booksCount, B.heTitle, B.commentsOn, B.categories FROM Books B, Links_small L, Texts T WHERE (" +
                "(L.tid1 = " + text.tid + " AND L.tid2=T._id AND T.bid= B._id) OR " +
                "(L.tid2 = " + text.tid + " AND L.tid1=T._id AND T.bid= B._id) ) GROUP BY B._id ORDER BY B.categories, B._id"
                ;

        Cursor cursor = db.rawQuery(sql, null);


        LinkCount countGroups = null;
        String lastCategory = "";
        if (cursor.moveToFirst()) {
            do {

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
                String childEnTitle = cursor.getString(0);
                int childCount = cursor.getInt(1);
                String childHeTitle = cursor.getString(2);
                if(cursor.getInt(3) == text.bid) {//Comments on this book
                    commentaryGroup.addCommentaryCount(childEnTitle,childCount,childHeTitle);
                    //LinkCount linkCount = new LinkCount(childEnTitle,childCount,childHeTitle,DEPTH_TYPE.BOOK);
                    //commentaryGroup.addChild(linkCount);
                }else{
                    LinkCount linkCount = new LinkCount(childEnTitle,childCount,childHeTitle,DEPTH_TYPE.BOOK);
                    countGroups.addChild(linkCount);
                }

            } while (cursor.moveToNext());
        }
        if(countGroups.count >0)
            allLinkCounts.addChild(countGroups);

        allLinkCounts = allLinkCounts.sortLinkCountCategories();

        if(commentaryGroup.count >0)
            allLinkCounts.addChild(commentaryGroup,true);
        Log.d("Link", "finished getCountsTitlesFromLinks_small");

        //getStringTree(allLinkCounts, 0, true);


        return allLinkCounts;
    }

    private LinkCount sortLinkCountCategories(){
        LinkCount newLinkCount = new LinkCount(enTitle,0,heTitle,depth_type);
        MenuNode menuNode = MenuState.getRootNode();
        String [] titles = menuNode.getChildrenTitles(Util.Lang.EN);
        for(String title: titles){
            for(int j=0;j<getChildren().size();j++){
                LinkCount child = getChildren().get(j);
                if(child.enTitle.equals(title)){
                    newLinkCount.addChild(child);
                    getChildren().remove(j);
                    break;
                }
            }
        }
        //if there's any categories left out them back in
        for(LinkCount child:getChildren()){
            newLinkCount.addChild(child);
        }

        return newLinkCount;
    }


}
