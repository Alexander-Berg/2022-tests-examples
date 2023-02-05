// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package android.util;

/*
 * This method provides implementation of logger for classes while unit tests are running.
 *
 * See: https://stackoverflow.com/questions/36787449/how-to-mock-method-e-in-log
 */
public class Log {
    @SuppressWarnings("unused")
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("unused")
    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("unused")
    public static int w(String tag, String msg) {
        System.err.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("unused")
    public static int w(String tag, String msg, Throwable t) {
        System.err.println("WARN: " + tag + ": " + msg);
        t.printStackTrace();
        return 0;
    }

    @SuppressWarnings("unused")
    public static int e(String tag, String msg) {
        System.err.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("unused")
    public static int e(String tag, String msg, Throwable t) {
        System.err.println("ERROR: " + tag + ": " + msg);
        t.printStackTrace();
        return 0;
    }
}
