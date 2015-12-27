package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 12/2/15.
 */
public class SectionView extends WebView {

    private Context context;
    private List<Text> segments;

    public SectionView(Context context, List<Text> segments, Util.Lang lang, boolean isCts, float textSize) {
        super(context);
        this.context = context;
        this.segments = segments;
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (true) {
                    Log.d("url", "URL = " + url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        String formatStr = getFormattedText(lang,isCts,textSize);
        loadData(formatStr,"text/html; charset=utf-8", "utf-8");

    }

    //generates html/css for current layout params
    private String getFormattedText(Util.Lang lang, boolean isCts, float textSize) {
        Log.d("text","start");
        String formatStr = "<html>\n" +
                " <head></head>\n" +
                " <body id='bob' style='text-align:justify;font-size:" + textSize +"px '>";
        for (int i = 0; i < segments.size(); i++) {
            Text seg = segments.get(i);
            String tempSegStr;
            if (lang == Util.Lang.EN) tempSegStr = "(" + seg.levels[0] + ") " + seg.enText;
            else if (lang == Util.Lang.HE) tempSegStr = "(" + Util.int2heb(seg.levels[0]) + ") " + seg.heText;
            else {
                tempSegStr = "(" + Util.int2heb(seg.levels[0]) + ") " + seg.heText + "<br><br>" +
                        "(" + seg.levels[0] + ") " +  seg.enText;
            }
            formatStr += "<br><br><a style='text-decoration:none;color:black' href='http://www.c.com/" + i +"'> " + tempSegStr + "</a>";
        }
        formatStr += " </body></html>";
        Log.d("text","end");
        return formatStr;
    }
}
