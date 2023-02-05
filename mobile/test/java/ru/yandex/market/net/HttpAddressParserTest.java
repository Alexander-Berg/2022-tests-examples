package ru.yandex.market.net;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.HttpUrl;
import ru.yandex.market.base.network.common.address.HttpAddress;
import ru.yandex.market.base.network.common.address.HttpAddressParser;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class HttpAddressParserTest {

    @NonNull
    private final HttpAddressParser parser = new HttpAddressParser();

    @Test
    public void testConsiderBlankUrlAsEmpty() {
        assertEquals(HttpAddress.empty(), parser.parse(HttpAddressParser.BLANK_URL));
    }

    @Test
    public void testReturnEmptyHttpAddressForNullString() {
        assertEquals(HttpAddress.empty(), parser.parse((String) null));
    }

    @Test
    public void testReturnEmptyHttpAddressForNullUri() {
        assertEquals(HttpAddress.empty(), parser.parse((Uri) null));
    }

    @Test
    public void testReturnEmptyHttpAddressForNullHttpUrl() {
        assertEquals(HttpAddress.empty(), parser.parse((HttpUrl) null));
    }

    @Test
    public void testContainingTrailingPathSeparator() {
        final HttpAddress httpAddress = parser.parse("https://yandex.ru/");
        assertThat(httpAddress.getEncodedPathSegments(), contains(""));
        assertThat(httpAddress.getNormalizedPathSegments(), empty());
    }

    @Test
    public void testRemoveLeadingAndTrailingWhitespacesDuringCreation() {
        final HttpAddress a = HttpAddress.builder()
                .scheme("http")
                .host("yandex.ru")
                .build();
        final HttpAddress b = parser.parse("   http://yandex.ru/   ");
        assertEquals(

                HttpAddress.builder()
                        .scheme("http")
                        .host("yandex.ru")
                        .build(),

                parser.parse("   http://yandex.ru/   ")
        );

        assertEquals(HttpAddress.empty(), parser.parse("        "));
    }

    @Test
    public void testEndingTrailingPathSeparator() {
        final HttpAddress httpAddress =
                parser.parse("https://yandex.ru/path/to/something/");
        assertThat(httpAddress.getEncodedPath(), endsWith(HttpAddress.PATH_SEPARATOR));
    }
}