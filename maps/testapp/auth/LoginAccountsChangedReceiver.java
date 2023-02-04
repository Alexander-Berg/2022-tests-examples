package com.yandex.maps.testapp.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.yandex.passport.api.PassportUid;

public class LoginAccountsChangedReceiver extends BroadcastReceiver {
    private static final String TAG = LoginAccountsChangedReceiver.class.getCanonicalName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "action is null in intent");
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "extras is null in intent");
            return;
        }

        Log.e(TAG, "Action is " + action);
        PassportUid passportUid = PassportUid.Factory.fromExtras(extras);

        if (action.equals("com.yandex.passport.client.ACCOUNT_REMOVED")) {
            AuthUtil.removeAccount(passportUid);
        }
    }
}
