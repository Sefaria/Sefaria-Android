package org.sefaria.sefaria.LinkElements;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.LinkCount;
import org.sefaria.sefaria.database.Text;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by nss on 1/26/16.
 */
public class LinkSelectorBar extends LinearLayout {
    public static final int MAX_NUM_LINK_SELECTORS = 5;
    private LinkedList<LinkCount> linkSelectorQueue; //holds the linkCounts that display the previously selected linkCounts

    LinearLayout selectorListLayout;
    View backButton;
    SuperTextActivity activity;
    OnClickListener linkSelectorBarButtonClick;
    LinkCount currLinkCount;

    public LinkSelectorBar(SuperTextActivity activity, OnClickListener linkSelectorBarButtonClick, OnClickListener linkSelectorBackClick) {
        super(activity);
        inflate(activity, R.layout.link_selector_bar, this);
        this.activity = activity;
        this.linkSelectorBarButtonClick = linkSelectorBarButtonClick;
        selectorListLayout = (LinearLayout) findViewById(R.id.link_selection_bar_list);
        backButton = findViewById(R.id.link_back_btn);
        backButton.setOnClickListener(linkSelectorBackClick);
        linkSelectorQueue = new LinkedList<>();
    }

    public void add(LinkCount linkCount, Util.Lang lang) {

        //make sure linkCount is unique. technically should be overriding equals() in LinkCount, but this actually isn't a strict definition right n
        boolean exists = false;
        ListIterator<LinkCount> linkIt = linkSelectorQueue.listIterator(0);
        while(linkIt.hasNext()) {
            LinkCount tempLC = linkIt.next();
            if (LinkCount.pseudoEquals(tempLC,linkCount)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            if (linkSelectorQueue.size() >= MAX_NUM_LINK_SELECTORS) linkSelectorQueue.remove();
            linkSelectorQueue.add(linkCount);
        }

        update(linkCount, lang);
    }

    //this function is used to only update the language of the link selector bar
    public void update(Util.Lang lang) {
        update(currLinkCount,lang);
    }

    public void update(LinkCount currLinkCount,Util.Lang lang ) {
        this.currLinkCount = currLinkCount;
        selectorListLayout.removeAllViews();

        ListIterator<LinkCount> linkIt = linkSelectorQueue.listIterator(linkSelectorQueue.size());
        while(linkIt.hasPrevious()) {
            //add children in reverse order
            LinkCount tempLC = linkIt.previous();
            LinkSelectorBarButton lssb = new LinkSelectorBarButton(activity,tempLC,activity.getBook(),lang);
            lssb.setOnClickListener(linkSelectorBarButtonClick);
            selectorListLayout.addView(lssb);

            if (!LinkCount.pseudoEquals(tempLC,currLinkCount)) {
                lssb.setTextColor(Color.parseColor("#999999"));
            }
        }

    }


    private void flipViews() {

    }

}
