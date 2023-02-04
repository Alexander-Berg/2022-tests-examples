package ru.yandex.intranet.d.web;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Adds user headers for stub auth.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class MockUser implements WebTestClientConfigurer {

    private final String uid;
    private final Long tvmClientId;

    private MockUser(String uid, Long tvmClientId) {
        this.uid = uid;
        this.tvmClientId = tvmClientId;
    }

    public static MockUser uid(String uid) {
        return new MockUser(uid, null);
    }

    public static MockUser uidTvm(String uid, long tvmClientId) {
        return new MockUser(uid, tvmClientId);
    }

    public static MockUser tvm(long tvmClientId) {
        return new MockUser(null, tvmClientId);
    }

    @Override
    public void afterConfigurerAdded(@NonNull WebTestClient.Builder builder, WebHttpHandlerBuilder httpHandlerBuilder,
                                     ClientHttpConnector connector) {
        builder.filter((request, next) ->
                next.exchange(ClientRequest.from(request).headers(headers -> {
                    if (uid != null) {
                        headers.set("X-Ya-Uid", uid);
                    }
                    if (tvmClientId != null) {
                        headers.set("X-Ya-Service-Id", String.valueOf(tvmClientId));
                    }
                }).build()));
    }

}
