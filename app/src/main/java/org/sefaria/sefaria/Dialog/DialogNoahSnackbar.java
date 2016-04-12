package org.sefaria.sefaria.Dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;

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
        DialogNoahSnackbar dialogNoahSnackbar = new DialogNoahSnackbar(activity);
        rootView.addView(dialogNoahSnackbar);
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
