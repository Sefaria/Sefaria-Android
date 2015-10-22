package org.sefaria.sefaria.menu;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Util;

import java.io.IOException;
import java.io.InputStream;
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
    private static final String jsonIndexFileName = "index.json";

    private static MenuNode rootNode;
    private MenuNode currNode;
    private List<MenuNode> currPath;
    private int currLang;

    public MenuState() {
        if (!isMenuInited()) initMenu();
        currNode = rootNode;
        currPath = new ArrayList<>();
        currPath.add(rootNode);
    }

    public MenuState(List<MenuNode> currPath) {
        this.currNode = currPath.get(currPath.size()-1);
        this.currPath = currPath;
        Log.d("menu","CURRNODECHLIDREN = " + currNode.getNumChildren());
    }

    private static boolean isMenuInited() {return rootNode != null;}

    private static void initMenu() {
        ObjectMapper om = new ObjectMapper(); //for jackson
        try {
            InputStream is = MyApp.getContext().getResources().getAssets().open("index.json");
            JsonNode jsonRoot = om.readTree(is);
            createChildrenNodes(jsonRoot, null, true);
        } catch (IOException e) {
            Log.d("IO", "JSON not loaded");
        }
    }

    private static void createChildrenNodes(JsonNode node, MenuNode parent, boolean isRoot) {

        //make sure to save the root
        if (isRoot) {
            rootNode = new MenuNode();
            parent = rootNode;
        }

        for (int i = 0; i < node.size(); i++) {
            //this is a book, so add its bid
            JsonNode tempNode = node.get(i);
            JsonNode tempChildNode = tempNode.findPath("contents");
            String enTitle;
            String heTitle;
            MenuNode tempMenuNode;
            if (tempChildNode.isMissingNode()) { //this is a book
                enTitle = tempNode.findPath("title").textValue();
                heTitle = tempNode.findPath("heTitle").textValue();
                List<String> categories = new ArrayList<>();
                //cant find a better way to extract arrays using jackson...
                JsonNode catNode = tempNode.findPath("categories");
                for (int j = 0; j < catNode.size(); j++) {
                    categories.add(catNode.get(j).textValue());
                }
                new MenuNode(enTitle, heTitle, parent,categories);

            } else {
                //recurse
                enTitle = tempNode.findPath("category").textValue();
                heTitle = tempNode.findPath("heCategory").textValue();
                tempMenuNode = new MenuNode(enTitle, heTitle, parent,null);
                createChildrenNodes(tempChildNode, tempMenuNode,false);
            }

        }

        if (isRoot) {
            MenuNode tosefta = rootNode.getChildren().remove(rootNode.getChildIndex("Tosefta",Util.EN));
            rootNode.getChildren().add(rootNode.getChildIndex("Philosophy",Util.EN)+1,tosefta);
        }
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
        List<MenuNode> tempChildren = currPath.get(currPath.size()-1).getChildren();
        int ind = tempChildren.indexOf(tempNode);
        MenuNode realNode = tempChildren.get(ind);

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.add(realNode);


        MenuState tempMenuState = new MenuState(tempCurrPath);


        if (sectionNode != null) return tempMenuState.goForward(node,null);
        else return tempMenuState;
    }

    public MenuState goBack(boolean hasSectionBack, boolean hasTabBack) {
        MenuNode tempParent = currNode.getParent();

        List<MenuNode> tempCurrPath = new ArrayList<>(currPath);
        tempCurrPath.remove(tempCurrPath.size() - 1);

        MenuState tempMenuState = new MenuState(tempCurrPath);

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
        return Arrays.asList(TAB_GRID_PAGE_LIST).contains(currNode.getTitle(Util.EN));
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
    public void getPageSections(List<MenuNode> sectionList, List<List<MenuNode>> subsectionList, List<MenuNode> sectionlessNodes) {
        boolean isHome = currNode.equals(rootNode);
        for (int i = 0; i < currNode.getNumChildren(); i++) {
            MenuNode tempChild = currNode.getChild(i);
            //commentary is not shown in the menu
            if (tempChild.getTitle(Util.EN).equals("Commentary")) continue;

            int minDepth = tempChild.getMinDepthToLeaf();
            if (minDepth >= 1 && minDepth != 2 && !isHome) {
                subsectionList.add(tempChild.getChildren());
                sectionList.add(tempChild);
            } else {
                sectionlessNodes.add(tempChild);
            }
        }

    }

    public int getLang() {
        return currLang;
    }

    public void setLang(int lang) {
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
            Log.d("menu","REBUILT NODE: " + daNode);
            tempMenuState = tempMenuState.goForward(daNode, null);
        }
        this.currPath = tempMenuState.currPath;
        this.currNode = tempMenuState.currNode;
    }



}
