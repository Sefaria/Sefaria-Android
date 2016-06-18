package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;

/**
 * Created by nss on 6/17/16.
 */
public class SearchFilterBox extends LinearLayout{

    private Context context;
    private boolean isOpen;
    private LinearLayout filterLists;

    public SearchFilterBox(Context context) {
        super(context);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        init(context);
    }

    public SearchFilterBox(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.search_filter_box,this);
        this.context = context;
        this.filterLists = (LinearLayout) findViewById(R.id.filterLists);
        setIsOpen(false);


    }

    private void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
        if (isOpen) {
            filterLists.setVisibility(View.VISIBLE);
        } else {
            filterLists.setVisibility(View.GONE);
        }
    }


}
