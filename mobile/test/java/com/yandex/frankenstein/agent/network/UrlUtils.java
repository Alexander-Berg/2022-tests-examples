package com.yandex.frankenstein.agent.network;

import java.net.URL;
import java.net.URLConnection;

public class UrlUtils {

    public static URLConnection openConnection(final MockHostUrlStreamHandler urlStreamHandler, final URL url) {
        return urlStreamHandler.openConnection(url);
    }
}