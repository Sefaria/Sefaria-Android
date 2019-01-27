package org.sefaria.sefaria.layouts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;

import java.util.Calendar;

/**
 * Created by nss on 2/8/16.
 */
public class LinkDraggerView extends LinearLayout {

    private float mPointerOffset;
    private int maxHeightViewId;
    private int dragViewId;
    private View dragView;

    //touch intercept vars
    private static final int MAX_CLICK_DURATION = 200;
    private long startClickTime;

    private SuperTextActivity activity;

    public LinkDraggerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        activity = (SuperTextActivity) context;

        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.LinkDraggerView);
        final int N = a.getIndexCount();
        for(int i = 0; i < N; i++){
            int attr = a.getIndex(i);
            switch(attr){
                case R.styleable.LinkDraggerView_dragView:
                    dragViewId = a.getResourceId(attr, -1);
                    break;
            }
        }
        a.recycle();

        init();
    }

    public LinkDraggerView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        init();
    }


    private void init() {

    }



    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (dragView == null) dragView = getRootView().findViewById(dragViewId);

        int index = ev.getActionIndex();
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerId = ev.getPointerId(index);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mPointerOffset = ev.getY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int tempNewHeight = (int) (dragView.getHeight() - (ev.getY() - mPointerOffset));


                setNewHeight(tempNewHeight);


                break;
            }

            case MotionEvent.ACTION_UP: {

                int tempNewHeight = (int) (dragView.getHeight() - (ev.getY() - mPointerOffset));
                if (tempNewHeight > activity.getLinkFragMaxHeight() - SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE) {
                    int endPos = activity.getLinkFragMaxHeight();

                    ValueAnimator animation = ValueAnimator.ofInt(tempNewHeight, endPos);
                    animation.setDuration(300);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int tempNewHeight = (int) animation.getAnimatedValue();
                            setNewHeight(tempNewHeight);
                        }
                    });
                    animation.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {}
                        @Override
                        public void onAnimationEnd(Animator animation) {}
                        @Override
                        public void onAnimationCancel(Animator animation) {}
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                    });
                    animation.start();
                } else if (tempNewHeight < SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE) {
                    activity.AnimateLinkFragClose(dragView);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                break;
            }
        }
        return true;
    }

    //roughly based off of:
    //https://stackoverflow.com/questions/9965695/how-to-distinguish-between-move-and-click-in-ontouchevent
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                startClickTime = Calendar.getInstance().getTimeInMillis();

            }
            case MotionEvent.ACTION_MOVE: {
                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                if(clickDuration < MAX_CLICK_DURATION) {
                    return false; //click event has occurred
                } else {
                    //drag event
                    return true;
                }
            }
            case MotionEvent.ACTION_UP: {

            }
        }
        return false;
    }

    private boolean setNewHeight(int newHeight) {
        // the new primary content height should not be less than 0 to make the
        // handler always visible
        newHeight = Math.max(0, newHeight);


        newHeight = Math.min(newHeight,activity.getLinkFragMaxHeight()); //dragView can't be taller than parentView

        //newHeight = newHeight > maxH-SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE ? maxH : newHeight;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dragView.getLayoutParams();
        params.height = newHeight;
        dragView.setLayoutParams(params);
        return true;

    }
}
