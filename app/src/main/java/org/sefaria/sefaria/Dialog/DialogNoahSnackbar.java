package org.sefaria.sefaria.Dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.database.Database;

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
        dismissCurrentDialog(rootView);
        currentDialogRoot = rootView;
        DialogNoahSnackbar dnsb = new DialogNoahSnackbar(activity);
        rootView.addView(dnsb);
    }

    public static void dismissCurrentDialog(ViewGroup rootView) {
        if (rootView != null) {
            rootView.removeAllViews();
        }
    }

    public static void dismissCurrentDialog() {
        dismissCurrentDialog(currentDialogRoot);
    }


    public static void checkCurrentDialog(Activity activity, ViewGroup viewGroup) {
        try {
            if (Database.isDownloadingDatabase) {
                DialogNoahSnackbar.showDialog(activity, viewGroup);
            } else {
                DialogNoahSnackbar.dismissCurrentDialog(viewGroup);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    OnClickListener cancelClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogManager2.showDialog(activity, DialogManager2.DialogPreset.ARE_YOU_SURE_CANCEL);
        }
    };
}
