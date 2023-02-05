package com.yandex.mail.runners;

import android.os.Build;

import com.google.firebase.iid.FirebaseInstanceId;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

import androidx.annotation.NonNull;

/**
 * Base class encapsulating various Robolectric hacks
 */
public abstract class YandexMailRobolectricRunner extends RobolectricTestRunner {

    /**
     * List of additional classes to instrument
     */
    @NonNull
    private static Class[] toInstrument = new Class[]{
            FirebaseInstanceId.class
    };

    public YandexMailRobolectricRunner(@NonNull Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    @NonNull
    protected InstrumentationConfiguration createClassLoaderConfig(@NonNull FrameworkMethod method) {
        final InstrumentationConfiguration.Builder builder = new InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method));
        for (Class clazz : toInstrument) {
            builder.addInstrumentedClass(clazz.getName());
        }
        return builder.build();
    }

    /**
     * @see http://robolectric.org/configuring/ for documentation
     */
    @NonNull
    protected Config.Builder preBuildGlobalConfig() {
        return new Config.Builder()
                .setPackageName("com.yandex.mail")
                .setSdk(Build.VERSION_CODES.M);
    }
}
