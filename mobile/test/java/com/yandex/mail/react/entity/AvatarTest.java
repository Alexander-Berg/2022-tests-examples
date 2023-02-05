package com.yandex.mail.react.entity;

import com.yandex.mail.network.response.GsonTest;
import com.yandex.mail.runners.IntegrationTestRunner;

import net.javacrumbs.jsonunit.JsonAssert;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class AvatarTest extends GsonTest {

    @Test
    public void equals_hashcode_verify() {
        assertThat(
                new Avatar.Builder()
                        .type("1")
                        .imageUrl(null)
                        .monogram("A")
                        .build()
        ).isNotEqualTo(
                new Avatar.Builder()
                        .type("1")
                        .imageUrl("http://ya.ru")
                        .monogram("A")
                        .build());
    }

    @Test
    public void toJson_noNulls() {
        // The default behaviour that is implemented in Gson is that null object fields are ignored.

        final Avatar avatarWithNullUrl = new Avatar.Builder()
                .type("1")
                .imageUrl(null)
                .monogram("A")
                .build();
        JsonAssert.assertJsonEquals(gson.toJson(avatarWithNullUrl), "{\"type\":\"monogram\",\"monogram\":\"A\"}");

        final Avatar avatarWithNonNullUrl = new Avatar.Builder()
                .type("1")
                .imageUrl("http://ya.ru")
                .monogram("A")
                .build();
        JsonAssert.assertJsonEquals(gson.toJson(avatarWithNonNullUrl), "{\"type\":\"image\",\"monogram\":\"A\",\"image\":\"http://ya.ru\"}");
    }
}
