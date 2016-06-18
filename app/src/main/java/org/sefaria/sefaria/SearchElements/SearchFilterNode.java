package org.sefaria.sefaria.SearchElements;

import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterNode extends MenuNode {

    private SearchFilterNode parent;
    private List<SearchFilterNode> children;

    public SearchFilterNode(String enTitle, String heTitle, SearchFilterNode parent) {
        super(enTitle,heTitle,parent);

    }


}
