package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.SearchElements.SearchAdapter;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.SearchElements.SearchActionbar;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.database.Searching;
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
    private boolean APIError = false;

    private String numberOfResults = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
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
                    runSearch();
                    return true;
                }
                return false;
            }
        });
        searchBox.requestFocus();
        searchBox.setTypeface(MyApp.getFont(MyApp.Font.QUATTROCENTO));

        //open the keyboard focused in the edtSearch
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        if (adapter == null)
            adapter = new SearchAdapter(this, R.layout.search_item_mono,new ArrayList<Text>());

        if (listView == null) {
            listView = (ListView) findViewById(R.id.listview);
            listView.setAdapter(adapter);
            listView.setOnScrollListener(this);
            listView.setOnItemClickListener(onItemClickListener);
        }
        numResultsTV = (SefariaTextView) findViewById(R.id.numResults);
        numResultsTV.setFont(Settings.getSystemLang(),false);
        isLoadingSearch = false;


        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) findViewById(R.id.dialogNoahSnackbarRoot));
    }

    private void runSearch(){
        searchBox.clearFocus();
        AsyncSearch asyncSearch = new AsyncSearch(0);
        asyncSearch.execute();
        // Check if no view has focus:
        View view = SearchActivity.this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
            runSearch();
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
            numResultsTV.setText(numberOfResults + " Loading...");
            query = searchBox.getText().toString();
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            APIError = false;
            try {
                return SearchAPI.search(query, pageNum, PAGE_SIZE);
            } catch (API.APIException e) {
                APIError = true;
                return new ArrayList<>();
            }
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
            numberOfResults = SearchAPI.getNumResults() + " Results";
            numResultsTV.setText(numberOfResults);
            if(APIError) {
                GoogleTracker.sendEvent(GoogleTracker.CATEGORY_RANDOM_ERROR,MyApp.getRString(R.string.searching_requires_internet));
                API.makeAPIErrorToast(SearchActivity.this, MyApp.getRString(R.string.searching_requires_internet));
            }
        }
    }

    ListView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String place = adapter.getItem(position).getLocationString(Util.Lang.EN);
            API.PlaceRef placeRef = null;
            try {
                placeRef = API.PlaceRef.getPlace(place);
                SuperTextActivity.startNewTextActivityIntent(SearchActivity.this,placeRef.book,placeRef.text,placeRef.node,false,searchBox.getText().toString(),-1);
            } catch (API.APIException e) {
                API.makeAPIErrorToast(SearchActivity.this);//MyApp.openURLInBrowser(SearchActivity.this,"https://sefaria.org/" + place);
            } catch (Book.BookNotFoundException e) {
                MyApp.openURLInBrowser(SearchActivity.this,"https://sefaria.org/" + place);
            }

        }
    };

}
