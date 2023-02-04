package ru.yandex.solomon.alert.dao.kikimr;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class KikimrSchemaVersionTest {

    @Test
    public void versionNumUnique() {
        Map<Integer, YdbSchemaVersion> map = new HashMap<>();
        for (YdbSchemaVersion version : YdbSchemaVersion.values()) {
            YdbSchemaVersion prev = map.put(version.getNumber(), version);
            assertThat("Not unique version number: " + prev + ", " + version, prev, nullValue());
        }
    }

    @Test
    public void maxOnTop() {
        YdbSchemaVersion[] values = YdbSchemaVersion.values();
        YdbSchemaVersion prev = values[0];
        for (int index = 1; index < values.length; index++) {
            YdbSchemaVersion version = values[index];
            assertThat(version.name(), version.getNumber(), lessThan(prev.getNumber()));
            prev = version;
        }
    }
}
