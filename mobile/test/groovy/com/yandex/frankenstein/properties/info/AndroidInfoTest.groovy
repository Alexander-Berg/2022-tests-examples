package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AndroidInfoTest {

    @Test
    void testIsBuildTypeBinary() {
        final AndroidInfo androidInfo = new AndroidInfo("", "", "", "", "", [:], LibraryBuildType.BINARY, "")
        assertThat(androidInfo.isBuildTypeBinary()).isTrue()
    }
}
