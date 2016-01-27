package org.sefaria.sefaria.LinkElements;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

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

    public LinkSelectorBarButton(Context context, LinkCount linkCount, Book book) {
        super(context);
        this.context = context;
        this.linkCount = linkCount;
        this.book = book;
        setLang(Util.Lang.EN);

        int padding = 15;
        this.setPadding(padding,padding,padding,padding);
        this.setTextColor(Color.parseColor("#000000"));
    }

    public void setLang(Util.Lang lang) {
        setText(linkCount.getSlimmedTitle(book,lang));
    }

    public LinkCount getLinkCount() { return linkCount; }
}
