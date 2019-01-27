package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
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
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.layouts.IndeterminateCheckBox;
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

    private Boolean[] isCheckedArrayMain;
    private Boolean[][] isCheckedArraySlave;

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

        Util.Lang systemLang = Settings.getSystemLang();
        if (systemLang == Util.Lang.HE) {
            findViewById(R.id.arrow_right).setVisibility(GONE);
            findViewById(R.id.arrow_left).setVisibility(VISIBLE);
            LinearLayout yo = (LinearLayout) findViewById(R.id.filterTitle);
            yo.setGravity(Gravity.RIGHT);
        } else /* if (systemLang == Util.Lang.EN) */ {
            findViewById(R.id.arrow_right).setVisibility(VISIBLE);
            findViewById(R.id.arrow_left).setVisibility(GONE);
            LinearLayout yo = (LinearLayout) findViewById(R.id.filterTitle);
            yo.setGravity(Gravity.LEFT);
        }
    }

    public void initFilters(JSONArray jsonArray) {
        try {
            root = new SearchFilterNode();
            Map<String, SearchFilterNode> nodeMap = new HashMap<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String filterPath = jsonObject.getString("key");


                String[] filterStringList = filterPath.split("/");
                String[] filterStringListHe = MenuState.translatePath(MenuState.IndexType.SEARCH, filterStringList); //TODO suspicious
                String[] fullFilterStringList = getFullFilterStringList(filterStringList);
                //Log.d("Search", Arrays.toString(filterStringList) + " ==> " + Arrays.toString(filterStringListHe));
                int count = jsonObject.getInt("doc_count");

                int j = 0;
                //boolean foundCommentary = false;

                for (String filterString : filterStringList) {
                    String filterStringHe = filterStringListHe[j];
                    String fullFilterString = fullFilterStringList[j];

                    /*if (foundCommentary) {
                        j++;
                        foundCommentary = false;
                        continue;
                    }

                    if ((filterString.equals("Commentary") || filterString.equals("Commentary2")) && j == 0) {
                        filterString = filterStringList[1] + " Commentary";
                        filterStringList[1] = filterString; //so that it will recognize "tanakh commentary" as the parent in the next iteration


                        if (filterStringListHe[1] != null) {
                            filterStringHe = "מפרשי " + filterStringListHe[1];
                            filterStringListHe[1] = filterStringHe;
                        } else {
                            filterStringHe = filterString;
                            filterStringListHe[1] = filterStringHe;
                        }

                        foundCommentary = true;

                        fullFilterString = filterString;
                        fullFilterStringList[1] = fullFilterString;
                    }*/
                    SearchFilterNode searchFilterNode;
                    if (nodeMap.containsKey(fullFilterString))
                        searchFilterNode = nodeMap.get(fullFilterString);
                    else {
                        SearchFilterNode parent;
                        if (j == 0)
                            parent = root;
                        else
                            parent = nodeMap.get(fullFilterStringList[j-1]);
                        if (filterString.replaceAll("\\s+","").equals("") ) continue;
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
            //Log.d("YOYO","OLD FILTERS " + minSelectedFilterNodes.toString());
            filterAdapterMain.clearAndAdd(root.getChildren(),minSelectedFilterNodes);
            filterListMain.setSelection(0);

            filterAdapterSlave.clear();
            filterAdapterSlave.setMasterPosition(-1);

            isCheckedArrayMain = new Boolean[root.getNumChildren()];
            for (int i = 0; i < isCheckedArrayMain.length; i++) {
                isCheckedArrayMain[i] = false;
            }
            isCheckedArraySlave = new Boolean[root.getNumChildren()][];
            for (int i = 0; i < isCheckedArraySlave.length; i++) {
                Boolean[] innerArray = new Boolean[((SearchFilterNode)root.getChild(i)).getLeaves().size()];
                for (int j = 0; j < innerArray.length; j++) {
                    innerArray[j] = false;
                }
                isCheckedArraySlave[i] = innerArray;
            }
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

    private void updateSelectedFilterNodes(BilingualNode node, @Nullable Boolean isChecked) {
        if (node == null) return;
        if (isChecked != null) {
            if (isChecked) {
                allSelectedFilterNodes.add(node);
            } else {
                allSelectedFilterNodes.remove(node);
            }
        }


        minSelectedFilterNodes = SearchAPI.getMinFilterNodes(new ArrayList<>(allSelectedFilterNodes));

        Log.d("YOYO","Min Nodes from Master = " + minSelectedFilterNodes.toString());
    }

    private void updateSelectedFilterNodes(List<BilingualNode> nodes, @Nullable Boolean isChecked) {
        if (nodes == null) return;

        if (isChecked != null) {
            if (isChecked) {
                allSelectedFilterNodes.addAll(nodes);
            } else {
                allSelectedFilterNodes.removeAll(nodes);
            }
        }


        minSelectedFilterNodes = SearchAPI.getMinFilterNodes(new ArrayList<>(allSelectedFilterNodes));

        Log.d("YOYO","Min Nodes = " + minSelectedFilterNodes.toString());
        for (BilingualNode yo : minSelectedFilterNodes) {
            Log.d("YOYO","\tMin Node Children = " + yo.getChildren().toString() + " HASH " + yo.hashCode());
        }
    }

    private void openFilterAtPos(int position) {
        SearchFilterNode node = (SearchFilterNode) filterAdapterMain.getItem(position);
        filterAdapterSlave.clearAndAdd(node.getLeaves(),isCheckedArraySlave[position]);
        filterListSlave.setSelection(0);
        filterAdapterSlave.setMasterPosition(position);
        filterAdapterMain.notifyDataSetChanged(); //update so that it shows nice little arrow
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
            openFilterAtPos(position);
        }
    };

    /*IndeterminateCheckBox.setOnStateChangedListener(new IndeterminateCheckBox.OnStateChangedListener() {
        @Override
        public void onStateChanged(IndeterminateCheckBox check, @Nullable Boolean state) {
            String stateText = (state != null) ? (state ? "Checked" : "Unchecked") : "Indeterminate";
            Toast.makeText(MainActivity.this, "IndeterminateCheckBox: " + stateText, Toast.LENGTH_SHORT).show();
        }
    });*/


    IndeterminateCheckBox.OnStateChangedListener checkedChangeListener = new IndeterminateCheckBox.OnStateChangedListener() {
        @Override
        public void onStateChanged(IndeterminateCheckBox buttonView, @Nullable Boolean state) {


            ListViewCheckBox listViewCheckBox = (ListViewCheckBox) buttonView;
            int position = listViewCheckBox.getPosition();
            SearchFilterAdapter searchFilterAdapter = (SearchFilterAdapter) listViewCheckBox.getAdapter();
            searchFilterAdapter.setIsCheckedAtPos(state,position);

            //openFilterAtPos(position);

            //if this is the master listview and this checkbox is the one that is controlling the slave listview
            if (searchFilterAdapter.getIsMaster() && state != null) {
                for (int i = 0; i < isCheckedArraySlave[position].length; i++) {
                    isCheckedArraySlave[position][i] = state;
                }
                if (searchFilterAdapter.getIsMasterPosition(position)) {
                    searchFilterAdapter.setSlaveCheckBoxes(state);
                }
            }

            SearchFilterNode tempNode = (SearchFilterNode) searchFilterAdapter.getItem(position);
            if (searchFilterAdapter.getIsMaster()) {
                updateSelectedFilterNodes(tempNode.getLeaves(),state);
                isCheckedArrayMain[position] = state;
            } else {
                updateSelectedFilterNodes(tempNode,state);

                int masterPos = filterAdapterSlave.getMasterPosition();
                isCheckedArraySlave[masterPos][position] = state;
                if (state != null) {

                    if (state && filterAdapterSlave.isUniformValue(true)) {
                        filterAdapterMain.setIsCheckedAtPos(true,masterPos);
                    } else {
                        if (filterAdapterSlave.isUniformValue(false)) filterAdapterMain.setIsCheckedAtPos(false,masterPos);
                        else filterAdapterMain.setIsCheckedAtPos(null,masterPos);
                    }
                }

            }
        }
    };

    OnClickListener onCheckBoxClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            searchActivity.runFilteredSearch();
            ListViewCheckBox listViewCheckBox = (ListViewCheckBox) v;
            SearchFilterAdapter searchFilterAdapter = (SearchFilterAdapter) listViewCheckBox.getAdapter();
            int position = listViewCheckBox.getPosition();
            if (searchFilterAdapter.getIsMaster() && listViewCheckBox.getState()) {
                openFilterAtPos(position);
            }
        }
    };


}
