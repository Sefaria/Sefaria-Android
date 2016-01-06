package org.sefaria.sefaria.TextElements;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SectionActivity;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.menu.MenuNode;

import java.util.List;

/**
 * Created by nss on 11/29/15.
 */
public class SectionAdapter extends ArrayAdapter<Text> {

    private SectionActivity context;
    private List<Text> texts;
    private int resourceId;
    private int preLast;

    public SectionAdapter(SectionActivity context, int resourceId, List<Text> objects) {
        super(context,resourceId,objects);
        this.context = context;
        this.texts = objects;
        this.resourceId = resourceId;


    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Text segment = texts.get(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(resourceId,null);
        }

        TextView tv = (TextView) view.findViewById(R.id.mono);
        TextChapterHeader tch = (TextChapterHeader) view.findViewById(R.id.chapHeader);
        if (segment.isChapter) {

            tch.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);

            tch.setSectionTitle(segment);
            tch.setTextSize(context.getTextSize());

        } else {
            tch.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);

            if (context.getLang() == Util.Lang.HE) {
                tv.setText(Html.fromHtml(segment.heText));
            } else /*if (context.getLang() == Util.Lang.EN)*/ {
                tv.setText(Html.fromHtml(segment.enText));
            }
            tv.setTextColor(Color.parseColor("#000000"));
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            tv.setTextSize(context.getTextSize());
        }
        return view;
    }
}
