package org.sefaria.sefaria.Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SettingsActivity;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.database.UpdateReceiver;
import org.sefaria.sefaria.database.UpdateService;

/**
 * Created by nss on 4/4/16.
 */
public class DialogManager2 {

    public enum DialogPreset {
        FIRST_TIME_OPEN,
        FIRST_UPDATE,NEW_UPDATE,
        NO_NEW_UPDATE,UPDATE_STARTED,
        ARE_YOU_SURE_CANCEL,CHECKING_FOR_UPDATE,
        SWITCHING_TO_API,NO_INTERNET,DATA_CONNECTED,
        HOW_TO_REPORT_CORRECTIONS
    }

    private static Dialog currDialog;

    private static void init() {

    }

    public static void showDialog(Context context, DialogCallable dialogCallable) {
        init();
        makeDialog(context,dialogCallable);
    }

    public static void showDialog(final Activity activity, DialogPreset dialogPreset) {
        showDialog(activity,dialogPreset,null);
    }

    public static void showDialog(final Activity activity, DialogPreset dialogPreset, final Object object) {
        switch (dialogPreset) {
            case FIRST_TIME_OPEN:
                DialogManager2.showDialog(activity, new DialogCallable(MyApp.getRString(R.string.first_time_title),
                        MyApp.getRString(R.string.first_time_message),MyApp.getRString(R.string.first_time_positive),
                        MyApp.getRString(R.string.LATER),null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void positiveClick() {
                        Downloader.updateLibrary(activity,false);
                        DialogNoahSnackbar.showDialog(activity, (ViewGroup) activity.findViewById(R.id.dialogNoahSnackbarRoot));
                    }

                    @Override
                    public void negativeClick() {
                        dismissCurrentDialog();
                    }
                });
                break;
            case FIRST_UPDATE:
                DialogManager2.showDialog(activity, new DialogCallable(MyApp.getRString(R.string.UPDATE_STARTED_TITLE),
                        MyApp.getRString(R.string.UPDATE_STARTED_MESSAGE), null, MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.PROGRESS) {
                    @Override
                    public void negativeClick() {
                        try {
                            showDialog(activity, DialogPreset.ARE_YOU_SURE_CANCEL);
                        } catch (Exception e) {
                            Toast.makeText(activity, MyApp.getRString(R.string.update_preparing), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case CHECKING_FOR_UPDATE:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.CHECKING_FOR_UPDATE_TITLE), "", null, null, null, DialogCallable.DialogType.ALERT) {
                });
                break;
            case UPDATE_STARTED:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.UPDATE_STARTED_TITLE),
                        MyApp.getRString(R.string.UPDATE_STARTED_MESSAGE), null, MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.PROGRESS) {
                    @Override
                    public void negativeClick() {
                        try {
                            dismissCurrentDialog();
                            showDialog(activity, DialogPreset.ARE_YOU_SURE_CANCEL);
                        } catch (Exception e) {
                            Toast.makeText(activity, MyApp.getRString(R.string.update_preparing), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case NO_NEW_UPDATE:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.NO_NEW_UPDATE_TITLE),
                        MyApp.getRString(R.string.NO_NEW_UPDATE_MESSAGE),null,null,
                        MyApp.getRString(R.string.OK), DialogCallable.DialogType.ALERT) {
                    @Override
                    public void neutralClick() {
                        UpdateService.unlockOrientation(Downloader.activity);
                        UpdateService.endService();
                    }
                });
                break;
            case NEW_UPDATE:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.NEW_UPDATE_TITLE),
                        MyApp.getRString(R.string.NEW_UPDATE_MESSAGE),MyApp.getRString(R.string.YES),
                        MyApp.getRString(R.string.LATER),null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void positiveClick() {
                        Intent intent = new Intent(activity,UpdateReceiver.class);
                        intent.putExtra("isPre",true);
                        intent.putExtra("userInit",true);
                        activity.sendBroadcast(intent);
                        showDialog(activity, DialogManager2.DialogPreset.UPDATE_STARTED);
                    }

                    @Override
                    public void negativeClick() {
                        UpdateService.unlockOrientation(Downloader.activity);
                        UpdateService.endService();
                    }
                });
                break;

