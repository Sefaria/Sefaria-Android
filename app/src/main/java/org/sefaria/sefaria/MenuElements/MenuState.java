package org.sefaria.sefaria.MenuElements;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.UpdateService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by nss on 9/8/15.
 */
public class MenuState implements Parcelable {

    public static final int HOME_PAGE = 0;
    public static final int GRID_PAGE = 1; //normal grid page
    public static final int BROKEN_GRID_PAGE = 2; //when you go two levels deep
    public static final int TAB_GRID_PAGE = 3; //primarily used for talmud

    private static final String[] TAB_GRID_PAGE_LIST = {"Talmud"};
    private static final String[] PAGE_EXCEPTIONS = {"Mishneh Torah", "Shulchan Arukh", "Midrash Rabbah", "Maharal"};
    public static final String jsonIndexFileName = "index.json";

    private static MenuNode rootNode;
    private MenuNode currNode;
    private List<MenuNode> currPath;
    private Util.Lang currLang;

    public MenuState() {
        if (!isMenuInited()) initMenu();
        currNode = rootNode;
        currPath = new ArrayList<>();
        currPath.add(rootNode);
    }

    public static MenuNode getRootNode(){
        if (!isMenuInited()) initMenu();
        return rootNode;
    }

    public MenuState(List<MenuNode> currPath, Util.Lang lang) {
        this.currNode = currPath.get(currPath.size()-1);
        this.currPath = currPath;
        this.currLang = lang;
        //Log.d("menu","CURRNODECHLIDREN = " + currNode.getNumChildren());
    }

    private static boolean isMenuInited() {return rootNode != null;}

    private static JSONArray getMenuJSON() throws IOException, JSONException {
        JSONArray jsonRoot;
        if(!Settings.getUseAPI()){
            try {
                jsonRoot = new JSONArray(Util.readFile(Database.getDbPath(MyApp.getContext()) + MenuState.jsonIndexFileName));
            }catch (Exception e1){
                e1.printStackTrace();
                jsonRoot = Util.openJSONArrayFromAssets(jsonIndexFileName);
            }
        }else{
            jsonRoot = Util.openJSONArrayFromAssets(jsonIndexFileName);
        }
        return jsonRoot;
    }

