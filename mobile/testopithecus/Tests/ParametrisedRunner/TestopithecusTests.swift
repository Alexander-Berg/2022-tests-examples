//
//  TestopithecusTestCase.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 20/01/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import testopithecus
import SnapshotTesting

open class TestopithecusTests: YOParametrizedTestCase {

    var runner: TestopithecusTestRunner<MailboxBuilder>!
    public private(set) var application: XCUIApplication!
    private static var properties: NSDictionary = {
        if let propertiesPath = Bundle(for: TestopithecusTests.self).path(forResource: "autotests", ofType: "plist"),
            let dict = NSDictionary(contentsOfFile: propertiesPath) {
            return dict
        }
        return [:]
    }()

    public var launchArguments: [String] {
        return []
    }

    public var shouldCleanApplication: Bool {
        return true
    }

    public override func setUp() {
        super.setUp()

        // set isRecording to true for recording screenshot
        isRecording = false

        self.continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        XCUIApplication.yandexMobileMail.terminate()
        XCUIApplication.springBoard.activate()

        if self.shouldCleanApplication {
            ApplicationState.shared.reset()
            ApplicationState.shared.general.pushNotificationSubscriptionProcessed = SpringboardPage().applicationIcon.exists
            ApplicationState.shared.general.applicationInstalled = SpringboardPage().applicationIcon.exists
        }

        self.application = XCUIApplication.yandexMobileMail
        var launchArguments = self.launchArguments + [CommandLineArguments.autoTestsRunningKey]
        if self.shouldCleanApplication {
            launchArguments.append(CommandLineArguments.cleanApplicationKey)
        }
        launchArguments.append(contentsOf: AutoTestState.shared.savedToSWCAccounts.flatMap {
            [CommandLineArguments.savedToSWCAccount] + [$0]
        })
        self.application.launchArguments = launchArguments
    }

    public override class func yo_testMethodSelectors() -> [YOQuickSelectorWrapper] {
        var quickSelectors: [YOQuickSelectorWrapper] = []
        testopithecus.Log.registerDefaultLogger(TestopithecusDefaultLogger.instance)
        Registry.get().errorThrower = DefaultErrorThrower.instance
        XPromisesFramework.setup(queue: DispatchQueue.main)
        let network = DefaultSyncNetwork()
        let jsonSerializer = DefaultJSONSerializer()
        let sleep = DefaultSyncSleep()
        let imap = DefaultImapProvider()
        let registry = getTests()
        let preparerProvider = MailboxPreparerProvider(MBTPlatform.IOS, jsonSerializer, network, TestopithecusDefaultLogger.instance, sleep, imap)
        let testsToRun = registry.getTestsPossibleToRun(MBTPlatform.IOS, MailboxModel.allSupportedFeatures, IOSMailboxApplication.staticSupportedFeatures).filter {
            registry.isTestEnabled($0, MBTPlatform.IOS, MailboxModel.allSupportedFeatures, IOSMailboxApplication.staticSupportedFeatures)
        }.sort { l, r in
            (l.description.uppercased() < r.description.uppercased() ? -1 : l.description.uppercased() == r.description.uppercased() ? 0 : 1)
        }
        for test in testsToRun {
            let runner = TestopithecusTestRunner(MBTPlatform.IOS,
                                                 test,
                                                 registry,
                                                 preparerProvider,
                                                 PublicBackendConfig.mailApplicationCredentials,
                                                 network,
                                                 jsonSerializer,
                                                 TestopithecusDefaultLogger.instance,
                                                 true)
            runner.reporter = DefaultReportIntegration()
            guard runner.isEnabled(MailboxModel.allSupportedFeatures, IOSMailboxApplication.staticSupportedFeatures) else {
                continue
            }
            let testName = test.suites[0].toString() + "." + test.description.replacingOccurrences(of: " ", with: "_")
            // Оборачиваем тестовый метод в блок, который принимает инстанс TestCase
            let block: @convention(block) (TestopithecusTests) -> Void = {
                $0.testRun(runner, testsToRun)
            }
            // Добавляем тестовый метод в класс
            let implementation = imp_implementationWithBlock(block)
            let selector = NSSelectorFromString(testName)
            class_addMethod(self, selector, implementation, "v@:")
            quickSelectors.append(YOQuickSelectorWrapper(selector: selector))
        }
        // Возвращаем обёрнутые селекторы
        return quickSelectors
    }

    public override func tearDown() {
        if self.runner != nil {
            self.runner.finish()
        }
        super.tearDown()
        XCUIApplication.yandexMobileMail.terminate()
    }

