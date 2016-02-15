package org.sefaria.sefaria.layouts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by nss on 2/8/16.
 */
public class LinkDraggerView extends RelativeLayout {

    private float mPointerOffset;
    private int maxHeightViewId;
    private int dragViewId;
    private View maxHeightView;
    private View dragView;

    private SuperTextActivity activity;
    //velocity info found here: https://developer.android.com/training/gestures/movement.html#velocity
    private VelocityTracker mVelocityTracker = null;
    private Queue<Float> velocityQ;

    private boolean dragDisabled;

    public LinkDraggerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        activity = (SuperTextActivity) context;
        dragDisabled = false;

        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.LinkDraggerView);
        final int N = a.getIndexCount();
        for(int i = 0; i < N; i++){
            int attr = a.getIndex(i);
            switch(attr){
                case R.styleable.LinkDraggerView_dragView:
                    dragViewId = a.getResourceId(attr, -1);
                    break;
                case R.styleable.LinkDraggerView_maxHeightView:
                    maxHeightViewId = a.getResourceId(attr,-1);
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


        if (maxHeightView == null) maxHeightView = getRootView().findViewById(maxHeightViewId);
        if (dragView == null) dragView = getRootView().findViewById(dragViewId);

        int index = ev.getActionIndex();
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerId = ev.getPointerId(index);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if(mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                    velocityQ = new LinkedList<>();
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    // Reset the velocity tracker back to its initial state.
                    velocityQ.clear();
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(ev);


                mPointerOffset = ev.getY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                mVelocityTracker.addMovement(ev);
                // When you want to determine the velocity, call
                // computeCurrentVelocity(). Then call getXVelocity()
                // and getYVelocity() to retrieve the velocity for each pointer ID.
                mVelocityTracker.computeCurrentVelocity(100);

                velocityQ.add(mVelocityTracker.getYVelocity(pointerId));
                if (velocityQ.size() > 5) velocityQ.remove();

                float vy = 0;// = mVelocityTracker.getYVelocity(pointerId);
                for (Float tempVy : velocityQ) {
                    vy += tempVy.floatValue();
                }



                Log.d("blah", "Y velocity: " + vy);
                int tempNewHeight = (int) (dragView.getHeight() - (ev.getY() - mPointerOffset));

                //TODO this is way too jittery
                if ((tempNewHeight > activity.getLinkFragMaxHeight() - SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE && vy < 0)||
                        (tempNewHeight < SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE && vy > 0)) {
                    int endPos = tempNewHeight < SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE ? 0 : activity.getLinkFragMaxHeight();

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
                        public void onAnimationStart(Animator animation) {dragDisabled = true;}
                        @Override
                        public void onAnimationEnd(Animator animation) {dragDisabled = false;}
                        @Override
                        public void onAnimationCancel(Animator animation) {}
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                    });
                    animation.start();
                } else if (!dragDisabled){
                    setNewHeight(tempNewHeight);
                }



                break;
            }

            case MotionEvent.ACTION_UP: {
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                // Return a VelocityTracker object back to be re-used by others.
                mVelocityTracker.recycle();
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                break;
            }
        }
        return true;
    }

    private boolean setNewHeight(int newHeight) {
        // the new primary content height should not be less than 0 to make the
        // handler always visible
        newHeight = Math.max(0, newHeight);


        newHeight = Math.min(newHeight,activity.getLinkFragMaxHeight()); //dragView can't be taller than parentView

        //newHeight = newHeight > maxH-SuperTextActivity.MAX_LINK_FRAG_SNAP_DISTANCE ? maxH : newHeight;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dragView.getLayoutParams();
        if (newHeight >= 0) {
            params.height = newHeight;
        }
        dragView.setLayoutParams(params);
        return true;

    }
}
