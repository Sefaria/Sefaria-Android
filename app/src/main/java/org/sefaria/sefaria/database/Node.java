package org.sefaria.sefaria.database;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TOCElements.TOCVersion;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node{// implements  Parcelable{


    public final static int NODE_TYPE_BRANCH = 1;
    public final static int NODE_TYPE_TEXTS = 2;
    public final static int NODE_TYPE_REFS = 3;


    public final static int NID_NON_COMPLEX = -3;
    public  final static int NID_NO_INFO = -1;
    public final static int NID_CHAP_NO_NID = -4;
    public final static int NID_DUMMY = -5;

    private int nid;
    private int bid;
    private int parentNodeID;
    private Node parent = null;
    //private int siblingNum;
    private String enTitle;
    private String heTitle;
    private String [] sectionNames;
    private String [] heSectionNames;
    private int structNum;
    private int textDepth;
    private int startTid;
    private int endTid;
    private String extraTidsRef;
    private String startLevels;
    private List<Node> children;
    private List<Text> textList;

    private Book book;
    private boolean isTextSection = false;
    private boolean isGridItem = false;
    private boolean isComplex = false;
    private boolean isRef = false;
    int gridNum = -1;
    /**
     * this number should only be defined for the root of a tree.
     */
    private int tocRootsNum = -1;


    private String lastSearchedTerm = null;
    private List<Text> foundFindOnPageList = null;

    private Boolean gotTextListInAPI;

    private static Map<Integer,Node> allSavedNodes = new HashMap<>();

    public static Node getSavedNode(int hash){
        return allSavedNodes.get(hash);
    }
    public static void saveNode(Node node){
        allSavedNodes.put(node.hashCode(),node);
    }


    public static Map<String,List<Node>> allSavedBookTOCroots = new HashMap<>();
    private static List<Node> getSavedBookTOCroots(Book book){ return allSavedBookTOCroots.get(book);}
    private static String NODE_TABLE = "Nodes";

    public static Node dummyNode = new Node(true);


    public Node(boolean dummy){
        children = new ArrayList<>();
        nid = NID_DUMMY;
        textList = new ArrayList<>();
        parent = null;
        parentNodeID = NID_NO_INFO;
    }

    public Node(){
        children = new ArrayList<>();
        nid = NID_NO_INFO;
        parentNodeID = NID_NO_INFO;
    }

    public Node(Book book){
        children = new ArrayList<>();
        nid = NID_NO_INFO;
        parentNodeID = NID_NO_INFO;

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
        parentNodeID = NID_NO_INFO;
        getFromCursor(cursor);
    }

    public String getMenuBarTitle(Book book, Util.Lang menuLang){
        String str = book.getTitle(menuLang);
        if(isComplex)
            str += ", ";
        else
            str += " ";
        str += this.getWholeTitle(menuLang, false, true);
        return str;
    }

    public Node getParent(){return parent;}
    public String getExtraTidsRef() {return extraTidsRef;}

    /**
     * @param lang
     * @return full description of current text. For example, Chelek 3 Siman 23
     */
    public String getWholeTitle(Util.Lang lang, boolean doSectionName, boolean showTextVersion){
        String str = "";
        Node node = this;
        boolean usedSpaceAlready = true;
        while(node.parent != null) {
            if (str.length() == 0){
                str = node.getWholeTitleForOnly1Node(lang,doSectionName);
            }else {
                if (node.isComplex && !node.isGridItem)
                    str = node.getWholeTitleForOnly1Node(lang,doSectionName) + ", " + str;
                else {
                    String separator;
                    if(usedSpaceAlready && !node.isDaf() && !doSectionName)
                        separator = ":";
                    else
                        separator = " ";
                    str = node.getWholeTitleForOnly1Node(lang, doSectionName) + separator + str;
                    usedSpaceAlready = true;
                }
            }
            node = node.parent;
        }
        if(showTextVersion && getTextVersion() != null && !getTextVersion().isDefaultVersion())
            str += " - " + getTextVersion().getPrettyString();
        return str;
    }

    private String getWholeTitleForOnly1Node(Util.Lang lang, boolean doSectionName){
        String str = "";
        if(isGridItem || nid == NID_CHAP_NO_NID) {
            if(doSectionName) {
                String sectionName = getSectionName(lang);
                if(sectionName.length()>0)
                    str = sectionName + " ";
            }
            str += getNiceGridNum(lang);
        }
        else
            str = getTitle(lang);
        return str;
    }

    private String getSectionName(Util.Lang lang){
        String name = "";
        if(lang == Util.Lang.BI)
            lang = Settings.getMenuLang();

        String [] names;
        if(lang == Util.Lang.EN){
            names = sectionNames;
        }else{// if(lang == Util.Lang.HE){
            names = heSectionNames;
        }
        if(names.length > 0)
            name = names[names.length-1];
        if(name == null)
            name = "";
        return name;
    }


    /**
     * will get the grid number in the right language and all converted if it's a daf
     * @param lang
     * @return
     */
    public String getNiceGridNum(Util.Lang lang){
        if(gridNum == 0)
            return "";
        return Header.getNiceGridNum(lang,gridNum,isDaf());
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
        else if(Util.Lang.BI == lang) {
            return enTitle + " - " + heTitle;
        }
        else{
            Log.e("Node", "wrong lang num");
            return "";
        }
    }

    public int getTocRootNum(){
        Node root = getAncestorRoot();
        return root.tocRootsNum;
    }

    public int getDepth(){
        int depth = 0;
        Node parent = this.parent;
        while(parent != null){
            depth++;
            parent = parent.parent;
        }
        return depth;
    }


    private Node getFirstDescendant(boolean checkForTexts) throws API.APIException {
        //Log.d("Node","getFirstDescendant" + checkForTexts + " " + this);
        //boolean checkForTexts = false;
        final int MAX_TEXTS = 5;
        Node node = getFirstDescendant();
        int failedTexts = 0;
        if(checkForTexts){
            while(node.getTexts().size() <1) {
                if(failedTexts++ > MAX_TEXTS){
                    Log.e("Node","getFirstDescendant: Couldn't find texts after looking at " + MAX_TEXTS);
                    break;
                }

                try {
                    node = node.getNextTextNode();
                }catch (Node.LastNodeException e){
                    break;
                }
            }
        }
        return node;
    }

    public Node getFirstDescendant(){
        Node node = this;
        while(node.getChildren().size() > 0){
            node = node.getChildren().get(0);
        }
        return node;
    }

    public Node getLastDescendant(){
        Node node = this;
        while(node.getChildren().size() > 0){
            node = node.getChildren().get(node.getChildren().size()-1);
        }
        return node;
    }

    /**
     * Return the List<Node> of all the children (in order) of this node.
     * If this node is a leaf, it will return a list of 0 elements.
     * @return children
     */
    public List<Node> getChildren(){
        if(children == null)
            children = new ArrayList<>();
        return children;
    }

    /**
     *
     * @return bid
     */
    public int getBid(){ return bid; }

    public Book getBook() throws Book.BookNotFoundException {
        if(book != null)
            return book;
        book = new Book(bid);
        return book;
    }
    /*
    private void addSectionNames1(){
        //TODO move to create SQL
        if(sectionNames.length > 0)
            return;
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
    */

    private void setFlagsFromNodeType(int nodeType, int siblingNum){
        final int IS_COMPLEX = 2;
        final int IS_TEXT_SECTION = 4;
        final int IS_GRID_ITEM = 8;
        final int IS_REF = 16;
        if(nodeType == 0)
            Log.e("Node", "Node.setFlagsFromNodeType: nodeType == 0. I don't know anything");
        isComplex = (nodeType & IS_COMPLEX) != 0;
        isTextSection = (nodeType & IS_TEXT_SECTION) != 0;
        isGridItem = (nodeType & IS_GRID_ITEM) != 0;
        isRef = (nodeType & IS_REF) != 0;
        if(isComplex){
            gridNum = siblingNum + 1;
            //TODO this needs to be more complex for when there's: Intro,1,2,3,conclusion
        }
    }

    public boolean isComplex(){ return isComplex;}
    public boolean isTextSection(){ return isTextSection; }
    public boolean isGridItem() { return isGridItem; }
    public boolean isRef(){ return isRef;}



    private void getFromCursor(Cursor cursor){
        try{
            nid = cursor.getInt(0);
            bid = cursor.getInt(1);
            parentNodeID = cursor.getInt(2);
            int nodeType = cursor.getInt(3);
            int siblingNum = cursor.getInt(4);
            enTitle = cursor.getString(5);
            heTitle = cursor.getString(6);
            sectionNames = Util.str2strArray(cursor.getString(7));
            heSectionNames = Util.str2strArray(cursor.getString(8));
            structNum = cursor.getInt(9);
            textDepth = cursor.getInt(10);
            startTid = cursor.getInt(11);
            endTid = cursor.getInt(12);
            extraTidsRef = cursor.getString(13);
            try{ //this is to make it work with databases older than version 150
                startLevels = cursor.getString(14);//This might crash on old DBs
            }catch (Exception e2){}

            setFlagsFromNodeType(nodeType, siblingNum);

        }
        catch(Exception e){
            GoogleTracker.sendException(e);
            Log.e("Node", "failure to pull from DB" +  e.toString());
            nid = 0;
            return;
        }
    }

    private void addChapChild(int chapNum){
        Node node = new Node();
        node.bid = bid;
        node.isGridItem = true;
        node.isTextSection = true;
        node.isRef = false;
        node.isComplex = isComplex;
        node.gridNum = chapNum;
        //node.siblingNum = -1;
        node.enTitle = node.heTitle =  "";
        node.textDepth = 2;
        node.sectionNames = Arrays.copyOfRange(this.sectionNames,0,node.textDepth);
        node.heSectionNames = Arrays.copyOfRange(this.heSectionNames,0,node.textDepth);
        node.parentNodeID = nid;

        node.structNum = structNum;
        node.nid = NID_CHAP_NO_NID;
        node.parent = this;
        addChild(node);
    }

    private void addChild(Node node){
        this.children.add(node);
        node.parent = this;
        if(node.parentNodeID == NID_NO_INFO)
            node.parentNodeID = this.nid;
        /*
        if(node.siblingNum == -1){
            node.siblingNum = this.children.size() -1;
        }else if(node.siblingNum != this.children.size() -1) {
            //TODO make sure the order is correct
            Log.e("Node", "wrong sibling num. siblingNum:" + node.siblingNum + " children.size():" + children.size()  + "__" + this);
        }
        */
    }

    /**
     * this is only meant to be called on a node that node.isTextSection()
     * @return the next node in the tree that contains text
     */
    public Node getNextTextNode() throws LastNodeException {
        Log.d("Node","getNextTextNode started: " + this);
        if(parent == null){
            throw new LastNodeException();
        }
        int index = parent.getChildren().indexOf(this);
        Node nextNode;
        if(index == -1){
            Log.e("Node.getNextTextNode","Couldn't find index in parent's children: " + this);
            //Log.d("Node", "getNextTextNode returning: " + getFirstDescendant());
            nextNode = getFirstDescendant();
        } else if(index < parent.getChildren().size()-1){
            Node node = parent.getChildren().get(index +1);
            if(node.isTextSection){
                //Log.d("Node", "getNextTextNode returning: " + node);
                nextNode = node;
            }else {
                //Log.d("Node", "getNextTextNode returning: " + node.getFirstDescendant());
                nextNode = node.getFirstDescendant();
            }
        }else {// if(index >=parent.getChildren().size()){
            nextNode = parent.getNextTextNode();
        }
        nextNode.setTextVersion(this.getTextVersion());
        return nextNode;
    }
    /**
     * this is only meant to be called on a node that node.isTextSection()
     * @return the prev node in the tree that contains text
     */
    public Node getPrevTextNode() throws LastNodeException {
        if(parent == null){
            throw new LastNodeException();
        }
        int index = parent.getChildren().indexOf(this);
        Node prevNode;
        if(index == -1){
            Log.e("Node.getNextTextNode", "Couldn't find index in parent's children: " + this);
            prevNode = getLastDescendant();
        }else if(index > 0){
            Node node = parent.getChildren().get(index - 1);
            if(node.isTextSection){
                prevNode = node;
            }else {
                prevNode = node.getLastDescendant();
            }
        }else {// if(index == 0){
            prevNode = parent.getPrevTextNode();
        }
        prevNode.setTextVersion(this.getTextVersion());
        return prevNode;
    }

    /**
     * //TODO work for complex text
     * @param spot is a number as the level number or the title of the Complex text node
     * @return correct child for spot name (or null if can't find it)
     */
    public Node getChild(String spot) {
        Log.d("Node","spot:" + spot);
        try {
            if (spot.matches("[0-9]+[ab]?")) {
                int num = Util.convertDafOrIntegerToNum(spot);
                Node lastChild = null;
                for (Node child : getChildren()) {
                    if (num == child.gridNum)
                        return child;
                    else if (num < child.gridNum) {
                        if (lastChild == null)
                            lastChild = child;
                        return lastChild;
                    }
                    lastChild = child;
                }
            } else {
                for (Node child : getChildren()) {
                    if (child.getTitle(Util.Lang.EN).equals(spot)) {
                        return child;
                    }
                }
            }
        }catch (Exception e){
            Log.d("Node",e.getMessage() + "...Complex string spot:" + spot);

        }

        return null;
    }

    public class LastNodeException extends Exception{
        public LastNodeException(){
            super();
        }

        private static final long serialVersionUID = 1L;

    }



    public List<Text> findWords(String searchingTerm) throws API.APIException {
        Log.d("Node","findwords1:" + searchingTerm + " lastWord:" + lastSearchedTerm + "  ---" + this);
        if(lastSearchedTerm != null && !lastSearchedTerm.equals(searchingTerm)){
            Searching.removeRed(textList);
        }

        if(searchingTerm == null)
            return null;
        if(searchingTerm.equals(lastSearchedTerm)){
            return foundFindOnPageList;
        }
        lastSearchedTerm = searchingTerm;

        Log.d("Node","Findwords:" + searchingTerm + "... " + getTexts().size());
        foundFindOnPageList = Searching.findWordsInList(getTexts(),searchingTerm,false,false);
        return foundFindOnPageList;
    }

    public String getPath(Util.Lang lang, boolean forURL, boolean includeBook, boolean replaceSpaces){
        String path = "";
        Log.d("Node","getPath"+ this);

        Node node = this;
        String separator;
        boolean addSpace = true;

        while(node.getParent() != null){//checking parent node so that don't get root (or book name) in there
            if(forURL)
                separator = ".";
            else if(!addSpace)
                separator = ":";
            else
                separator = " ";

            if(!node.isGridItem() && (forURL && isComplex || !forURL)) //TODO tech this is wrong. B/c if it forURL && isComplex and it 3 or more levels of depth then it shouldn't be using the comma
                path = ", " + node.getTitle(lang) + path;
            else
                path = separator + node.getNiceGridNum(lang) + path;


            if(path.equals(separator)) //fixes problems with texts with only 1 level (like Hadran where it ends up looking like "Hadran." otherwise)
                path = "";

            addSpace = node.isDaf() && (lang == Util.Lang.HE) && !forURL;

            node = node.getParent();
        }

        if(includeBook) {
            try {
                path = (new Book(this.bid)).getTitle(lang) + path;
            } catch (Book.BookNotFoundException e) {
                e.printStackTrace();
            }
        }
        if(replaceSpaces)
            path = path.replace(" ", "_");
        return path;
    }

    public Node getAncestorRoot(){
        Node node = this;
        while(node.parent != null){
            node = node.parent;
        }
        //Log.d("Node.getAncestorRoot", "root:" + node);
        return node;
    }

    /**
     * Shows the TOC Node tree. This is only used for debugging.
     * @param node
     */
    private static void showTree(Node node){
        showTree(node, "");
    }

    /**
     * Shows the TOC Node tree. This is only used for debugging.
     * @param node
     */
    private static void showTree(Node node, String tabs){
        Log.d("Node", tabs + node);
        if(node.getChildren().size() == 0) {
            return;
        }
        else{
            for(int i=0;i<node.getChildren().size();i++){
                showTree(node.getChildren().get(i), tabs + "\t");
            }
        }
    }

    /**
     * converts complex nodes list into a complete tree
     * @param nodes
     * @return root
     * @throws API.APIException
     */
    private static Node convertToTree(List<Node> nodes) throws API.APIException{
        Node root = null;
        //TODO for each struct
        int startID = -1;
        for(int i =0;i<nodes.size();i++){
            Node node = nodes.get(i);

            if(startID < 0)
                startID = node.nid;

            if(node.parentNodeID == 0){
                if(root == null)
                    root = node;
                else
                    Log.e("Node", "Root already taken!!");
            }else{
                Node node2 = nodes.get(node.parentNodeID - startID);
                if(node2.nid == node.parentNodeID)
                    node2.addChild(node);
                else{
                    Log.e("Node","Parent in wrong spot");
                    for(int j=0;j<nodes.size();j++){
                        node2 = nodes.get(j);
                        if(node2.nid == node.parentNodeID) {
                            node2.addChild(node);
                            break;
                        }
                    }
                }

                // add chap count (if it's a leaf with 2 or more levels):
                if(node.textDepth >= 2 && !node.isTextSection) {
                    node.setAllChapsDB(true);
                }
            }

        }

        root.setAllChaps_API();
        return root;
    }



    private Node createTempNode(int sectionNum){
        this.isTextSection = false;
        this.isGridItem = false;
        this.isRef = false;
        Node tempNode = new Node();
        try{
            tempNode.enTitle = this.sectionNames[textDepth -1] + " " + sectionNum; //using textDepth value to get the last section name (3-1 = 2)
            tempNode.heTitle = this.heSectionNames[textDepth -1] + " " + Util.int2heb(sectionNum);
            tempNode.sectionNames = Arrays.copyOfRange(this.sectionNames, 0, textDepth);//using textDepth value to get the last 2 section names (3-2=1,3)
            tempNode.heSectionNames = Arrays.copyOfRange(this.heSectionNames, 0, textDepth);
        }catch (Exception e){//This seems to happen with Complex texts with 3 levels (see Footnotes on Orot)
            e.printStackTrace();
            tempNode.sectionNames = tempNode.heSectionNames = new String []{"",""};//{"Section","Segment"};
            tempNode.enTitle = "" + sectionNum;
            tempNode.heTitle = Util.int2heb(sectionNum);
        }
        tempNode.textDepth = this.textDepth -1;
        tempNode.isComplex = isComplex;
        tempNode.isRef = false;
        tempNode.isGridItem = false;
        tempNode.isTextSection = false;
        //tempNode.siblingNum = sectionNum - 1;
        tempNode.gridNum = sectionNum;
        tempNode.nid = NID_CHAP_NO_NID;
        tempNode.bid = bid;

        this.addChild(tempNode);

        return tempNode;
    }


    private static void addSubChaps(Node upperNode, int currDepth, JSONObject jsonObject) throws JSONException {
        JSONArray counts;
        try {
            counts = jsonObject.getJSONObject("_all").getJSONArray("availableTexts");
        } catch (JSONException e) {
            counts = null;
        }
        if(counts != null)
            addSubChaps(upperNode, currDepth, counts);
    }

    private static int getDepth(JSONArray jsonArray, int depth){
        if(jsonArray.length() == 0)
            return depth;
        try{
            JSONArray newJsonArray = jsonArray.getJSONArray(0);
            return getDepth(newJsonArray, depth + 1);
        }catch (JSONException e){
            return depth +1;
        }
    }

    private static void addSubChaps(Node upperNode, int currDepth, JSONArray counts) throws JSONException {
        //currDepth = getDepth(counts,0);


        if(currDepth <= 1) {
            if(!upperNode.isComplex)
                upperNode.addChapChild(0);

            //else upperNode.isTextSection = true;
            return;
        }
        for (int i = 0; i < counts.length(); i++) {
            JSONArray subCounts = counts.getJSONArray(i);
            if (subCounts.length() > 0) {
                if(currDepth == 2)
                    upperNode.addChapChild(i + 1);
                else{
                    Node tempNode = upperNode.createTempNode(i + 1);
                    addSubChaps(tempNode,currDepth-1,subCounts);
                }
            }
        }
    }



    private TOCVersion textVersion;
    public void setTextVersion(TOCVersion textVersion){
        if(textVersion != null && textVersion.isDefaultVersion()) //default version
            textVersion = null;

        if((textVersion != null && !textVersion.equals(this.textVersion)) || (textVersion == null && this.textVersion != null))  //it's changed in some way
            textList = null; //so that it will get this specific version next time

        this.textVersion = textVersion;
        try {
            Settings.BookSettings.setTextVersion(getBook(),textVersion);
        } catch (Book.BookNotFoundException e) {
            //e.printStackTrace();
        }
    }

    public TOCVersion getTextVersion(){
        return textVersion;
    }

    private static void setChaps_API(Node node, JSONObject jsonData) {
        //Log.d("Node", "setChaps_API" + node);
        for(Node child:node.getChildren()){
            try{
                JSONObject subObject = jsonData.getJSONObject(child.enTitle);
                setChaps_API(child,subObject);
            }catch (JSONException e){
                Log.e("Node", child.enTitle + " __didn't get subJSON_" + child);
            }
        }
        try {
            addSubChaps(node, node.textDepth, jsonData);
        } catch (JSONException e) {
            Log.e("Node", "addSubChaps fail: " + node);
        }
    }

    /**
     *
     * @throws API.APIException
     */
    private void setAllChaps_API() throws API.APIException {
        if(!Settings.getUseAPI())
            return;

        //Log.i("Node", "settAllChaps_API: " + this);
        String bookTitle = Book.getTitle(bid);

        String place = bookTitle.replace(" ", "_");
        String url = API.COUNT_URL + place;
        String data = API.getDataFromURL(url);
        try {
            JSONObject jsonData = new JSONObject(data);
            if(jsonData.has("error")){
                Log.e("API","Book doesn't exist in Sefaria");
                API api = new API();
                //throw api.new APIException();
                //addChapChild(-1);
                return;
            }
            setChaps_API(this, jsonData);
            //addSubChaps(this, textDepth, jsonData);
        } catch(Exception e){
            Log.e("api", "Error: " + e.toString());
        }
        return;

    }


    private void setAllChapsDB(boolean useNID) throws API.APIException {
        if(Settings.getUseAPI())
            return;

        if(textDepth == 1){
            addChapChild(0);
            return;
        }else if(textDepth < 2){
            Log.e("Node", "called setAllChaps with too low texdepth" + this.toString());
            return;
        }

        SQLiteDatabase db = Database.getDB();

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
        Node tempNodeLevel4 = this;
        int lastLevel3 = 0, lastLevel4 = 0;



        try{
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    if(textDepth == 2) {
                        addChapChild(cursor.getInt(0));
                    }else if(textDepth >=3){//textDepth == 3 || textDepth == 4
                        int level3 = cursor.getInt(textDepth - 3);
                        if(textDepth == 4){
                            int level4 = cursor.getInt(0);
                            if(level4 != lastLevel4 || tempNodeLevel4 == this){
                                lastLevel4 = level4;
                                lastLevel3 = 0;
                                tempNodeLevel4 = this.createTempNode(level4);
                            }
                        }
                        if(level3 != lastLevel3 || tempNode == null){
                            lastLevel3 = level3;
                            tempNode = tempNodeLevel4.createTempNode(level3);
                        }
                        tempNode.addChapChild(cursor.getInt(textDepth-2));
                    }
                } while (cursor.moveToNext());
            }
        }catch(Exception e){
            Log.e("Node", e.toString());
            e.printStackTrace();
        }

        return;
    }

    public String getTextFromAPIData() throws API.APIException{
        return getTextFromAPIData(API.TimeoutType.REG);
    }

    public String getTextFromAPIData(API.TimeoutType timeoutType) throws API.APIException{
        String completeUrl = API.TEXT_URL + getPath(Util.Lang.EN, true, true, true);

        if(getTextVersion() != null) {
            String textVersion = getTextVersion().getDBString();
            completeUrl += "/" + textVersion.replace(" ", "_");
        }
        completeUrl += "?" + API.ZERO_CONTEXT + API.ZERO_COMMENTARY;
        Log.d("Node",completeUrl);
        return API.getDataFromURL(completeUrl,timeoutType);
    }

    private List<Text> getTextsFromAPI() throws API.APIException{ //(String booktitle, int []levels)
        String data = getTextFromAPIData();
        List<Text> textList = new ArrayList<>();
        if(data.length()==0)
            return textList;

        int [] levels = getLevels();
        int bid = getBid();
        try {
            JSONObject jsonData = new JSONObject(data);
            //Log.d("api", "jsonData:" + jsonData.toString());

            //TODO make work for 1 and 3 (or more) levels of depth (exs. Hadran, Arbaah Turim)
            JSONArray textArrayBig = jsonData.getJSONArray("text");
            JSONArray heArrayBig = jsonData.getJSONArray("he");
            int stop = Math.max(textArrayBig.length(), heArrayBig.length());

            int startLevel1 = levels[0];
            if(startLevel1 == 0)
                startLevel1 = 1;

            for (int k = 0; k < stop; k++) {
                JSONArray textArray;
                JSONArray heArray;
                try {
                    textArray = textArrayBig.getJSONArray(k);
                    heArray = heArrayBig.getJSONArray(k);
                } catch (JSONException e1) {
                    //Log.d("Node","didn't find sub arrays in text");
                    textArray = textArrayBig;
                    heArray = heArrayBig;
                    stop = 0;
                }

                int maxLength = Math.max(textArray.length(), heArray.length());
                //Log.d("api",textArray.toString() + " " + heArray.toString());
                for (int i = 0; i < maxLength; i++) {
                    //get the texts if i is less it's within the length (otherwise use "")
                    String enText = "";
                    try {
                        enText = textArray.getString(i);
                    } catch (JSONException e) {
                        Log.d("api", e.toString());
                        //GoogleTracker.sendException(e,);
                    }
                    String heText = "";
                    try {
                        heText = heArray.getString(i);
                    } catch (JSONException e) {
                        Log.d("api", e.toString());
                    }
                    Text text = new Text(enText,heText,bid,null);
                    text.parentNode = this;
                    for (int j = 0; j < levels.length; j++) {
                        text.levels[j] = levels[j]; //TODO get full level info in there
                    }
                    //only do it at the 2nd level, but currently this can only handle at this level, but can't handle 3 levels of depth in a ref.
                    text.levels[1] += k;
                    text.levels[0] = i + startLevel1;

                    textList.add(text);
                }
                startLevel1 = 1;
            }
        }catch(JSONException e){
            e.printStackTrace();
            Log.e("api", "error processing json data");
            GoogleTracker.sendException(e, "getTextsFromAPI: proc data");
        }


        return textList;
    }

    /**
     * Get texts for all types of texts
     * @return textList
     * @throws API.APIException
     */
    public List<Text> getTexts() throws API.APIException{
        return getTexts(false);
    }

    /**
     *  Get texts
     *
     * @return textList
     * @throws API.APIException
     */
    public List<Text> getTexts(boolean ignoreUsingAPI) throws API.APIException{
        //Log.d("Node","getTexts called");
        if(textList != null && (nid == NID_DUMMY || gotTextListInAPI == Settings.getUseAPI()|| ignoreUsingAPI)){
            Log.d("Node","text list not null: " + ignoreUsingAPI + "...." + (nid == NID_DUMMY || gotTextListInAPI == Settings.getUseAPI()));
            return textList;
        }
        Log.d("Node","found no textList");
        if(Downloader.getNetworkStatus() == Downloader.ConnectionType.NONE) {
            setTextVersion(null); //don't use alt text version if there's no internet
            //TODO set the SuperTextAct.lastLoadedNode = null if possible
        }

        if(!isTextSection){
            Log.e("Node", "getTexts() was called when it's not a textSection!" + this);
            //Integer.valueOf("aadfa");
            textList = new ArrayList<>();
        }else if(Settings.getUseAPI() || (getTextVersion() != null)) {
            textList = getTextsFromAPI();
        }else if(!isComplex && !isGridItem){
            Log.e("Node", "It thinks (!isComplex() && !isGridItem())... I don't know how.");
            textList = new ArrayList<>();
        }else if(!isComplex && isGridItem && !isRef){
            textList =  Text.getFromDB(bid,getLevels(),0);
        }else if(isRef()){
            if(!isComplex){
                Log.e("Node", "It thinks (!isComplex && isRef)... I don't know how.");
                textList = new ArrayList<>();
            }
            else if(startTid>0 && endTid >0) {
                textList = Text.getWithTids(startTid, endTid);
            }
            else{
                Log.e("Node.getTexts", "My start and end TIDs are no good for trying to get ref. TID:" + startTid + "-" + endTid + " ref:" + extraTidsRef);
                textList = new ArrayList<>();
            }
        }else if(isComplex){
            //levels will be diff based on if it's a gridItem
            if(isGridItem)
                textList = Text.getFromDB(bid, getLevels(), getNodeInDBParentNID());
            else
                textList = Text.getFromDB(bid, getLevels(), nid);
        }
        else{
            textList = new ArrayList<>();
            Log.e("Node", "In Node.getText() and I'm confused. NodeTypeFlags: " + getNodeTypeFlagsStr());
        }
        //Log.d("Node", "finishing getTexts algo. textList.size():" + textList.size());
        for(Text text:textList){
            text.parentNode = this;
        }
        gotTextListInAPI = Settings.getUseAPI();
        return textList;
    }

    public boolean isDaf(){
        try {
            return (sectionNames[sectionNames.length - 1].equals("Daf"));
        }catch (Exception e){
            return false;
        }
    }

     /**
     * gets the NID of a parent that is actually in the database. So if it's 3 levels of text, it will be using a real nid
     * @return ancestorNID
     */
    private int getNodeInDBParentNID(){
        Node parent = this.parent;
        while(parent.nid < 0) {
            parent = parent.parent;
        }
        return parent.nid;
    }
    /**
     * create an array describing the levels:
     * ex. chap 4 and verse 7 would be {7,3}
     * return levels
     */
    public int [] getLevels(){
        //TODO make work for more than 2 levels

        List<Integer> levels = new ArrayList<>();

        if(isRef){
            String [] strArray = Util.str2strArray(startLevels.replace(" ",""));
            for(String level: strArray){
                Log.d("Node", "start.level: " + level);
                levels.add(Integer.valueOf(level));
            }
        }else {
            levels.add(0);
            if (isGridItem) {
                levels.add(gridNum);
            }
            Node parent = this.parent;
            while (parent.nid == NID_CHAP_NO_NID) {
                levels.add(parent.gridNum);
                parent = parent.parent;
            }
        }
        int [] levels2 = new int [levels.size()];
        for(int i=0;i<levels.size();i++){
            levels2[i] = levels.get(i);
        }
        return levels2;
    }

    /**
     * This is the version number, so that we can change the schema, and it won't break the app, but it will just know it's a diff version.
     */
    final static private String PATH_DEFINING_NODE_VERSION = "a";
    public String makePathDefiningNode(){
        String str = "";
        if(nid == NID_DUMMY)
            return str;
        Node node = this;
        while(node.parent != null){
            int index =  node.parent.getChildren().indexOf(node);
            if(index < 0) {
                Log.e("Node", "makeStringDefiningTreeAndNode: index is -1. node:" + node);
                return "";
            }
            str = index + "." + str;
            node = node.parent;
        }
        int index =  node.tocRootsNum;
        if(index < 0) {
            Log.e("Node", "makeStringDefiningTreeAndNode: tocRootsNum is <0 node:" + node);
            return "";
        }
        str = index + "." + str;
        str = PATH_DEFINING_NODE_VERSION + str;
        return str;
    }


    /**
     *
     * @param book
     * @param path
     * @return
     * @throws InvalidPathException
     */
    static public Node getNodeFromPathStr(Book book, String path) throws InvalidPathException, API.APIException {
        return getNodeFromPathStr(getRoots(book),path);
    }



    static public Node getNodeFromPathStr(List<Node> tocRoots, String path) throws InvalidPathException {
        Node node;
        try {
            if(path.length() == 0 || path.indexOf(PATH_DEFINING_NODE_VERSION) != 0){
                throw (new Node()).new InvalidPathException();
            }
            path = path.replaceFirst(PATH_DEFINING_NODE_VERSION,"");
            String [] nums = path.split("\\.");
            if(nums.length == 0) {
                throw (new Node()).new InvalidPathException();
            }

            node = tocRoots.get(Integer.valueOf(nums[0]));
            for(int i=1;i<nums.length;i++) { //starts at 1 b/c it already used 0 for root
                String num = nums[i];
                if(num.length() > 0)
                    node = node.getChildren().get(Integer.valueOf(num));
            }
        }catch(Exception e){
            throw (new Node()).new InvalidPathException();
        }
        return node;
    }

    public class InvalidPathException extends Exception{
        public InvalidPathException(){
            super();
        }
        private static final long serialVersionUID = 1L;
    }

    public static List<Node> getRoots(Book book) throws API.APIException{
        List<Node> allRoots = allSavedBookTOCroots.get(book.title);
        if(allRoots != null){
            return allRoots;
        }

        String textVersion = Settings.BookSettings.getTextVersion(book);

        allRoots = new ArrayList<>();
        SQLiteDatabase db = Database.getDB();
        Cursor cursor = db.query(NODE_TABLE, null, "bid" + "=?",
                new String[]{String.valueOf(book.bid)}, null, null, "structNum,_id", null); //structNum, parentNode, siblingNum
        Node root;

        int lastStructNum = -1;
        List<List<Node>> allNodeStructs = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()){
            do {
                Node node = new Node(cursor);
                //node.log();
                if(lastStructNum != node.structNum){
                    allNodeStructs.add(new ArrayList<Node>());
                    lastStructNum = node.structNum;
                    //Log.d("Node", "On structNum" + node.structNum);
                }
                allNodeStructs.get(allNodeStructs.size()-1).add(node);
                //nodes.add(node);
            } while (cursor.moveToNext());
        }

        /**
         * this is for the rigid structure
         */
        root = new Node(book);
        root.nid = NID_NON_COMPLEX;
        root.setAllChapsDB(false);
        if(!root.isComplex && Settings.getUseAPI())
            root.setAllChaps_API();

        if(root.getChildren().size()>0) {
            root.tocRootsNum = allRoots.size();
            allRoots.add(root);
            //showTree(root);
        }

        for(int i=0;i<allNodeStructs.size();i++) {
            List<Node> nodes = allNodeStructs.get(i);
            if (nodes.size() > 0) {
                //Complex text combining nodes into root
                root = convertToTree(nodes);
            }
            root.tocRootsNum = allRoots.size();
            allRoots.add(root);
        }


        allSavedBookTOCroots.put(book.title, allRoots);
        //for(Node tempRoot:allRoots)  showTree(tempRoot);//for debugging only
        return allRoots;
    }

    public String getTabName(Util.Lang lang){
        String name = "";
        Node root = getAncestorRoot();
        if (!isComplex || isRef) {
            try {
                if (lang == Util.Lang.HE) {
                    name = root.heSectionNames[root.heSectionNames.length - 1];
                } else { //use EN for BI and EN
                    name = root.sectionNames[root.sectionNames.length - 1];
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (!name.equals("")) return name;
        else return getTitle(lang);
    }

    private String getNodeTypeFlagsStr(){
        String str = "";
        if(isComplex)
            str += " IS_COMPLX";
        if(isGridItem)
            str += " IS_GRID";
        if(isTextSection)
            str += " IS_TEXT";
        if(isRef)
            str += " IS_REF";
        return str;
    }

    public boolean pseudoEquals(Node node){
        if(nid > 0 || node.nid > 0){
            return (node.nid == nid);
        }
        else{
            return equals(node);
        }
    }

    @Override
    public String toString() {
        String str;
        try {
            str = "{" + nid + ",bid:" + bid + ",titles:" + enTitle + " " + heTitle + ",sections:" + Util.array2str(sectionNames) + "," + Util.array2str(heSectionNames) + ",structN:" + structNum + ",textD:" + textDepth + ",tids:" + startTid + "-" + endTid + ",ref:" + extraTidsRef;
            str += ", child.len:" + getChildren().size();
            str += ",gridN:" + getNiceGridNum(Util.Lang.EN);
            //str +=  ",siblingN:" + siblingNum;
            str += getNodeTypeFlagsStr();
            str += "}";
        }catch (Exception e){
            str = "{node (problem getting string): " + nid +  ",bid:" + bid + ",titles:" + enTitle + " " + heTitle  + "}";
        }
        return str;
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
            return new Node[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bid);
        dest.writeString(makePathDefiningNode());
    }

    private Node(Parcel in) {
        bid = in.readInt();
        String nodePath = in.readString();
        try {
            Node node =  Node.getNodeFromPathStr(new Book(bid),nodePath);
            //TODO make this work correctly.
            //this = node;
        } catch (InvalidPathException e) {

        } catch (API.APIException e) {

        }
    }*/

}
