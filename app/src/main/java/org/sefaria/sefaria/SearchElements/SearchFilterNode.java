package org.sefaria.sefaria.SearchElements;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterNode extends BilingualNode implements Comparable<SearchFilterNode> {

    private int count;
    private int bid;
    private int menuIndex;

    public SearchFilterNode() {
        super();
        count = 0;
    }

    public SearchFilterNode(String enTitle, String heTitle, SearchFilterNode parent) {
        super(enTitle,heTitle,parent);
        count = 0;
        try {
            bid = Book.getBid(enTitle);
        } catch (Book.BookNotFoundException e) {
            bid = -1;
        }
        if (bid == -1) {
            menuIndex = MenuState.getRootNode().indexOfTitle(enTitle, Util.Lang.EN);
            if (enTitle.contains("Commentary"))
                menuIndex = MenuState.getRootNode().indexOfTitle(enTitle.split(" ")[0], Util.Lang.EN);
            else
                menuIndex = MenuState.getRootNode().indexOfTitle(enTitle, Util.Lang.EN);
        }
        //Log.d("yosup","Title " + enTitle + " Bid " + bid);

    }

    public SearchFilterNode(Parcel in) {
        super(in);
        count = 0;
    }

    public static final Parcelable.Creator<SearchFilterNode> CREATOR
            = new Parcelable.Creator<SearchFilterNode>() {
        public SearchFilterNode createFromParcel(Parcel in) {
            return new SearchFilterNode(in);
        }

        public SearchFilterNode[] newArray(int size) {
            return new SearchFilterNode[size];
        }
    };

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public void addCount(int count) { this.count += count; }

    public String getFilterString() {
        String filterString = "";

        BilingualNode currNode = this;
        Stack<BilingualNode> nodeStack = new Stack<>();
        while (currNode.getParent() != null) {
            nodeStack.push(currNode);
            currNode = currNode.getParent();
        }

        boolean isFirst = true;
        while (!nodeStack.isEmpty()) {
            String title = nodeStack.pop().getTitle(Util.Lang.EN);

            //change "Tanakh Commentary" to "Commentary/Tanakh"
            if (isFirst && title.contains("Commentary")) {
                String[] titleArray = title.split(" ");
                title = titleArray[1] + "/" + titleArray[0];
            }
            filterString += title;
            if (!nodeStack.isEmpty()) filterString += "/";

            isFirst = false;
        }

        return filterString;
    }

    //Returns a list of all the leaves from this node
    public List<BilingualNode> getLeaves() {
        List<BilingualNode> leaves = new ArrayList<>();

        // BFS uses Queue data structure
        Queue queue = new LinkedList();
        queue.add(this);
        while(!queue.isEmpty()) {
            BilingualNode node = (BilingualNode)queue.remove();

            for (BilingualNode child : node.getChildren()) {
                if (child.getNumChildren() == 0)
                    leaves.add(child);
                queue.add(child);
            }
        }

        return leaves;
    }


    @Override
    public int compareTo(@Nullable SearchFilterNode another) {
        if (another == null) return -1;
        int order = this.bid - another.bid;
        if (this.bid == 0 && another.bid != 0) {
            order = 1;
        } else if (this.bid != 0 && another.bid == 0) {
            order = -1;
        } else if (order == 0) {
            //probably means these are root-level menu items.
            if (this.enTitle.contains(another.enTitle) && this.enTitle.contains("Commentary"))
                order = 1;
            else if (another.enTitle.contains(this.enTitle) && another.enTitle.contains("Commentary"))
                order = -1;
            else if (this.menuIndex == -1 && another.menuIndex != -1) { //for those weird cases that aren't books and aren't in the menu
                order = 1;
            } else if (this.menuIndex != -1 && another.menuIndex == -1) {
                order = -1;
            } else {
                order = this.menuIndex - another.menuIndex;
            }
        }

        return order;
    }

    /*
        PRINTING - (note that this code should probably be in BilingualNode, but there are specific things to SearchFilterNode here)
         */
    public String printTree() {
        int indent = 0;
        StringBuilder sb = new StringBuilder();
        printTree(this, indent, sb);
        return sb.toString();
    }

    private void printTree(SearchFilterNode node, int indent, StringBuilder sb) {
        sb.append(getIndentString(indent));
        sb.append("+--");
        int count = node.getCount();
        sb.append(node.getTitle(Util.Lang.EN) + "(" + count + ")");
        sb.append("/");
        sb.append("\n");
        for (BilingualNode childNode : node.getChildren()) {

            if (childNode.getNumChildren() > 0) {
                printTree((SearchFilterNode)childNode, indent + 1, sb);
            } else {
                printNode((SearchFilterNode)childNode, indent + 1, sb);
            }

        }
    }

    private void printNode(SearchFilterNode node, int indent, StringBuilder sb) {
        sb.append(getIndentString(indent));
        sb.append("+--");
        sb.append(node.getTitle(Util.Lang.EN) + "(" + node.getCount() + ")");
        sb.append("\n");
    }

    private String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("|  ");
        }
        return sb.toString();
    }

    /**
     *
     * @param root - root of current tree. this will be the root of the final tree
     * @param mergeNodes - list of nodes that you want to merge into root. NOTE, you need to generate all parent of these nodes, assuming they don't already exist
     */
    public static void mergeTrees(SearchFilterNode root, List<BilingualNode> mergeNodes) {
        for (BilingualNode tempNode : mergeNodes) {
            Stack<BilingualNode> path = new Stack<>();
            while(tempNode.getParent() != null) {
                path.add(tempNode);
                tempNode = tempNode.getParent();
            }

            SearchFilterNode currNode1 = root;
            if(!path.isEmpty()) {
                SearchFilterNode currNode2 = (SearchFilterNode) path.pop();

                int index = currNode1.getChildren().indexOf(currNode2);
                if (index == -1) {
                    currNode1.addChild(currNode2);
                    //although you selected this filter, it doesn't exist in the new results, so set count to 0
                    currNode2.removeChildren();
                    currNode2.setCount(0);
                } else {
                    currNode1.replaceChild(currNode1.getChild(index),currNode2);
                }
            }

        }
    }

}
