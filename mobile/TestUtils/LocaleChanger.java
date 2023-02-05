package ru.yandex.direct.ui.testutils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import androidx.test.InstrumentationRegistry;

import java.util.Locale;


public class LocaleChanger {

    public static void setLocale(String language) {
        Locale locale = new Locale(language);
        Context context = InstrumentationRegistry.getTargetContext();
        Resources resource = context.getResources();
        Configuration configuration = resource.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        resource.updateConfiguration(configuration, resource.getDisplayMetrics());
    }
}
