package android.util;

public class Log {

    public static void v(String tag, String message) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void v(String tag, String message, Throwable th) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void d(String tag, String message) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void d(String tag, String message, Throwable th) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void w(String tag, String message) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void w(String tag, Throwable tr) {
        System.out.println(String.format("Tag : %s, message: %s", tag, tr.getMessage()));
    }

    public static void w(String tag, String message, Throwable th) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void i(String tag, String message) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void i(String tag, String message, Throwable th) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void e(String tag, String message) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

    public static void e(String tag, String message, Throwable th) {
        System.out.println(String.format("Tag : %s, message: %s", tag, message));
    }

}
