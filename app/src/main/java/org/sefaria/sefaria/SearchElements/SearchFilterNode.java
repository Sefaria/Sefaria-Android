package org.sefaria.sefaria.SearchElements;

import android.os.Parcel;
import android.os.Parcelable;

import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterNode extends BilingualNode {

    private int count;

    public SearchFilterNode() {
        super();
        count = 0;
    }

    public SearchFilterNode(String enTitle, String heTitle, SearchFilterNode parent) {
        super(enTitle,heTitle,parent);
        count = 0;

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
        sb.append(node.getTitle(Util.Lang.EN) + "(" + node.getCount() + ")");
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


}
