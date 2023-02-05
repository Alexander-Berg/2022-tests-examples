package ru.yandex.disk.remote;

import com.google.common.base.Charsets;
import ru.yandex.disk.util.Exceptions;
import ru.yandex.disk.util.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;

public class RemoteRepoTestHelper {

    public static <I, E extends Exception> RemoteRepoOnNext<I, E> stubOnNext() {
        return item -> {};
    }

    public static String load(String fileName) {
        try {
            InputStream in = RemoteRepoListTrashMethodTest.class.getResourceAsStream(fileName);
            if (in == null) {
                throw new FileNotFoundException(fileName);
            }
            return IOHelper.readInputStream(in);
        } catch (IOException e) {
            return Exceptions.crashValue(e);
        }
    }

    public static String encoded(String s) {
        try {
            return URLEncoder.encode(s, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return Exceptions.crashValue(e);
        }
    }

    public static long time(String date) throws ParseException {
        return DateFormat.asLong(date);
    }
}
