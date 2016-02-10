package org.sefaria.sefaria.TextElements;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SectionActivity;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.Text;

import java.util.Collection;
import java.util.List;

/**
 * Created by nss on 11/29/15.
 */
public class SectionAdapter extends ArrayAdapter<Text> {

    private static int MAX_ALPHA_NUM_LINKS = 70;
    private static int MIN_ALPHA_NUM_LINKS = 20;
    private static float MIN_ALPHA = 0.2f;
    private static float MAX_ALPHA = 0.8f;

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

        float linkAlpha = ((float)segment.getNumLinks()-MIN_ALPHA_NUM_LINKS) / (MAX_ALPHA_NUM_LINKS-MIN_ALPHA_NUM_LINKS);
        if (linkAlpha < MIN_ALPHA) linkAlpha = MIN_ALPHA;
        else if (linkAlpha > MAX_ALPHA) linkAlpha = MAX_ALPHA;

        if (segment.getNumLinks() == 0) linkAlpha = 0;

        Util.Lang lang = context.getTextLang();
        boolean isCts = context.getIsCts();
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


        boolean isCurrLinkSegment = segment.equals(context.getCurrLinkSegment());
        if (isCurrLinkSegment && context.getFragmentIsOpen()) view.setBackgroundColor(context.getResources().getColor(R.color.text_selected));
        else view.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));

        TextChapterHeader tch = (TextChapterHeader) view.findViewById(R.id.chapHeader);
        TextView enNum = (TextView) view.findViewById(R.id.enVerseNum);
        TextView heNum = (TextView) view.findViewById(R.id.heVerseNum);

        if (lang == Util.Lang.BI) {
            TextView enTv = (TextView) view.findViewById(R.id.en);
            TextView heTv = (TextView) view.findViewById(R.id.he);

            if (segment.isChapter()) {
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

                if(segment.getEnText().length() > 0)
                    enTv.setText(Html.fromHtml(segment.getEnText()));
                else
                    enTv.setVisibility(View.GONE);
                //enTv.setText(""+segment.getNumLinks() + " / " + maxNumLinks + "\nALPHA = " + linkAlpha);
                if(segment.getHeText().length() > 0)
                    heTv.setText(Html.fromHtml(segment.getHeText()));
                else
                    heTv.setVisibility(View.GONE);



                //enTv.setTextColor(Color.parseColor("#999999"));
                enTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                enTv.setTextSize(context.getTextSize());

                //heTv.setTextColor(Color.parseColor("#000000"));
                heTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                heTv.setTextSize(context.getTextSize());
                if(segment.displayNum)
                    heNum.setText("" + Util.int2heb(segment.levels[0]));
                else
                    heNum.setText("");

                heNum.setAlpha(1);
                heNum.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                //heNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                enNum.setText(Util.VERSE_BULLET);
                enNum.setAlpha(linkAlpha);

            }

        } else { //Hebrew or English
            TextView tv = (TextView) view.findViewById(R.id.mono);

            if (segment.isChapter()) {

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
                    if (segment.getHeText().length() == 0)
                        tv.setText(context.getResources().getString(R.string.no_text));
                    else
                        tv.setText(Html.fromHtml(segment.getHeText()));
                    enNum.setText(Util.VERSE_BULLET);
                    enNum.setAlpha(linkAlpha);
                    enNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                    if(segment.displayNum)
                        heNum.setText(Util.int2heb(segment.levels[0]));
                    else
                        heNum.setText("");
                    heNum.setAlpha(1);
                    heNum.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                } else /*if (context.getTextLang() == Util.Lang.EN)*/ {
                    if (segment.getEnText().length() == 0)
                        tv.setText(context.getResources().getString(R.string.no_text));
                    else
                        tv.setText(Html.fromHtml(segment.getEnText()));
                    if(segment.displayNum)
                        enNum.setText(""+segment.levels[0]);
                    else
                        enNum.setText("");

                    enNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                    enNum.setAlpha(1);
                    heNum.setText(Util.VERSE_BULLET);
                    heNum.setAlpha(linkAlpha);
                    heNum.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
                }
                tv.setTextColor(Color.parseColor("#000000"));
                tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
                tv.setTextSize(context.getTextSize());
            }
        }
        return view;
    }

    public void updateFocusedSegment() {

    }

    @Override
    public void add(Text object) {
        add(texts.size(), object);

    }

    @Override
    public void addAll(Collection<? extends Text> collection) {
       addAll(texts.size(), collection);

    }

    public void add(int location, Text object) {
        texts.add(location, object);
        notifyDataSetChanged();
    }

    public void addAll(int location, Collection<? extends Text> collection) {
        texts.addAll(location, collection);
        notifyDataSetChanged();
    }

    @Override
    public Text getItem(int position) {
        try {
            return super.getItem(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public int getPosition(Text item) {
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).tid == item.tid) return i;
        }
        return -1;
    }
}
