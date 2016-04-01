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

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.search_item_mono,null);
        }
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView mono = (TextView) view.findViewById(R.id.mono);

        title.setText(text.getLocationString(Settings.getMenuLang()));
        mono.setText(Html.fromHtml(text.getText(Util.Lang.EN)));
        return view;
    }
    /**
     *
     * @param resultStrings - raw results array from ElasticSearch query. refs still need to be parsed
     */
    public void setResults( JSONArray resultStrings) throws JSONException {
        //TODO parse refs
        results.clear();
        clear();
        Set<String> refSet = new HashSet<>();
        for (int i = 0; i < resultStrings.length(); i++) {
            JSONObject source = resultStrings.getJSONObject(i).getJSONObject("_source");
            String ref = source.getString("ref");
            if(refSet.contains(ref)) {
                continue;
            }else {
                refSet.add(ref);
            }

            String content = resultStrings.getJSONObject(i).getJSONObject("highlight").getJSONArray("content").getString(0);
            String title = ref.replaceAll("\\s[0-9]+.*$", "");


            Text text = new Text(content,"",Book.getBid(title),ref);
            //TODO deal with levels stuff
            results.add(text);

        }
        addAll(results);
        notifyDataSetChanged();
    }
}
