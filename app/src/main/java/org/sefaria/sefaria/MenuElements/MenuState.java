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
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
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

    private static final String[] START_SECTION_RIGHT_AWAY = {"Commentary", "Targum"};

    private static final String[] PRE_DB_262_DONT_DISPLAY = {"Commentary", "Targum"};
    private static final int PRE_DB_262_DONT_DISPLAY_NUM = 263;
    /**
     * sections that we don't care how many levels it has left to leaf, but want to display on main page right away
     */

    public static final String jsonIndexFileName = "index.json";
    public static final String searchIndexFileName = "search-filter-index.json";

    private static MenuNode rootNode;
    private static MenuNode rootSearchNode;
    private MenuNode currNode;
    private List<MenuNode> currPath;
    private Util.Lang currLang;
    private IndexType type;

    public enum IndexType {
        MAIN, SEARCH
    }

    public MenuState(IndexType type) {
        if (!isMenuInited(type)) initMenu(type);
        this.type = type;
        if (type == IndexType.MAIN) {
            currNode = rootNode;
            currPath = new ArrayList<>();
            currPath.add(rootNode);
        } else {
            currNode = rootSearchNode;
            currPath = new ArrayList<>();
            currPath.add(rootSearchNode);
        }
    }

    public static MenuNode getRootNode(IndexType type){
        if (!isMenuInited(type)) initMenu(type);
        if (type == IndexType.MAIN) {
            return rootNode;
        } else {
            return rootSearchNode;
        }
    }

    public MenuState(IndexType type, List<MenuNode> currPath, Util.Lang lang) {
        this.currNode = currPath.get(currPath.size()-1);
        this.currPath = currPath;
        this.currLang = lang;
        this.type = type;
        //Log.d("menu","CURRNODECHLIDREN = " + currNode.getNumChildren());
    }

    private static boolean isMenuInited(IndexType type) {
        if (type == IndexType.MAIN)
            return rootNode != null;
        else
            return rootSearchNode != null;
    }

    private static JSONArray getMenuJSON(IndexType type) throws IOException, JSONException {
        JSONArray jsonRoot;
        String tempIndexName = type == IndexType.MAIN ? jsonIndexFileName : searchIndexFileName;
        if(!Settings.getUseAPI()){
            try {
                jsonRoot = new JSONArray(Util.readFile(Database.getInternalFolder() + tempIndexName));
            }catch (Exception e1){
                e1.printStackTrace();
                jsonRoot = Util.openJSONArrayFromAssets(tempIndexName);
            }
        }else{
            jsonRoot = Util.openJSONArrayFromAssets(tempIndexName);
        }
        return jsonRoot;
    }

    private static void initMenu(IndexType type) {
        try {
            JSONArray jsonRoot = getMenuJSON(type);
            createChildrenNodes(type, jsonRoot, null, true);
        } catch (IOException e) {
            e.printStackTrace();
            //Log.d("IO", "JSON not loaded");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void createChildrenNodes(IndexType type, JSONArray node, MenuNode parent, boolean isRoot) throws JSONException {

        //make sure to save the root
        if (isRoot) {
            if (type == IndexType.MAIN) {
                rootNode = new MenuNode();
                parent = rootNode;
            } else {
                rootSearchNode = new MenuNode();
                parent = rootSearchNode;
            }
        }

        for (int i = 0; i < node.length(); i++) {
            String enTitle;
            String heTitle;
            MenuNode tempMenuNode;
            JSONObject tempNode = node.getJSONObject(i);
            try {
                JSONArray tempChildNode = tempNode.getJSONArray("contents");
                enTitle = tempNode.getString("category");
                //if(enTitle.equals("Commentary2"))
                //    continue;
                heTitle = tempNode.getString("heCategory");
                tempMenuNode = new MenuNode(enTitle, heTitle, parent);
                createChildrenNodes(type, tempChildNode, tempMenuNode, false);
            }catch (JSONException e){//This means it didn't find contents and it's at a book
                enTitle = tempNode.getString("title");
                heTitle = tempNode.getString("heTitle");
                new MenuNode(enTitle, heTitle, parent);
            }
        }

        if (isRoot) {
            if (type == IndexType.MAIN) {
                MenuNode tosefta = (MenuNode) rootNode.getChildren().remove(rootNode.getChildIndex("Tosefta", Util.Lang.EN));
                rootNode.getChildren().add(rootNode.getChildIndex("Philosophy", Util.Lang.EN) + 1, tosefta);
            } else {
                MenuNode tosefta = (MenuNode) rootSearchNode.getChildren().remove(rootSearchNode.getChildIndex("Tosefta", Util.Lang.EN));
                rootSearchNode.getChildren().add(rootSearchNode.getChildIndex("Philosophy", Util.Lang.EN) + 1, tosefta);
            }
        }
    }

    public static String[] translatePath(IndexType type, String[] enPath) {
        String[] hePath = new String[enPath.length];
        BilingualNode currNode = getRootNode(type);

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
        MenuNode realNode;
        if (ind == -1) {
            tempChildren = tempChildren.get(0).getChildren();
            ind = tempChildren.indexOf(tempNode);
            realNode = (MenuNode) tempChildren.get(ind);
        } else {
            realNode = (MenuNode) tempChildren.get(ind);
        }

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.add(realNode);


        MenuState tempMenuState = new MenuState(this.type, tempCurrPath,currLang);


        if (sectionNode != null) return tempMenuState.goForward(node,null);
        else return tempMenuState;
    }

    public MenuState goBack(boolean hasSectionBack, boolean hasTabBack) {

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.remove(tempCurrPath.size() - 1);

        MenuState tempMenuState = new MenuState(this.type, tempCurrPath,currLang);

        if (hasSectionBack) return tempMenuState.goBack(false, false); //go back twice to account for clicking on subsection
        if (hasTabBack) return tempMenuState.goBack(false, false); //potentially go back thrice if tabs

        return tempMenuState;
    }

    //a bit confusing. the difference between this function and the property in MenuGrid 'hasTabs'
    //is that this can tell if the currNode has tabs. Later on, however, when you goForward into the tab,
    //it will be impossible to tell, so that state is save in the property 'hasTabs'
    public boolean hasTabs() {
        return Arrays.asList(TAB_GRID_PAGE_LIST).contains(currNode.getTitle(Util.Lang.EN));
    }


    private List<BilingualNode> sectionlessGroup = null;
    private void addSectionlessNode(MenuNode tempSubChild, List<BilingualNode> sectionList, List<List<BilingualNode>> subsectionList){
        if(sectionlessGroup == null){
            sectionlessGroup = new ArrayList<>();
            sectionList.add(null);
            subsectionList.add(sectionlessGroup);
        }
        sectionlessGroup.add(tempSubChild);
    }

    public class SectionAndSub{
        List<BilingualNode> sections = new ArrayList<>();
        List<List<BilingualNode>> subsections = new ArrayList<>();
        public SectionAndSub(){
        }
    }

    //TODO currently nonsections are only books. probs want to expand that to anything else
    public SectionAndSub getPageSections() {
        SectionAndSub sectionAndSub = new SectionAndSub();
        final List<String> START_SECTION_RIGHT_AWAY_LIST = Arrays.asList(START_SECTION_RIGHT_AWAY);
        final List<String> PRE_START_SECTION_RIGHT_AWAY_LIST = Arrays.asList(PRE_DB_262_DONT_DISPLAY);
        boolean isHome = currNode.equals(rootNode);
        for (int i = 0; i < currNode.getNumChildren(); i++) {
            MenuNode tempChild = (MenuNode) currNode.getChild(i);
            if(Database.getVersionInDB(false) < PRE_DB_262_DONT_DISPLAY_NUM && PRE_START_SECTION_RIGHT_AWAY_LIST.contains(tempChild.getTitle(Util.Lang.EN))){
                //don't display these guys
                continue;
            }
            int minDepth = tempChild.getMinDepthToLeaf();
            // with minDepth == 2 (like Mishneh Torah) we want to display it on it's own page b/c
            // that would look nice. If it's in START_SECTION_RIGHT_AWAY_LIST, then just put it there right away
            if (minDepth >= 1 && !isHome &&
                (
                    minDepth != 2 ||
                    START_SECTION_RIGHT_AWAY_LIST.contains(tempChild.getTitle(Util.Lang.EN))
                )
            ){
                if(tempChild.getNumChildren() == 1 && tempChild.getChild(0).getNumChildren() == 0
                        && tempChild.getChild(0).getTitle(Util.Lang.EN).startsWith(tempChild.getTitle(Util.Lang.EN))) {
                    //check if any of the children has only one child which is a leaf
                    // and the leaf starts with the parents' name (so it's just a repeat).
                    // In that case, put the leaf in instead of the actual child
                    MenuNode tempSubChild = (MenuNode) tempChild.getChild(0);
                    tempSubChild.overridePrettyTitle(true);
                    addSectionlessNode(tempSubChild, sectionAndSub.sections, sectionAndSub.subsections);
                }else {
                    //check if any of the children has only one child which is a leaf. in that case, put the leaf in instead of the actual child
                    List<BilingualNode> tempSubsection = new ArrayList<>();
                    for (BilingualNode child : tempChild.getChildren()) {
                        if (child.getNumChildren() == 1 && child.getChild(0).getNumChildren() == 0) {
                            MenuNode tempSubChild = (MenuNode) child.getChild(0);
                            tempSubChild.overridePrettyTitle(true);
                            tempSubsection.add(tempSubChild);
                        } else {
                            tempSubsection.add(child);
                        }
                    }
                    sectionAndSub.subsections.add(tempSubsection);
                    sectionAndSub.sections.add(tempChild);
                    sectionlessGroup = null;
                }
            } else {
                addSectionlessNode(tempChild, sectionAndSub.sections, sectionAndSub.subsections);
            }
        }
        return sectionAndSub;
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
        dest.writeString(type.name());
    }

    private MenuState(Parcel in) {
        List<MenuNode> tempPath = new ArrayList<>();


        in.readTypedList(tempPath, MenuNode.CREATOR);
        type = IndexType.valueOf(in.readString());
        if (!isMenuInited(type)) initMenu(type);
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
