package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class BuildInfoTest {

    final String buildUrl = "some_build_url"
    final String osApiLevel = "some_os_api_level"
    final String buildName = "some_name"
    final String reportsUrl = "resource_link"
    final String schedulerId = "scheduler_id"
    final String taskId = "task_id"

    @Test
    void testExtractInfo() {
        final BuildInfo buildInfo = new BuildInfo(buildUrl, osApiLevel, buildName, reportsUrl, schedulerId, taskId)
        assertThat(buildInfo.buildUrl).isEqualTo(buildUrl)
        assertThat(buildInfo.osApiLevel).isEqualTo(osApiLevel)
        assertThat(buildInfo.buildName).isEqualTo(buildName)
        assertThat(buildInfo.reportsUrl).isEqualTo(reportsUrl)
        assertThat(buildInfo.schedulerId).isEqualTo(schedulerId)
        assertThat(buildInfo.taskId).isEqualTo(taskId)
    }

    @Test
    void testExtractInfoWithEmptyParameters() {
        final BuildInfo buildInfo = new BuildInfo(null, null, null, null, null, null)
        assertThat(buildInfo.buildUrl).isEqualTo(null)
        assertThat(buildInfo.osApiLevel).isEqualTo(null)
        assertThat(buildInfo.buildName).isEmpty()
        assertThat(buildInfo.reportsUrl).isEmpty()
        assertThat(buildInfo.schedulerId).isEmpty()
        assertThat(buildInfo.taskId).isEmpty()
    }

    @Test
    void testToMap() {
        final BuildInfo buildInfo = new BuildInfo(buildUrl, osApiLevel, buildName, reportsUrl, schedulerId, taskId)
        final Map<String, String> buildInfoMap = [
                'Build url': buildUrl,
                'Build name': buildName,
                'Os api level': osApiLevel,
                'Reports URL': reportsUrl,
                'Scheduler ID': schedulerId,
                'Task ID': taskId,
        ]
        assertThat(buildInfo.toMap()).isEqualTo(buildInfoMap)
    }

    @Test
    void testToMapWithEmptyParameters() {
        final BuildInfo buildInfo = new BuildInfo(null, null, null, null, null, null)
        final Map<String, String> buildInfoMap = [
                'Build url': '',
                'Build name': '',
                'Os api level': '',
                'Reports URL': '',
                'Scheduler ID': '',
                'Task ID': '',
        ]
        assertThat(buildInfo.toMap()).isEqualTo(buildInfoMap)
    }
}
