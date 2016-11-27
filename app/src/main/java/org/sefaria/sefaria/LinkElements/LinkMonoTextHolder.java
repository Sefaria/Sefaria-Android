package org.sefaria.sefaria.LinkElements;

import android.view.View;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 2/9/16.
 */
public class LinkMonoTextHolder extends LinkTextHolder{

    public SefariaTextView monoTv;

    public LinkMonoTextHolder(View v, List<Segment> itemList, SuperTextActivity activity) {
        super(v,itemList,activity);
        monoTv = (SefariaTextView) v.findViewById(R.id.monoTv);
    }
}
