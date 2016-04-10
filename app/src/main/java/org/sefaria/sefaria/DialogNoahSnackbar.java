package org.sefaria.sefaria;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.UpdateService;

/**
 * Created by nss on 4/8/16.
 */
public class DialogNoahSnackbar extends LinearLayout {

    private static ViewGroup currentDialogRoot;
    private Activity activity;

    public DialogNoahSnackbar(Activity activity) {
        super(activity);
        inflate(activity, R.layout.dialog_noah_snackbar, this);
        findViewById(R.id.cancel_action).setOnClickListener(cancelClick);
        this.activity = activity;
    }

    public static void showDialog(Activity activity, ViewGroup rootView) {
        DialogNoahSnackbar dnsb = new DialogNoahSnackbar(activity);
        rootView.addView(dnsb);
        currentDialogRoot = rootView;
    }

    public static void dismissCurrentDialog(ViewGroup rootView) {
        rootView.removeAllViews();
    }

    public static void dismissCurrentDialog() {
        dismissCurrentDialog(currentDialogRoot);
    }

    OnClickListener cancelClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogManager2.showDialog(activity, DialogManager2.DialogPreset.ARE_YOU_SURE_CANCEL);
        }
    };
}
