package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ScrollViewExt extends ScrollView {

    private ScrollViewListener scrollViewListener = null;
    private OnScrollStoppedListener onScrollStoppedListener;

    private Runnable scrollerTask;
    private int initialPosition;

    private int newCheck = 100;

    public interface OnScrollStoppedListener{
        void onScrollStopped();
    }


    public ScrollViewExt(Context context) {
        super(context);
        init();
    }

    public ScrollViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ScrollViewExt(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scrollerTask = new Runnable() {

            public void run() {

                int newPosition = getScrollY();
                if(initialPosition - newPosition == 0){//has stopped

                    if(onScrollStoppedListener!=null){

                        onScrollStoppedListener.onScrollStopped();
                    }
                }else{
                    initialPosition = getScrollY();
                    ScrollViewExt.this.postDelayed(scrollerTask, newCheck);
                }
            }
        };
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (scrollViewListener != null) {
            scrollViewListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    public void setOnScrollStoppedListener(ScrollViewExt.OnScrollStoppedListener listener){
        onScrollStoppedListener = listener;
    }

    public void startScrollerTask(){

        initialPosition = getScrollY();
        ScrollViewExt.this.postDelayed(scrollerTask, newCheck);
    }
}
