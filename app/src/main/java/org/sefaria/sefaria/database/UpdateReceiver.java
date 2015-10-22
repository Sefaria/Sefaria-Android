package org.sefaria.sefaria.database;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

public class UpdateReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // This is the Intent to deliver to our service.
        boolean isPre = intent.getBooleanExtra("isPre", false);
        boolean userInit = intent.getBooleanExtra("userInit", false);
        Intent service = new Intent(context, UpdateService.class);
        service.putExtra("isPre", isPre);
        service.putExtra("userInit", userInit);
        startWakefulService(context, service);
    }
}
