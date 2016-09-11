package org.sefaria.sefaria.TextElements;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ListView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;

/**
 * Created by nss on 1/9/16.
 * With help from Gandroid. Thank you!! https://stackoverflow.com/questions/8181828/android-detect-when-scrollview-stops-scrolling
 */
public class TextListView extends ListView implements ScaleGestureDetector.OnScaleGestureListener {
    private boolean isZoomZooming;
    private int sensitivity = 0;

    private long lastScrollUpdate = -1;
    private Point clickPos;
    ScaleGestureDetector scaleDetector =
            new ScaleGestureDetector(getContext(), this);
    private SuperTextActivity superTextActivity;

    public interface OnScrollStoppedListener{
        void onScrollStopped();
    }

    public interface OnScrollStartedListener{
        void onScrollStarted();
    }

    private OnScrollStoppedListener onScrollStoppedListener;
    private OnScrollStartedListener onScrollStartedListener;

    public TextListView(Context context) {
        super(context);
        this.superTextActivity = (SuperTextActivity) context;
        isZoomZooming = false;
    }

    public TextListView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        this.superTextActivity = (SuperTextActivity) context;
        isZoomZooming = false;
    }

    public TextListView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
        this.superTextActivity = (SuperTextActivity) context;
        isZoomZooming = false;
    }


    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {

        super.onScrollChanged(x, y, oldX, oldY);
        if (lastScrollUpdate == -1) {
            if(onScrollStartedListener!=null){
                onScrollStartedListener.onScrollStarted();
            }
            postDelayed(new ScrollStateHandler(), sensitivity);
        }

        lastScrollUpdate = System.currentTimeMillis();
    }

    private class ScrollStateHandler implements Runnable {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastScrollUpdate) > sensitivity) {
                lastScrollUpdate = -1;
                if(onScrollStoppedListener!=null){
                    onScrollStoppedListener.onScrollStopped();
                }
            } else {
                postDelayed(this, sensitivity);
            }
        }
    }

    public void setOnScrollStoppedListener(TextListView.OnScrollStoppedListener listener){
        onScrollStoppedListener = listener;
    }

    public void setOnScrollStartedListener(TextListView.OnScrollStartedListener listener){
        onScrollStartedListener = listener;
    }

    public View getViewByPosition(int pos) {
        final int firstListItemPosition = this.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + this.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return this.getAdapter().getView(pos, null, this);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return this.getChildAt(childIndex);
        }
    }

    public void setClickPos(Point pos) {
        clickPos = pos;
    }

    public Point getClickPos() { return clickPos; }

    /*
    GESTURES
     */

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = detector.getScaleFactor();
        float currTextSize = superTextActivity.getTextSize();
        if (scale > 1) {
            scale = 1.005f;
        } else if (scale < 1) {
            scale = 0.995f;
        }

        float newTextSize = currTextSize * scale;

        float max_font = MyApp.getRDimen(R.dimen.max_text_font_size);
        float min_font = MyApp.getRDimen(R.dimen.min_text_font_size);
        //Log.d("zoom", "min: " + min_font + " max: " + max_font + " current: " + currentSize);
        if ((newTextSize <  max_font   && newTextSize > min_font)
                ||(newTextSize >= max_font && scale < 1)
                || (newTextSize <= min_font && scale > 1)) {
            superTextActivity.setTextSize(newTextSize);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        isZoomZooming = true;
        setClickable(false);
        superTextActivity.unregisterForContextMenu(this);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isZoomZooming = false;
        setClickable(true);
       superTextActivity.registerForContextMenu(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        return isZoomZooming || super.onTouchEvent(ev);
    }

}
