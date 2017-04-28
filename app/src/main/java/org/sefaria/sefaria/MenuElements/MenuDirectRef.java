package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.SefariaTextView;


public class MenuDirectRef extends LinearLayout{

    private Node node = null;
    private String nodePath;
    private String enTitle;
    private String heTitle;
    private Context context;
    private Book book;
    private int textNum;

    private SefariaTextView tv;
    private View colorBar;

    public MenuDirectRef(Context context, String enTitle, String heTitle, String nodePath, Book book, String colorWording, Integer textNum){
        super(context);
        inflate(context, R.layout.menu_direct_ref_button, this);


        this.context = context;
        if(textNum == null){
            this.textNum = -1;
        }else {
            this.textNum = textNum;
        }

        if(enTitle.length()>0)
            this.enTitle = enTitle;
        else
            this.enTitle = book.title;
        if(heTitle.length() >0)
            this.heTitle = heTitle;
        else
            this.heTitle = book.heTitle;
        this.nodePath = nodePath;
        this.book = book;


        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));

        this.tv = (SefariaTextView) this.findViewById(R.id.tv);
        this.colorBar = this.findViewById(R.id.color_bar);

        /*
        if(colorWording != null) {
            colorBar.setText(colorWording);
            colorBar.setTextSize(20);
        }*/

        this.setClickable(true);
        setLang(Settings.getMenuLang());
        int colorInt = book.getCatColor();
        if (colorInt != -1) {
            this.colorBar.setBackgroundColor(getResources().getColor(colorInt));
        }
        setOnClickListener(clickListener);
    }

    public void setTVGravity(int gravity){
        tv.setGravity(gravity);
    }

    public void setLongClickPinning(){
        setOnLongClickListener(longClickListener);
    }

    public void setLang(Util.Lang lang) {
        tv.setFont(lang,true,getResources().getDimension(R.dimen.menu_button_font_size), TypedValue.COMPLEX_UNIT_PX);
        if (lang == Util.Lang.HE) {
            tv.setText(heTitle);
        } else {
            tv.setText(enTitle);
        }
    }

    private Node getNode() {
        if(node == null){
            try {
                if(nodePath == null) {
                    Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
                    node = bookSettings.node;
                    textNum = bookSettings.textNum;
                }else {
                    node = Node.getNodeFromPathStr(book, nodePath);
                }

            } catch (Exception e) {
                try {
                    node = book.getTOCroots().get(0).getFirstDescendant(); //true); //tihs was used to check about if it has the segment
                    textNum = -1;
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }
        }
        return node;
    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            GoogleTracker.sendEvent("MenuDirectRef", enTitle);
            //Segment segment = new Segment();
            ;
            SuperTextActivity.startNewTextActivityIntent(context,
                    book,
                    null,
                    getNode(),
                    false,
                    null,
                    textNum
            );
        }
    };

    OnLongClickListener longClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if(Settings.RecentTexts.addPinnedBook(book.title))
                Toast.makeText(context, R.string.pinned_item,Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, R.string.remove_pinned_item,Toast.LENGTH_SHORT).show();
            return true;
        }
    };
}