    func testRun(_ runner: TestopithecusTestRunner<MailboxBuilder>, _ testsToRun: YSArray<MBTTest<MailboxBuilder>>) {
        self.runner = runner
        if runner.isPassed() {
            runner.setInfoForReporter()
            return
        }

        self.runner.statToken = AutotestTokenProvider.shared.getToken(for: .stat)

        UserService.userServiceOauthToken = AutotestTokenProvider.shared.getToken(for: .tus)

        let file = EventusLogs.eventsFile
        do {
            try FileManager.default.removeItem(at: file)
        } catch {
            XCTFail("Can't remove file \(file), cause: \(error)")
        }

        self.application.launch()

        let expectation = self.expectation(description: "AccountsPrepared")
        var accounts: YSArray<OAuthUserAccount> = YSArray()
        runner.lockAndPrepareAccountData().then { data in
            accounts = data
            expectation.fulfill()
        }
        do {
            let result: XCTWaiter.Result = XCTWaiter().wait(for: [expectation], timeout: 2 * 60)
            if result != .completed {
                throw YSError("Accounts wan't prepeared for a \(2 * 60) seconds")
            }
            if accounts.length == 0 {
                throw YSError("Cant prepare test for run")
            }
            try runner.runTest(accounts, LoginComponent(), IOSMailboxApplication(self.runner.oauthService))
        } catch {
            runner.failed()
            if !runner.isNeedTryMore() {
                runner.sendTestsResults(testsToRun[testsToRun.length - 1].description)
                XCTFail("Test finally failed with assertion error: \(error.localizedDescription)")
            }
            testopithecus.Log.error("Test failed with assertion error (but will be retried): \(error.localizedDescription)")
        }
        runner.sendTestsResults(testsToRun[testsToRun.length - 1].description)
    }

    public class func getTests() -> TestsRegistry<MailboxBuilder> {
        let deviceTag = UIDevice.isIpad() ? DeviceType.Tab : DeviceType.Phone
        var trustedCases: YSArray<Int32>?
        do {
            trustedCases = try getTrustedCases(MBTPlatform.IOS, AutotestTokenProvider.shared.getToken(for: .testPalm), DefaultSyncNetwork(), DefaultJSONSerializer())
        } catch {
            XCTFail("Failed to get cases, error: \(error.localizedDescription)")
        }
        return AllMailTests.get
//            .registerAll(RandomWalkTest.generate(5, DefaultLogger.instance))
            .ignoreLogEvent(EventNames.MODEL_SYNC_MESSAGE_LIST)
            .onlyTestsWithCaseIds(required, trustedCases!, MBTPlatform.IOS)
            .onlyWithTag(deviceTag, MBTPlatform.IOS)
            .retries(1)
            .bucket(bucketIndex, bucketsTotal)
    }

    public class var required: Bool {
        let dict = Self.properties
        return dict["TestIsRequired"] as? Bool ?? true
    }

    public class var bucketIndex: Int32 {
        let dict = Self.properties
        let index = dict["TestBucketIndex"] as? Int32 ?? 0
        return index + bucketIndexIncrement
    }

    public class var bucketsTotal: Int32 {
        let dict = Self.properties
        return dict["TestBucketTotal"] as? Int32 ?? 1
    }

    public class var bucketIndexIncrement: Int32 {
        return 0
    }
}

private final class AutotestTokenProvider {
    static let shared = AutotestTokenProvider()

    private init() {}

    enum TestService: String {
        case testPalm = "TestPalmOAuthToken"
        case stat = "StatOAuthToken"
        case tus = "UserServiceOAuthToken"
    }

    func getToken(for service: TestService) -> String {
        if let propertiesPath = Bundle(for: type(of: self)).path(forResource: "autotests", ofType: "plist"),
            let dict = NSDictionary(contentsOfFile: propertiesPath),
            let token = dict[service.rawValue] as? String {
                    return token
        }
        fatalError("Missing \(service.rawValue)."
            + " Please TUS get token here: https://wiki.yandex-team.ru/test-user-service/#autentifikacija"
            + " Then get Stat token here: https://yav.yandex-team.ru/secret/sec-01cvpy390a83q49c4ak0bgwvjh (stat)"
            + " Then get testpalm token here: https://yav.yandex-team.ru/secret/sec-01cvpy390a83q49c4ak0bgwvjh/explore/versions (tp_oauth)"
            + " and place it into YandexMobileMail/YandexMobileMailAutoTests/autotests.plist by running:"
            + " USER_SERVICE_OAUTH_TOKEN=<you token> STAT_OAUTH_TOKEN=<your stat token> TESTPALM_OAUTH_TOKEN=<your token> ./prepare_autotests_properties.sh")
    }
}
