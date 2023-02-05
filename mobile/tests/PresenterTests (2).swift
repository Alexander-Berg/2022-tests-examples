//
//  UnsubscribeMessageTests.swift
//  UnsubscribeMessageTests
//
//  Created by Timur Turaev on 17.02.2021.
//

import XCTest
import TestUtils
import Utils
@testable import UnsubscribeMessage

internal final class PresenterTests: XCTestCase {
    private var newsletter: Newsletter!
    private var logger: TestLogger!
    private var router: TestRouter!
    private var networkPerformer: TestNetworkPerformer!
    private var render: TestRender!

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.logger = TestLogger()
        self.networkPerformer = TestNetworkPerformer()
        self.router = TestRouter()
        self.render = TestRender()
        self.newsletter = Newsletter(email: "email@e.mail",
                                     displayName: "Display Name",
                                     messageType: 13,
                                     destinationFolderToUnsubscribe: 3,
                                     avatar: nil)
    }

    func testNormalScenario() throws {
        func testScenario(trashUnsubscribedMessages: Bool) throws {
            try self.setUpWithError()

            let presenter = Presenter(newsletter: self.newsletter,
                                      networkPerformer: self.networkPerformer,
                                      router: self.router,
                                      render: self.render,
                                      logger: self.logger)
            presenter.start()

            XCTAssertNotNil(self.render.props?.initialProps)
            XCTAssertFalse(self.render.props!.initialProps!.inProcessing)

            if trashUnsubscribedMessages {
                XCTAssertFalse(self.render.props!.initialProps!.trashUnsubscribedMessages)

                self.render.props!.initialProps!.selectTrashUnsubscribedMessagesCommand(true)
                XCTAssertTrue(self.render.props!.initialProps!.trashUnsubscribedMessages)

                self.render.props!.initialProps!.selectTrashUnsubscribedMessagesCommand(false)
                XCTAssertFalse(self.render.props!.initialProps!.trashUnsubscribedMessages)

                self.render.props!.initialProps!.selectTrashUnsubscribedMessagesCommand(true)
                XCTAssertTrue(self.render.props!.initialProps!.trashUnsubscribedMessages)
            }

            self.render.props!.initialProps!.unsubscribeMessageCommand()

            XCTAssertTrue(self.render.props!.initialProps!.inProcessing)

            self.wait(for: {
                return self.render.props!.finishProps != nil
            }, timeout: 1.0)

            XCTAssertEqual(self.networkPerformer.trashUnsubscribedMessages, [trashUnsubscribedMessages])
            XCTAssertTrue(self.logger.errors.isEmpty)
            XCTAssertTrue(self.logger.infos.isEmpty)

            XCTAssertEqual(self.router.events, [.done(trash: trashUnsubscribedMessages)])

            self.render.props!.finishProps!.doneCommand()
            XCTAssertEqual(self.router.events, [.done(trash: trashUnsubscribedMessages),
                                                .close])
        }

        try testScenario(trashUnsubscribedMessages: false)
        try testScenario(trashUnsubscribedMessages: true)
    }

    func testScenarioWithErrors() throws {
        func testScenario(trashUnsubscribedMessages: Bool) throws {
            try self.setUpWithError()
            self.networkPerformer.completeWithError = true

            let presenter = Presenter(newsletter: self.newsletter,
                                      networkPerformer: self.networkPerformer,
                                      router: self.router,
                                      render: self.render,
                                      logger: self.logger)
            presenter.start()

            XCTAssertNotNil(self.render.props?.initialProps)
            XCTAssertFalse(self.render.props!.initialProps!.inProcessing)

            if trashUnsubscribedMessages {
                self.render.props!.initialProps!.selectTrashUnsubscribedMessagesCommand(true)
            }

            // first try
            self.render.props!.initialProps!.unsubscribeMessageCommand()
            XCTAssertTrue(self.render.props!.initialProps!.inProcessing)
            self.wait(for: {
                return self.render.props!.errorProps != nil
            }, timeout: 1.0)
            XCTAssertFalse(self.render.props!.errorProps!.inProcessing)
            XCTAssertEqual(self.networkPerformer.trashUnsubscribedMessages, [trashUnsubscribedMessages])
            XCTAssertEqual(self.logger.errors.count, 1)

            // second try
            self.render.props!.errorProps!.unsubscribeMessageCommand()
            XCTAssertTrue(self.render.props!.errorProps!.inProcessing)
            self.wait(for: {
                return self.render.props!.errorProps!.inProcessing == false
            }, timeout: 1.0)
            XCTAssertEqual(self.networkPerformer.trashUnsubscribedMessages, [trashUnsubscribedMessages, trashUnsubscribedMessages])
            XCTAssertEqual(self.logger.errors.count, 2)

            // disable errors and retry
            self.networkPerformer.completeWithError = false

            self.render.props!.errorProps!.unsubscribeMessageCommand()
            XCTAssertTrue(self.render.props!.errorProps!.inProcessing)
            self.wait(for: {
                return self.render.props!.finishProps != nil
            }, timeout: 1.0)
            XCTAssertEqual(self.networkPerformer.trashUnsubscribedMessages, [trashUnsubscribedMessages,
                                                                             trashUnsubscribedMessages,
                                                                             trashUnsubscribedMessages])
            XCTAssertEqual(self.logger.errors.count, 2)
            XCTAssertEqual(self.router.events, [.done(trash: trashUnsubscribedMessages)])

            self.render.props!.finishProps!.doneCommand()
            XCTAssertEqual(self.router.events, [.done(trash: trashUnsubscribedMessages), .close])
        }

        try testScenario(trashUnsubscribedMessages: false)
        try testScenario(trashUnsubscribedMessages: true)
    }

    func testOpeningUnsubscribeManager() {
        let presenter = Presenter(newsletter: self.newsletter,
                                  networkPerformer: self.networkPerformer,
                                  router: self.router,
                                  render: self.render,
                                  logger: self.logger)
        presenter.start()

        self.render.props!.initialProps!.unsubscribeMessageCommand()

        self.wait(for: {
            return self.render.props!.finishProps != nil
        }, timeout: 1.0)

        self.render.props!.finishProps!.openUnsubscribeManagerCommand()
        XCTAssertEqual(self.router.events, [.done(trash: false), .close, .openManager])
    }
}

private final class TestRender: Rendering {
    var props: Props?

    func render(props: Props) {
        self.props = props
    }
}

private final class TestRouter: Routing {
    enum Event: Equatable {
        case close
        case openManager
        case done(trash: Bool)
    }

    var events: [Event] = .empty

    func openUnsubscribeManager() {
        self.events.append(.openManager)
    }

    func close() {
        self.events.append(.close)
    }

    func processUnsubscriptionCompletion(withTrashMessages: Bool) {
        self.events.append(.done(trash: withTrashMessages))
    }
}

private extension Props {
    var initialProps: InitialProps? {
        if case let .initial(props) = self {
            return props
        }
        return nil
    }

    var errorProps: ErrorProps? {
        if case let .error(props) = self {
            return props
        }
        return nil
    }

    var finishProps: FinishProps? {
        if case let .finish(props) = self {
            return props
        }
        return nil
    }
}
