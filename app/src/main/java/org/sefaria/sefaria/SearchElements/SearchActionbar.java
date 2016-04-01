package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;

/**
 * Created by nss on 3/31/16.
 */
public class SearchActionbar extends LinearLayout {

    public SearchActionbar(Context context, OnClickListener closeClick, OnClickListener searchClick) {
        super(context);
        inflate(context, R.layout.search_actionbar, this);

        findViewById(R.id.close_btn).setOnClickListener(closeClick);
        findViewById(R.id.search_btn).setOnClickListener(searchClick);
    }


}
