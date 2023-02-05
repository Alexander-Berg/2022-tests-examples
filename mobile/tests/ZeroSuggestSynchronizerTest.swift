//
//  ZeroSuggestSynchronizerTest.swift
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

internal final class ZeroSuggestSynchronizerTest: XCTestCase {
    private var dataSource: TestZeroSuggestSynchronizerDataSource!
    private var logger: TestLogger!

    private var loaderRequestLogins: [String] = .empty
    private var zeroSuggestResult: Result<ZeroSuggestionModel> = .success(ZeroSuggestionModel(email: Email.empty))

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.logger = TestLogger()
        self.loaderRequestLogins = .empty
        self.dataSource = TestZeroSuggestSynchronizerDataSource()
    }

    func testSynchronization() throws {
        let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSource(forLogin:),
                                                   dataSource: self.dataSource,
                                                   logger: self.logger)

        let sync1 = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: "test") {
            sync1.fulfill()
        }
        self.wait(for: [sync1], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)

        let sync2SameLogin = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: "test1") {
            sync2SameLogin.fulfill()
        }
        self.wait(for: [sync2SameLogin], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)

        let syncNoLoader = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: "") {
            syncNoLoader.fulfill()
        }
        self.wait(for: [syncNoLoader], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testTwoSerialSync() throws {
        let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSource(forLogin:),
                                                   dataSource: self.dataSource,
                                                   logger: self.logger)

        let sync = self.expectation(description: #function)
        sync.expectedFulfillmentCount = 2
        synchronizer.startZeroSuggestSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        synchronizer.startZeroSuggestSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test"]) // build only one loader
    }

    func testBadStatusResponse() throws {
        self.zeroSuggestResult = .failure(TestError.error)
        let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSource(forLogin:),
                                                   dataSource: self.dataSource,
                                                   logger: self.logger)

        let sync = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: "test") {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test"])

        let sync2 = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: "test") {
            sync2.fulfill()
        }
        self.wait(for: [sync2], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, ["test", "test"])
    }

    func testShouldSaveLoginEmailAndDate() throws {
        let testEmail = Email(login: "test", domain: "domain")
        let firstTestLogin = "testLogin1"
        let secondTestLogin = "testLogin2"
        self.zeroSuggestResult = .success(ZeroSuggestionModel(email: testEmail))
        let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSource(forLogin:),
                                                   dataSource: self.dataSource,
                                                   logger: self.logger)

        XCTAssertTrue(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))
        let sync = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: firstTestLogin) {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, [firstTestLogin])
        XCTAssertEqual(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.email, testEmail)
        XCTAssertNotNil(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date)
        XCTAssertTrue(Calendar.current.isDate(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date ?? Date(), equalTo: Date(), toGranularity: .minute))
        XCTAssertFalse(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))

        XCTAssertTrue(synchronizer.needUpdateZeroSuggest(forLogin: secondTestLogin))
        let sync2 = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: secondTestLogin) {
            sync2.fulfill()
        }
        self.wait(for: [sync2], timeout: 1)
        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, [firstTestLogin, secondTestLogin])
        XCTAssertEqual(self.dataSource.zeroSuggestByLogin[secondTestLogin]?.email, testEmail)
        XCTAssertNotNil(self.dataSource.zeroSuggestByLogin[secondTestLogin]?.date)
        XCTAssertTrue(Calendar.current.isDate(self.dataSource.zeroSuggestByLogin[secondTestLogin]?.date ?? Date(), equalTo: Date(), toGranularity: .second))
        XCTAssertFalse(synchronizer.needUpdateZeroSuggest(forLogin: secondTestLogin))
    }

    func testShouldUpdateEmailIfSevenDaysHavePassed() throws {
        let testEmail = Email(login: "test", domain: "domain")
        let firstTestLogin = "testLogin1"
        self.zeroSuggestResult = .success(ZeroSuggestionModel(email: testEmail))
        let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSource(forLogin:),
                                                   dataSource: self.dataSource,
                                                   logger: self.logger)

        var oldDate = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        oldDate = Calendar.current.date(byAdding: .minute, value: -1, to: oldDate) ?? Date()

        self.dataSource.zeroSuggestByLogin[firstTestLogin] = ZeroSuggestSynhronizerModel(email: Email(login: "old", domain: "domain"), date: oldDate)

        XCTAssertTrue(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))
        let sync = self.expectation(description: #function)
        synchronizer.startZeroSuggestSynchronization(forLogin: firstTestLogin) {
            sync.fulfill()
        }
        self.wait(for: [sync], timeout: 1)

        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
        XCTAssertEqual(self.loaderRequestLogins, [firstTestLogin])
        XCTAssertEqual(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.email, testEmail)
        XCTAssertNotNil(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date)
        XCTAssertTrue(Calendar.current.isDate(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date ?? Date(), equalTo: Date(), toGranularity: .second))
        XCTAssertFalse(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))
    }

    func testShouldNotUpdateEmailIfSevenDaysHaveNotPassed() throws {
        func check(daysPassed: Int) {
            let testEmail = Email(login: "test", domain: "domain")
            let firstTestLogin = "testLogin1"
            self.zeroSuggestResult = .success(ZeroSuggestionModel(email: testEmail))
            let synchronizer = ZeroSuggestSynchronizer(zeroSuggestDataSourceProvider: self.buildZeroSuggestDataSourceSync(forLogin:),
                                                       dataSource: self.dataSource,
                                                       logger: self.logger)

            let oldDate = Calendar.current.date(byAdding: .day, value: -daysPassed, to: Date()) ?? Date()

            self.dataSource.zeroSuggestByLogin[firstTestLogin] = ZeroSuggestSynhronizerModel(email: Email(login: "old", domain: "domain"), date: oldDate)

            XCTAssertFalse(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))
            synchronizer.startZeroSuggestSynchronization(forLogin: firstTestLogin, completion: nil)

            XCTAssertEqual(self.logger.errors.count, 0)
            XCTAssertNotEqual(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.email, testEmail)
            XCTAssertNotNil(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date)
            XCTAssertTrue(Calendar.current.isDate(self.dataSource.zeroSuggestByLogin[firstTestLogin]?.date ?? Date(), equalTo: oldDate, toGranularity: .second))
            XCTAssertFalse(synchronizer.needUpdateZeroSuggest(forLogin: firstTestLogin))
        }

        (0...6).forEach { days in
            check(daysPassed: days)
        }
    }

    private func buildZeroSuggestDataSource(forLogin login: String) -> ZeroSuggestDataSource? {
        self.loaderRequestLogins.append(login)

        return login.makeNilIfEmpty.map { _ in return TestZeroSuggestDataSource(zeroSuggestLoaderResult: self.zeroSuggestResult) }
    }

    private func buildZeroSuggestDataSourceSync(forLogin login: String) -> ZeroSuggestDataSource? {
        self.loaderRequestLogins.append(login)

        let dataSource = TestZeroSuggestDataSource(zeroSuggestLoaderResult: self.zeroSuggestResult)
        dataSource.synchronously = true
        return login.makeNilIfEmpty.map { _ in return dataSource }
    }
}

private enum TestError: Error {
    case error
}

private final class TestZeroSuggestDataSource: ZeroSuggestDataSource {
    let zeroSuggestLoaderResult: Result<ZeroSuggestionModel>
    var synchronously = false

    init(zeroSuggestLoaderResult: Result<ZeroSuggestionModel>) {
        self.zeroSuggestLoaderResult = zeroSuggestLoaderResult
    }

    func loadZeroSuggest(completion: @escaping (Result<ZeroSuggestionModel>) -> Void) {
        if self.synchronously {
            completion(self.zeroSuggestLoaderResult)
        } else {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(10)) {
            completion(self.zeroSuggestLoaderResult)
        }
        }
    }
}

private final class TestZeroSuggestSynchronizerDataSource: ZeroSuggestSynchronizerDataSource {
    var zeroSuggestByLogin = [String: ZeroSuggestSynhronizerModel]()
    subscript(zeroSuggestForLogin login: String) -> ZeroSuggestSynhronizerModel? {
        get {
            return self.zeroSuggestByLogin[login]
        }
        set(newValue) {
            self.zeroSuggestByLogin[login] = newValue
        }
    }
}
