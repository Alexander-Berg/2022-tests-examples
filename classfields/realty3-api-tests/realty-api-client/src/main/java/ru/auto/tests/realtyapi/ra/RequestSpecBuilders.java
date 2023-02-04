package ru.auto.tests.realtyapi.ra;

import io.restassured.builder.RequestSpecBuilder;

import java.util.function.Consumer;

import static io.restassured.http.ContentType.JSON;

public class RequestSpecBuilders {

    public static final String AUTH_APP = "Vertis 02470ea1-2f88-49c5-9292-3479c2b7da1d regression-tests-e038aa806df76c8e817c14b4b7532900a68fb8ca";
    public static final String AUTH_NODE_JS = "Vertis nodejs-0fccb0b7a9ad06f9660c4e1114b5e7b5414e1087";
    public static final String AUTH_NGINX = "Vertis nginx-f48af8f9f6bae24cb73af77bb9d65871da9a123a";
    public static final String X_REAL_IP_HEADER_NAME = "x-real-ip";
    public static final String UBER_TRACE_ID = "uber-trace-id";
    public static final String X_REAL_IP_HEADER_VALUE = "0:0:0:0:0:0:0:1";


    private RequestSpecBuilders() {
    }

    public static Consumer<RequestSpecBuilder> platformSpec(String platform) {
        return r -> r.addHeader("X-Vertis-Platform", platform);
    }

    public static Consumer<RequestSpecBuilder> suidSpec(String suid) {
        return r -> r.addHeader("X-Suid", suid);
    }

    public static Consumer<RequestSpecBuilder> fingerpringSpec(String fingerpring) {
        return r -> r.addHeader("X-Fingerprint", fingerpring);
    }

    public static Consumer<RequestSpecBuilder> authUidSpec(String uid) {
        return requestSpec -> requestSpec.addHeader("X-Uid", uid);
    }

    public static Consumer<RequestSpecBuilder> authSpec() {
       return authSpec(AUTH_APP).andThen(platformSpec("android/d2"));
    }

    public static Consumer<RequestSpecBuilder> authNodeJsSpec() {
        return authSpec(AUTH_NODE_JS).andThen(platformSpec("Mobile"));
    }

    public static Consumer<RequestSpecBuilder> authSpec(String headerValue) {
        return requestSpec -> requestSpec.addHeader("X-Authorization", headerValue);
    }

    public static Consumer<RequestSpecBuilder> oauthSpec() {
        return requestSpec -> requestSpec.addHeader("Authorization", "OAuth AQAAAADu08P7AAAFX-OHvz8o_EABmS1tIw6B79Q");
    }

    public static Consumer<RequestSpecBuilder> oauthSpec(String oauth) {
        return requestSpec -> requestSpec.addHeader("Authorization", oauth);
    }

    public static Consumer<RequestSpecBuilder> jsonBody(String body) {
        return requestSpec -> requestSpec.setContentType(JSON).setBody(body);
    }
}
