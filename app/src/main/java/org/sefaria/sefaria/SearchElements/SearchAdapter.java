package org.sefaria.sefaria.SearchElements;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by nss on 3/31/16.
 */
public class SearchAdapter extends ArrayAdapter<Text> {

    private List<Text> results;
    private Context context;

    public SearchAdapter(Context context, int resourceId, List<Text> results) {
        super(context, resourceId);
        this.context = context;
        this.results = results;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Text text = results.get(position);
        //Language is exclusively either Hebrew or Enlgihs, depending on which exists in the text
        Util.Lang lang;
        if (text.getText(Util.Lang.EN) == "") lang = Util.Lang.HE;
        else /*if (text.getText(Util.Lang.HE) == "")*/ lang = Util.Lang.EN;


        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.search_item_mono,null);
        }
        SefariaTextView title = (SefariaTextView) view.findViewById(R.id.title);
        SefariaTextView mono = (SefariaTextView) view.findViewById(R.id.mono);

        title.setText(text.getLocationString(Settings.getMenuLang()));
        mono.setText(Html.fromHtml(text.getText(lang)));

        title.setFont(Settings.getMenuLang(), true);
        mono.setFont(lang, true);


        return view;
    }

    public void setResults(List<Text> results, boolean reset) {
        //TODO parse refs
        if (reset) {
            this.results.clear();
            clear();
            this.results = results;
        } else {
            this.results.addAll(results);
        }

        addAll(results);
        notifyDataSetChanged();
    }
}
