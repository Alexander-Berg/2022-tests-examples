package com.yandex.frankenstein

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class TestDevicesInfo {

    private static final String TEST_DEVICES_INFO_FILENAME = "devices_info.json"

    List<String> booted
    List<String> running

    @Override
    String toString() {
        return "booted: $booted, running: $running"
    }

    void save(final File buildDir) {
        final File devicesInfoFile = new File(buildDir, TEST_DEVICES_INFO_FILENAME)

        final Map<String, List<String>> devicesInfo = [
                booted: booted,
                running: running,
        ]

        final String devicesInfoJson = JsonOutput.prettyPrint(JsonOutput.toJson(devicesInfo))
        devicesInfoFile.parentFile.mkdirs()
        devicesInfoFile.createNewFile()
        devicesInfoFile.write(devicesInfoJson, 'UTF-8')
    }

    List<String> getAllDeviceIds() {
        return booted + running
    }

    static TestDevicesInfo load(final File buildDir) {
        final File devicesInfoFile = new File(buildDir, TEST_DEVICES_INFO_FILENAME)
        Map<String, List<String>> devicesInfo = [:]
        devicesInfoFile.withInputStream { final InputStream inputStream ->
            devicesInfo = new JsonSlurper().parse(inputStream) as Map<String, List<String>>
        }
        final TestDevicesInfo testDevicesInfo = new TestDevicesInfo(
                booted: devicesInfo['booted'], running: devicesInfo['running']
        )
        return testDevicesInfo
    }
}
