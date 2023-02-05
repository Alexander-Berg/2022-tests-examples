package ru.yandex.disk.test;

import com.google.common.base.Charsets;
import ru.yandex.disk.util.Exceptions;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class TestUnits {

    public static void setDefaultCharsetLikeOnTeamcity() {
        try {
            Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
            defaultCharset.setAccessible(true);
            defaultCharset.set(null, Charsets.US_ASCII);
        } catch (Exception e) {
            Exceptions.crash(e);
        }
    }

}
