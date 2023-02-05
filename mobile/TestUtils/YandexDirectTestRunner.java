// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.ui.testutils;

import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;

import com.facebook.testing.screenshot.ScreenshotRunner;
import com.squareup.rx2.idler.Rx2Idler;

import io.reactivex.plugins.RxJavaPlugins;
import ru.yandex.direct.util.CustomSchedulersPlugins;

public class YandexDirectTestRunner extends AndroidJUnitRunner {
    @Override
    public void onCreate(Bundle arguments) {
        ScreenshotRunner.onCreate(this, arguments);
        arguments.putString("class", "ru.yandex.direct.ui.events.EventsTest");
        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        RxJavaPlugins.setInitComputationSchedulerHandler(Rx2Idler.create("RxJava Computation Scheduler"));
        RxJavaPlugins.setInitIoSchedulerHandler(Rx2Idler.create("RxJava IO Scheduler"));
        CustomSchedulersPlugins.setInitNetworkSchedulerHandler(Rx2Idler.create("Network Scheduler"));
        super.onStart();
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        ScreenshotRunner.onDestroy();
        super.finish(resultCode, results);
    }
}
