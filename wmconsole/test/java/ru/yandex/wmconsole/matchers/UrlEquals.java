package ru.yandex.wmconsole.matchers;

import org.easymock.IArgumentMatcher;
import ru.yandex.wmtools.common.Constants;

/**
 * Matches an url if it's equal to an expected url ("http://" prefixes are ignored)
 *
 * @author ailyin
 */
public class UrlEquals implements IArgumentMatcher, Constants {
    private String url1;

    public UrlEquals(String url1) {
        this.url1 = url1;
    }

    void setUrl(String url) {
        url1 = url;
    }

    public boolean matches(Object obj) {
        if (!(obj instanceof String)) {
            return false;
        }

        String url2 = (String) obj;
        url1 = addHTTP(url1);
        url2 = addHTTP(url2);

        return url1.equals(url2);
    }

    private String addHTTP(String url) {
        return url.startsWith(HTTP_PREFIX) ? url : HTTP_PREFIX + url;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("urlEq(\"");
        buffer.append(url1);
        buffer.append("\")");
    }
}
