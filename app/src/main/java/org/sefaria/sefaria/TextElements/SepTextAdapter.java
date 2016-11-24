package org.sefaria.sefaria.TextElements;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SepTextActivity;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.Collection;
import java.util.List;

/**
 * Created by nss on 11/29/15.
 */
public class SepTextAdapter extends ArrayAdapter<Text> {

    private static int MAX_ALPHA_NUM_LINKS = 70;
    private static int MIN_ALPHA_NUM_LINKS = 20;
    private static float MIN_ALPHA = 0.2f;
    private static float MAX_ALPHA = 0.8f;

    private SepTextActivity sepTextActivity;
    private List<Text> texts;

    private Text highlightIncomingText;
    private boolean loadedLastText = false;

    private int resourceId;
    private int preLast;

    public SepTextAdapter(SepTextActivity sepTextActivity, int resourceId, List<Text> objects) {
        super(sepTextActivity,resourceId,objects);
        this.sepTextActivity = sepTextActivity;
        this.texts = objects;
        this.resourceId = resourceId;


    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Text segment = texts.get(position);

        if (segment.isLoader()) {
            LayoutInflater inflater = (LayoutInflater)
                    sepTextActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.adapter_text_loader,null);
            view.setClickable(false);
            return view;
        }



        String enText = segment.getText(Util.Lang.EN);
        String heText = segment.getText(Util.Lang.HE);
        Util.Lang lang = sepTextActivity.getTextLang();

        boolean originalIsBILang = (lang == Util.Lang.BI);
        if (lang == Util.Lang.BI) {
            if (enText.length() == 0) {
                lang = Util.Lang.HE;
            } else if (heText.length() == 0) {
                lang = Util.Lang.EN;
            }
        }


        float linkAlpha = ((float)segment.getNumLinks() - MIN_ALPHA_NUM_LINKS) / (MAX_ALPHA_NUM_LINKS-MIN_ALPHA_NUM_LINKS);
        if (linkAlpha < MIN_ALPHA) linkAlpha = MIN_ALPHA;
        else if (linkAlpha > MAX_ALPHA) linkAlpha = MAX_ALPHA;

        if (segment.getNumLinks() == 0) linkAlpha = 0;


        boolean isCts = sepTextActivity.getIsCts();
        boolean isSideBySide = sepTextActivity.getIsSideBySide();
        if (view == null
                || (view.findViewById(R.id.he) == null && lang == Util.Lang.BI)
                || (view.findViewById(R.id.mono) == null && (lang == Util.Lang.HE || lang == Util.Lang.EN))
                || (view.findViewById(R.id.top_bottom_layout) == null && !isSideBySide && lang == Util.Lang.BI)
                || (view.findViewById(R.id.side_by_side_layout) == null && isSideBySide && lang == Util.Lang.BI)) {
            LayoutInflater inflater = (LayoutInflater)
                    sepTextActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (lang == Util.Lang.BI) {
                if (isSideBySide) view = inflater.inflate(R.layout.adapter_text_bilingual_side_by_side,null);
                else view = inflater.inflate(R.layout.adapter_text_bilingual_top_bottom,null);
            } else {
                view = inflater.inflate(R.layout.adapter_text_mono,null);
            }
        }


        boolean isCurrLinkSegment = segment.equals(sepTextActivity.getCurrLinkSegment());



        if (isCurrLinkSegment && sepTextActivity.getFragmentIsOpen()) {
            view.setBackgroundColor(Util.getColor(sepTextActivity,R.attr.text_verse_selected_bg));
        } else if(segment.equals(highlightIncomingText)){
            view.setBackgroundColor(Util.getColor(sepTextActivity,R.attr.text_verse_selected_bg));
        } else {
            view.setBackgroundColor(sepTextActivity.getResources().getColor(android.R.color.transparent));
        }

        TextChapterHeader tch = (TextChapterHeader) view.findViewById(R.id.chapHeader);
        SefariaTextView enNum = (SefariaTextView) view.findViewById(R.id.enVerseNum);
        SefariaTextView heNum = (SefariaTextView) view.findViewById(R.id.heVerseNum);