            case ARE_YOU_SURE_CANCEL:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.ARE_YOU_SURE_TITLE),
                        MyApp.getRString(R.string.ARE_YOU_SURE_MESSAGE), MyApp.getRString(R.string.YES), MyApp.getRString(R.string.no), null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void negativeClick() {
                        //nothing
                    }

                    @Override
                    public void positiveClick() {
                        if (Downloader.downloadIdList.size() != 0) { //meaning the download is in progress (hopefully)
                            for (int i = 0; i < Downloader.downloadIdList.size(); i++) {
                                Downloader.manager.remove(Downloader.downloadIdList.get(i));
                            }
                        } else {
                            //you probably need to delete the db which was partially copied
                        }
                        UpdateService.endService();
                        UpdateService.unlockOrientation(Downloader.activity);
                    }
                });
                break;
            case SWITCHING_TO_API:
                showDialog(activity, new DialogCallable(MyApp.getRString(R.string.switching_to_api),"",
                        MyApp.getRString(R.string.OK),null,null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void positiveClick() {
                        dismissCurrentDialog();
                    }
                });
                break;
            case NO_INTERNET:
                dismissCurrentDialog();
                showDialog(Downloader.activity,
                        new DialogCallable(MyApp.getRString(R.string.NO_INTERNET_TITLE), MyApp.getRString(R.string.NO_INTERNET_MESSAGE),
                                null, MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.ALERT) {
                            @Override
                            public void negativeClick() {
                                dismissCurrentDialog();
                                UpdateService.unlockOrientation(Downloader.activity);
                                UpdateService.endService();
                            }
                        });
                break;
            case DATA_CONNECTED:
                dismissCurrentDialog();
                showDialog(Downloader.activity,
                        new DialogCallable(Downloader.activity.getString(R.string.USING_DATA_TITLE), Downloader.activity.getString(R.string.USING_DATA_MESSAGE),
                                MyApp.getRString(R.string.CONTINUE), MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.ALERT) {

                            @Override
                            public void positiveClick() {
                                Intent intent = new Intent(activity, UpdateReceiver.class);
                                intent.putExtra("isPre", false);
                                intent.putExtra("userInit", true);
                                activity.sendBroadcast(intent);
                                //showDialog(activity,DialogPreset.``);
                            }

                            @Override
                            public void negativeClick() {
                                dismissCurrentDialog();
                                UpdateService.unlockOrientation(Downloader.activity);
                                UpdateService.endService();
                            }
                        });
                break;
            case HOW_TO_REPORT_CORRECTIONS:
                DialogManager2.showDialog(activity, new DialogCallable(MyApp.getRString(R.string.how_to_report_mistake),
                        MyApp.getRString(R.string.how_to_report_mistake_message_short), MyApp.getRString(R.string.OK),
                        MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void positiveClick() {
                        Text text = (Text) object;
                        String email = "corrections@sefaria.org";
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", email, null));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sefaria Text Correction from Android");
                        emailIntent.putExtra(Intent.EXTRA_TEXT,

                                MyApp.getEmailHeader()
                                        + text.getURL(true, false) + "\n\n"
                                        + Html.fromHtml(text.getText(Util.Lang.BI))
                                        + "\n\nDescribe the error: \n\n"
                        );
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                        activity.startActivity(Intent.createChooser(emailIntent, "Send email"));

                    }
                });
                break;
        }
    }

    private static void makeDialog(Context context, final DialogCallable dialogCallable) {

        // Add the buttons
        if (dialogCallable.getType() == DialogCallable.DialogType.ALERT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (dialogCallable.getPositiveText() != null) {
                builder.setPositiveButton(dialogCallable.getPositiveText(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialogCallable.positiveClick();
                    }
                });
            }
            if (dialogCallable.getNegativeText() != null) {
                builder.setNegativeButton(dialogCallable.getNegativeText(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialogCallable.negativeClick();
                    }
                });
            }
            if (dialogCallable.getNeutralText() != null) {
                builder.setNeutralButton(dialogCallable.getNeutralText(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialogCallable.neutralClick();
                    }
                });
            }
            builder.setTitle(dialogCallable.getTitle());
            builder.setMessage(dialogCallable.getMessage());
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
            currDialog = dialog;
        } else if (dialogCallable.getType() == DialogCallable.DialogType.PROGRESS) {
            final ProgressDialog dialog = new ProgressDialog(context);
            dialog.setTitle(dialogCallable.getTitle());
            dialog.setMessage(dialogCallable.getMessage());
            //((ProgressDialog) dialog).setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //((ProgressDialog) dialog).setProgress(0);
            //((ProgressDialog) dialog).setMax(100);

            dialog.setCancelable(false);
            if (dialogCallable.getPositiveText() != null) {
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, dialogCallable.getPositiveText(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //will override later
                    }
                });
            }

            if (dialogCallable.getNegativeText() != null) {
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, dialogCallable.getNegativeText(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //will override later
                    }
                });
            }

            if (dialogCallable.getNeutralText() != null) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, dialogCallable.getNeutralText(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //will override later
                    }
                });
            }


            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dia) {
                    if (dialogCallable.getPositiveText() != null) {
                        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogCallable.positiveClick();
                            }
                        });
                    }

                    if (dialogCallable.getNegativeText() != null) {
                        Button b = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogCallable.negativeClick();
                            }
                        });
                    }

                    if (dialogCallable.getNeutralText() != null) {
                        Button b = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogCallable.neutralClick();
                            }
                        });
                    }
                }
            });

            dialog.show();
            currDialog = dialog;
        }


    }

    public static void dismissCurrentDialog() {
        try {
            currDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
