package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;

/**
 * Created by nss on 2/8/16.
 */
public class LinkDraggerView extends RelativeLayout {
    private float mPointerOffset;
    private int maxHeightViewId;
    private int dragViewId;
    private View maxHeightView;
    private View dragView;

    public LinkDraggerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

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
        // Let the ScaleGestureDetector inspect all events.
        //mScaleDetector.onTouchEvent(ev);

        if (maxHeightView == null) maxHeightView = getRootView().findViewById(maxHeightViewId);
        if (dragView == null) dragView = getRootView().findViewById(dragViewId);


        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mPointerOffset = ev.getY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                setNewHeight((int)(dragView.getHeight() - (ev.getY() - mPointerOffset)));
                break;
            }

            case MotionEvent.ACTION_UP: {
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

    private boolean setNewHeight(int newHeight) {
        // the new primary content height should not be less than 0 to make the
        // handler always visible
        newHeight = Math.max(0, newHeight);
        newHeight = Math.min(newHeight,getRootView().findViewById(R.id.root).getHeight()-getRootView().findViewById(R.id.actionbarRoot).getHeight()-getHeight()); //dragView can't be taller than parentView
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) dragView.getLayoutParams();
        if (newHeight >= 0) {
            params.height = newHeight;
        }
        dragView.setLayoutParams(params);
        return true;

    }
}
