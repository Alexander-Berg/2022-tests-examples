package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.BuildInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class BuildInfoExtractorTest {

    final String buildUrl = "/some/build/url"
    final String apiLevel = "some_build_api_level"
    final String name = "some_build_name"
    final String reportsUrl = "resource_link"

    final BuildInfoExtractor mBuildInfoExtractor = new BuildInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "build.url": buildUrl,
                        "build.api.level": apiLevel,
                        "build.name": name,
                        "build.reports.url": reportsUrl,
                ]}
        ] as Project
        final BuildInfo buildInfo = mBuildInfoExtractor.extractInfo(project)
        assertThat(buildInfo.buildUrl).isEqualTo(buildUrl)
        assertThat(buildInfo.osApiLevel).isEqualTo(apiLevel)
        assertThat(buildInfo.buildName).isEqualTo(name)
        assertThat(buildInfo.reportsUrl).isEqualTo(reportsUrl)
    }
}
