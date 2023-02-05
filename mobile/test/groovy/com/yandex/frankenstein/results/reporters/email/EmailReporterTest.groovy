package com.yandex.frankenstein.results.reporters.email

import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.EmailInfo
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.reporters.TestRunInfo
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.gradle.api.logging.Logger
import org.junit.Test

import javax.mail.internet.MimeMultipart

import static org.assertj.core.api.Assertions.assertThat

class EmailReporterTest {

    final TestRunResult mTestRunResult = new TestRunResult()
    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    final TestRunDescription mTestRunDescription = new TestRunDescription()

    final Logger dummyLogger = [
            info: {}
    ] as Logger

    final EmailInfo mEmailInfo = new EmailInfo("smtpHost", "1234", "user", "password", "from", "to")
    final BuildInfo mBuildInfo = new BuildInfo("test.com", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")
    final TestPalmInfo mTestPalmInfo = new TestPalmInfo("projectId", "suiteid", "mVersionId", "token", "baseUrl", "one")

    final List<TestRunInfo> testInfoList = []
    final MimeMultipart dummyMimeMultipart = new MimeMultipart()

    final EmailMessageComposer mEmailMessageComposer = new EmailMessageComposer(dummyLogger, mTestPalmInfo) {
        MimeMultipart composeMessage(final List<TestRunInfo> emailTestInfoList) {
            testInfoList.addAll(emailTestInfoList)
            return dummyMimeMultipart
        }
    }

    final List<MimeMultipart> sentMimeMultiparts = []
    final List<String> sentSubjects = []
    final EmailMessageSender mEmailMessageSender = new EmailMessageSender(dummyLogger, mEmailInfo) {
        void sendEmail(final MimeMultipart mimeMultipart, final String subject) {
            sentMimeMultiparts.add(mimeMultipart)
            sentSubjects.add(subject)
        }
    }

    final EmailReporter mReporter = new EmailReporter(dummyLogger, mEmailInfo, mBuildInfo, mEmailMessageComposer,
            mEmailMessageSender)

    @Test
    void testReport() {
        mReporter.report(mTestRunResult, mTestCasesRunResult, mTestRunDescription)

        assertThat(testInfoList).hasSize(1)
        assertThat(testInfoList.first().buildInfo).isSameAs(mBuildInfo)
        assertThat(testInfoList.first().testRunResult).isSameAs(mTestRunResult)
        assertThat(testInfoList.first().testCasesRunResult).isSameAs(mTestCasesRunResult)
        assertThat(testInfoList.first().testRunDescription).isSameAs(mTestRunDescription)

        assertThat(sentMimeMultiparts).containsExactly(dummyMimeMultipart)
        assertThat(sentSubjects).containsExactly("${mBuildInfo.buildName} ${mBuildInfo.osApiLevel} frankenstein autotests".toString())
    }

    @Test
    void testCanReport() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "user", "password", "from", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutHost() {
        final EmailInfo emailInfo = new EmailInfo("", "1234", "user", "password", "from", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutPort() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "", "user", "password", "from", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutUser() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "", "password", "from", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutPassword() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "user", "", "from", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutFrom() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "user", "password", "", "to")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutTo() {
        final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "user", "password", "from", "")
        final EmailReporter reporter = new EmailReporter(dummyLogger, emailInfo, null, null)

        assertThat(reporter.canReport()).isFalse()
    }


    @Test
    void testGetFailureMessage() {
        final EmailReporter reporter = new EmailReporter(dummyLogger, mEmailInfo, null, null)
        assertThat(reporter.getFailureMessage()).contains(mEmailInfo.toString())
    }
}
