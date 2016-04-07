package org.sefaria.sefaria;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.UpdateReceiver;
import org.sefaria.sefaria.database.UpdateService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nss on 4/4/16.
 */
public class DialogManager2 {

    public enum DialogPreset {
        FIRST_UPDATE,NEW_UPDATE,
        NO_NEW_UPDATE,UPDATE_STARTED,
        ARE_YOU_SURE_CANCEL,CHECKING_FOR_UPDATE
    }

    private static Dialog currDialog;
    private static boolean isShowingDialog;

    private static void init() {

    }

    public static void showDialog(Context context, DialogCallable dialogCallable) {
        init();
        makeDialog(context,dialogCallable);
    }

    public static void showDialog(final Context context, DialogPreset dialogPreset) {
        switch (dialogPreset) {
            case FIRST_UPDATE:
                DialogManager2.showDialog(context, new DialogCallable(MyApp.getRString(R.string.UPDATE_STARTED_TITLE),
                        MyApp.getRString(R.string.UPDATE_STARTED_MESSAGE), null, MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.PROGRESS) {
                    @Override
                    public void negativeClick() {
                        try {
                            showDialog(context, DialogPreset.ARE_YOU_SURE_CANCEL);
                        } catch (Exception e) {
                            Toast.makeText(context, MyApp.getRString(R.string.update_preparing), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case CHECKING_FOR_UPDATE:
                showDialog(context, new DialogCallable(MyApp.getRString(R.string.CHECKING_FOR_UPDATE_TITLE), "", null, null, null, DialogCallable.DialogType.ALERT) {
                });
                break;
            case UPDATE_STARTED:
                showDialog(context, new DialogCallable(MyApp.getRString(R.string.UPDATE_STARTED_TITLE),
                        MyApp.getRString(R.string.UPDATE_STARTED_MESSAGE), null, MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.PROGRESS) {
                    @Override
                    public void negativeClick() {
                        try {
                            dismissCurrentDialog();
                            showDialog(context, DialogPreset.ARE_YOU_SURE_CANCEL);
                        } catch (Exception e) {
                            Toast.makeText(context, MyApp.getRString(R.string.update_preparing), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case NO_NEW_UPDATE:
                showDialog(context, new DialogCallable(MyApp.getRString(R.string.NO_NEW_UPDATE_TITLE),
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
                showDialog(context, new DialogCallable(MyApp.getRString(R.string.NEW_UPDATE_TITLE),
                        MyApp.getRString(R.string.NEW_UPDATE_MESSAGE),MyApp.getRString(R.string.YES),
                        MyApp.getRString(R.string.LATER),null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void positiveClick() {
                        Intent intent = new Intent(context,UpdateReceiver.class);
                        intent.putExtra("isPre",true);
                        intent.putExtra("userInit",true);
                        context.sendBroadcast(intent);
                        showDialog(context, DialogManager2.DialogPreset.UPDATE_STARTED);
                    }

                    @Override
                    public void negativeClick() {
                        UpdateService.unlockOrientation(Downloader.activity);
                        UpdateService.endService();
                    }
                });
                break;

            case ARE_YOU_SURE_CANCEL:
                showDialog(context, new DialogCallable(MyApp.getRString(R.string.ARE_YOU_SURE_TITLE),
                        MyApp.getRString(R.string.ARE_YOU_SURE_MESSAGE), MyApp.getRString(R.string.YES), MyApp.getRString(R.string.no), null, DialogCallable.DialogType.ALERT) {
                    @Override
                    public void negativeClick() {
                        //go back to previous dialog (which i'm assuming is update_started)
                        DialogManager2.showDialog(context,DialogPreset.UPDATE_STARTED);
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
        isShowingDialog = false;
    }
}
