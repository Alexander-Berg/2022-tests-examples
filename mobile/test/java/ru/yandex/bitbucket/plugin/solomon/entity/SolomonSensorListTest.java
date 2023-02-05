package ru.yandex.bitbucket.plugin.solomon.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;
import ru.yandex.bitbucket.plugin.solomon.entity.SolomonSensorList.SensorData;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.yandex.bitbucket.plugin.solomon.entity.SolomonSensorList.Labels;

public class SolomonSensorListTest {
    private final String preparedJsonString = "{\"sensors\":[{\"labels\":{\"sensor\":\"MERGE_COMMIT_UPDATE\"}," +
            "\"value\":1},{\"labels\":{\"sensor\":\"MERGE_COMMIT_UPDATE\"},\"value\":1}]}";
    private final Labels labels = new Labels(SensorLabel.MERGE_COMMIT_UPDATE);
    private final SensorData sensor = new SensorData(labels, 1);
    private final List<SensorData> sensors = Arrays.asList(sensor, sensor);
    private final SolomonSensorList preparedSolomonSensorList = new SolomonSensorList(sensors);
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<SolomonSensorList> adapter = moshi.adapter(SolomonSensorList.class);

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedSolomonSensorList);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        SolomonSensorList solomonSensorList = adapter.fromJson(preparedJsonString);
        assertEquals(preparedSolomonSensorList, solomonSensorList);
    }
}