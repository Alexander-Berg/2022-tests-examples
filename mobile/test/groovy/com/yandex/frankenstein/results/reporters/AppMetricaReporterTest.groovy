package com.yandex.frankenstein.results.reporters

import com.yandex.appmetrica.autotests.core.Metrica.ReportMessage
import com.yandex.appmetrica.autotests.core.Metrica.ReportMessage.Session
import com.yandex.appmetrica.autotests.core.Metrica.ReportMessage.Session.Event
import com.yandex.frankenstein.DeviceSettings
import com.yandex.frankenstein.properties.info.AppMetricaInfo
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.assertj.core.api.JUnitSoftAssertions
import org.gradle.api.logging.Logger
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AppMetricaReporterTest {

    final String expectedEventValue = """"testrun" : {
    "status": "failed",
    "total": 100,
    "passed": 85,
    "failed": 10,
    "skipped": 5,
    "test cases": 0,
    "duration": "00h:03m:20s"
}
"""
    final AppMetricaInfo mAppMetricaInfo = new AppMetricaInfo("apiKey", "reportName", "baseUrl")

    final TestRunResult mRunResult = new TestRunResult()
    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    final Logger mDummyLogger = [
            info: {}
    ] as Logger
    final DeviceSettings mDummyDeviceSettings = new DeviceSettings(mDummyLogger)

    final AppMetricaReporter mReporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo)

    @Rule public final JUnitSoftAssertions softly = new JUnitSoftAssertions()

    @Before
    void setUp() {
        mRunResult.allTests = 100
        mRunResult.tried = 95
        mRunResult.failed = 10
        mRunResult.passed = 85
        mRunResult.skipped = 5
        mRunResult.duration = 200

        mRunResult.tasksDurationsMillis.test = 10000L

        mTestCasesRunResult.total = 100
        mTestCasesRunResult.tried = 95
        mTestCasesRunResult.failed = 10
        mTestCasesRunResult.passed = 85
        mTestCasesRunResult.filtered = 5
        mTestCasesRunResult.hasBugs = 5
    }

    @Test
    void testReport() {
        final List<String> methods = []
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo) {
            protected void startSession() {
                methods.add("startSession")
            }

            protected void reportToAppMetrica(final TestRunResult runResult) {
                methods.add("reportToAppMetrica")
            }
        }

        reporter.report(mRunResult, null, null)
        assertThat(methods).containsExactly("startSession", "reportToAppMetrica")
    }

    @Test
    void testStartSession() {
        int expectedEventNumber = 0
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo) {

            protected void sendEventsToAppMetrica(final Session session) {
                assertSession(session)
                final List<Event> events = session.events
                softly.assertThat(events.size()).isEqualTo(1)
                final Event event = events.get(0)
                softly.assertThat(event.number).isEqualTo(expectedEventNumber)
                softly.assertThat(event.type).isEqualTo(Event.EVENT_START)
                softly.assertThat(event.time).isNotNull()
            }
        }
        reporter.startSession()
        expectedEventNumber = 1
        reporter.startSession()
    }

    @Test
    void testReportToAppMetrica() {
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo) {

            protected void sendEventsToAppMetrica(final Session session) {
                assertSession(session)
                final List<Event> events = session.events
                softly.assertThat(events.size()).isEqualTo(1)
                final Event event = events.get(0)
                softly.assertThat(event.number).isEqualTo(0)
                softly.assertThat(event.type).isEqualTo(Event.EVENT_CLIENT)
                softly.assertThat(event.time).isNotNull()
                softly.assertThat(event.name).isEqualTo(mAppMetricaInfo.reportName)
                softly.assertThat(String.valueOf(event.value as char[])).isEqualTo(expectedEventValue)
            }
        }
        reporter.reportToAppMetrica(mRunResult)
    }

    @Test
    void testSendEventsToAppMetrica() {
        final DeviceSettings fullDeviceSettings = new DeviceSettings(mDummyLogger)
        fullDeviceSettings.runnerConfiguration.defaultVersion = '1.0.0'
        fullDeviceSettings.deviceConfiguration.common = [
                app_platform: 'Android_App_Platform',
                app_version_name: 'AppMetricaVersionName',
                app_id: 'SomeAppId',
                manufacturer: 'AppMetricaManufacturer',
                device_name: 'Samsung Galaxy Note 7',
                device_type: 'physical',
                is_rooted: 'yes',
                os_api_level: '42',
        ]
        fullDeviceSettings.deviceConfiguration.versions = [
                '1.0.0': [
                        analytics_sdk_version: 'some sdk version',
                        os_version: 'some os version',
                        screen_width: '123',
                        screen_height: '4567',
                        locale: 'by_BY',
                        app_build_number: 'some app build number',
                        app_framework: 'some framework',
                        analytics_sdk_build_type: 'some analytics sdk build type',
                        analytics_sdk_build_number: 'some analytics sdk build number',
                ]
        ]
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, fullDeviceSettings, mAppMetricaInfo) {

            protected String send(final String reportUrl, final Map<String, String> params,
                                  final Map<String, String> headers, final byte[] body) {
                softly.assertThat(reportUrl).isEqualTo("$mAppMetricaInfo.baseUrl/report".toString())
                softly.assertThat(params)
                        .containsKey('uuid')
                        .containsKey('deviceid')
                        .containsEntry('app_platform', 'Android_App_Platform')
                        .containsEntry('app_version_name', 'AppMetricaVersionName')
                        .containsEntry('analytics_sdk_version', 'some sdk version')
                        .containsEntry('kit_version', 'some sdk version')
                        .containsEntry('app_id', 'SomeAppId')
                        .containsEntry('manufacturer', 'AppMetricaManufacturer')
                        .containsEntry('model', 'Samsung Galaxy Note 7')
                        .containsEntry('os_version', 'some os version')
                        .containsEntry('screen_width', '123')
                        .containsEntry('screen_height', '4567')
                        .containsEntry('locale', 'by_BY')
                        .containsEntry('device_type', 'physical')
                        .containsEntry('is_rooted', 'yes')
                        .containsEntry('app_build_number', 'some app build number')
                        .containsEntry('api_key_128', mAppMetricaInfo.apiKey)
                        .containsEntry('os_api_level', '42')
                        .containsEntry('app_framework', 'some framework')
                        .containsEntry('analytics_sdk_build_type', 'some analytics sdk build type')
                        .containsEntry('analytics_sdk_build_number', 'some analytics sdk build number')
                softly.assertThat(headers).isEqualTo(['Content-Encoding':'gzip'])
                softly.assertThat(body).isNotNull()
            }
        }
        reporter.reportToAppMetrica(mRunResult)
    }

    @Test
    void testSendEventsToAppMetricaWithoutSettings() {
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo) {

            protected String send(final String reportUrl, final Map<String, String> params,
                                  final Map<String, String> headers, final byte[] body) {
                softly.assertThat(reportUrl).isEqualTo("$mAppMetricaInfo.baseUrl/report".toString())
                softly.assertThat(params)
                        .containsKey('uuid')
                        .containsKey('deviceid')
                        .containsEntry('app_platform', '')
                        .containsEntry('app_version_name', '')
                        .containsEntry('analytics_sdk_version', '0')
                        .containsEntry('kit_version', '0')
                        .containsEntry('app_id', '')
                        .containsEntry('manufacturer', '')
                        .containsEntry('model', '')
                        .containsEntry('os_version', '0')
                        .containsEntry('screen_width', '0')
                        .containsEntry('screen_height', '0')
                        .containsEntry('locale', 'en_US')
                        .containsEntry('device_type', 'phone')
                        .containsEntry('is_rooted', '1')
                        .containsEntry('app_build_number', '1')
                        .containsEntry('api_key_128', mAppMetricaInfo.apiKey)
                        .containsEntry('os_api_level', '')
                        .containsEntry('app_framework', '')
                        .containsEntry('analytics_sdk_build_type', '')
                        .containsEntry('analytics_sdk_build_number', '')
                softly.assertThat(headers).isEqualTo(['Content-Encoding':'gzip'])
                softly.assertThat(body).isNotNull()
            }
        }
        reporter.reportToAppMetrica(mRunResult)
    }

    @Test
    void testCanReport() {
        assertThat(mReporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutApiKey() {
        final AppMetricaInfo appMetricaInfo = new AppMetricaInfo("", "reportName", "baseUrl")
        final AppMetricaReporter reporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, appMetricaInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testGetFailureMessage() {
        final AppMetricaReporter appMetricaReporter = new AppMetricaReporter(mDummyLogger, mDummyDeviceSettings, mAppMetricaInfo)
        assertThat(appMetricaReporter.getFailureMessage()).contains(mAppMetricaInfo.toString())
    }

    private void assertSession(final Session session) {
        softly.assertThat(session.sessionDesc).isNotNull()
        softly.assertThat(session.sessionDesc.locale).isEqualTo('en_US')
        softly.assertThat(session.sessionDesc.sessionType).isEqualTo(Session.SessionDesc.SESSION_FOREGROUND)
        softly.assertThat(session.sessionDesc.networkInfo).isNotNull()
        softly.assertThat(session.sessionDesc.networkInfo.connectionType).isEqualTo(1)
        softly.assertThat(session.sessionDesc.startTime).isNotNull()
        final ReportMessage.Time time = session.sessionDesc.startTime
        softly.assertThat(time.timestamp).isNotNull()
        softly.assertThat(time.serverTimeOffset).isEqualTo(0)
        softly.assertThat(time.timeZone).isNotNull()
    }
}
