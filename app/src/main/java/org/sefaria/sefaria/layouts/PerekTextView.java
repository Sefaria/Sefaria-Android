/*package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.SegmentSpannable;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.util.List;

public class PerekTextView extends SefariaTextView {

    private List<Text> textList;
    private int relativeTop;

    private boolean isLoader = false;

    public PerekTextView(Context context, boolean isLoader) {
        super(context);
        this.isLoader = isLoader;
    }

    public PerekTextView(Context context, List<Text> textList,Util.Lang lang, float textSize) {
        super(context);
        this.textList = textList;

        setLang(lang);
        setTextSize(textSize);
        setTextColor(Util.getColor(context, R.attr.text_color_main));
    }

    public void setLang(Util.Lang lang) {

        setFont(lang, true);

        boolean isFirst = true;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        setMovementMethod(LinkMovementMethod.getInstance());

        for (int i = 0; i < textList.size(); i++) {
            Text segment = textList.get(i);
            String words;
            if (lang == Util.Lang.BI) {
                if (segment.displayNum) {
                    words = "(" + Util.int2heb(segment.levels[0]) + ") " + segment.getText(Util.Lang.HE)
                            + "\n\n(" + segment.levels[0] + ") " + segment.getText(Util.Lang.EN);
                } else {
                    words = segment.getText(Util.Lang.HE) + "\n\n" + segment.getText(Util.Lang.EN);
                }

            } else { //mono lingual
                if (segment.displayNum) {
                    words = "(" + Util.int2heb(segment.levels[0]) + ") " + segment.getText(lang);
                } else {
                    words = segment.getText(lang);
                }
            }
            if (!isFirst)
                words = " " + words;


            SpannableString ss = new SpannableString(Html.fromHtml(words));
            ss.setSpan(new SegmentSpannable(words), 0, ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }

        setText(ssb, BufferType.SPANNABLE);
    }

    public void setRelativeTop(int newTop) {
        this.relativeTop = newTop;
    }

    public boolean getIsLoader() { return isLoader; }
}*/