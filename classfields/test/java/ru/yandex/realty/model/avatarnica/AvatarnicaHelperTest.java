package ru.yandex.realty.model.avatarnica;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class AvatarnicaHelperTest {

    @Test
    public void trimLeadingSlashes() {

        Map<String, String> urls = new HashMap<>();
        urls.put("some-url", "some-url");
        urls.put("/some-url", "/some-url");
        urls.put("//some-url", "//some-url");
        urls.put("///some-url", "//some-url");
        urls.put("////some-url", "//some-url");
        urls.put("/////some-url", "//some-url");
        urls.put("http://///some-url", "http://///some-url");

        urls.forEach((rawUrl, trimmedUrl) -> {
            List<String> prefixes = Arrays.asList(rawUrl);

            List<String> trimmed = AvatarnicaHelper.mapImagePrefixes(prefixes, Aliases.MINICARD);

            assertEquals(trimmed.size(), 1);
            assertTrue(trimmed.get(0).startsWith(trimmedUrl));
        });
    }
}
