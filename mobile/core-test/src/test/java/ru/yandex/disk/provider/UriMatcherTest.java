package ru.yandex.disk.provider;

import android.net.Uri;
import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.disk.test.TestCase2;

public class UriMatcherTest extends TestCase2 {

    private UriMatcher matcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
    }

    @Test
    public void testMatch() throws Exception {
        matcher.addURI("authority", "path/**?list", 2);
        matcher.addURI("authority", "path/**", 1);

        testMatch(UriMatcher.NO_MATCH, "scheme://path");
        testMatch(UriMatcher.NO_MATCH, "scheme://authority/path");
        testMatch(UriMatcher.NO_MATCH, "scheme://authority/path/");
        testMatch(1, "scheme://authority/path/1");
        testMatch(1, "scheme://authority/path/1/2/3");
        testMatch(1, "scheme://authority/path/1/2/3/list");
        testMatch(2, "scheme://authority/path/1/2/3/?list");
        testMatch(2, "scheme://authority/path?list");
        testMatch(2, "scheme://authority/path?list&filter=A");
        testMatch(UriMatcher.NO_MATCH, "scheme://authority/path?listfilter");
        testMatch(2, "scheme://authority/path?list");

    }

    private void testMatch(int expected, String uri) {
        Assert.assertEquals(expected, matcher.match(Uri.parse(uri)));
    }

}
