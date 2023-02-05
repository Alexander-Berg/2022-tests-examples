package com.yandex.mail.react.entity;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ReactLabelTest {

    @Test
    public void equals_hashcode_verify() {
        assertThat(new ReactLabel("1", "color1", "label1"))
                .isNotEqualTo(new ReactLabel("2", "color2", "label2"));
    }

    // TODO add tests for serialization to JSON
}