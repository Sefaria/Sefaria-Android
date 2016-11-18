package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.SearchElements.SearchAdapter;
import org.sefaria.sefaria.SearchElements.SearchFilterBox;
import org.sefaria.sefaria.SearchElements.SearchResultContainer;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.SearchElements.SearchActionbar;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.database.SearchingDB;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SearchActivity extends Activity implements AbsListView.OnScrollListener {


    private static final int PAGE_SIZE = 30;
    private static final String INTENT_SEARCH_TERM = "searchTerm";

    private SearchAdapter adapter;
    private ListView listView;
    private SefariaTextView numResultsTV;
    private boolean isLoadingSearch;
    private int currPageLoaded; //based on ElasticSearch page loaded
    private int preLast;
    private boolean APIError = false;
    private AutoCompleteTextView autoCompleteTextView;
    private SearchFilterBox searchFilterBox;
    private int oldTheme = Settings.getTheme();

    private SearchingDB searchingDB;

    private String numberOfResults = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_search);



        LinearLayout actionbarRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        actionbarRoot.addView(new SearchActionbar(this, closeClick, searchClick, null, null, -1, "<i>" + MyApp.getRString(R.string.search) + "</i>", searchLongClick));



        String searchTerm;
        if(savedInstanceState == null){//it's coming from a saved to ram state
            Intent intent = getIntent();
            searchTerm = intent.getStringExtra(INTENT_SEARCH_TERM);
        }else{
            searchTerm = savedInstanceState.getString(INTENT_SEARCH_TERM);
        }

        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto_complete_text_view);
        if(searchTerm != null)
            autoCompleteTextView.setText(searchTerm);
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        ArrayList<String> allBookNames = Book.getAllBookNames(Util.Lang.BI);
        ArrayAdapter<String> autoComAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item,allBookNames);
        autoCompleteTextView.setAdapter(autoComAdapter);
        autoCompleteTextView.setOnItemClickListener(autoCompleteItemClick);
        //autoCompleteTextView.setOnFocusChangeListener(autoComFocus);
        autoCompleteTextView.setOnEditorActionListener(autoComEnterClick);
        //autoCompleteTextView.setCompletionHint("Click book to open");

        searchFilterBox = (SearchFilterBox) findViewById(R.id.search_filter_box);
    }

    TextView.OnEditorActionListener autoComEnterClick = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.d("SearchAct","autoComEnterClick");
                runNewSearch();
                return true;
            }
            return false;
        }
    };

    AdapterView.OnItemClickListener autoCompleteItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(android.widget.AdapterView<?> parent, View v, int pos, long id) {
            autoCompleteTextView.setText("");//So that it doesn't fill the bar with the book you just clicked
            String title = (String) ((TextView)v).getText();
            try {
                Book book = new Book(title,true);
                SuperTextActivity.startNewTextActivityIntent(SearchActivity.this,book,false);
            } catch (Book.BookNotFoundException e) {
                Toast.makeText(SearchActivity.this,MyApp.getRString(R.string.error_getting_book),Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnFocusChangeListener autoComFocus = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            Log.d("SearchAct","onFocusChange" + hasFocus);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(INTENT_SEARCH_TERM,autoCompleteTextView.getText().toString());
    }

    private void restartActivity(){
        Intent intent = new Intent(this,SearchActivity.class);
        intent.putExtra(INTENT_SEARCH_TERM,autoCompleteTextView.getText().toString());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(Settings.getTheme() != oldTheme){
            restartActivity();
            return;
        }

        //open the keyboard focused in the edtSearch
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(autoCompleteTextView, InputMethodManager.SHOW_IMPLICIT);
        if (adapter == null)
            adapter = new SearchAdapter(this, R.layout.search_item_mono,new ArrayList<Text>());

        if (listView == null) {
            listView = (ListView) findViewById(R.id.listview);
            listView.setAdapter(adapter);
            listView.setOnScrollListener(this);
            listView.setOnItemClickListener(onItemClickListener);
            listView.setDivider(null);
        }
        numResultsTV = (SefariaTextView) findViewById(R.id.numResults);
        numResultsTV.setFont(Settings.getSystemLang(),false);
        isLoadingSearch = false;


        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) findViewById(R.id.dialogNoahSnackbarRoot));
    }

    //run a new search
    private void runNewSearch(){
        findViewById(R.id.results_box).setVisibility(View.VISIBLE);
        autoCompleteTextView.clearFocus();
        currPageLoaded = 0;
        AsyncSearch asyncSearch = new AsyncSearch(true,new ArrayList<String>(),currPageLoaded);
        //AsyncSearch asyncSearch = new AsyncSearch(true,searchFilterBox.getSelectedFilterStrings(),0);
        asyncSearch.execute();
        // Check if no view has focus:
        View view = SearchActivity.this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }

    public void runFilteredSearch() {
        findViewById(R.id.results_box).setVisibility(View.VISIBLE);
        autoCompleteTextView.clearFocus();
        currPageLoaded = 0;
        AsyncSearch asyncSearch = new AsyncSearch(false,searchFilterBox.getSelectedFilterStrings(),currPageLoaded);
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
            runNewSearch();
        }
    };

    View.OnLongClickListener searchLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if(Settings.getIsDebug() && SearchingDB.hasSearchTable()){
                (new SearchingDB.AsyncRunTests(SearchActivity.this)).execute();
                return true;
            }else{
                return false;
            }

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
                        if(currPageLoaded != 0 && SearchingDB.hasSearchTable() && (!Downloader.getHasInternet()
                            && searchingDB == null)) {
                                Toast.makeText(SearchActivity.this, "Lost Connection: Restarting search in offline mode", Toast.LENGTH_LONG).show();
                                runNewSearch();
                        }else {
                            AsyncSearch asyncSearch = new AsyncSearch(false, searchFilterBox.getSelectedFilterStrings(), currPageLoaded);
                            asyncSearch.execute();
                        }
                    }
                }
        }
    }

    private JSONArray getAllFilterJSON(){
        JSONArray json = new JSONArray();
        List<Book> books = Book.getAll();
        int index = 0;
        for(Book book: books){
            StringBuilder key = new StringBuilder();
            for(String cat: book.categories) {
                key.append(cat + "/");
            }
            key.append(book.title);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("key",key);
                jsonObject.put("doc_count",-1);
                json.put(index++, jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return json;
    }

    public class AsyncSearch extends AsyncTask<Void,Void,SearchResultContainer> {

        private String query;
        private boolean getFilters;
        private List<String> appliedFilters;
        private int pageNum;
        private boolean usingOfflineSearch = false;


        public AsyncSearch(boolean getFilters, List<String> appliedFilters, int pageNum) {
            this.getFilters = getFilters;
            this.appliedFilters = appliedFilters;
            this.pageNum = pageNum;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(Downloader.getNetworkStatus() == Downloader.ConnectionType.NONE && SearchingDB.hasSearchTable()) {
                if (pageNum != 0 && searchingDB == null) {
                    Toast.makeText(SearchActivity.this, "Restarting search in offline mode", Toast.LENGTH_SHORT).show();
                    pageNum = 0;
                }
                usingOfflineSearch = true;
            }

            isLoadingSearch = true;
            if(usingOfflineSearch){
                numResultsTV.setText("Using offline search." + " " + MyApp.getRString(R.string.loading));
                //getFilters = false;
                Log.d("Searching","offline");
            }else{
                numResultsTV.setText(numberOfResults + " " + MyApp.getRString(R.string.loading));
                Log.d("Searching","online");
            }
            query = autoCompleteTextView.getText().toString();
            adapter.setLangSearchedIn(Util.hasHebrew(query) ? Util.Lang.HE : Util.Lang.EN);
        }

        @Override
        protected SearchResultContainer doInBackground(Void... params) {
            APIError = false;
            try {
                if(usingOfflineSearch){
                    try {
                        if(searchingDB == null || pageNum == 0) {
                            searchingDB = new SearchingDB(query,appliedFilters);
                        }
                        List<Text> results = searchingDB.getResults();
                        Log.d("search","results size:" + results.size());
                        JSONArray jsonRoot = null;
                        if(getFilters) {
                            jsonRoot = getAllFilterJSON();
                        }
                        SearchResultContainer searchResultContainer = new SearchResultContainer(results,-1, jsonRoot);
                        return searchResultContainer;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        APIError = true;
                        return null;
                    }
                }
                else if (getFilters && (appliedFilters != null && appliedFilters.size() > 0)) {
                    SearchResultContainer getFilterResults = SearchAPI.search(query, getFilters, null,pageNum, PAGE_SIZE);
                    SearchResultContainer filteredResults = SearchAPI.search(query, false, appliedFilters,pageNum, PAGE_SIZE);
                    filteredResults.setAllFilters(getFilterResults.getAllFilters());
                    return  filteredResults;
                } else {
                    return SearchAPI.search(query, getFilters, appliedFilters, pageNum, PAGE_SIZE);
                }

            } catch (API.APIException e) {
                APIError = true;
                return null;
            }
        }

        @Override
        protected void onPostExecute(SearchResultContainer resultContainer) {
            super.onPostExecute(resultContainer);
            isLoadingSearch = false;
            if(APIError || resultContainer == null) { //these 2 things should really be the same
                API.makeAPIErrorToast(SearchActivity.this, MyApp.getRString(R.string.searching_requires_internet));
                numResultsTV.setText(MyApp.getRString(R.string.Error) + ": " + MyApp.getRString(R.string.NO_INTERNET_TITLE));
                return;
            }

            //page 0 means you're starting a new search. reset everything
            if (pageNum == 0) {
                adapter.setResults(resultContainer.getResults(),true);
                listView.setSelection(0);
            } else {
                adapter.setResults(resultContainer.getResults(),false);
            }
            numberOfResults = resultContainer.getNumResults() + " " + MyApp.getRString(R.string.results);
            if (getFilters) {
                searchFilterBox.initFilters(resultContainer.getAllFilters());
            }
            if(usingOfflineSearch){
                numResultsTV.setText("Using offline search");
            }else {
                numResultsTV.setText(numberOfResults);
            }
        }
    }

    ListView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Text text = adapter.getItem(position);
            String place = text.getLocationString(Util.Lang.EN);
            Book book;
            try {
                book = new Book(text.bid);
            } catch (Book.BookNotFoundException e) {
                book = null;
            }
            API.PlaceRef placeRef = null;
            try {
                placeRef = API.PlaceRef.getPlace(place,book);
                SuperTextActivity.startNewTextActivityIntent(SearchActivity.this,placeRef.book,placeRef.text,placeRef.node,false,autoCompleteTextView.getText().toString(),-1);
            } catch (API.APIException e) {
                API.makeAPIErrorToast(SearchActivity.this);//MyApp.openURLInBrowser(SearchActivity.this,"https://sefaria.org/" + place);
            } catch (Book.BookNotFoundException e) {
                MyApp.openURLInBrowser(SearchActivity.this,"https://sefaria.org/" + place);
            }

        }
    };

}