    private static void initMenu() {
        try {
            JSONArray jsonRoot = getMenuJSON();
            createChildrenNodes(jsonRoot, null, true);
        } catch (IOException e) {
            e.printStackTrace();
            //Log.d("IO", "JSON not loaded");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void createChildrenNodes(JSONArray node, MenuNode parent, boolean isRoot) throws JSONException {

        //make sure to save the root
        if (isRoot) {
            rootNode = new MenuNode();
            parent = rootNode;
        }

        for (int i = 0; i < node.length(); i++) {
            //this is a book, so add its bid
            String enTitle;
            String heTitle;
            MenuNode tempMenuNode;
            JSONObject tempNode = node.getJSONObject(i);
            try {
                JSONArray tempChildNode = tempNode.getJSONArray("contents");
                enTitle = tempNode.getString("category");
                if(enTitle.equals("Commentary2"))
                    continue;
                heTitle = tempNode.getString("heCategory");
                tempMenuNode = new MenuNode(enTitle, heTitle, parent);
                createChildrenNodes(tempChildNode, tempMenuNode, false);
            }catch (JSONException e){//This means it didn't find contents and it's at a book
                enTitle = tempNode.getString("title");
                heTitle = tempNode.getString("heTitle");
                new MenuNode(enTitle, heTitle, parent);
            }
        }

        if (isRoot) {
            MenuNode tosefta = (MenuNode) rootNode.getChildren().remove(rootNode.getChildIndex("Tosefta",Util.Lang.EN));
            rootNode.getChildren().add(rootNode.getChildIndex("Philosophy",Util.Lang.EN)+1,tosefta);
        }
    }

    public static String[] translatePath(String[] enPath) {
        String[] hePath = new String[enPath.length];
        BilingualNode currNode = getRootNode();
        if (enPath[0].equals("Commentary") || enPath[0].equals("Commentary2")) {
            //if it's a commentary, hard code the first element, search for the second and query for the last. those are the only elements you actually need

            hePath[0] = "מפרשים";
            List<BilingualNode> children = currNode.getChildren();
            for (BilingualNode child : children) {
                if (child.getTitle(Util.Lang.EN).equals(enPath[1])) {
                    hePath[1] = child.getTitle(Util.Lang.HE);
                    break;
                }
            }
            try{
                SQLiteDatabase db = Database.getDB();
                Cursor cursor = db.query(Book.TABLE_BOOKS,new String[]{"heTitle"},"title=?",new String[]{enPath[enPath.length-1]},null,null,null);
                if (cursor != null) {

                    cursor.moveToFirst();
                    hePath[hePath.length-1] = cursor.getString(0);

                    hePath[hePath.length-1] = enPath[enPath.length-1];

                }
            }catch(Exception e){

            }
        } else {


            for (int i = 0; i < enPath.length; i++) {
                List<BilingualNode> children = currNode.getChildren();
                for (BilingualNode child : children) {
                    if (child.getTitle(Util.Lang.EN).equals(enPath[i])) {
                        hePath[i] = child.getTitle(Util.Lang.HE);
                        currNode = child;
                        break;
                    }
                }
            }
        }

        return hePath;
    }

    /***************
    MEMBER FUNCTIONS
     ***************/

    public MenuNode getCurrNode() {return currNode;}

    //optional parameter 'sectionNode' is used when user clicks on button in a section
    //technically, goForward needs to be run twice, once for the section and once for the button
    public MenuState goForward(MenuNode node, MenuNode sectionNode) {

        MenuNode tempNode;
        if (sectionNode != null) tempNode = sectionNode;
        else tempNode = node;

        //if coming from memory restore, currPath contains half-nodes, but you need full-node
        List<BilingualNode> tempChildren = currPath.get(currPath.size()-1).getChildren();
        int ind = tempChildren.indexOf(tempNode);
        MenuNode realNode = (MenuNode) tempChildren.get(ind);

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.add(realNode);


        MenuState tempMenuState = new MenuState(tempCurrPath,currLang);


        if (sectionNode != null) return tempMenuState.goForward(node,null);
        else return tempMenuState;
    }

    public MenuState goBack(boolean hasSectionBack, boolean hasTabBack) {

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.remove(tempCurrPath.size() - 1);

        MenuState tempMenuState = new MenuState(tempCurrPath,currLang);

        if (hasSectionBack) return tempMenuState.goBack(false, false); //go back twice to account for clicking on subsection
        if (hasTabBack) return tempMenuState.goBack(false, false); //potentially go back thrice if tabs

        return tempMenuState;
    }

    public void goHome() {
        currNode = rootNode;
        currPath = new ArrayList<>();
    }

    public int getPageType() {
        if (currNode == rootNode) return HOME_PAGE;
        else if ( isBrokenGrid()) return BROKEN_GRID_PAGE;
        else return GRID_PAGE;
    }

    public List<MenuNode> getCurrPath() { return currPath; }

    //a bit confusing. the difference between this function and the property in MenuGrid 'hasTabs'
    //is that this can tell if the currNode has tabs. Later on, however, when you goForward into the tab,
    //it will be impossible to tell, so that state is save in the property 'hasTabs'
    public boolean hasTabs() {
        return Arrays.asList(TAB_GRID_PAGE_LIST).contains(currNode.getTitle(Util.Lang.EN));
    }

    private boolean isBrokenGrid() {
        boolean isBrokenGrid = false;
        if (currNode.getNumChildren() > 0) {
            for (int i = 0; i < currNode.getNumChildren(); i++) {
                if (currNode.getChild(i).getNumChildren() > 0) {
                    isBrokenGrid = true;
                    break;
                }
            }
        } else {
            isBrokenGrid = false;
        }

        return isBrokenGrid;
    }

    //parameters are changed in-place and "returned"
    //TODO currently nonsections are only books. probs want to expand that to anything else
    public void getPageSections(List<BilingualNode> sectionList, List<List<BilingualNode>> subsectionList, List<BilingualNode> sectionlessNodes) {
        boolean isHome = currNode.equals(rootNode);
        for (int i = 0; i < currNode.getNumChildren(); i++) {
            MenuNode tempChild = (MenuNode) currNode.getChild(i);
            //commentary is not shown in the menu
            if (tempChild.getTitle(Util.Lang.EN).equals("Commentary")) continue;

            int minDepth = tempChild.getMinDepthToLeaf();
            if (minDepth >= 1 && minDepth != 2 && !isHome) {
                subsectionList.add(tempChild.getChildren());
                sectionList.add(tempChild);
            } else {
                sectionlessNodes.add(tempChild);
            }
        }

    }

    public Util.Lang getLang() {
        return currLang;
    }

    public void setLang(Util.Lang lang) {
        currLang = lang;
    }

    /*

    PARCELABLE

     */

    public static final Parcelable.Creator<MenuState> CREATOR
            = new Parcelable.Creator<MenuState>() {
        public MenuState createFromParcel(Parcel in) {
            return new MenuState(in);
        }

        public MenuState[] newArray(int size) {
            return new MenuState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(currPath);
    }

    private MenuState(Parcel in) {
        List<MenuNode> tempPath = new ArrayList<>();


        in.readTypedList(tempPath, MenuNode.CREATOR);
        if (!isMenuInited()) initMenu();
        currNode = rootNode;
        currPath = new ArrayList<>();
        currPath.add(rootNode);
        //recreate path starting from root

        MenuState tempMenuState = this;
        for (int i = 1; i < tempPath.size(); i++) {
            MenuNode daNode = tempPath.get(i);
            //Log.d("menu","REBUILT NODE: " + daNode);
            tempMenuState = tempMenuState.goForward(daNode, null);
        }
        this.currPath = tempMenuState.currPath;
        this.currNode = tempMenuState.currNode;
    }



}
