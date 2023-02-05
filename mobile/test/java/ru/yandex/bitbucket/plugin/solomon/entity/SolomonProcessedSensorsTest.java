package ru.yandex.bitbucket.plugin.solomon.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SolomonProcessedSensorsTest {
    private final String preparedJsonString = "{\"sensorsProcessed\":2,\"status\":\"OK\"}";
    private final SolomonProcessedSensors preparedSolomonProcessedSensors = new SolomonProcessedSensors("OK", 2);
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<SolomonProcessedSensors> adapter = moshi.adapter(SolomonProcessedSensors.class);

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedSolomonProcessedSensors);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        SolomonProcessedSensors solomonProcessedSensors = adapter.fromJson(preparedJsonString);
        assertEquals(preparedSolomonProcessedSensors, solomonProcessedSensors);
    }

}