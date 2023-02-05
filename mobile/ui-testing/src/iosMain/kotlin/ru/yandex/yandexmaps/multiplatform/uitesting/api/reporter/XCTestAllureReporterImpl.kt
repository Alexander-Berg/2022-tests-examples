package ru.yandex.yandexmaps.multiplatform.uitesting.api.reporter

import platform.CoreFoundation.CFStringRef
import platform.CoreServices.UTTypeCreatePreferredIdentifierForTag
import platform.CoreServices.kUTTagClassMIMEType
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSString
import platform.XCTest.XCTActivityProtocol
import platform.XCTest.XCTAttachment
import platform.XCTest.XCTContext
import platform.XCTest.XCTExpectFailure
import platform.XCTest.xctFail
import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.toNsData
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.ReporterLink
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

public class XCTestAllureReporterImpl : TestRunnerReporter {

    private var currentActivity: XCTActivityProtocol? = null

    public override fun suite(name: String, block: () -> Unit) {
        XCTContext.runActivityNamed("allure.label.suite:$name") {}
        block()
    }

    public override fun case(name: String, tryIndex: Int, retryCount: Int, link: ReporterLink?, block: () -> Unit) {
        XCTContext.runActivityNamed("allure.name:$name") {}
        if (link != null) {
            XCTContext.runActivityNamed("allure.link.${link.name}:${link.url}") {}
        }
        block()
    }

    public override fun step(name: String, block: () -> Unit) {
        XCTContext.runActivityNamed(name) { activity ->
            currentActivity = activity
            block()
            currentActivity = null
        }
    }

    public override fun attachment(name: String, data: ByteArray, mimeType: String, fileExtension: String) {
        currentActivity?.let { activity ->
            attachment(activity, name, data, mimeType)
        } ?: run {
            XCTContext.runActivityNamed("Attachment") { activity ->
                attachment(activity, name, data, mimeType)
            }
        }
    }

    private fun attachment(activity: XCTActivityProtocol?, name: String, data: ByteArray, mimeType: String) {
        val mimeTypeCFStringRef = CFBridgingRetain(mimeType as NSString) as CFStringRef
        val uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeTypeCFStringRef, null) ?: run {
            xctFail("Unable to create uniformTypeIdentifier")
            return
        }
        val utiString = CFBridgingRelease(uti) as String
        activity?.addAttachment(
            XCTAttachment.attachmentWithUniformTypeIdentifier(utiString, name, data.toNsData(), null)
        )
    }
}
