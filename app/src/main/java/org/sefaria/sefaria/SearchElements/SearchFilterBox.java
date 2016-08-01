package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SearchActivity;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.layouts.ListViewCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterBox extends LinearLayout{

    private SearchActivity searchActivity;
    private boolean isOpen;
    private LinearLayout filterLists;
    private ListView filterListMain;
    private ListView filterListSlave;
    private SearchFilterAdapter filterAdapterMain;
    private SearchFilterAdapter filterAdapterSlave;
    private SearchFilterNode root;

    private List<BilingualNode> minSelectedFilterNodes;
    private HashSet<BilingualNode> allSelectedFilterNodes;

    public SearchFilterBox(Context context) {
        super(context);
        inflate(context, R.layout.search_filter_box,this);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        inflate(context, R.layout.search_filter_box,this);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
        inflate(context, R.layout.search_filter_box,this);

        init(context);
    }

    private void init(Context context) {
        //Yes, I know this line is terrible. But I can't think of any other way to have the checkboxes communicate with SearchActivity
        this.searchActivity = (SearchActivity) context;
        this.filterLists = (LinearLayout) findViewById(R.id.filterLists);
        if (Settings.getMenuLang() == Util.Lang.EN) {
            this.filterListMain = (ListView) findViewById(R.id.filterListLeft);
            this.filterListSlave = (ListView) findViewById(R.id.filterListRight);
        } else /*if (Settings.getMenuLang() == Util.Lang.HE)*/ {
            this.filterListMain = (ListView) findViewById(R.id.filterListRight);
            this.filterListSlave = (ListView) findViewById(R.id.filterListLeft);
        }
        filterListMain.setDivider(null);
        filterListSlave.setDivider(null);

        filterListMain.setOnItemClickListener(filterListItemClick);


        filterAdapterSlave = new SearchFilterAdapter(context,R.layout.search_filter_item,new ArrayList<BilingualNode>(),checkedChangeListener,onCheckBoxClick,null);
        filterAdapterMain = new SearchFilterAdapter(context,R.layout.search_filter_item,new ArrayList<BilingualNode>(),checkedChangeListener,onCheckBoxClick,filterAdapterSlave);
        filterListMain.setAdapter(filterAdapterMain);
        filterListSlave.setAdapter(filterAdapterSlave);
        setIsOpen(false);

        findViewById(R.id.filterTitle).setOnClickListener(filterTitleClick);

        minSelectedFilterNodes = new ArrayList<>();
        allSelectedFilterNodes = new HashSet<>();


    }

    public void initFilters(JSONArray jsonArray) {
        try {
            root = new SearchFilterNode();
            Map<String, SearchFilterNode> nodeMap = new HashMap<>();

            int yo = 3;
            if (jsonArray.length() == 0) {
                yo += 4;
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String filterPath = jsonObject.getString("key");


                String[] filterStringList = filterPath.split("/");
                String[] filterStringListHe = MenuState.translatePath(filterStringList);
                String[] fullFilterStringList = getFullFilterStringList(filterStringList);
                //Log.d("Search", Arrays.toString(filterStringList) + " ==> " + Arrays.toString(filterStringListHe));
                int count = jsonObject.getInt("doc_count");

                int j = 0;
                boolean foundCommentary = false;

                for (String filterString : filterStringList) {
                    String filterStringHe = filterStringListHe[j];
                    String fullFilterString = fullFilterStringList[j];

                    if (foundCommentary) {
                        j++;
                        foundCommentary = false;
                        continue;
                    }
                    if ((filterString.equals("Commentary") || filterString.equals("Commentary2")) && j == 0) {
                        filterString = filterStringList[1] + " Commentary";
                        filterStringList[1] = filterString; //so that it will recognize "tanakh commentary" as the parent in the next iteration

                        /*if (filterStringListHe[1] == null) {
                            Log.d("Search",Arrays.toString(filterStringList));
                        }*/

                        filterStringHe = "מפרשי " + filterStringListHe[1];
                        filterStringListHe[1] = filterStringHe;
                        foundCommentary = true;

                        fullFilterString = filterString;
                        fullFilterStringList[1] = fullFilterString;
                    }
                    SearchFilterNode searchFilterNode;
                    if (nodeMap.containsKey(fullFilterString))
                        searchFilterNode = nodeMap.get(fullFilterString);
                    else {
                        SearchFilterNode parent;
                        if (j == 0)
                            parent = root;
                        else
                            parent = nodeMap.get(fullFilterStringList[j-1]);
                        searchFilterNode = new SearchFilterNode(filterString,filterStringHe,parent);

                        int minIndex = minSelectedFilterNodes.indexOf(searchFilterNode);
                        //make sure you replace old filter objects so that the hashcodes are the same. whatever, this makes sense...don't worry about it
                        if (minIndex != -1 && searchFilterNode.hashCode() != minSelectedFilterNodes.get(minIndex).hashCode()) {
                            minSelectedFilterNodes.remove(minIndex);
                            minSelectedFilterNodes.add(searchFilterNode);
                        }

                    }
                    searchFilterNode.addCount(count);
                    nodeMap.put(fullFilterString,searchFilterNode);
                    j++;
                }
            }
            //TODO NOTE that the below line is what makes it forget the previous filters. Also check out SearchActivity.runNewSearch()
            minSelectedFilterNodes = new ArrayList<>();
            this.setIsOpen(false);
            allSelectedFilterNodes = new HashSet<>();
            SearchFilterNode.mergeTrees(root,minSelectedFilterNodes);
            Log.d("YOYO","OLD FILTERS " + minSelectedFilterNodes.toString());
            filterAdapterMain.clearAndAdd(root.getChildren(),minSelectedFilterNodes);
            filterListMain.setSelection(0);

            filterAdapterSlave.clear();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //helper to initFilters()
    //returns an array of filterPath, split by "/" where each consecutive element is a full path in itself
    private String[] getFullFilterStringList(String[] filterList) {
        String[] fullFilterList = new String[filterList.length];
        String currPath = "";

        int count = 0;
        for (String filterString : filterList) {
            currPath += filterString;
            fullFilterList[count] = currPath;
            count++;
        }

        return fullFilterList;
    }

    private void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
        if (isOpen) {
            filterLists.setVisibility(View.VISIBLE);
        } else {
            filterLists.setVisibility(View.GONE);
        }
    }

    public List<String> getSelectedFilterStrings() {
        List<String> filterStrings = new ArrayList<>();
        for (BilingualNode node : minSelectedFilterNodes) {
            SearchFilterNode snode = (SearchFilterNode) node;
            filterStrings.add(snode.getFilterString());
        }
        return filterStrings;
    }

    private void updateSelectedFilterNodes(BilingualNode node, boolean isChecked) {
        if (node == null) return;

        if (isChecked) {
            allSelectedFilterNodes.add(node);
        } else {
            allSelectedFilterNodes.remove(node);
        }

        minSelectedFilterNodes = SearchAPI.getMinFilterNodes(new ArrayList<>(allSelectedFilterNodes));

        Log.d("YOYO","Min Nodes from Master = " + minSelectedFilterNodes.toString());
    }

    private void updateSelectedFilterNodes(List<BilingualNode> nodes, boolean isChecked) {
        if (nodes == null) return;

        if (isChecked) {
            allSelectedFilterNodes.addAll(nodes);
        } else {
            allSelectedFilterNodes.removeAll(nodes);
        }

        minSelectedFilterNodes = SearchAPI.getMinFilterNodes(new ArrayList<>(allSelectedFilterNodes));

        Log.d("YOYO","Min Nodes = " + minSelectedFilterNodes.toString());
        for (BilingualNode yo : minSelectedFilterNodes) {
            Log.d("YOYO","\tMin Node Children = " + yo.getChildren().toString() + " HASH " + yo.hashCode());
        }
    }

    OnClickListener filterTitleClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setIsOpen(!isOpen);
        }
    };

    AdapterView.OnItemClickListener filterListItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SearchFilterNode node = (SearchFilterNode) filterAdapterMain.getItem(position);
            filterAdapterSlave.clearAndAdd(node.getLeaves(),filterAdapterMain.getIsCheckedAtPos(position));
            filterListSlave.setSelection(0);
            filterAdapterSlave.setMasterPosition(position);
            filterAdapterMain.notifyDataSetChanged(); //update so that it shows nice little arrow
        }
    };

    CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ListViewCheckBox listViewCheckBox = (ListViewCheckBox) buttonView;
            int position = listViewCheckBox.getPosition();
            SearchFilterAdapter searchFilterAdapter = (SearchFilterAdapter) listViewCheckBox.getAdapter();
            searchFilterAdapter.setIsCheckedAtPos(isChecked,position);

            //if this is the master listview and this checkbox is the one that is controlling the slave listview
            if (searchFilterAdapter.getIsMaster() && searchFilterAdapter.getIsMasterPosition(position)) {
                searchFilterAdapter.setSlaveCheckBoxes(isChecked);
            }

            SearchFilterNode tempNode = (SearchFilterNode) searchFilterAdapter.getItem(position);
            Log.d("YOYO","CheckedChange. TempNode = " + tempNode.toString() + " HASH " + tempNode.hashCode());
            if (searchFilterAdapter.getIsMaster()) {
                updateSelectedFilterNodes(tempNode.getLeaves(),isChecked);
            } else {
                updateSelectedFilterNodes(tempNode,isChecked);
            }
        }
    };

    OnClickListener onCheckBoxClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            searchActivity.runFilteredSearch();
        }
    };


}
