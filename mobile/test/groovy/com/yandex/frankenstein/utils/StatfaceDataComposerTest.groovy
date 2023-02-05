package com.yandex.frankenstein.utils

import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class StatfaceDataComposerTest {

    final BuildInfo mBuildInfo = new BuildInfo("/build/url", "os_api_level", "build_name", "/reports/url", "schedulerId", "taskId")
    final TestRunResult mRunResult = new TestRunResult()
    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    final StatfaceDataComposer mComposer = new StatfaceDataComposer()

    @Before
    void setUp() {
        mRunResult.allTests = 100
        mRunResult.tried = 95
        mRunResult.failed = 10
        mRunResult.passed = 85
        mRunResult.skipped = 5
        mRunResult.duration = 200

        mRunResult.tasksDurationsMillis['test'] = 10000L

        mTestCasesRunResult.total = 100
        mTestCasesRunResult.tried = 95
        mTestCasesRunResult.failed = 10
        mTestCasesRunResult.passed = 85
        mTestCasesRunResult.filtered = 5
        mTestCasesRunResult.hasBugs = 5
    }

    @Test
    void testGetStatfaceData() {
        final List<Map<String, String>> data = mComposer.getStatfaceData(mBuildInfo, mRunResult, mTestCasesRunResult)
        assertThat(data).hasSize(1)
        final Map<String, String> dataMap = data[0]
        assertThat(dataMap).containsKey('fielddate')
    }
}