        //add padding to the last text so that you're able to get to the links for the last text
        if(loadedLastText && position == texts.size() -1 && sepTextActivity.getFragmentIsOpen()){
            view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),MyApp.getScreenSizePixels().y/4);
        }else{
            view.setPadding(view.getPaddingLeft(),view.getPaddingTop(),view.getPaddingRight(),0);
        }

        if (lang == Util.Lang.BI) {


            SefariaTextView enTv = (SefariaTextView) view.findViewById(R.id.en);
            SefariaTextView heTv = (SefariaTextView) view.findViewById(R.id.he);

            if (segment.isChapter()) {
                view.setClickable(true); //TODO why is this so weird?! I'm literally setting this to the opposite of what I want and it works

                tch.setVisibility(View.VISIBLE);
                enTv.setVisibility(View.GONE);
                heTv.setVisibility(View.GONE);
                enNum.setVisibility(View.GONE);
                heNum.setVisibility(View.GONE);

                tch.setSectionTitle(segment);
                tch.setTextSize(sepTextActivity.getTextSize());
                SefariaTextView reloadTv = (SefariaTextView) view.findViewById(R.id.reload_tv);
                if(segment.getChapterHasTexts()){
                    reloadTv.setVisibility(View.GONE);
                }else{
                    reloadTv.setVisibility(View.VISIBLE);
                }
            } else {
                view.setClickable(false);

                tch.setVisibility(View.GONE);
                enTv.setVisibility(View.VISIBLE);
                heTv.setVisibility(View.VISIBLE);
                enNum.setVisibility(View.VISIBLE);
                heNum.setVisibility(View.VISIBLE);

                enTv.setText(Html.fromHtml(Util.getBidiString(enText,Util.Lang.EN)));
                heTv.setText(Html.fromHtml(Util.getBidiString(heText,Util.Lang.HE)));

                //enTv.setTextColor(Color.parseColor("#999999"));
                enTv.setFont(Util.Lang.EN, true, sepTextActivity.getTextSize());
                //enTv.setTextSize(sepTextActivity.getTextSize());

                //heTv.setTextColor(Color.parseColor("#000000"));
                heTv.setFont(Util.Lang.HE,true, sepTextActivity.getTextSize());
                Util.Lang menuLang = Settings.getMenuLang();
                if(segment.displayNum) {
                    if(menuLang == Util.Lang.HE) {
                        heNum.setText(Util.int2heb(segment.levels[0]));
                    }else { // if == EN
                        heNum.setText("" + segment.levels[0]);
                    }
                } else
                    heNum.setText("");

                heNum.setAlpha(1);
                heNum.setFont(menuLang,false);
                enNum.setText(Util.VERSE_BULLET);
                enNum.setAlpha(linkAlpha);
                enNum.setFont(Util.Lang.HE, false);

            }

        } else { //Hebrew or English
            SefariaTextView tv = (SefariaTextView) view.findViewById(R.id.mono);
            tv.setLangGravity(lang);
            if (segment.isChapter()) {
                //view.setClickable(true);

                tch.setVisibility(View.VISIBLE);
                tv.setVisibility(View.GONE);
                enNum.setVisibility(View.GONE);
                heNum.setVisibility(View.GONE);

                tch.setSectionTitle(segment);
                tch.setTextSize(sepTextActivity.getTextSize());

            } else {
                //view.setClickable(false);

                tch.setVisibility(View.GONE);
                tv.setVisibility(View.VISIBLE);
                enNum.setVisibility(View.VISIBLE);
                heNum.setVisibility(View.VISIBLE);

                String monoText = segment.getText(lang);
                if (!originalIsBILang && monoText.length() == 0)
                    monoText = MyApp.getRString(R.string.no_text);

                if (lang == Util.Lang.HE) {
                    tv.setText(Html.fromHtml(Util.getBidiString(monoText,lang)));
                    //tv.setText(Html.fromHtml(monoText));
                    enNum.setText(Util.VERSE_BULLET);
                    enNum.setAlpha(linkAlpha);
                    enNum.setFont(Util.Lang.HE, false);
                    if(segment.displayNum) //todo maybe this should be based on menuLang like in bilang mode
                        heNum.setText(Util.int2heb(segment.levels[0]));
                    else
                        heNum.setText("");
                    heNum.setAlpha(1);
                    heNum.setFont(Util.Lang.HE, false);


                } else /*if (lang == Util.Lang.EN)*/ {
                    tv.setText(Html.fromHtml(Util.getBidiString(monoText,lang)));
                    if(segment.displayNum) //todo maybe this should be based on menuLang like in bilang mode
                        enNum.setText(""+segment.levels[0]);
                    else
                        enNum.setText("");

                    enNum.setFont(Util.Lang.EN, false);
                    enNum.setAlpha(1);
                    heNum.setText(Util.VERSE_BULLET);
                    heNum.setAlpha(linkAlpha);
                    heNum.setFont(Util.Lang.HE, false);
                }
                tv.setFont(lang,true);
                float newTextSize = sepTextActivity.getTextSize();
                if(lang == Util.Lang.EN)
                    newTextSize *=.85;

                tv.setTextSize(newTextSize);
            }
        }
        return view;
    }

    public void updateFocusedSegment() {

    }

    public void setLoadedLastText(){
        loadedLastText = true;
        notifyDataSetChanged();
    }

    public void highlightIncomingText(Text text){
        highlightIncomingText = text;
        notifyDataSetChanged();
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
            if (texts.get(i).equals(item))
                return i;
        }
        return -1;
    }
}
