//
//  PresenterTests.swift
//  PresenterTests
//
//  Created by Aleksey Makhutin on 16.04.2021.
//

import XCTest
import Utils
import TestUtils
@testable import PrintMessage

internal final class PresenterTests: XCTestCase {
    private var model: Model {
        return Model(messageID: 1,
                     subject: "subject",
                     to: [.init(name: "toName", email: "toEmail")],
                     cc: [.init(name: "ccName", email: "ccEmail")],
                     from: [.init(name: "fromName", email: "fromEmail"), .init(name: "fromName2", email: "fromEmail2")],
                     date: "date",
                     bodies: ["body", "body2"],
                     inlineAttachmentsURLs: ["123": URL(string: "https://test.ru")!])
    }

    func testNormalScenario() {
        func runScenario(finishWithPrinting: Bool) {
            let render = MockRender()
            let router = MockRouter()
            let mode = self.model
            let testLogger = TestLogger()
            let analytics = MockAnalytics()
            let delegate = Delegate()

            let presenter = Presenter(render: render, router: router, model: mode, logger: testLogger, analytics: analytics)
            presenter.delegate = delegate
            presenter.start()

            assertEqual(model: model, props: render.props)

            render.finishRender(error: false)

            XCTAssertEqual(render.props, (router.formatter as? MockFormatter)?.props)
            XCTAssertEqual(model.subject, router.jobName)
            XCTAssertFalse(delegate.isFinish)

            router.closePrintInteractionControllerWith(result: .success(finishWithPrinting))

            if finishWithPrinting {
                XCTAssertEqual(analytics.eventChain, [.open, .finishPrinting])
            } else {
                XCTAssertEqual(analytics.eventChain, [.open, .cancelPrinting])
            }
            XCTAssertEqual(analytics.messageID, model.messageID)
            XCTAssertTrue(testLogger.errors.isEmpty)
            XCTAssertFalse(router.wasErrorShown)
            XCTAssertTrue(delegate.isFinish)
        }
        runScenario(finishWithPrinting: true)
        runScenario(finishWithPrinting: false)
    }

    func testRenderErrorScenario() {
        let render = MockRender()
        let router = MockRouter()
        let mode = self.model
        let testLogger = TestLogger()
        let analytics = MockAnalytics()
        let delegate = Delegate()

        let presenter = Presenter(render: render, router: router, model: mode, logger: testLogger, analytics: analytics)
        presenter.delegate = delegate
        presenter.start()

        assertEqual(model: model, props: render.props)

        render.finishRender(error: true)

        XCTAssertNil((router.formatter as? MockFormatter)?.props)
        XCTAssertNil(router.jobName)
        XCTAssertTrue(router.wasErrorShown)
        XCTAssertTrue(delegate.isFinish)
        XCTAssertEqual(analytics.messageID, model.messageID)
        XCTAssertEqual(analytics.eventChain, [.error])
    }

    func testPrintError() {
        let render = MockRender()
        let router = MockRouter()
        let mode = self.model
        let testLogger = TestLogger()
        let analytics = MockAnalytics()
        let delegate = Delegate()

        let presenter = Presenter(render: render, router: router, model: mode, logger: testLogger, analytics: analytics)
        presenter.delegate = delegate
        presenter.start()

        assertEqual(model: model, props: render.props)

        render.finishRender(error: false)

        XCTAssertEqual(render.props, (router.formatter as? MockFormatter)?.props)
        XCTAssertEqual(model.subject, router.jobName)
        XCTAssertFalse(delegate.isFinish)

        router.closePrintInteractionControllerWith(result: .failure(TestError.testError))

        XCTAssertEqual(analytics.eventChain, [.open, .error])

        XCTAssertEqual(analytics.messageID, model.messageID)
        XCTAssertTrue(testLogger.errors.first?.contains("\(TestError.testError.localizedDescription)") ?? false)
        XCTAssertTrue(router.wasErrorShown)
        XCTAssertTrue(delegate.isFinish)
    }

    private func assertEqual(model: Model?, props: Props?) {
        XCTAssertEqual(model?.bodies.joined(separator: "\n"), props?.body)
        XCTAssertEqual(model?.subject, props?.subject)

        let to = model?.to.map { "\($0.name ?? "") <\($0.email ?? "")>" }
        XCTAssertEqual(to, props?.toRecipients)
        let cc = model?.cc.map { "\($0.name ?? "") <\($0.email ?? "")>" }
        XCTAssertEqual(cc, props?.ccRecipients)
        let from = model?.from.map { "\($0.name ?? "") <\($0.email ?? "")>" }
        XCTAssertEqual(from, props?.fromRecipients)

        XCTAssertEqual(model?.date, props?.date)
        XCTAssertEqual(model?.inlineAttachmentsURLs.first?.value.absoluteString, props?.contentIDsToImageURL.first?.value)
        XCTAssertEqual(model?.inlineAttachmentsURLs.first?.key, props?.contentIDsToImageURL.first?.key)
    }
}

extension PresenterTests {
    private final class MockFormatter: UIViewPrintFormatter {
        var props: Props

        init(props: Props) {
            self.props = props
        }
    }

    private final class MockRender: Rendering {
        var props: Props?

        func render(props: Props) {
            self.props = props
        }

        func finishRender(error: Bool) {
            guard let props = self.props else { return }

            props.didLoadWithPrintFormatter(error ? nil : MockFormatter(props: props))
        }
    }

    private final class MockRouter: Routing {
        var formatter: UIPrintFormatter?
        var jobName: String?
        var wasErrorShown = false

        var completionHandler: ((Result<Bool>) -> Void)?

        func openPrintInteractionController(formatter: UIPrintFormatter, jobName: String, completionHandler: @escaping (Result<Bool>) -> Void) {
            self.formatter = formatter
            self.jobName = jobName
            self.completionHandler = completionHandler
        }

        func showError() {
            self.wasErrorShown = true
        }

        func closePrintInteractionControllerWith(result: Result<Bool>) {
            self.completionHandler?(result)
        }
    }

    private final class MockAnalytics: Analytics {
        var messageID: Int64?
        var eventChain = [Event]()

        func log(event: Event, messageID: Int64) {
            self.eventChain.append(event)
            self.messageID = messageID
        }
    }

    private final class Delegate: PresenterDelegate {
        var isFinish = false

        func presenterDidFinish(_ presenter: Presenter) {
            if isFinish {
                XCTFail("Should execute one")
            }
            self.isFinish = true
        }
    }

    private enum TestError: Error {
        case testError
    }
}

extension Props: Equatable {
    static public func == (lhs: Self, rhs: Self) -> Bool {
        return lhs.body == rhs.body
            && lhs.contentIDsToImageURL == rhs.contentIDsToImageURL
            && lhs.date == rhs.date
            && lhs.fromRecipients == rhs.fromRecipients
            && lhs.toRecipients == rhs.toRecipients
            && lhs.subject == rhs.subject
    }
}
