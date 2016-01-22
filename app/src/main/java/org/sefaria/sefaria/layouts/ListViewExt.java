package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

/**
 * Created by nss on 1/9/16.
 * With help from Gandroid. Thank you!! https://stackoverflow.com/questions/8181828/android-detect-when-scrollview-stops-scrolling
 */
public class ListViewExt extends ListView {

    private static int SENSITIVITY = 0;

    private long lastScrollUpdate = -1;

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


    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);
        if (lastScrollUpdate == -1) {
            //onScrollStart();
            postDelayed(new ScrollStateHandler(), SENSITIVITY);
        }

        lastScrollUpdate = System.currentTimeMillis();
    }

    private void onScrollStart() {}

    private class ScrollStateHandler implements Runnable {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastScrollUpdate) > SENSITIVITY) {
                lastScrollUpdate = -1;
                if(onScrollStoppedListener!=null){
                    onScrollStoppedListener.onScrollStopped();
                }
            } else {
                postDelayed(this, SENSITIVITY);
            }
        }
    }

    public void setOnScrollStoppedListener(ListViewExt.OnScrollStoppedListener listener){
        onScrollStoppedListener = listener;
    }



}
