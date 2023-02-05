package com.yandex.frankenstein

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class TestDevicesInfoTest {

    private final Map<String, List<String>> devicesInfo = [
            booted: ['booted1', 'booted2'],
            running: ['running1', 'running2', 'running3']
    ]

    private final Map<String, List> emptyDevicesInfo = [
            booted: [],
            running: []
    ]


    private final TestDevicesInfo mTestDevicesInfo = new TestDevicesInfo(
            booted: devicesInfo['booted'],
            running: devicesInfo['running']
    )
    private final File tmpDir = File.createTempDir()
    private final File devicesFile = new File(tmpDir, "devices_info.json")

    @Test
    void testSave() {
        final String expectedFileOutput = JsonOutput.prettyPrint(JsonOutput.toJson(devicesInfo))

        mTestDevicesInfo.save(tmpDir)

        assertThat(devicesFile.text).isEqualTo(expectedFileOutput)
    }

    @Test
    void testLoad() {
        devicesFile << JsonOutput.prettyPrint(JsonOutput.toJson(devicesInfo))

        TestDevicesInfo testDevicesInfo = TestDevicesInfo.load(tmpDir)

        assertThat(testDevicesInfo.booted).containsExactlyElementsOf(devicesInfo['booted'])
        assertThat(testDevicesInfo.running).containsExactlyElementsOf(devicesInfo['running'])
    }

    @Test
    void testGetAllSerials() {
        devicesFile << JsonOutput.prettyPrint(JsonOutput.toJson(devicesInfo))

        TestDevicesInfo testDevicesInfo = TestDevicesInfo.load(tmpDir)

        assertThat(testDevicesInfo.getAllDeviceIds()).containsExactlyElementsOf(devicesInfo['booted'] + devicesInfo['running'])
    }

    @Test
    void testGetAllSerialsEmpty() {
        devicesFile << JsonOutput.prettyPrint(JsonOutput.toJson(emptyDevicesInfo))

        TestDevicesInfo testDevicesInfo = TestDevicesInfo.load(tmpDir)

        assertThat(testDevicesInfo.getAllDeviceIds()).isEmpty()
    }
}
