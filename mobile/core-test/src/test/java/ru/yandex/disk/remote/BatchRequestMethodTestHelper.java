package ru.yandex.disk.remote;

import okhttp3.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class BatchRequestMethodTestHelper {
    public static void assertBatchRequest(final Request request, final Map<String, String> query) {
        assertThat(request.method(), equalTo("POST"));
        assertThat(request.url().url().getPath(),
                equalTo("/v1/batch/request"));
        assertThat(query.get("fields"), equalTo("items.code,items.body"));
        assertThat(request.body().contentType().toString(), equalTo("application/json; charset=utf-8"));
    }

    public static void assertCorrectSubRequest(final JSONObject request,
                                               final String method, final String url)
            throws JSONException {
        assertThat(request.getString("method"), equalTo(method));
        final String requestUrl = request.getString("relative_url");
        assertThat(requestUrl, equalTo(url));
    }

    public static JSONObject getSubRequest(final String requestBody, final int which)
            throws JSONException {
        final JSONObject body = new JSONObject(requestBody);
        return body.getJSONArray("items").getJSONObject(which);
    }
}
