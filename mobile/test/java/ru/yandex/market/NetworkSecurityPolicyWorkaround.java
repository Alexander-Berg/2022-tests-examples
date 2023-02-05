package ru.yandex.market;

import android.os.Build;
import android.security.NetworkSecurityPolicy;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(NetworkSecurityPolicy.class)
public class NetworkSecurityPolicyWorkaround {

    @Implementation
    @SuppressWarnings("unused")
    public static NetworkSecurityPolicy getInstance() {
        //noinspection OverlyBroadCatchBlock
        try {
            Class<?> shadow = Class.forName("android.security.NetworkSecurityPolicy");
            return (NetworkSecurityPolicy) shadow.newInstance();
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    @Implementation
    @SuppressWarnings("unused")
    public boolean isCleartextTrafficPermitted(String hostname) {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.P);
    }
}