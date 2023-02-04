package ru.auto.tests.desktop.matchers;

import com.google.gson.Gson;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.devtools.v101.network.model.Request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.tests.desktop.utils.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;

public class RequestHasBodyMatcher extends TypeSafeMatcher<Request> {

    private final boolean convertToJson;
    private final Matcher<String> bodyMatcher;

    private RequestHasBodyMatcher(Matcher<String> bodyMatcher, boolean convertToJson) {
        this.bodyMatcher = bodyMatcher;
        this.convertToJson = convertToJson;
    }

    @Override
    protected boolean matchesSafely(Request request) {
        String body = request.getPostData()
                .orElse("")
                .replace("site-info=", "");

        String decodedBody = decodeBody(body);
        decodedBody = (convertToJson) ? convertToJson(decodedBody) : decodedBody;

        return bodyMatcher.matches(decodedBody);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has request body ").appendDescriptionOf(bodyMatcher);
    }

    private static String decodeBody(String bodyString) {
        try {
            return URLDecoder.decode(bodyString, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(format("Can't URL decode %s", bodyString), e.getCause());
        }
    }

    // todo Это нужно только для поффера и изначально было сделано похоже
    // Надо подумать как сделать лучше, я больше не вывожу пока :(
    // Тут в запросе строка вида key1=value1&key2=value2, а сравниваем его почему-то с JSON
    private static String convertToJson(String keyValueString) {
        Map<String, String> map = new HashMap<>();

        Arrays.stream(keyValueString.split("&"))
                .map(item -> item.split("="))
                .forEach(item -> map.put(item[0], item.length == 1 ? "" : item[1]));

        return new Gson().toJson(map);
    }

    @Factory
    public static RequestHasBodyMatcher hasSiteInfo(String siteInfo) {
        return new RequestHasBodyMatcher(containsString(siteInfo), false);
    }

    @Factory
    public static RequestHasBodyMatcher hasJsonBody(String bodyJsonPath) {
        String bodyJson = getResourceAsString(bodyJsonPath);
        return new RequestHasBodyMatcher(jsonEquals(bodyJson), false);
    }

    @Factory
    public static RequestHasBodyMatcher pofferHasJsonBody(String bodyJsonPath) {
        String bodyJson = getResourceAsString(bodyJsonPath);
        return new RequestHasBodyMatcher(jsonEquals(bodyJson), true);
    }
}
