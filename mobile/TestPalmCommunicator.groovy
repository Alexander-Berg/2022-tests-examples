package com.yandex.frankenstein.utils

import com.yandex.frankenstein.DeviceSettings
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.ReportType
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.utils.authorization.OauthAuthorization
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

@CompileStatic
class TestPalmCommunicator {

    private static final String RESPONSE_ATTRIBUTE_STATUS = "status"
    private static final String RESPONSE_ATTRIBUTE_STATUS_ERROR = "error"

    private final TestPalmInfo mTestPalmInfo
    private final Logger mLogger
    private final NetworkHelper mNetworkHelper
    private String mTestSuiteTitle = null

    TestPalmCommunicator(final TestPalmInfo testPalmInfo, final Logger logger) {
        mTestPalmInfo = testPalmInfo
        mLogger = logger
        mNetworkHelper = new NetworkHelper(logger)
    }

    List<Map<String, ?>> getVersionsInfo() {
        return downloadList("$mTestPalmInfo.baseUrl/version/$mTestPalmInfo.projectId")
    }

    List<Map<String, ?>> getDefinitionsInfo() {
        return downloadList("$mTestPalmInfo.baseUrl/definition/$mTestPalmInfo.projectId")
    }

    List<Map<String, ?>> getSuiteInfo() {
        return downloadList("$mTestPalmInfo.baseUrl/testcases/$mTestPalmInfo.projectId/suite/$mTestPalmInfo.suiteId")
    }

    String setTestCaseStatus(final String testRunId, final String caseUuid, final String status) {
        return mNetworkHelper.post("$mTestPalmInfo.baseUrl/testrun/$mTestPalmInfo.projectId/$testRunId/$caseUuid/resolve?status=$status", [:] as Map, requestHeaders)
    }

    String setTestCaseCodeComment(final String testRunId, final String caseUuid, final String comment) {
        final String codeComment = """```
$comment
```"""
        return setTestCaseComment(testRunId, caseUuid, codeComment)
    }

    String setTestCaseComment(final String testRunId, final String caseUuid, final String comment) {
        final String body = new JsonBuilder([
                text: """
$comment
"""
        ]).toString()
        return mNetworkHelper.post("$mTestPalmInfo.baseUrl/testrun/$mTestPalmInfo.projectId/$testRunId/$caseUuid/comments", [:] as Map,
                requestHeaders,
                body.bytes)
    }

    Map<String, ?> createTestRun(final String versionId, final BuildInfo buildInfo, final DeviceSettings deviceSettings) {
        final String body = new JsonBuilder([
                testSuite: [
                        id: mTestPalmInfo.suiteId
                ],
                title: "$testSuiteTitle run" + (buildInfo.osApiLevel ? ", api level: ${buildInfo.osApiLevel}" : ""),
                version: versionId,
                properties: composeTestPalmProperties(buildInfo.toMap(), deviceSettings)
        ]).toString()
        final String response = mNetworkHelper.post("$mTestPalmInfo.baseUrl/testrun/$mTestPalmInfo.projectId/create", [:] as Map, requestHeaders, body.bytes)
        final Map responseJson = (new JsonSlurper().parseText(response) as List<Map>)[0]
        final String testRunId = responseJson.id as String
        if (testRunId == null) {
            mLogger.info(responseJson.toString())
        }
        return [
                'id': testRunId,
                'cases': testRunId ? extractCases(responseJson) : null
        ]
    }

    String skipRemainingTestCases(final String testRunId) {
        return mNetworkHelper.post("$mTestPalmInfo.baseUrl/testrun/$mTestPalmInfo.projectId/$testRunId/finish", [:] as Map, requestHeaders)
    }

    String getReportType() {
        if (isEnoughInfo()) {
            if (mTestPalmInfo.reportType == ReportType.ALL) {
                return ReportType.ALL
            }
            if (mTestPalmInfo.reportType == ReportType.ONE && mTestPalmInfo.versionId) {
                return ReportType.ONE
            }
        }
        return ReportType.NO
    }

    boolean isEnoughInfo() {
        return mTestPalmInfo.suiteId && mTestPalmInfo.projectId && mTestPalmInfo.token
    }

    private Map<Integer, String> extractCases(final Map responseJson) {
        final Map<Integer, String> cases = [:]
        responseJson.testGroups.each { final Map testGroup ->
            testGroup.testCases.each { final Map testCase ->
                cases.put((testCase.testCase as Map).id as Integer, testCase.uuid as String)
            }
        }
        return cases
    }

    private String getTestSuiteTitle() {
        if (mTestSuiteTitle == null) {
            mLogger.info("Gettings test suite title for id $mTestPalmInfo.suiteId")
            final String suiteUrl = "$mTestPalmInfo.baseUrl/testsuite/$mTestPalmInfo.projectId/$mTestPalmInfo.suiteId?include=title"
            final String response = makeGetRequest(suiteUrl)
            final Map responseJson = parseJsonResponse(response) as Map
            mTestSuiteTitle = responseJson.title
        }
        return mTestSuiteTitle
    }

    private List<Map<String, String>> composeTestPalmProperties(final Map<String, String> map, final DeviceSettings deviceSettings) {
        final List<Map<String, String>> props = map.collect { final String k, final String v ->
            composeProperty(k, v)
        }

        props.add(composeProperty('Device Type', deviceSettings.deviceConfiguration.common?.device_type as String ?: 'phone'))

        return props
    }

    private Map<String, String> composeProperty(final String propertyKey, final String propertyValue) {
        return [
                key: propertyKey,
                value: propertyValue.isEmpty() ? "-" : propertyValue
        ]
    }

    private List<Map<String, ?>> downloadList(final String url) {
        final String response = makeGetRequest(url)
        return parseTestPalmResponse(response) as List<Map<String, ?>>
    }

    private String makeGetRequest(final String url) {
        final String response = mNetworkHelper.get(url, [:] as Map, requestHeaders)
        if (response.empty) {
            throw new GradleException("Failed to request data from TestPalm")
        } else {
            return response
        }
    }

    private static Object parseTestPalmResponse(final String response) {
        final Object responseJson = parseJsonResponse(response)
        if (isValidTestPalmResponse(responseJson)) {
            return responseJson
        } else {
            throw new GradleException("Server returned invalid response:\n$response")
        }
    }

    private static Object parseJsonResponse(final String response) {
        try {
            return new JsonSlurper().parseText(response)
        } catch (Exception e) {
            throw new GradleException("Failed to parse JSON response", e)
        }
    }

    private static boolean isValidTestPalmResponse(final Object response) {
        if (response instanceof Map<String, ?>) {
            final String status = response.get(RESPONSE_ATTRIBUTE_STATUS)
            return status == null || status != RESPONSE_ATTRIBUTE_STATUS_ERROR
        }

        return true
    }

    private Map<String, String> getRequestHeaders() {
        final Map<String, String> headers = [:] + OauthAuthorization.getHeaders(mTestPalmInfo.token)
        headers[HttpHeaders.CONTENT_TYPE] = 'application/json'
        return headers
    }
}
