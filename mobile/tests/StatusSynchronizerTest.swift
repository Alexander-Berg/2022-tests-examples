//
//  StatusSynchronizerTest.swift
//  StatusSynchronizerTest
//
//  Created by Timur Turaev on 25.07.2021.
//

import XCTest
import Foundation
import Dispatch
import Utils
import TestUtils
@testable import ExclusiveEmail

internal final class StatusSynchronizerTest: XCTestCase {
    private var notificationPresenter: TestStatusSynchronizerDelegate!
    private var dataSource: TestStatusSynchronizerDataSource!
    private var logger: TestLogger!

    private var loaderRequestLogins: [String] = .empty
    private var domainLoaderResult: Result<DomainStatusModel> = .success(.notFound)

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.logger = TestLogger()
        self.loaderRequestLogins = .empty
        self.dataSource = TestStatusSynchronizerDataSource()
        self.notificationPresenter = TestStatusSynchronizerDelegate()
    }

    func testSimpleSynchronization() throws {
        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        let sync1 = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync1.fulfill()
        }
        self.wait(for: [sync1], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)

        let sync2SameLogin = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync2SameLogin.fulfill()
        }
        self.wait(for: [sync2SameLogin], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)

        let syncNoLoader = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "") {
            syncNoLoader.fulfill()
        }
        self.wait(for: [syncNoLoader], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testTwoSerialSync() throws {
        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        let sync = self.expectation(description: #function)
        sync.expectedFulfillmentCount = 2
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test"]) // build only one loader
    }

    func testBadStatusResponse() throws {
        self.domainLoaderResult = .failure(TestError.error)
        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        let sync = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test"])

        let sync2 = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync2.fulfill()
        }
        self.wait(for: [sync2], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test", "test"])
    }

    func testFoundExlusiveEmail() throws {
        self.domainLoaderResult = .success(.found(.init(email: Email(login: "a", domain: "b"),
                                                        status: .registered,
                                                        registerAllowed: false,
                                                        nextAvailableRegisterDate: Date())))
        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        let sync = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 3)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test"])
        XCTAssertEqual(self.notificationPresenter.notificationLogins, []) // no listening --> no notification
        XCTAssertEqual(self.notificationPresenter.notificationEmails, []) // no listening --> no notification
    }

    func testFoundExlusiveEmailAndListening() throws {
        func check(prevStatus: ExclusiveEmailStatusModel.Status?) {
            try? self.setUpWithError()

            let email = Email(login: "a", domain: "b")
            self.domainLoaderResult = .success(.found(.init(email: email,
                                                            status: .registered,
                                                            registerAllowed: false,
                                                            nextAvailableRegisterDate: Date())))
            self.dataSource.loginsToListenExclusiveEmailActivation.append("test")
            if let prevStatus = prevStatus {
                self.dataSource[exclusiveEmailStatusForLogin: "test"] = prevStatus.rawValue
            }

        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        let sync = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

            switch prevStatus {
            case .registered:
                XCTAssertEqual(self.logger.infos.count, 2)
                XCTAssertEqual(self.logger.errors.count, 0)
                XCTAssertEqual(self.loaderRequestLogins, ["test"])
                XCTAssertEqual(self.notificationPresenter.notificationLogins, [])
                XCTAssertEqual(self.notificationPresenter.notificationEmails, [])
            default:
                XCTAssertEqual(self.logger.infos.count, 4)
                XCTAssertEqual(self.logger.errors.count, 0)
                XCTAssertEqual(self.loaderRequestLogins, ["test"])
                XCTAssertEqual(self.notificationPresenter.notificationLogins, ["test"])
                XCTAssertEqual(self.notificationPresenter.notificationEmails, [email.description])
            }
        }

        check(prevStatus: nil)
        check(prevStatus: .pending)
        check(prevStatus: .registered)
        check(prevStatus: .deleted)
        check(prevStatus: .emailNotAvailable)
        check(prevStatus: .infoNotAvailable)
        check(prevStatus: .subscriptionExpired)
    }

    func testListeningStatus() throws {
        let synchronizer = StatusSynchronizer(domainStatusLoadingProvider: self.buildDomainStatusLoader(forLogin:),
                                              dataSource: self.dataSource,
                                              delegate: self.notificationPresenter,
                                              logger: self.logger)
        synchronizer.addLoginToListenActivation("test1")
        let email = Email(login: "a", domain: "b")
        self.domainLoaderResult = .success(.found(.init(email: email,
                                                        status: .registered,
                                                        registerAllowed: false,
                                                        nextAvailableRegisterDate: Date())))

        let sync = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test1") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        let sync2 = self.expectation(description: #function)
        synchronizer.startDomainStatusSynchronization(forLogin: "test1") {
            sync2.fulfill()
        }
        self.wait(for: [sync2], timeout: 1)

        XCTAssertEqual(self.notificationPresenter.notificationLogins, ["test1"])
        XCTAssertEqual(self.loaderRequestLogins, Array(repeating: "test1", count: 2))
        XCTAssertTrue(self.dataSource.loginsToListenExclusiveEmailActivation.isEmpty)
        XCTAssertEqual(self.dataSource.loginMemory, ["test1": true])
    }

    private func buildDomainStatusLoader(forLogin login: String) -> DomainStatusLoading? {
        self.loaderRequestLogins.append(login)

        return login.makeNilIfEmpty.map { _ in return TestDomainStatusLoader(domainLoaderResult: self.domainLoaderResult) }
    }
}

private enum TestError: Error {
    case error
}

private final class TestDomainStatusLoader: DomainStatusLoading {
    let domainLoaderResult: Result<DomainStatusModel>

    init(domainLoaderResult: Result<DomainStatusModel>) {
        self.domainLoaderResult = domainLoaderResult
    }

    func loadDomainStatus(completion: @escaping (Result<DomainStatusModel>) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(10)) {
            completion(self.domainLoaderResult)
        }
    }
}

private final class TestStatusSynchronizerDataSource: StatusSynchronizerDataSource {
    var loginMemory: [String: Bool] = .empty
    var statusMemorry: [String: String] = .empty
    var loginsToListenExclusiveEmailActivation: [String] = .empty

    subscript(exclusiveEmailStatusForLogin login: String) -> String? {
        get {
            return self.statusMemorry[login]
        }
        set(newValue) {
            self.statusMemorry[login] = newValue
        }
    }

    func hasActiveExclusiveEmail(for login: String) -> Bool? {
        return self.loginMemory[login] ?? false
    }

    func set(hasActiveExclusiveEmail: Bool, for login: String) {
        self.loginMemory[login] = hasActiveExclusiveEmail
    }
}

private final class TestStatusSynchronizerDelegate: StatusSynchronizerDelegate {
    var notificationLogins: [String] = .empty
    var notificationEmails: [String] = .empty

    func statusSynchronizerDidRequestToShowNotificationAboutExclusiveEmailForLogin(_ login: String, email: String) {
        self.notificationLogins.append(login)
        self.notificationEmails.append(email)
    }
}
