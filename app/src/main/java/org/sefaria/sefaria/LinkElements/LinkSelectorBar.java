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

    //TouchEvent
    private float lastTouchX,lastTouchY;
    private int activePointerId;
    private static final int INVALID_POINTER_ID = -1;

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

    public void add(LinkCount linkCount) {

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

        update(linkCount);
    }

    public void update(LinkCount currLinkCount ) {
        this.currLinkCount = currLinkCount;
        selectorListLayout.removeAllViews();

        ListIterator<LinkCount> linkIt = linkSelectorQueue.listIterator(linkSelectorQueue.size());
        while(linkIt.hasPrevious()) {
            //add children in reverse order
            LinkCount tempLC = linkIt.previous();
            LinkSelectorBarButton lssb = new LinkSelectorBarButton(activity,tempLC,activity.getBook());
            lssb.setOnClickListener(linkSelectorBarButtonClick);
            selectorListLayout.addView(lssb);

            if (!LinkCount.pseudoEquals(tempLC,currLinkCount)) {
                lssb.setTextColor(Color.parseColor("#999999"));
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        //mScaleDetector.onTouchEvent(ev);

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);

                // Remember where we started (for dragging)
                lastTouchX = x;
                lastTouchY = y;
                // Save the ID of this pointer (for dragging)
                activePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                final int pointerIndex =
                        MotionEventCompat.findPointerIndex(ev, activePointerId);

                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);

                // Calculate the distance moved
                final float dx = x - lastTouchX;
                final float dy = y - lastTouchY;

                //mPosX += dx;
                //mPosY += dy;

                invalidate();

                // Remember this touch position for the next move event
                lastTouchX = x;
                lastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                activePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                activePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = MotionEventCompat.getX(ev, newPointerIndex);
                    lastTouchY = MotionEventCompat.getY(ev, newPointerIndex);
                    activePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

}
