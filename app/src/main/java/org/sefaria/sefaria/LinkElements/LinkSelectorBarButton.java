package org.sefaria.sefaria.LinkElements;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkCount;

/**
 * Created by nss on 1/21/16.
 */
public class LinkSelectorBarButton extends TextView {

    private Context context;
    private LinkCount linkCount;
    private Book book;

    public LinkSelectorBarButton(Context context, LinkCount linkCount, Book book, Util.Lang lang) {
        super(context);
        this.context = context;
        this.linkCount = linkCount;
        this.book = book;
        setLang(lang);


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.leftMargin = 10;
        this.setLayoutParams(lp);

        int padding = 15;
        this.setPadding(padding,padding,padding,padding);
        this.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        this.setTextColor(Color.parseColor("#000000"));
        this.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.button_ripple_rect));
    }

    public void setLang(Util.Lang lang) {
        setText(linkCount.getSlimmedTitle(book,lang));
    }

    public LinkCount getLinkCount() { return linkCount; }
}
