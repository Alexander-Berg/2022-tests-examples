package ru.yandex.market;

import android.net.Network;

import org.robolectric.annotation.Implements;

@Implements(Network.class)
public class ShadowNetwork {
    @SuppressWarnings("unused")
    public void __constructor__(Class<? extends Network> arg0) {
    }
}
