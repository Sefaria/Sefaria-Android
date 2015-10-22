package org.sefaria.sefaria.menu;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by nss on 9/8/15.
 */
public class MenuNode implements Parcelable {

    private static final String[] HOME_BUTTON_NAMES = {"Tanach","Mishnah","Talmud",
                                                    "Tosefta","Liturgy","Philosophy",
                                                    "Chasidut","Musar","Other",
                                                    "Halakhah","Midrash","Kabbalah",
                                                    "Responsa","Parshanut","Apocrypha","More >"};
    private static final int[] HOME_BUTTON_COLORS = {R.color.tanach, R.color.mishnah,R.color.talmud,
                                                    R.color.tosefta,R.color.liturgy,R.color.philosophy,
                                                    R.color.chasidut,R.color.musar,R.color.other,
                                                    R.color.halkhah,R.color.midrash,R.color.kabbalah,
                                                    R.color.responsa,R.color.parshanut,R.color.apocrypha,R.color.more};

    private String enTitle;
    private String heTitle;
    private List<MenuNode> children;
    private MenuNode parent;
    private int depth;
    private int color;

    //default constructor for root
    public MenuNode() {
        this("","",null,null);
        this.depth = 0;
    }

    public MenuNode(String enTitle, String heTitle, MenuNode parent, List<String> categories) {
        this.enTitle = enTitle;
        this.heTitle = heTitle;
        this.children = new ArrayList<>();
        if (parent != null) {
            this.parent = parent;
            parent.addChild(this);
            this.depth = parent.depth + 1;
        }

        //set color
        int colorInd = Arrays.asList(HOME_BUTTON_NAMES).indexOf(enTitle);
        if (colorInd != -1) this.color = HOME_BUTTON_COLORS[colorInd];
        else this.color = -1;
    }

    //as long as they have equal titles, they're equal.
    //this is to help avoid problems when comparing nodes that don't have parents/children
    @Override
    public boolean equals(Object o) {
        if (o instanceof MenuNode) {
            MenuNode mn = (MenuNode)o;
            if (mn.enTitle.equals(this.enTitle) && mn.heTitle.equals(this.heTitle)) return true;
        }
        return false;
    }

    public void addChild(MenuNode child) {
        children.add(child);
    }

    public String getTitle(int lang) {
        MenuNode tempNode = this;
        boolean foundTitleMatch = false;
        String currTitle;
        if (lang == Util.EN) currTitle = enTitle;
        else currTitle = heTitle;

        while (tempNode.parent != null && !foundTitleMatch) {
            String tempTitle;
            if (lang == Util.EN) tempTitle = tempNode.parent.enTitle;
            else tempTitle = tempNode.parent.heTitle;

            if (currTitle.contains(tempTitle)) {
                if (currTitle.contains(tempTitle + ", "))
                    tempTitle = tempTitle + ", ";
                foundTitleMatch = true;
                int start = currTitle.indexOf(tempTitle);
                int end = start + tempTitle.length();
                currTitle = currTitle.substring(0,start) + currTitle.substring(end);
            } else {
                tempNode = tempNode.parent;
            }

        }

        return currTitle;
    }

    //I'm assuming here the enTitle is always the same as the book title...
    public String getBookTitle() { return enTitle; }

    public MenuNode getParent() {
        return parent;
    }

    public int getNumChildren() {
        return children.size();
    }

    public List<MenuNode> getChildren() {
        return children;
    }

    public int getColor() { return color; }

    public int getTopLevelColor() {
        MenuNode topNode = getTopLevelNode();
        int colorInd = Arrays.asList(HOME_BUTTON_NAMES).indexOf(topNode.getTitle(Util.EN));
        if (colorInd == -1) return -1;
        else return HOME_BUTTON_COLORS[colorInd];
    }

    //return top level node for this node
    public MenuNode getTopLevelNode() {
        MenuNode tempNode = this;
        while(tempNode.getParent() != null) {
            MenuNode tempParent = tempNode.getParent();
            if (tempParent.getParent() == null)
                return tempNode;
            else
                tempNode = tempParent;

        }
        //should never get here, but just to make IDE happy
        return new MenuNode();
    }

    public MenuNode getChild(int pos) {
        return children.get(pos);
    }

    public int getDepth() {
        return depth;
    }

    public int getMinDepthToLeaf() {
        Queue<MenuNode> currNodes = new LinkedList<>();
        currNodes.addAll(children);
        int currDepth = 0;
        int numPopped = 0;
        int currSize = 0;
        boolean foundLeaf = false;
        while (currNodes.size() > 0 && !foundLeaf) {
            if (numPopped == currSize) {
                currDepth++;
                numPopped = 0;
                currSize = currNodes.size();
            }

            MenuNode currNode = currNodes.remove();
            if (currNode.getNumChildren() != 0)
                currNodes.addAll(currNode.children);
            else
                foundLeaf = true;
            numPopped++;


        }
        return currDepth;
    }

    public int getChildIndex(String title, int lang) {
        for (int i = 0; i < children.size(); i++) {
            MenuNode node = children.get(i);
            if (node.getTitle(lang).equals(title)) return i;
        }
        return -1;
    }

    public String[] getChildrenTitles(int lang) {
        String[] childrenTitles = new String[getNumChildren()];
        int count = 0;
        for (MenuNode child : children) {
            String childTitle = child.getTitle(lang);
            childrenTitles[count] = childTitle;
            count++;
        }

        return childrenTitles;
    }

    /*

    PARCELABLE

     */

    public static final Parcelable.Creator<MenuNode> CREATOR
            = new Parcelable.Creator<MenuNode>() {
        public MenuNode createFromParcel(Parcel in) {
            return new MenuNode(in);
        }

        public MenuNode[] newArray(int size) {
            return new MenuNode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    //Specifically not writing children so that there isn't a memory leak
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(enTitle);
        dest.writeString(heTitle);
        dest.writeInt(depth);
    }

    private MenuNode(Parcel in) {
        children = new ArrayList<MenuNode>();

        enTitle = in.readString();
        heTitle = in.readString();

        //parent = :(
        depth = in.readInt();
    }

    @Override
    public String toString() {
        return "EN: " + this.enTitle;
    }
}
