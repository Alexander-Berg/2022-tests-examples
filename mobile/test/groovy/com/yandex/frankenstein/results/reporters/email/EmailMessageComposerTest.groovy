package com.yandex.frankenstein.results.reporters.email

import com.yandex.frankenstein.description.TestDescription
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.TestStatus
import com.yandex.frankenstein.results.reporters.TestRunInfo
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.gradle.api.logging.Logger
import org.junit.Before
import org.junit.Test

import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart

import static org.assertj.core.api.Assertions.assertThat

class EmailMessageComposerTest {

    final Logger dummyLogger = [:] as Logger

    final TestRunResult mTestRunResult = new TestRunResult()
    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    final TestRunDescription mTestRunDescription = new TestRunDescription()

    final BuildInfo mBuildInfo = new BuildInfo("test.com", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")
    final TestPalmInfo mTestPalmInfo = new TestPalmInfo("projectId", "suiteid", "mVersionId", "token", "baseUrl", "baseUiUrl", "one")

    final TestRunInfo mTestInfo = new TestRunInfo(mBuildInfo, mTestRunResult, mTestCasesRunResult, mTestRunDescription)

    final EmailMessageComposer mEmailMessageComposer =
            new EmailMessageComposer(dummyLogger, mTestPalmInfo)

    @Before
    void setUp() {
        mTestRunResult.allTests = 100
        mTestRunResult.testCases = 100
        mTestRunResult.tried = 95
        mTestRunResult.failed = 10
        mTestRunResult.passed = 85
        mTestRunResult.skipped = 5
        mTestRunResult.duration = 200

        mTestRunResult.tasksDurationsMillis['test'] = 10000L
        mTestRunResult.testResults = [
                new TestResult(
                        testDescription: new TestDescription(testName: "testName1", testClass: "testClass1", testCaseId: 1),
                        status: TestStatus.FAILED,
                        description: "description1"
                ),
                new TestResult(
                        testDescription: new TestDescription(testName: "testName2", testClass: "testClass2", testCaseId: 2),
                        status: TestStatus.FAILED,
                        description: "description2"
                ),
                new TestResult(
                        testDescription: new TestDescription(testName: "testName3", testClass: "testClass3", testCaseId: 3),
                        status: TestStatus.PASSED,
                        description: "description3"
                )
        ] as List<TestResult>

        mTestCasesRunResult.total = 100
        mTestCasesRunResult.tried = 95
        mTestCasesRunResult.failed = 10
        mTestCasesRunResult.passed = 85
        mTestCasesRunResult.filtered = 5
        mTestCasesRunResult.hasBugs = 5
    }

    @Test
    void testGetIconPart() {
        final String cid = UUID.randomUUID()
        final MimeBodyPart iconPart = mEmailMessageComposer.getIconPart(cid)

        assertThat(iconPart.getContentID()).isEqualTo("<" + cid + ">")
        assertThat(iconPart.getDisposition()).isEqualTo(MimeBodyPart.INLINE)
    }

    @Test
    void testGetMainPart() {
        final String cid = "cid-123"
        final List<TestRunInfo> testInfoList = Arrays.asList(mTestInfo, mTestInfo)
        final Map<String, Object> binding = mEmailMessageComposer.getTemplateBinding(testInfoList, cid)
        final MimeBodyPart mainPart = mEmailMessageComposer.getMainPart(binding)

        assertThat(mainPart.getContentType()).isEqualTo("text/plain")
        assertThat(mainPart.getContent() as String).isEqualTo(expectedEmailMessageBody)
    }

    @Test
    void testGetTemplateBinding() {
        final String cid = "cid-123"
        final List<TestRunInfo> testInfoList = Arrays.asList(mTestInfo)
        final Map<String, Object> binding = mEmailMessageComposer.getTemplateBinding(testInfoList, cid)

        assertThat(binding['cid']).isEqualTo(cid)
        assertThat(binding['emailTestInfoList']).isSameAs(testInfoList)
        assertThat(binding['testCaseBaseUrl'] as String).isEqualTo(
                "${mTestPalmInfo.baseUiUrl}/testcase/${mTestPalmInfo.projectId}-".toString())
    }

    @Test
    void testComposeMessage() {
        final MimeBodyPart mainPart = new MimeBodyPart()
        final MimeBodyPart iconPart = new MimeBodyPart()
        final EmailMessageComposer emailMessageComposer = new EmailMessageComposer(dummyLogger, mTestPalmInfo) {
            MimeBodyPart getIconPart(final String cid) {
                return iconPart
            }

            MimeBodyPart getMainPart(final Map<String, Object> binding) {
                return mainPart
            }
        }
        final MimeMultipart message = emailMessageComposer.composeMessage(Collections.EMPTY_LIST)
        assertThat(message.getBodyPart(0)).isSameAs(mainPart)
        assertThat(message.getBodyPart(1)).isSameAs(iconPart)
    }

    private String expectedEmailMessageBody = """\
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <meta name="viewport" content="width=device-width">
    <style type="text/css" id="media-query">
        body {
            margin: 0;
            padding: 0;
        }

        table, tr, td {
            vertical-align: top;
            border-collapse: collapse;
        }

        * {
            line-height: inherit;
        }
    </style>
</head>
<body class="clean-body" style="margin: 0;padding: 0;-webkit-text-size-adjust: 100%;background-color: #e2eace">

<span class="preheader" style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">
    FAILURE
    tests: 85/95
</span>

<span class="preheader" style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">
    FAILURE
    tests: 170/190
</span>

    <table class="nl-container" style="border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;min-width: 320px;Margin: 0 auto;background-color: #e2eace;width: 100%" cellpadding="0" cellspacing="0">
        <tbody>
            <tr style="vertical-align: top">
                <td style="word-break: break-word;border-collapse: collapse !important;vertical-align: top">
                    <div style="background-color:transparent;">
                        <div style="Margin: auto;min-width: 320px;max-width: 900px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #FFFFFF;" class="block-grid ">
                            <div style="border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;">
                                <div class="col num12" style="min-width: 320px;max-width: 900px;display: table-cell;vertical-align: top;">
                                    <div style="background-color: transparent; width: 100% !important;">
                                        <div align="center" class="img-container center  autowidth  " style="padding-right: 0px;  padding-left: 0px;">
                                            <img align="center" src="cid:cid-123" style="width: 72px">
                                        </div>
                                        <p style="margin: 0;line-height: 14px;text-align: center;font-size: 12px">
                                            <span style="font-size: 28px; line-height: 33px;">
                                                <strong>Frankenstein email report</strong>
                                            </span>
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>





    
<div style="background-color:transparent;">
    <div style="Margin: 0 auto;min-width: 320px;max-width: 900px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #FFFFFF;" class="block-grid ">
        <div style="border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;">
            <div class="col num12" style="min-width: 320px;max-width: 900px;display: table-cell;vertical-align: top;">
                <div style="background-color: transparent; width: 100% !important;">
                    <hr width="84%" size="5" noshade>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="text-align:center;color:#0D0D0D;">
                            <p style="margin: 0;text-align: center">
                                <span style="font-size: 24px; line-height: 36px;">
                                    1
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#555555;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px;">
                        <div style="line-height: 14px;margin-top: 0;margin-bottom: 0;font-size: 12px;color:#555555;">
                            <ul>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Build name: testBuildName
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Build url: test.com
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    OS: 1
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Reports URL: reportsUrl
                                </span></li>
                            </ul>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <p style="margin: 0;font-size: 14px;line-height: 21px;text-align: center">
                                <span style="font-size: 17px; line-height: 25px;">
                                    tests: 85/95
                                </span><br>
                                <span style="font-size: 17px; line-height: 25px;">
                                    (all: 100, failed: 10, skipped: 5)
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <p style="margin: 0;font-size: 14px;line-height: 21px;text-align: center">
                                <span style="font-size: 17px; line-height: 25px;">
                                    test cases: 85/95
                                </span><br>
                                <span style="font-size: 17px; line-height: 25px;">
                                    (all: 100, failed: 10, skipped: 0, filtered: 5/5)
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <ul>
                                <li style="font-size: 14px; line-height: 21px; text-align: left;">
                                    <span style="font-size: 14px; line-height: 21px;">
                                        All tests duration: 00h:03m:20s
                                    </span>
                                </li>
                                <li style="font-size: 14px; line-height: 21px; text-align: left;">
                                    <span style="font-size: 14px; line-height: 21px;">
                                        Task ':test' duration: 00h:00m:10s
                                    </span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div style="background-color:transparent;">
    <div style="Margin: 0 auto;min-width: 320px;max-width: 900px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #FFFFFF;" class="block-grid ">
        <div style="border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;">
            <div class="col num12" style="min-width: 320px;max-width: 900px;display: table-cell;vertical-align: top;">
                <div style="background-color: transparent; width: 100% !important;">
                    <hr width="84%" size="5" noshade>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="text-align:center;color:#0D0D0D;">
                            <p style="margin: 0;text-align: center">
                                <span style="font-size: 24px; line-height: 36px;">
                                    1
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#555555;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px;">
                        <div style="line-height: 14px;margin-top: 0;margin-bottom: 0;font-size: 12px;color:#555555;">
                            <ul>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Build name: testBuildName
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Build url: test.com
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    OS: 1
                                </span></li>
                                <li style="font-size: 12px; line-height: 18px;"><span style="font-size: 14px; line-height: 21px;">
                                    Reports URL: reportsUrl
                                </span></li>
                            </ul>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <p style="margin: 0;font-size: 14px;line-height: 21px;text-align: center">
                                <span style="font-size: 17px; line-height: 25px;">
                                    tests: 85/95
                                </span><br>
                                <span style="font-size: 17px; line-height: 25px;">
                                    (all: 100, failed: 10, skipped: 5)
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <p style="margin: 0;font-size: 14px;line-height: 21px;text-align: center">
                                <span style="font-size: 17px; line-height: 25px;">
                                    test cases: 85/95
                                </span><br>
                                <span style="font-size: 17px; line-height: 25px;">
                                    (all: 100, failed: 10, skipped: 0, filtered: 5/5)
                                </span>
                            </p>
                        </div>
                    </div>
                    <div style="color:#0D0D0D;line-height:150%; padding-right: 10px; padding-left: 10px; padding-top: 20px; padding-bottom: 10px;">
                        <div style="font-size:12px;line-height:18px;color:#0D0D0D;text-align:left;">
                            <ul>
                                <li style="font-size: 14px; line-height: 21px; text-align: left;">
                                    <span style="font-size: 14px; line-height: 21px;">
                                        All tests duration: 00h:03m:20s
                                    </span>
                                </li>
                                <li style="font-size: 14px; line-height: 21px; text-align: left;">
                                    <span style="font-size: 14px; line-height: 21px;">
                                        Task ':test' duration: 00h:00m:10s
                                    </span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>







            </td>
        </tr>
    </tbody>
</table>

</body>
</html>\
"""
}
