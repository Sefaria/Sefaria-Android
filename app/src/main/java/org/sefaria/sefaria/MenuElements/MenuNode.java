package org.sefaria.sefaria.MenuElements;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Created by nss on 9/8/15.
 */
public class MenuNode extends BilingualNode {

    private String enPrettyTitle;
    private String hePrettyTitle;
    private int color;
    private boolean isHomeButton;

    public MenuNode() {
        super();
    }

    public MenuNode(String enTitle, String heTitle, MenuNode parent) {
        super(enTitle,heTitle,parent);
        this.enPrettyTitle = makePrettyTitle(Util.Lang.EN);
        this.hePrettyTitle = makePrettyTitle(Util.Lang.HE);

        //set color
        this.color = MyApp.getCatColor(enTitle);
        this.isHomeButton = color != -1;
    }

    public static final Parcelable.Creator<MenuNode> CREATOR
            = new Parcelable.Creator<MenuNode>() {
        public MenuNode createFromParcel(Parcel in) {
            return new MenuNode(in);
        }
        public MenuNode[] newArray(int size) {
            return new MenuNode[size];
        }
    };

    public MenuNode(Parcel in) {
        super(in);
    }

    private String makePrettyTitle(Util.Lang lang) {
        MenuNode tempNode = this;
        boolean foundTitleMatch = false;
        String currTitle;
        if (lang == Util.Lang.EN) currTitle = enTitle;
        else currTitle = heTitle;

        if (currTitle.equals("Midrash Rabbah")) return currTitle;
        while (tempNode.parent != null && !foundTitleMatch) {
            String tempTitle;
            if (lang == Util.Lang.EN) tempTitle = ((MenuNode)tempNode.parent).enTitle;
            else tempTitle = ((MenuNode)tempNode.parent).heTitle;

            Pattern titlePattern = Pattern.compile("\\b" + tempTitle + "( |, )");
            if (tempTitle.length() > 0 && titlePattern.matcher(currTitle).find()) {
                foundTitleMatch = true;
                currTitle = titlePattern.matcher(currTitle).replaceAll("");
            } else {
                tempNode = (MenuNode)tempNode.parent;
            }

        }

        return currTitle;
    }

    public String getPrettyTitle(Util.Lang lang) {
        if (lang == Util.Lang.EN) return enPrettyTitle;
        else return hePrettyTitle;
    }

    //I'm assuming here the enTitle is always the same as the book title...
    public String getBookTitle() { return enTitle; }

    public boolean isHomeButton() {
        return isHomeButton;
    }

    public int getColor() { return color; }

    public int getTopLevelColor() {
        MenuNode topNode = getTopLevelNode();
        int colorInd = Arrays.asList(MyApp.CAT_NAMES).indexOf(topNode.getTitle(Util.Lang.EN));
        if (colorInd == -1) return -1;
        else return MyApp.CAT_COLORS[colorInd];
    }

    //return top level node for this node
    public MenuNode getTopLevelNode() {
        MenuNode tempNode = this;
        while(tempNode.getParent() != null) {
            MenuNode tempParent = (MenuNode) tempNode.getParent();
            if (tempParent.getParent() == null)
                return tempNode;
            else
                tempNode = tempParent;

        }
        //should never get here, but just to make IDE happy
        return new MenuNode();
    }

    public int getMinDepthToLeaf() {
        Queue<BilingualNode> currNodes = new LinkedList<>();
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

            MenuNode currNode = (MenuNode)currNodes.remove();
            if (currNode.getNumChildren() != 0)
                currNodes.addAll(currNode.children);
            else
                foundLeaf = true;
            numPopped++;


        }
        return currDepth;
    }
}
