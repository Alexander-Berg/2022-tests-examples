package com.yandex.mail.react.entity;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class AttachmentTest {

    @Test
    public void equals_hashcode_verify() {
        assertThat(
                new Attachment.Builder()
                        .hid("1")
                        .name("attachment1")
                        .previewUrl("url1")
                        .size("size1")
                        .supportsPreview(false)
                        .hasThumbnail(false)
                        .type("type1")
                        .extension("ext1")
                        .disk(false)
                        .build()
        ).isNotEqualTo(
                new Attachment.Builder()
                        .hid("2")
                        .name("attachment2")
                        .previewUrl("url2")
                        .supportsPreview(false)
                        .hasThumbnail(false)
                        .size("size2")
                        .type("type2")
                        .extension("ext2")
                        .disk(false)
                        .build());
    }
    // TODO add tests for serialization to JSON
}