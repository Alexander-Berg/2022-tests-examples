package com.yandex.frankenstein.utils.android

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.junit.Test

import java.util.function.Predicate

import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class AdbPropertiesTest {

    final Logger dummyLogger = [:] as Logger

    @Test
    void testGetDisplayDimensionsIncorrectOutput() {
        boolean result = true
        final String logOutput = "Can't find service:"
        final AdbExecutor adbExecutor = new AdbExecutor(dummyLogger, "") {
            String getAdbCommandOutput(final List<String> commands, final Predicate<String> predicate) {
                result &= predicate.test(logOutput)
                return logOutput
            }
        }
        final AdbProperties adb = new AdbProperties(dummyLogger, adbExecutor)
        adb.getDisplayDimensions()
        assertThat(result).isTrue()
    }

    @Test
    void testGetDisplayDimensionsPhysicalDisplayInfoApi19() {
        final AdbProperties adb = getAdbWithFakeOutput('mPhys=PhysicalDisplayInfo{1280 x 768, 60.000004 fps, density 2.0, 320.0 x 320.0 dpi,' +
                ' secure true}')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).isNull()
    }

    @Test
    void testGetDisplayDimensionsDisplayDeviceInfoApi19() {
        final AdbProperties adb = getAdbWithFakeOutput('DisplayDeviceInfo{"Built-in Screen": 1280 x 768, 60.000004 fps, density 320,' +
                        ' 320.0 x 320.0 dpi, touch INTERNAL, rotation 0, type BUILT_IN, FLAG_DEFAULT_DISPLAY,' +
                        ' FLAG_ROTATES_WITH_CONTENT, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS}')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).containsExactlyInAnyOrder(1280, 768)
    }

    @Test
    void testGetDisplayDimensionsDisplayDeviceInfoZeroApi19() {
        final AdbProperties adb = getAdbWithFakeOutput('DisplayDeviceInfo{"Built-in Screen": 0 x 0, 60.000004 fps, density 320}')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).isNull()
    }

    @Test
    void testGetDisplayDimensionsDisplayInfoApi23() {
        final AdbProperties adb = getAdbWithFakeOutput('mBaseDisplayInfo=DisplayInfo{"Built-in Screen", uniqueId "local:0", app 1080 x 1920,' +
                        ' real 1080 x 1920, largest app 1080 x 1920, smallest app 1080 x 1920, mode 1,' +
                        ' defaultMode 1, modes [{id=1, width=1080, height=1920, fps=60.000004}], rotation 0,' +
                        ' density 480 (480.0 x 480.0) dpi, layerStack 0, appVsyncOff 0, presDeadline 17666666,' +
                        ' type BUILT_IN, state ON, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS}')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).isNull()
    }

    @Test
    void testGetDisplayDimensionsDisplayDeviceInfoApi23() {
        final AdbProperties adb = getAdbWithFakeOutput('DisplayDeviceInfo{"Built-in Screen": uniqueId="local:0", 1080 x 1920, modeId 1,' +
                        ' defaultModeId 1, supportedModes [{id=1, width=1080, height=1920, fps=60.000004}],' +
                        ' density 480, 480.0 x 480.0 dpi, appVsyncOff 0, presDeadline 17666666, touch INTERNAL,' +
                        ' rotation 0, type BUILT_IN, state ON, FLAG_DEFAULT_DISPLAY, FLAG_ROTATES_WITH_CONTENT,' +
                        ' FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS}')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).containsExactlyInAnyOrder(1920, 1080)
    }

    @Test
    void testGetDisplayDimensionsDisplayDeviceInfoMultiLine() {
        final AdbProperties adb = getAdbWithFakeOutput("""
Display Devices: size=1
DisplayDeviceInfo{"Встроенный экран": uniqueId="local:0", 1080 x 1920, modeId 1, defaultModeId 1, 397.565 x 399.737}
mAdapter=LocalDisplayAdapter
""")
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).containsExactlyInAnyOrder(1080, 1920)
    }

    @Test
    void testGetDisplayDimensionsDisplayDeviceInfoZero() {
        final AdbProperties adb = getAdbWithFakeOutput('DisplayDeviceInfo{"Built-in Screen": uniqueId="local:0", 0 x 0, modeId 1,' +
                        ' density 420, 420.0 x 420.0 dpi,')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).isNull()
    }

    @Test
    void testGetDisplayDimensionsWindowBelowApi27() {
        final AdbProperties adb = getAdbWithFakeOutput('mUnrestrictedScreen=(0,0) 100x200')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).containsExactlyInAnyOrder(100, 200)
    }

    @Test
    void testGetDisplayDimensionsWindowMultiline() {
        final AdbProperties adb = getAdbWithFakeOutput("""
    mRestrictedOverscanScreen=(0,0) 1080x1920
    mUnrestrictedScreen=(0,0) 1000x2000
    mRestrictedScreen=(0,0) 1080x1920
    """)
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).containsExactlyInAnyOrder(2000, 1000)
    }

    @Test
    void testGetDisplayDimensionsWindowZero() {
        final AdbProperties adb = getAdbWithFakeOutput('mUnrestrictedScreen=(0,0) 0x0')
        final List<Integer> list = adb.getDisplayDimensions()
        assertThat(list).isNull()
    }

    @Test
    void testGetSystemLocaleBelowApi23() {
        final AdbProperties adb = getAdbWithFakeProps(
                'persist.sys.language': 'ru', 'persist.sys.country': 'RU',
                'ro.product.locale.language': 'en', 'ro.product.locale.region': 'US',
                'ro.build.version.sdk': '22',
        )

        assertThat(adb.getLocale()).isEqualTo('ru_RU')
    }

    @Test
    void testGetSystemLocaleAboveApi23() {
        final AdbProperties adb = getAdbWithFakeProps(
                'persist.sys.locale': 'ru-RU',
                'ro.product.locale': 'en-US',
                'ro.build.version.sdk': '23',
        )

        assertThat(adb.getLocale()).isEqualTo('ru_RU')
    }

    @Test
    void testGetProductLocaleBelowApi23() {
        final AdbProperties adb = getAdbWithFakeProps(
                'ro.product.locale.language': 'en', 'ro.product.locale.region': 'US',
                'ro.build.version.sdk': '22',
        )

        assertThat(adb.getLocale()).isEqualTo('en_US')
    }

    @Test
    void testGetProductLocaleAboveApi23() {
        final AdbProperties adb = getAdbWithFakeProps(
                'ro.product.locale': 'en-US',
                'ro.build.version.sdk': '23',
        )

        assertThat(adb.getLocale()).isEqualTo('en_US')
    }
    
    private AdbProperties getAdbWithFakeOutput(final String output) {
        final AdbExecutor adbExecutor = new AdbExecutor(dummyLogger, "") {
            String getAdbCommandOutput(final List<String> commands, final Predicate<String> predicate) {
                return output
            }
        }
        return new AdbProperties(dummyLogger, adbExecutor)
    }

    private AdbProperties getAdbWithFakeProps(final Map<String, String> props) {
        final AdbExecutor adbExecutor = new AdbExecutor(dummyLogger, "") {

            @Override
            String getprop(final String key) {
                return props.getOrDefault(key, "")
            }
        }

        return new AdbProperties(dummyLogger, adbExecutor)
    }
}
