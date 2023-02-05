package ru.yandex.disk.remote;

import org.junit.Test;
import ru.yandex.disk.test.TestCase2;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.remote.RemoteApiCompatibility.convertToRestApiPath;

public class RemoteApiCompatibilityTest extends TestCase2 {

    @Test
    public void shouldConvertToRestApiPath() throws Exception {
        assertThat(convertToRestApiPath("/disk/"), equalTo("disk:/"));
        assertThat(convertToRestApiPath("/disk/a/b"), equalTo("disk:/a/b"));
        assertThat(convertToRestApiPath("/disk"), equalTo("disk:/"));
    }

    @Test
    public void shouldConvertPhotounlimToRestApiPath() throws Exception {
        assertThat(convertToRestApiPath("/photounlim/"), equalTo("photounlim:/"));
        assertThat(convertToRestApiPath("/photounlim/a.jpg"), equalTo("photounlim:/a.jpg"));
        assertThat(convertToRestApiPath("/photounlim"), equalTo("photounlim:/"));
    }
}