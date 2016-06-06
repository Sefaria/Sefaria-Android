package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;

/**
 * Created by nss on 1/9/16.
 * With help from Gandroid. Thank you!! https://stackoverflow.com/questions/8181828/android-detect-when-scrollview-stops-scrolling
 */
public class ListViewExt extends ListView {

    private int sensitivity = 0;

    private long lastScrollUpdate = -1;
    private Point clickPos;

    public interface OnScrollStoppedListener{
        void onScrollStopped();
    }

    private OnScrollStoppedListener onScrollStoppedListener;

    public ListViewExt(Context context) {
        super(context);
    }

    public ListViewExt(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
    }

    public ListViewExt(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
    }


    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);
        if (lastScrollUpdate == -1) {
            //onScrollStart();
            postDelayed(new ScrollStateHandler(), sensitivity);
        }

        lastScrollUpdate = System.currentTimeMillis();
    }

    private void onScrollStart() {}

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

    public void setOnScrollStoppedListener(ListViewExt.OnScrollStoppedListener listener){
        onScrollStoppedListener = listener;
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



}
