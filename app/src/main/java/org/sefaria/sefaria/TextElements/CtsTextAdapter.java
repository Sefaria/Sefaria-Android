package org.sefaria.sefaria.TextElements;

import android.app.Activity;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Section;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.Collection;
import java.util.List;

/**
 * Created by nss on 5/17/16.
 */
public class CtsTextAdapter extends ArrayAdapter<Section> {

    private static int MAX_ALPHA_NUM_LINKS = 70;
    private static int MIN_ALPHA_NUM_LINKS = 20;
    private static float MIN_ALPHA = 0.2f;
    private static float MAX_ALPHA = 0.8f;

    private SuperTextActivity activity;
    private List<Section> sections;

    private Segment highlightIncomingSegment;

    private int resourceId;
    private int preLast;

    private OnSegmentSpanClickListener onSegmentSpanClickListener;

    public CtsTextAdapter(SuperTextActivity activity, int resourceId, List<Section> objects, OnSegmentSpanClickListener onSegmentSpanClickListener) {
        super(activity, resourceId, objects);
        this.activity = activity;
        this.sections = objects;
        this.resourceId = resourceId;
        this.onSegmentSpanClickListener = onSegmentSpanClickListener;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Section currSection = sections.get(position);

        if (currSection.getIsLoader()) {
            LayoutInflater inflater = (LayoutInflater)
                    activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.adapter_text_loader,null);
            view.setClickable(false);
            return view;
        }

        Util.Lang lang = activity.getTextLang();

        if (view == null ||
                view.findViewById(R.id.sectionTV) == null) {
            LayoutInflater inflater = (LayoutInflater)
                    activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                view = inflater.inflate(R.layout.adapter_section_mono,null);
        }

        TextChapterHeader headerTv = (TextChapterHeader) view.findViewById(R.id.chapHeader);
        SefariaTextView sectionTv = (SefariaTextView) view.findViewById(R.id.sectionTV);

        sectionTv.setFont(lang, true);
        setSectionText(currSection, sectionTv, lang);

        headerTv.setSectionTitle(currSection.getHeaderSegment());

        headerTv.setTextSize(activity.getTextSize());
        sectionTv.setTextSize(activity.getTextSize());


        return view;
    }

    private void setSectionText(Section currSection,SefariaTextView tv, Util.Lang lang) {
        /*boolean isFirst = true;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        for (int i = 0; i < currSection.getSegmentList().size(); i++) {
            Segment segment = currSection.getSegmentList().get(i);
            String words;
            //wait off on this
            if (segment.displayNum && false) {
                words = "(" + Util.int2heb(segment.levels[0]) + ") " + segment.getText(lang);
            } else {
                words = segment.getText(lang);
            }
            if (!isFirst)
                words = "&nbsp;" + words;


            SpannableString ss = new SpannableString(Html.fromHtml(words));
            SegmentSpannable segmentSpannable = new SegmentSpannable(words,segment,onSegmentSpanClickListener);
            ss.setSpan(segmentSpannable, 0, ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }

        tv.setText(ssb, TextView.BufferType.SPANNABLE);*/

        //tv.setMovementMethod(LinkMovementMethod.getInstance());
        int[] lens = new int[currSection.getSegmentList().size()];
        StringBuilder all = new StringBuilder();
        int count = 0;
        for (Segment t : currSection.getSegmentList()) {
            Spanned spanned;
            if (count == 0)
                spanned = Html.fromHtml(t.getText(lang));
            else
                spanned = Html.fromHtml("&nbsp;" + t.getText(lang));

            lens[count] = spanned.length();
            all.append(spanned);
            count++;
        }
        SpannableString ss = new SpannableString(all);
        int currPos = 0;
        for (int i = 0; i < currSection.getSegmentList().size(); i++) {
            Segment segment = currSection.getSegmentList().get(i);
            String s = segment.getText(lang);
            SegmentSpannable segmentSpannable = new SegmentSpannable(s,segment,onSegmentSpanClickListener);
            ss.setSpan(segmentSpannable,currPos,currPos+lens[i],0);
            currPos += lens[i];
        }
        tv.setText(ss,TextView.BufferType.SPANNABLE);
    }

    @Override
    public void add(Section object) {
        add(sections.size(), object);

    }

    @Override
    public void addAll(Collection<? extends Section> collection) {
        addAll(sections.size(), collection);

    }

    public void add(int location, Section object) {
        sections.add(location, object);
        notifyDataSetChanged();
    }

    public void addAll(int location, Collection<? extends Section> collection) {
        sections.addAll(location, collection);
        notifyDataSetChanged();
    }
}
