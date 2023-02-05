package ru.yandex.telepathy;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test is written on Java, because TelepathistAssignment should be compatible with Java.
 */
public class TelepathistAssignmentSyntaxTest {
    @Test
    public void fluentInterfaceCheck() throws MalformedURLException {
        String url = "https://ya.ru";
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        long cacheTtl = 10L;
        TimeUnit timeUnit = TimeUnit.DAYS;
        Map<String, Object> override = new HashMap<>();
        override.put("key", "value");
        MergeStrategy mergeStrategy = MergeStrategy.RewriteOnly;

        TelepathistAssignment assignment = TelepathistAssignment.compose()
            .loadFrom(ConfigSource.custom(url))
            .withOkHttp(builder)
            .thenMerge(override, mergeStrategy)
            .andKeepItFor(cacheTtl, timeUnit)
            .sealInEnvelope();

        assertThat(assignment.getConfigSource().getUrl()).isEqualTo(new URL(url));
        assertThat(assignment.getMerge()).isEqualTo(override);
        assertThat(assignment.getMergeStrategy()).isEqualTo(mergeStrategy);
        assertThat(assignment.getCacheTimeToLiveMs()).isEqualTo(timeUnit.toMillis(cacheTtl));
        assertThat(assignment.getOkHttpClientBuilder()).isEqualTo(builder);
    }
}
