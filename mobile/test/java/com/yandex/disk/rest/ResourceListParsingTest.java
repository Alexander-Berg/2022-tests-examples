package com.yandex.disk.rest;

import com.yandex.disk.rest.json.Resource;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealInterceptorChain;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class ResourceListParsingTest {

    private FakeOkHttpInterceptor fakeHttp;
    private RestClient restApiClient;

    @Test
    public void shouldParseExifDateTime() throws Exception {
        final String body = readResourceListSampleString();
        fakeHttp.addResponse(200, body);

        final Resource resources = restApiClient.getResources(new ResourcesArgs.Builder().build());
        final Resource resource = resources.getResourceList().getItems().get(0);
        assertThat(resource.getExifTime(), greaterThan(0L));
    }

    private String readResourceListSampleString() throws IOException {
        final StringBuilder builder = new StringBuilder();
        try (final BufferedReader reader =
                     new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/ResourceListSample.json")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @Before
    public void setUp() throws Exception {
        fakeHttp = new FakeOkHttpInterceptor();
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final DisableHttpInterceptor interceptor = new DisableHttpInterceptor(fakeHttp);
        builder.addInterceptor(interceptor);

        restApiClient = new RestClient(new Credentials("test", "test"), builder, "http://test");
        interceptor.setInterceptors(builder.interceptors());
    }

    private static class DisableHttpInterceptor implements Interceptor {
        private final Interceptor requestHandler;
        private List<Interceptor> interceptors;

        private DisableHttpInterceptor(final Interceptor requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        public Response intercept(final Chain chain) throws IOException {
            try {
                final Request request = chain.request();
                final RealInterceptorChain shortChain =
                        new RealInterceptorChain(interceptors, null, null,
                                null, 0, request, null, null,
                                0, 0, 0);
                return shortChain.proceed(request);
            } catch (final IOException e) {
                throw e;
            }
        }

        public void setInterceptors(List<Interceptor> interceptors) {
            interceptors = interceptors.subList(1, 2);
            interceptors.add(requestHandler);
            this.interceptors = interceptors;
        }
    }
}
