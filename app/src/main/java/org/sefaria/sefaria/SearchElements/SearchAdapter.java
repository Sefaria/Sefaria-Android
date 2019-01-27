package org.sefaria.sefaria.SearchElements;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 3/31/16.
 */
public class SearchAdapter extends ArrayAdapter<Segment> {

    private List<Segment> results;
    private Context context;
    private Util.Lang langSearchedIn = Util.Lang.HE;

    public SearchAdapter(Context context, int resourceId, List<Segment> results) {
        super(context, resourceId);
        this.context = context;
        this.results = results;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Segment segment = results.get(position);
        //Language is exclusively either Hebrew or English, depending on which exists in the segment
        Util.Lang lang;
        if (segment.getText(langSearchedIn).length() > 0 ){
            lang = langSearchedIn;
        } else if(segment.getText(Util.Lang.HE).length() > 0 ){
            lang = Util.Lang.HE;
        } else /*if (segment.getText(Util.Lang.EN)...)*/{
            lang = Util.Lang.EN;
        }


        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.search_item_mono,null);
        }
        SefariaTextView title = (SefariaTextView) view.findViewById(R.id.title);
        SefariaTextView mono = (SefariaTextView) view.findViewById(R.id.mono);

        title.setText(segment.getLocationString(Settings.getMenuLang()));
        mono.setText(Html.fromHtml(Util.getBidiString(segment.getText(lang),lang)));

        title.setFont(Settings.getMenuLang(), true, 18);
        mono.setFont(lang, true,18);


        return view;
    }

    public void setResults(List<Segment> results, boolean reset) {
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

    public void setLangSearchedIn(Util.Lang lang){
        langSearchedIn = lang;
    }

}
