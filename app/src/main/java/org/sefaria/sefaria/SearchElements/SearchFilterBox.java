package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.SearchAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterBox extends LinearLayout{

    private Context context;
    private boolean isOpen;
    private LinearLayout filterLists;

    public SearchFilterBox(Context context) {
        super(context);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.search_filter_box,this);
        this.context = context;
        this.filterLists = (LinearLayout) findViewById(R.id.filterLists);

        setIsOpen(false);


    }

    public void initFilters(JSONArray jsonArray) {
        try {
            SearchFilterNode root = new SearchFilterNode();
            Map<String, SearchFilterNode> nodeMap = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String filterPath = jsonObject.getString("key");
                String[] filterStringList = filterPath.split("/");
                int count = jsonObject.getInt("doc_count");

                int j = 0;
                boolean foundCommentary = false;
                for (String filterString : filterStringList) {
                    if (foundCommentary) {
                        j++;
                        foundCommentary = false;
                        continue;
                    }
                    if ((filterString.equals("Commentary") || filterString.equals("Commentary2")) && j == 0) {
                        filterString = filterStringList[1] + " Commentary";
                        filterStringList[1] = filterString; //so that it will recognize "tanach commentary" as the parent in the next iteration
                        foundCommentary = true;
                    }
                    SearchFilterNode searchFilterNode;
                    if (nodeMap.containsKey(filterString))
                        searchFilterNode = nodeMap.get(filterString);
                    else {
                        SearchFilterNode parent;
                        if (j == 0)
                            parent = root;
                        else
                            parent = nodeMap.get(filterStringList[j-1]);
                        searchFilterNode = new SearchFilterNode(filterString,filterString + " HE",parent);
                    }
                    searchFilterNode.addCount(count);
                    nodeMap.put(filterString,searchFilterNode);
                    j++;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
        if (isOpen) {
            filterLists.setVisibility(View.VISIBLE);
        } else {
            filterLists.setVisibility(View.GONE);
        }
    }


}
