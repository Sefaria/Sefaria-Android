package org.sefaria.sefaria.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Huffman;

public class SplashScreenActivity extends AppCompatActivity {

    static final private long MIN_SPLASH_TIME = 200;
    static private long startTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        new StartUp().execute();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        MyApp.handleIncomingURL(this, intent);
    }

    private class StartUp extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Huffman.makeTree(false);
            while (MIN_SPLASH_TIME > (System.currentTimeMillis() - startTime)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Book book;
            try {
                book = new Book(Settings.getLastBook());
            } catch (Exception e) {
                book = null;
            }
            if(MyApp.handleIncomingURL(SplashScreenActivity.this, getIntent()))
                return;
            SuperTextActivity.startNewTextActivityIntent(SplashScreenActivity.this,book,false);
            SplashScreenActivity.this.finish();
        }
    }
}
