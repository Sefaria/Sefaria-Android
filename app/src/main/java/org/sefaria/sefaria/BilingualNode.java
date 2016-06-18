package org.sefaria.sefaria;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 6/17/16.
 * Meant to represent any bilingual tree-structures
 */
public class BilingualNode implements Parcelable {

    protected String enTitle;
    protected String heTitle;
    protected List<BilingualNode> children;
    protected BilingualNode parent;
    protected int depth;

    public BilingualNode() {
        this("","",null);
        this.depth = 0;
    }

    public BilingualNode(String enTitle, String heTitle, BilingualNode parent) {
        this.children = new ArrayList<>();
        if (parent != null) {
            this.parent = parent;
            parent.addChild(this);
            this.depth = parent.depth + 1;
        }
        this.enTitle = enTitle;
        this.heTitle = heTitle;
    }

    //as long as they have equal titles, they're equal.
    //this is to help avoid problems when comparing nodes that don't have parents/children
    @Override
    public boolean equals(Object o) {
        if (o instanceof BilingualNode) {
            BilingualNode bilingualNode = (BilingualNode) o;
            if (bilingualNode.enTitle.equals(this.enTitle) && bilingualNode.heTitle.equals(this.heTitle)) return true;
        }
        return false;
    }

    public String getTitle(Util.Lang lang) {
        String currTitle;
        if (lang == Util.Lang.EN) currTitle = enTitle;
        else currTitle = heTitle;

        return currTitle;
    }

    public BilingualNode getParent() {
        return parent;
    }

    public int getNumChildren() {
        return children.size();
    }

    public List<BilingualNode> getChildren() {
        return children;
    }

    public void addChild(BilingualNode child) {
        children.add(child);
    }

    public BilingualNode getChild(int pos) {
        return children.get(pos);
    }

    public int getDepth() {
        return depth;
    }

    public int getChildIndex(String title, Util.Lang lang) {
        for (int i = 0; i < children.size(); i++) {
            BilingualNode node = children.get(i);
            if (node.getTitle(lang).equals(title)) return i;
        }
        return -1;
    }

    public String[] getChildrenTitles(Util.Lang lang) {
        String[] childrenTitles = new String[getNumChildren()];
        int count = 0;
        for (BilingualNode child : children) {
            String childTitle = child.getTitle(lang);
            childrenTitles[count] = childTitle;
            count++;
        }

        return childrenTitles;
    }

    /*

    PARCELABLE

     */

    public static final Parcelable.Creator<BilingualNode> CREATOR
            = new Parcelable.Creator<BilingualNode>() {
        public BilingualNode createFromParcel(Parcel in) {
            return new BilingualNode(in);
        }

        public BilingualNode[] newArray(int size) {
            return new BilingualNode[size];
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

    public BilingualNode(Parcel in) {
        children = new ArrayList<BilingualNode>();

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

