/*package org.sefaria.sefaria;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.UpdateReceiver;
import org.sefaria.sefaria.database.UpdateService;

public class DialogManager {

    public static final int FIRST_UPDATE = 0;
    public static final int LIBRARY_EMPTY = 7;
    public static final int NEW_UPDATE = 1;
    public static final int NO_INTERNET = 2;
    public static final int USING_DATA = 3;
    public static final int FIRST_UPDATE_FAIL = 4;
    public static final int NO_NEW_UPDATE = 5;
    public static final int UPDATE_STARTED = 6;
    public static final int FIRST_TIME_TEXT = 8;
    public static final int DL_INTERNET_LOST = 9;
    public static final int DL_NOT_ENOUGH_SPACE = 10;
    public static final int DL_UNKNOWN_ERROR = 11;
    public static final int ARE_YOU_SURE_CANCEL = 12;
    public static final int CHECKING_FOR_UPDATE = 13;

    private static final int LIBRARY_EMPTY_MESSAGE = 1;
    private static final int NEW_UPDATE_MESSAGE = 2;
    private static final int NO_INTERNET_MESSAGE = 3;
    private static final int USING_DATA_MESSAGE = 4;
    private static final int FIRST_UPDATE_FAIL_MESSAGE = 5;
    private static final int NO_NEW_UPDATE_MESSAGE = 6;
    private static final int UPDATE_STARTED_MESSAGE = 7;
    private static final int LIBRARY_EMPTY_TITLE = 8;
    private static final int NEW_UPDATE_TITLE = 9;
    private static final int YES = 10;
    private static final int LATER = 11;
    private static final int NO_INTERNET_TITLE = 12;
    private static final int CONTINUE = 13;
    private static final int CANCEL = 14;
    private static final int USING_DATA_TITLE = 15;
    private static final int FIRST_UPDATE_FAIL_TITLE = 16;
    private static final int OK = 17;
    private static final int NO_NEW_UPDATE_TITLE = 18;
    private static final int UPDATE_STARTED_TITLE = 19;
    private static final int MESSAGE_CANT_STOP_NOW = 20;
    private static final int FIRST_TIME_TEXT_MESSAGE = 21;
    private static final int FIRST_TIME_TEXT_TITLE = 22;

    public static AlertDialog dialog;
    private static AlertDialog.Builder builder;

    public static boolean isShowingDialog = false; //for orientation change
    public static int currentDialog = -1;


    public static void showDialog(Activity activity,int dialogId) {
        showDialog(activity,dialogId,-1);
    }

    public static void showDialog(final Activity activity, String title, String body) {
        if (isShowingDialog) dismissCurrentDialog();

        isShowingDialog = true;

        builder = new AlertDialog.Builder(activity);
        builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismissCurrentDialog();
                UpdateService.unlockOrientation(activity);
                UpdateService.endService();
            }
        });
        Log.d("DialogManager", "body:" + body);
        builder.setMessage(body);
        builder.setTitle(title);
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    public static void showDialog(final Activity activity,int dialogId, int errorCode) {
        String errorString = "";
        if (errorCode != -1) {
            errorString = " Error Number: " + errorCode;
        }

        if (isShowingDialog) dismissCurrentDialog();

        isShowingDialog = true;
        Intent intent;
        switch (dialogId) {
            case FIRST_UPDATE:
                //click yes very quickly...
                intent = new Intent(activity,UpdateReceiver.class);
                intent.putExtra("isPre",true);
                intent.putExtra("userInit",true);
                activity.sendBroadcast(intent);
                DialogManager.showDialog(activity,DialogManager.CHECKING_FOR_UPDATE);
                if(true)
                    break;//Auto updating if don't have db (will check for wifi first).
                currentDialog = FIRST_UPDATE;
                builder = new AlertDialog.Builder(activity);
                // Add the buttons
                builder.setPositiveButton(activity.getString(R.string.YES), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(activity,UpdateReceiver.class);
                        intent.putExtra("isPre",true);
                        intent.putExtra("userInit",true);
                        activity.sendBroadcast(intent);
                        DialogManager.showDialog(activity,DialogManager.CHECKING_FOR_UPDATE);

                    }
                });
                builder.setNegativeButton(activity.getString(R.string.LATER), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                    }
                });

                builder.setTitle("Welcome to Sefaria Mobile!");
                builder.setMessage("To use the app, you need to download the Sefaria library. Do you want to do this now?\nYou can always click 'Update Library' in settings to update.");
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();

                break;
            case LIBRARY_EMPTY:
                currentDialog = LIBRARY_EMPTY;
                builder = new AlertDialog.Builder(activity);
                // Add the buttons
                builder.setPositiveButton(activity.getString(R.string.YES), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(activity,UpdateReceiver.class);
                        intent.putExtra("isPre",true);
                        intent.putExtra("userInit",true);
                        activity.sendBroadcast(intent);
                        DialogManager.showDialog(activity,DialogManager.CHECKING_FOR_UPDATE);
                    }
                });
                builder.setNegativeButton(activity.getString(R.string.LATER), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                    }
                });

                builder.setTitle(activity.getString(R.string.LIBRARY_EMPTY_TITLE));
                builder.setMessage(activity.getString(R.string.LIBRARY_EMPTY_MESSAGE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case NEW_UPDATE:
                currentDialog = NEW_UPDATE;
                builder = new AlertDialog.Builder(activity);
                // Add the buttons
                builder.setPositiveButton(activity.getString(R.string.YES), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(activity,UpdateReceiver.class);
                        intent.putExtra("isPre",true);
                        intent.putExtra("userInit",true);
                        activity.sendBroadcast(intent);
                        DialogManager.showDialog(activity,DialogManager.UPDATE_STARTED);
                    }
                });
                builder.setNegativeButton(activity.getString(R.string.LATER), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setTitle(activity.getString(R.string.NEW_UPDATE_TITLE));
                builder.setMessage(activity.getString(R.string.NEW_UPDATE_MESSAGE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case NO_INTERNET:
                currentDialog = NO_INTERNET;
                builder = new AlertDialog.Builder(activity);
                builder.setNegativeButton(activity.getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.NO_INTERNET_MESSAGE));
                builder.setTitle(activity.getString(R.string.NO_INTERNET_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case USING_DATA:
                currentDialog = USING_DATA;
                builder = new AlertDialog.Builder(activity);
                // Add the buttons
                builder.setPositiveButton(activity.getString(R.string.CONTINUE), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(activity,UpdateReceiver.class);
                        intent.putExtra("isPre",false);
                        intent.putExtra("userInit",true);
                        activity.sendBroadcast(intent);
                        DialogManager.showDialog(activity,DialogManager.CHECKING_FOR_UPDATE);
                    }
                });
                builder.setNegativeButton(activity.getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.USING_DATA_MESSAGE));
                builder.setTitle(activity.getString(R.string.USING_DATA_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case FIRST_UPDATE_FAIL:
                currentDialog = FIRST_UPDATE_FAIL;
                builder = new AlertDialog.Builder(activity);
                builder.setNegativeButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                    }
                });
                builder.setMessage(activity.getString(R.string.FIRST_UPDATE_FAIL_MESSAGE));
                builder.setTitle(activity.getString(R.string.FIRST_UPDATE_FAIL_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case NO_NEW_UPDATE:
                currentDialog = NO_NEW_UPDATE;
                builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.NO_NEW_UPDATE_MESSAGE));
                builder.setTitle(activity.getString(R.string.NO_NEW_UPDATE_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case FIRST_TIME_TEXT:
                currentDialog = FIRST_TIME_TEXT;
                builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.FIRST_TIME_TEXT_MESSAGE));
                builder.setTitle(activity.getString(R.string.FIRST_TIME_TEXT_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case DL_INTERNET_LOST:
                currentDialog = DL_INTERNET_LOST;
                builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.DL_INTERNET_LOST_MESSAGE) + errorString);
                builder.setTitle(activity.getString(R.string.DL_INTERNET_LOST_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case DL_NOT_ENOUGH_SPACE:
                currentDialog = DL_NOT_ENOUGH_SPACE;
                builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });

                builder.setMessage(activity.getString(R.string.DL_NOT_ENOUGH_SPACE_MESSAGE) + errorString);
                builder.setTitle(activity.getString(R.string.DL_NOT_ENOUGH_SPACE_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;

            case DL_UNKNOWN_ERROR:
                currentDialog = DL_UNKNOWN_ERROR;
                builder = new AlertDialog.Builder(activity);
                builder.setNeutralButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                        UpdateService.endService();
                    }
                });
                builder.setMessage(activity.getString(R.string.DL_UNKNOWN_ERROR_MESSAGE) + errorString);
                builder.setTitle(activity.getString(R.string.DL_UNKNOWN_ERROR_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;


            case ARE_YOU_SURE_CANCEL:
                currentDialog = ARE_YOU_SURE_CANCEL;
                builder = new AlertDialog.Builder(activity);
                // Add the buttons
                builder.setPositiveButton(activity.getString(R.string.YES), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (Downloader.downloadIdList.size() != 0) { //meaning the download is in progress (hopefully)
                            for (int i = 0; i < Downloader.downloadIdList.size(); i++) {
                                Downloader.manager.remove(Downloader.downloadIdList.get(i));
                            }



                        } else {
                            //you probably need to delete the db which was partially copied
                        }
                        UpdateService.endService();

                        dismissCurrentDialog();
                        UpdateService.unlockOrientation(activity);
                    }
                });
                builder.setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismissCurrentDialog();
                        //Log.d("dialog","neg aysc (??) click");
                        showDialog(activity,UPDATE_STARTED);
                    }
                });
                builder.setMessage(activity.getString(R.string.ARE_YOU_SURE_MESSAGE));
                builder.setTitle(activity.getString(R.string.ARE_YOU_SURE_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case CHECKING_FOR_UPDATE:
                currentDialog = CHECKING_FOR_UPDATE;
                builder = new AlertDialog.Builder(activity);
                builder.setTitle(activity.getString(R.string.CHECKING_FOR_UPDATE_TITLE));
                dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                break;
            case UPDATE_STARTED:
                currentDialog = UPDATE_STARTED;
                dialog = new ProgressDialog(activity);
                dialog.setTitle(activity.getString(R.string.UPDATE_STARTED_TITLE));
                dialog.setMessage(activity.getString(R.string.UPDATE_STARTED_MESSAGE));
                //((ProgressDialog) dialog).setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                //((ProgressDialog) dialog).setProgress(0);
                //((ProgressDialog) dialog).setMax(100);

                dialog.setCancelable(false);
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.CANCEL),new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //will override later
                    }
                });


                dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dia) {

                        Button b = ((ProgressDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                try {
                                    dismissCurrentDialog();
                                    showDialog(activity,ARE_YOU_SURE_CANCEL);

                                } catch (Exception e) {
                                    Toast.makeText(activity, activity.getString(R.string.update_preparing), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                dialog.show();
        }
    }

    public static void dismissCurrentDialog() {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            //whatever, no big deal
        }
        isShowingDialog = false;
        currentDialog = -1;
    }







    private static String getString(int messageType){
        boolean makeItHebrew = Util.isSystemLangHe();
        //GOOD URL: http://www.rapidmonkey.com/unicodeconverter/
        switch(messageType){
            case LIBRARY_EMPTY_MESSAGE:
                if(makeItHebrew)
                    return "(Hebrew Version:) To use the app, you need to download the BetaMidrash library. Do you want to do this now?\nYou can always click 'Update Library' in settings.";
                else
                    return "To use the app, you need to download the BetaMidrash library. Do you want to do this now?\nYou can always click 'Update Library' in settings to update.";
            case LIBRARY_EMPTY_TITLE:
                if(makeItHebrew)
                    return "\u05E1\u05E4\u05E8\u05D9\u05D9\u05D4\20\u05E8\u05D9\u05E7\u05D4";
                else
                    return "Library Empty";
            case NEW_UPDATE_MESSAGE:
                if(makeItHebrew)
                    return "/u05D4/u05D5/u05E8/u05D3 /u05E2/u05DB/u05E9/u05D9/u05D5? - Do you want to download the new Sefaria library now?\nYou can always tap 'Update Library' in settings to update.";
                else
                    return "Do you want to download the new Sefaria library now?\nYou can always tap 'Update Library' in settings to update.";
            case NEW_UPDATE_TITLE:
                if(makeItHebrew)
                    return "Update Available - /u05E2/u05D3/u05DB/u05D5/u05DF/20/u05D6/u05DE/u05D9/u05DF";
                else
                    return "Update Available";
            case YES:
                if(makeItHebrew)
                    return "Yes";
                else
                    return "Yes";
            case LATER:
                if(makeItHebrew)
                    return "Later";
                else
                    return "Later";
            case NO_INTERNET_MESSAGE:
                if(makeItHebrew)
                    return "\u05D0\u05EA/\u05D4 \u05E6\u05E8\u05D9\u05DA \u05D7\u05D9\u05D1\u05D5\u05E8 \u05DC\u05E8\u05E9\u05EA \u05DC\u05D4\u05D5\u05E8\u05D9\u05D3 \u05D0\u05EA \u05D4\u05E2\u05D3\u05DB\u05D5\u05DF";
                else
                    return "You need internet in order to download the update. Try again later.";
            case NO_INTERNET_TITLE:
                if(makeItHebrew)
                    return "No Internet";
                else
                    return "No Internet";
            case CONTINUE:
                if(makeItHebrew)
                    return "Continue";
                else
                    return "Continue";
            case CANCEL:
                if(makeItHebrew)
                    return "Cancel";
                else
                    return "Cancel";
            case USING_DATA_MESSAGE:
                if(makeItHebrew)
                    return "You are using mobile data (not Wi-Fi). The update is about 100MB. Do you want to download it anyway?";
                else
                    return "You are using mobile data (not Wi-Fi). The update is about 100MB. Do you want to download it anyway?";
            case USING_DATA_TITLE:
                if(makeItHebrew)
                    return "Using mobile data";
                else
                    return "Using mobile data";
            case FIRST_UPDATE_FAIL_MESSAGE:
                if(makeItHebrew)
                    return "You don't have internet to download the library. You can install it manually when you have internet by going to Settings and tapping 'Update Library'.";
                else
                    return "You don't have internet to download the library. You can install it manually when you have internet by going to Settings and tapping 'Update Library'.";
            case FIRST_UPDATE_FAIL_TITLE:
                if(makeItHebrew)
                    return "Can't download library";
                else
                    return "Can't download library";
            case OK:
                if(makeItHebrew)
                    return "OK";
                else
                    return "OK";
            case NO_NEW_UPDATE_MESSAGE:
                if(makeItHebrew)
                    return "There is no new update to download. Try again later.";
                else
                    return "There is no new update to download. Try again later.";
            case NO_NEW_UPDATE_TITLE:
                if(makeItHebrew)
                    return "\u05D0\u05D9\u05DF \u05E2\u05D3\u05DB\u05D5\u05DF \u05D7\u05D3\u05E9";
                else
                    return "No New Update";
            case UPDATE_STARTED_MESSAGE:
                if(makeItHebrew)
                    return "Your library is downloading and installing. This will take a few minutes";
                else
                    return "Your library is downloading and installing. This will take a few minutes.";
            case UPDATE_STARTED_TITLE:
                if(makeItHebrew)
                    return "Updating Library";
                else
                    return "Updating Library";
            case MESSAGE_CANT_STOP_NOW:
                if(makeItHebrew)
                    return "The update cannot be canceled at this point. Please wait for it to finish.";
                else
                    return "The update cannot be canceled at this point. Please wait for it to finish.";
            case FIRST_TIME_TEXT_MESSAGE:
                if(makeItHebrew)
                    return "Swipe through the help dialogs learn about the app";
                else
                    return "Go through the help dialogs learn about the app";
            case FIRST_TIME_TEXT_TITLE:
                if (makeItHebrew)
                    return "Welcome to " + MyApp.APP_NAME + "!";
                else
                    return "Welcome to " + MyApp.APP_NAME + "!";
        }

        return "";
    }
}*/
