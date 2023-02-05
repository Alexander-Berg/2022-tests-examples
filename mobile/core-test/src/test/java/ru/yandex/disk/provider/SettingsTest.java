package ru.yandex.disk.provider;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;

import javax.annotation.NonnullByDefault;

import static org.hamcrest.Matchers.equalTo;

@NonnullByDefault
@Config(manifest = Config.NONE)
public class SettingsTest extends DiskContentProviderTest {

    private Settings settings;
    private static final String SCOPE = "ALL";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        settings = TestObjectsFactory.createSettings(getMockContext());
    }

    @Test
    public void shouldQueryAll() throws Exception {

        settings.put("ALL", "key", "value");
        settings.put("user1", "key1", "value1");
        settings.put("user2", "key2", "value2");

        final BetterCursorWrapper<SettingsEntry> entries = settings.queryAll();
        assertThat(entries.getCount(), equalTo(3));

        final SettingsEntry entry = entries.get(2);
        assertThat(entry.getScope(), equalTo("user2"));
        assertThat(entry.getKey(), equalTo("key2"));
        assertThat(entry.getValue(), equalTo("value2"));
    }

    @Test
    public void shouldGet() {

        settings.put(SCOPE, KEY, VALUE);
        final String result = settings.get(SCOPE, KEY, "default");

        assertThat(result, equalTo(VALUE));
    }

    @Test
    public void shouldUpdate() {
        settings.put(SCOPE, KEY, VALUE);
        final String new_value = "new_value";
        settings.put(SCOPE, KEY, new_value);

        final String result = settings.get(SCOPE, KEY, "default");

        assertThat(result, equalTo(new_value));
    }
}
