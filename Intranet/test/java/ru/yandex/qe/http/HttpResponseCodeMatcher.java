package ru.yandex.qe.http;

import org.apache.http.HttpResponse;
import org.mockito.ArgumentMatcher;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 30.06.13
 * Time: 17:28
 */
public class HttpResponseCodeMatcher implements ArgumentMatcher<HttpResponse>{


    private final int codeEqualsTo;

    public HttpResponseCodeMatcher(int codeEqualsTo) {
        this.codeEqualsTo = codeEqualsTo;
    }
    @Override
    public boolean matches(HttpResponse httpResponse) {
        if (httpResponse == null) {
            return false;
        }
        return httpResponse.getStatusLine().getStatusCode() == codeEqualsTo;
    }
}
