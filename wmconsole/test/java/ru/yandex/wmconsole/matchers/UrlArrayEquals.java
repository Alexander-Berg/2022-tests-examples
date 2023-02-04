package ru.yandex.wmconsole.matchers;

import org.easymock.IArgumentMatcher;

/**
 * Matches an url array if it has the same length and corresponding urls are equal
 * (in terms of {@link ru.yandex.wmconsole.matchers.UrlEquals} matcher).
 *
 * @author ailyin
 *
 * @see ru.yandex.wmconsole.matchers.UrlEquals
 */
public class UrlArrayEquals implements IArgumentMatcher {
    private final UrlEquals urlMatcher = new UrlEquals("");

    private final String[] urlArray1;

    public UrlArrayEquals(String[] urlArray) {
        this.urlArray1 = urlArray;
    }

    public boolean matches(Object obj) {
        if (!(obj instanceof String[])) {
            return false;
        }

        String[] urlArray2 = (String[]) obj;
        if (urlArray1.length != urlArray2.length) {
            return false;
        }
        for (int i = 0; i < urlArray1.length; i++) {
            urlMatcher.setUrl(urlArray1[i]);
            if (!urlMatcher.matches(urlArray2[i])) {
                return false;
            }
        }

        return true;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("urlAryEq(");
        for (int i = 0; i < urlArray1.length; i++) {
            buffer.append("\"");
            buffer.append(urlArray1[i]);
            buffer.append("\"");
            if (i != urlArray1.length - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
    }
}
