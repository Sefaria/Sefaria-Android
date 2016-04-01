package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity {

    private EditText searchBox;
    private SearchAdapter adapter;
    private ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        LinearLayout actionbarRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        actionbarRoot.addView(new SearchActionbar(this, closeClick, searchClick));

        searchBox = (EditText) findViewById(R.id.search_box);
        //this is a listener to do a search when the user clicks on search button
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchBox.getText().toString();
                    search(query);
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
            String query = searchBox.getText().toString();
            search(query);
        }
    };

    private void search(String query) {
        String jsonString =
                "{" +
                    "\"sort\": [{" +
                        "\"order\": {}" +
                    "}]," +
                    "\"query\": {" +
                        "\"query_string\": {" +
                            "\"query\": \"" + query + "\"," +
                            "\"default_operator\": \"AND\"," +
                            "\"fields\": [\"content\"]" +
                        "}" +
                    "}," +
                    "\"highlight\": {" +
                        "\"pre_tags\": [\"<b>\"]," +
                        "\"post_tags\": [\"</b>\"]," +
                        "\"fields\": {" +
                            "\"content\": {\"fragment_size\": 200}" +
                        "}" +
                    "}" +
                "}";
        try {
            String result = API.getDataFromURL(API.SEARCH_URL, jsonString, false);
            JSONObject resultJson = new JSONObject(result);
            JSONArray hits = resultJson.getJSONObject("hits").getJSONArray("hits");
            Toast.makeText(SearchActivity.this, hits.length() + " Results", Toast.LENGTH_SHORT).show();
            adapter.setResults(hits);
            listView.setSelection(0);
        } catch (API.APIException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
