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
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity implements AbsListView.OnScrollListener {


    private static final int PAGE_SIZE = 30;
    private static final String INTENT_SEARCH_TERM = "searchTerm";

    private SearchAdapter adapter;
    private ListView listView;
    private SefariaTextView numResultsTV;
    private boolean isLoadingSearch;
    private ArrayList<String> appliedFilters;
    private int currPageLoaded; //based on ElasticSearch page loaded
    private int preLast;
    private boolean APIError = false;
    private AutoCompleteTextView autoCompleteTextView;
    private int oldTheme = Settings.getTheme();

    private String numberOfResults = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_search);



        LinearLayout actionbarRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        actionbarRoot.addView(new SearchActionbar(this, closeClick, searchClick, null, null, -1, "<i>" + MyApp.getRString(R.string.search) + "</i>"));



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
        autoCompleteTextView.clearFocus();
        AsyncSearch asyncSearch = new AsyncSearch(true,null,0);
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
                        AsyncSearch asyncSearch = new AsyncSearch(false,appliedFilters,currPageLoaded);
                        asyncSearch.execute();
                    }
                }
        }
    }

    public class AsyncSearch extends AsyncTask<Void,Void,List<Text>> {

        private String query;
        private boolean getFilters;
        private ArrayList<String> appliedFilters;
        private int pageNum;

        public AsyncSearch(boolean getFilters, ArrayList<String> appliedFilters, int pageNum) {
            this.getFilters = getFilters;
            this.appliedFilters = appliedFilters;
            this.pageNum = pageNum;
            Log.d("Search","LOADING PAGE " + pageNum);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSearch = true;
            numResultsTV.setText(numberOfResults + " " + MyApp.getRString(R.string.loading));
            query = autoCompleteTextView.getText().toString();
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            APIError = false;
            try {
                return SearchAPI.search(query, getFilters, appliedFilters,pageNum, PAGE_SIZE);
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
            numberOfResults = SearchAPI.getNumResults() + " " + MyApp.getRString(R.string.results);
            findViewById(R.id.results_border).setVisibility(View.VISIBLE);
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
