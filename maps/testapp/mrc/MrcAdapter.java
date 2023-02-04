package com.yandex.maps.testapp.mrc;

import android.content.Context;

import com.yandex.maps.auth.AccountFactory;
import com.yandex.mrc.ride.MRCFactory;

public class MrcAdapter {
    private static volatile MrcAdapter mrcAdapter;

    private MrcAdapter(final Context context) {
        MRCFactory.initialize(context);
        AccountFactory.initialize(context);
    }

    public static void initialize(Context context) {
        if (mrcAdapter == null) {
            mrcAdapter = new MrcAdapter(context);
        }
    }

    public static void onResume(Context context) {
        MRCFactory.getInstance().onResume();
    }

    public static void onPause(Context context) {
        MRCFactory.getInstance().onPause();
    }
}
