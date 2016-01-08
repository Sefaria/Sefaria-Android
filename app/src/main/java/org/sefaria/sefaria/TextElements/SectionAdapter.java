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
        Util.Lang lang = context.getTextLang();
        if (view == null || (view.findViewById(R.id.he) == null && lang == Util.Lang.BI)
                || (view.findViewById(R.id.mono) == null && (lang == Util.Lang.HE || lang == Util.Lang.EN))) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (lang == Util.Lang.BI) {
                view = inflater.inflate(R.layout.adapter_text_bilingual,null);
            } else {
                view = inflater.inflate(R.layout.adapter_text_mono,null);
            }
        }
        TextChapterHeader tch = (TextChapterHeader) view.findViewById(R.id.chapHeader);
        TextView enNum = (TextView) view.findViewById(R.id.enVerseNum);
        TextView heNum = (TextView) view.findViewById(R.id.heVerseNum);

        if (lang == Util.Lang.BI) {
            TextView enTv = (TextView) view.findViewById(R.id.en);
            TextView heTv = (TextView) view.findViewById(R.id.he);

            if (segment.isChapter) {
                tch.setVisibility(View.VISIBLE);
                enTv.setVisibility(View.GONE);
                heTv.setVisibility(View.GONE);
                enNum.setVisibility(View.GONE);
                heNum.setVisibility(View.GONE);

                tch.setSectionTitle(segment);
                tch.setTextSize(context.getTextSize());
            } else {
                tch.setVisibility(View.GONE);
                enTv.setVisibility(View.VISIBLE);
                heTv.setVisibility(View.VISIBLE);
                enNum.setVisibility(View.VISIBLE);
                heNum.setVisibility(View.VISIBLE);

                enTv.setText(Html.fromHtml(segment.enText));
                heTv.setText(Html.fromHtml(segment.heText));

                //enTv.setTextColor(Color.parseColor("#999999"));
                enTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                enTv.setTextSize(context.getTextSize());

                //heTv.setTextColor(Color.parseColor("#000000"));
                heTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                heTv.setTextSize(context.getTextSize());

                heNum.setText("" + segment.levels[0]);
                heNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                enNum.setText(Util.VERSE_BULLET);
            }

        } else { //Hebrew or English
            TextView tv = (TextView) view.findViewById(R.id.mono);

            if (segment.isChapter) {

                tch.setVisibility(View.VISIBLE);
                tv.setVisibility(View.GONE);
                enNum.setVisibility(View.GONE);
                heNum.setVisibility(View.GONE);

                tch.setSectionTitle(segment);
                tch.setTextSize(context.getTextSize());

            } else {
                tch.setVisibility(View.GONE);
                tv.setVisibility(View.VISIBLE);
                enNum.setVisibility(View.VISIBLE);
                heNum.setVisibility(View.VISIBLE);

                if (context.getTextLang() == Util.Lang.HE) {
                    tv.setText(Html.fromHtml(segment.heText));
                    enNum.setText(Util.VERSE_BULLET);
                    enNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                    heNum.setText(Util.int2heb(segment.levels[0]));
                    heNum.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                } else /*if (context.getTextLang() == Util.Lang.EN)*/ {
                    tv.setText(Html.fromHtml(segment.enText));
                    enNum.setText(""+segment.levels[0]);
                    enNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                    heNum.setText(Util.VERSE_BULLET);
                    heNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                }
                tv.setTextColor(Color.parseColor("#000000"));
                tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                tv.setTextSize(context.getTextSize());
            }
        }
        return view;
    }
}
