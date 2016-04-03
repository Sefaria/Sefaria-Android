package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.SearchElements.SearchAdapter;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.SearchElements.SearchActionbar;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity implements AbsListView.OnScrollListener {

    private static final int PAGE_SIZE = 10;

    private EditText searchBox;
    private SearchAdapter adapter;
    private ListView listView;
    private SefariaTextView numResultsTV;
    private boolean isLoadingSearch;
    private int currPageLoaded; //based on ElasticSearch page loaded
    private int preLast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        LinearLayout actionbarRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        actionbarRoot.addView(new SearchActionbar(this, closeClick, searchClick));


    }

    @Override
    protected void onResume() {
        super.onResume();
        searchBox = (EditText) findViewById(R.id.search_box);
        //this is a listener to do a search when the user clicks on search button
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    AsyncSearch asyncSearch = new AsyncSearch(0);
                    asyncSearch.execute();
                    return true;
                }
                return false;
            }
        });
        searchBox.requestFocus();
        //open the keyboard focused in the edtSearch
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        adapter = new SearchAdapter(this, R.layout.search_item_mono,new ArrayList<Text>());
        listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        numResultsTV = (SefariaTextView) findViewById(R.id.numResults);

        isLoadingSearch = false;
    }


    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AsyncSearch asyncSearch = new AsyncSearch(0);
            asyncSearch.execute();
        }
    };

    //YOU actually need this function implemented because you're implementing AbsListView, but it's stupid...
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

    @Override
    public void onScroll(AbsListView lw, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {

        switch (lw.getId()) {
            case R.id.listview:
                if (!isLoadingSearch) {
                    int lastItem = firstVisibleItem + visibleItemCount;
                    if (lastItem == totalItemCount && preLast != lastItem) {
                        preLast = lastItem;
                        currPageLoaded++;
                        AsyncSearch asyncSearch = new AsyncSearch(currPageLoaded);

                        asyncSearch.execute();
                    }
                }
        }
    }

    public class AsyncSearch extends AsyncTask<Void,Void,List<Text>> {

        private String query;
        private int pageNum;

        public AsyncSearch(int pageNum) {
            this.pageNum = pageNum;
            Log.d("Search","LOADING PAGE " + pageNum);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSearch = true;
            query = searchBox.getText().toString();
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            return SearchAPI.search(query, pageNum, PAGE_SIZE);
        }

        @Override
        protected void onPostExecute(List<Text> results) {
            super.onPostExecute(results);
            isLoadingSearch = false;
            //page 0 means you're starting a new search. reset everything
            if (pageNum == 0) {
                adapter.setResults(results,true);
                listView.setSelection(0);
            } else {
                adapter.setResults(results,false);
            }
            numResultsTV.setText(SearchAPI.getNumResults() + " Results");
        }
    }

}
