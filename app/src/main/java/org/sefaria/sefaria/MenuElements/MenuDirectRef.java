package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;


public class MenuDirectRef extends LinearLayout{

    private Node node = null;
    private String nodePath;
    private String enTitle;
    private String heTitle;
    private Context context;
    private Book book;

    private TextView tv;
    private View colorBar;

    public MenuDirectRef(Context context, String enTitle, String heTitle, String nodePath, Book book, String colorWording){
        super(context);
        inflate(context, R.layout.button_home, this);


        this.context = context;
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

        this.tv = (TextView) this.findViewById(R.id.tv);
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

    public void setLang(Util.Lang lang) {

        if (lang == Util.Lang.HE) {
            tv.setText(heTitle);
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //tv.setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
        }
        else {
            tv.setText(enTitle);
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //tv.setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
    }

    private Node getNode() {
        if(node == null){
            try {
                if(nodePath == null) {
                    Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
                    node = bookSettings.node;
                }else
                    node = Node.getNodeFromPathStr(book,nodePath);
            } catch (Exception e) {
                try {
                    node = book.getTOCroots().get(0).getFirstDescendant();
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
            Text text = new Text(Settings.BookSettings.getSavedBook(book).tid);
            SuperTextActivity.startNewTextActivityIntent(context,book,text,getNode());
        }
    };
}
