package org.sefaria.sefaria.SearchElements;

import android.content.Context;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;

/**
 * Created by nss on 3/31/16.
 */
public class SearchActionbar extends LinearLayout {

    public SearchActionbar(Context context, OnClickListener closeClick, OnClickListener searchClick, OnClickListener upClick, OnClickListener downClick,int catColor,String hintText) {
        super(context);
        inflate(context, R.layout.search_actionbar, this);

        findViewById(R.id.close_btn).setOnClickListener(closeClick);

        if(searchClick != null) findViewById(R.id.search_btn).setOnClickListener(searchClick);
        else findViewById(R.id.search_btn).setVisibility(GONE);

        if(upClick != null) findViewById(R.id.up_button).setOnClickListener(upClick);
        else findViewById(R.id.up_button).setVisibility(GONE);

        if(downClick != null)findViewById(R.id.down_button).setOnClickListener(downClick);
        else findViewById(R.id.down_button).setVisibility(GONE);

        if (catColor != -1)
            findViewById(R.id.color_bar).setBackgroundColor(getResources().getColor(catColor));

        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto_complete_text_view);
        if(hintText != null)
            autoCompleteTextView.setHint(hintText);
        autoCompleteTextView.setTypeface(MyApp.getFont(MyApp.Font.QUATTROCENTO));
        autoCompleteTextView.setDropDownWidth((int) Math.floor(MyApp.getScreenSizePixels().x - MyApp.convertDpToPixel(60)));
    }

    public String getText(){
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto_complete_text_view);
        return autoCompleteTextView.getText().toString();
    }




}
