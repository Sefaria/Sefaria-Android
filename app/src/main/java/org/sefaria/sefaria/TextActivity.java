package org.sefaria.sefaria;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.menu.MenuState;
import org.sefaria.sefaria.menu.MenuTabController;

public class TextActivity extends AppCompatActivity {

    private MenuState menuState;
    private boolean isTextMenuVisible;
    private LinearLayout textMenuRoot;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_text);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        if (in != null) {
            menuState = in.getParcelable("menuState");
        }
        init();
    }

    private void init() {
        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);

        setTitle(menuState.getCurrNode().getTitle(menuState.getLang()));

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        CustomActionbar cab = new CustomActionbar(this, menuState.getCurrNode(),Util.EN,searchClick,null,null,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState", menuState);
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(TextActivity.this, HomeActivity.class);
            intent.putExtra("searchClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isTextMenuVisible) {
                textMenuRoot.removeAllViews();
            } else {
                TextMenuBar tmb = new TextMenuBar(TextActivity.this,textMenuBtnClick);
                textMenuRoot.addView(tmb);
            }
            isTextMenuVisible = !isTextMenuVisible;
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.en_btn:
                    Log.d("text","EN");
                    return;
                case R.id.he_btn:
                    Log.d("text","HE");
                    return;
                case R.id.bi_btn:
                    Log.d("text","BI");
                    return;
                case R.id.cts_btn:
                    Log.d("text","CTS");
                    return;
                case R.id.sep_btn:
                    Log.d("text","SEP");
                    return;
                case R.id.white_btn:
                    Log.d("text","WHITE");
                    return;
                case R.id.grey_btn:
                    Log.d("text","GREY");
                    return;
                case R.id.black_btn:
                    Log.d("text","BLACK");
                    return;
                case R.id.small_btn:
                    Log.d("text","SMALL");
                    return;
                case R.id.big_btn:
                    Log.d("text","BIG");
                    return;
            }
        }
    };

}
