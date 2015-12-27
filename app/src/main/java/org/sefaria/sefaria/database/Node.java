package org.sefaria.sefaria.database;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Util;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node{ //TODO implements  Parcelable

    public final static int NODE_TYPE_BRANCH = 1;
    public final static int NODE_TYPE_TEXTS = 2;
    public final static int NODE_TYPE_REFS = 3;

    public final static int NID_NON_COMPLEX = -3;
    public  final static int NID_NO_INFO = -1;

    private int nid;
    private int bid;
    private int parentNode;
    private int nodeType;
    private int siblingNum;
    private String enTitle;
    private String heTitle;
    private String [] sectionNames;
    private String [] heSectionNames;
    private int structNum;
    private int textDepth;
    private int startTid;
    private int endTid;
    private String extraTids;

    private List<Node> children;
    private List<Integer> chaps;
    private List<Text> textList;
    //private Node parent;

    private static String NODE_TABLE = "Nodes";


    public Node(){
        children = new ArrayList<>();
        chaps = new ArrayList<>();
        nid = NID_NO_INFO;
    }

    public Node(Book book){
        children = new ArrayList<>();
        chaps = new ArrayList<>();
        nid = NID_NO_INFO;

        bid = book.bid;
        sectionNames = book.sectionNamesL2B;
        heSectionNames = book.heSectionNamesL2B;
        textDepth = book.textDepth;
        enTitle = book.title;
        heTitle = book.heTitle;

    }

    public  Node (Cursor cursor){
        children = new ArrayList<>();
        nid = NID_NO_INFO;
        chaps = new ArrayList<>();
        getFromCursor(cursor);
        addSectionNames();
    }



    /**
     * use lang from Util.HE or EN (maybe or BI)
     * @param lang
     */
    public String getTitle(Util.Lang lang){
        if(Util.Lang.EN == lang)
            return enTitle;
        else if(Util.Lang.HE == lang)
            return heTitle;
        else if(Util.Lang.BI == lang)
            return enTitle + " " + heTitle;
        else{
            Log.e("Node", "wrong lang num");
            return "";
        }
    }

    /**
     * if (children.size() == 0)
     *      this is a leaf and you need to check
     *
     * @return children
     */
    public List<Node> getChildren(){
        if(children == null)
            children = new ArrayList<>();
        return children;
    }

    /**
     * chaps.size() == 0 when there's no chaps within it. If there's no children, this means that this Node contains Texts
     *
     *
     * @return chaps
     */
    public  List<Integer> getChaps(){
        if(chaps == null)
            chaps = new ArrayList<>();
        return chaps;
    }



    private void addSectionNames(){
        //TODO move to create SQL
        String sectionStr = "[";
        for(int i=0;i<textDepth;i++) {
            sectionStr += "Section" + i;
            if(i < textDepth-1)
                sectionStr += ",";
        }
        sectionStr += "]";
        //Log.d("Node",sectionStr);
        sectionNames = Util.str2strArray(sectionStr);
        heSectionNames = sectionNames;

    }

    private void getFromCursor(Cursor cursor){
        try{
            nid = cursor.getInt(0);
            bid = cursor.getInt(1);
            parentNode = cursor.getInt(2);
            nodeType = cursor.getInt(3);
            siblingNum = cursor.getInt(4);
            enTitle = cursor.getString(5);
            heTitle = cursor.getString(6);
            sectionNames = Util.str2strArray(cursor.getString(7));
            heSectionNames = Util.str2strArray(cursor.getString(8));
            structNum = cursor.getInt(9);
            textDepth = cursor.getInt(10);
            startTid = cursor.getInt(11);
            endTid = cursor.getInt(12);
            extraTids = cursor.getString(13);
        }
        catch(Exception e){
            MyApp.sendException(e);
            Log.e("Node", e.toString());
            nid = 0;
            return;
        }
    }

    private void addChild(Node node){
        this.children.add(node);
        //node.parent = this;
        if(node.siblingNum != this.children.size() -1) {
            //TODO make sure the order is correct
            Log.e("Node", "wrong sibling num");
            this.children.add(node);
        }
    }

    /**
     * just for debugging
     * @param node
     */
    private static void showTree(Node node){
        node.log();
        if(node.children.size() == 0) {
            //Log.d("Node showtree", "chaps: "+ node.chaps);
            return;
        }
        else{
            //Log.d("Node", "node " + node.nid + " children: ");
            for(int i=0;i<node.children.size();i++) {
                node.children.get(i).log();
            }
            //Log.d("node", "... end of chilren list");
            for(int i=0;i<node.children.size();i++){
                showTree(node.children.get(i));
            }

        }
    }

    private static Node convertToTree(List<Node> nodes) throws API.APIException{
        Node root = null;
        //TODO for each struct
        int startID = -1;
        for(int i =0;i<nodes.size();i++){
            Node node = nodes.get(i);

            if(startID < 0)
                startID = node.nid;

            if(node.parentNode == 0){
                if(root == null)
                    root = node;
                else
                    Log.e("Node", "Root already taken!!");
            }else{
                Node node2 = nodes.get(node.parentNode - startID);
                if(node2.nid == node.parentNode)
                    node2.addChild(node);
                else{
                    Log.e("Node","Parent in wrong spot");
                    for(int j=0;j<nodes.size();j++){
                        node2 = nodes.get(j);
                        if(node2.nid == node.parentNode) {
                            node2.addChild(node);
                            break;
                        }
                    }
                }

                // add chap count (if it's a leaf with 2 or more levels):
                if(node.textDepth >= 2 && node.nodeType == NODE_TYPE_TEXTS) {
                    node.getAllChaps(true);
                }
            }

        }

        return root;
    }


    private void getAllChaps(boolean useNID) throws API.APIException {
        //if(true) return;
        if(textDepth < 2){
            Log.e("Node", "called getAllChaps with too low texdepth" + this.toString());
            return;
        }

        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();


        String levels = "";
        for(int i=textDepth;i>1;i--){
            levels += "level" + i;
            if(i > 2)
                levels += ",";
        }
        //Log.d("Node", "node:" + this.toString());

        String sql = "SELECT DISTINCT " + levels + " FROM "+ Text.TABLE_TEXTS;

        if(useNID)  sql += " WHERE bid = " + this.bid + " AND  parentNode = " + this.nid;
        else        sql += " WHERE bid = " + this.bid;

        sql += " ORDER BY  " + levels;

        //Log.d("Node", "sql: " + sql);
        Node tempNode = null;
        int lastLevel3 = 0;
        int originalNodeType = this.nodeType;

        if(API.useAPI()){
            ;
            //chapList = API.getAllChaps(Book.getTitle(bid),levels);
            //TODO add complex text API stuff
        }

        try{
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    if(textDepth == 2) {
                        chaps.add(cursor.getInt(0));
                    }else if(textDepth == 3){
                        this.nodeType = NODE_TYPE_BRANCH;//TODO see if this needs a different nodeType number
                        int level3 = cursor.getInt(0);
                        if(level3 != lastLevel3 || tempNode == null){
                            lastLevel3 = level3;
                            tempNode = new Node();
                            tempNode.enTitle = this.sectionNames[2] + " " + level3;
                            tempNode.heTitle = this.heSectionNames[2] + " " + Util.int2heb(level3);
                            tempNode.sectionNames = Arrays.copyOfRange(this.sectionNames,0,2);
                            tempNode.heSectionNames = Arrays.copyOfRange(this.heSectionNames,0,2);
                            tempNode.textDepth = this.textDepth -1;
                            tempNode.nodeType = originalNodeType;
                            this.children.add(tempNode);
                        }
                        tempNode.chaps.add(cursor.getInt(1));
                        //TODO extend to more than 3 levels
                    }
                    else{
                        Log.e("Node", "add 4 or more levels");//TODO
                    }
                } while (cursor.moveToNext());
            }
        }catch(Exception e){
            Log.e("Node", e.toString());
        }

        return;
    }


    public List<Text> getTexts() throws API.APIException{
        if(textList != null)
            return  textList;
        return Text.get(this);
    }

    public List<Text> getTexts(int chapNum){
        List<Text> texts = new ArrayList<>();
        return texts;
    }

    private boolean isComplex(){
        return !(this.nid == NID_NON_COMPLEX);
    }

    public int getBid(){
        return bid;
    }

    /**
     * create an array describing the levels:
     * ex. chap 4 and verse 7 would be {7,3}
     * return levels
     */
    public int [] getLevels(){
        int levels [] = new int [] {1,2};//TODO based on logic
        return levels;
    }


    public static List<Node> getRoots(Book book) throws API.APIException {
        return getRoots(book,true);
    }

    private static List<Node> getRoots(Book book, boolean addMoreh) throws API.APIException{
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.query(NODE_TABLE, null, "bid" + "=?",
                new String[]{String.valueOf(book.bid)}, null, null, "_id", null); //structNum, parentNode, siblingNum
        Node root;

        List<Node> nodes = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()){
            do {
                Node node = new Node(cursor);
                //node.log();
                nodes.add(node);
            } while (cursor.moveToNext());
        }

        if(nodes.size() == 0){//TODO need to add way if the main text is regular but has alt... make it always try to add it.
            root = new Node(book);
            root.nid = NID_NON_COMPLEX;
            root.getAllChaps(false);
            nodes.add(root);
        }
        else
            root = convertToTree(nodes);
        //showTree(root);

        if(addMoreh){//TODO remove only for testing alt structures
            nodes.add(getRoots(new Book(1175),false).get(0));
        }


        return nodes;
        //return root;
    }

    @Override
    public String toString() {
        return nid + "," + bid + "," + enTitle + " " + heTitle + "," + Util.array2str(sectionNames) + "," + Util.array2str(heSectionNames) + "," + structNum + "," + textDepth + "," + startTid + "," + endTid + "," + extraTids;
    }

    public void log(){
        Log.d("Node", this.toString());
    }


    /*
    //TODO
    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Node> CREATOR
            = new Parcelable.Creator<Node>() {
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        public Node[] newArray(int size) {
            return new Book[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bid);
        dest.writeInt(commentsOn);
        dest.writeStringArray(sectionNames);
        dest.writeStringArray(sectionNamesL2B);
        dest.writeStringArray(heSectionNamesL2B);
        dest.writeStringArray(categories);
        dest.writeInt(textDepth);
        dest.writeInt(wherePage);
        dest.writeString(title);
        dest.writeString(heTitle);
        dest.writeInt(languages);
    }

    private Book(Parcel in) {
        bid = in.readInt();
        commentsOn = in.readInt();
        sectionNames = in.createStringArray();
        sectionNamesL2B = in.createStringArray();
        heSectionNamesL2B = in.createStringArray();
        categories = in.createStringArray();

        textDepth = in.readInt();
        wherePage = in.readInt();
        title = in.readString();
        heTitle = in.readString();
        languages = in.readInt();
    }
    */

}
