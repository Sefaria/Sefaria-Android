package org.sefaria.sefaria.SearchElements;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 3/31/16.
 */
public class SearchActionbar extends LinearLayout {

    public SearchActionbar(Activity activity, OnClickListener closeClick, OnClickListener searchClick, OnClickListener upClick, OnClickListener downClick, int catColor, String hintText) {
        super(activity);
        inflate(activity, R.layout.search_actionbar, this);

        findViewById(R.id.close_btn).setOnClickListener(closeClick);

        if(searchClick != null) findViewById(R.id.search_btn).setOnClickListener(searchClick);
        else findViewById(R.id.search_btn).setVisibility(GONE);

        if(upClick != null) findViewById(R.id.up_button).setOnClickListener(upClick);
        else findViewById(R.id.up_button).setVisibility(GONE);

        if(downClick != null)findViewById(R.id.down_button).setOnClickListener(downClick);
        else findViewById(R.id.down_button).setVisibility(GONE);

        if (catColor == -1) catColor = R.color.system_color; //default color
        int color = getResources().getColor(catColor);
        int tintColor = Util.tintColor(color,0.3f);
        Log.d("COLOR",color + " " + tintColor);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(tintColor);
        }
        findViewById(R.id.color_bar).setBackgroundColor(color);

        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto_complete_text_view);
        if(hintText != null)
            autoCompleteTextView.setHint(Html.fromHtml(hintText));
        autoCompleteTextView.setTypeface(MyApp.getFont(MyApp.Font.CARDO));
        autoCompleteTextView.setDropDownWidth((int) Math.floor(MyApp.getScreenSizePixels().x - MyApp.convertDpToPixel(60)));
    }

    public String getText(){
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto_complete_text_view);
        return autoCompleteTextView.getText().toString();
    }




}
