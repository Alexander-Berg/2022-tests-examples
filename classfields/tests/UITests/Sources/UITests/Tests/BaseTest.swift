import AppServer
import Snapshots
import SwiftProtobuf
import XCTest

struct AppLaunchOptions {
    enum LaunchType {
        case deeplink(String)
        case `default`
    }

    enum LaunchAction {
        case swipeDown
        case tap
        case none
    }

    var launchType: LaunchType = .default
    var overrideAppSettings: [String: Any] = [:]
    var userDefaults: [String: Any] = [:]
    var environment: [String: String] = [:]
    var launchAction: LaunchAction = .swipeDown

    static let `default` = Self(launchType: .default, overrideAppSettings: [:])
}

class BaseTest: XCTestCase {

    let app = XCUIApplication.make()
    var screenshotIndex: Int = 0
    lazy var mocker = Mocker()

    lazy var waiter = XCTWaiter(delegate: self)

    /// Don't use StubServer, user Mocker instead
    var server: StubServer { mocker.server }
    var port: UInt16 { mocker.port }

    var alertHandler: NSObjectProtocol?
    private lazy var steps = MainSteps(context: self)

    /// Don't use BasicMockReproducer, user Mocker instead
    var basicMockReproducer: BasicMockReproducer { mocker.basicMockReproducer }

    /// Don't use AdvancedMockReproducer, user Mocker instead
    var advancedMockReproducer: AdvancedMockReproducer { mocker.advancedMockReproducer }

    let appClient = AppClient()

    var appSettings: [String: Any] {
        return [
            "darkModeEnabled": false,
            "autoRecognizeVin": "RUMKE8938EV028756",
            "shouldUseVarity": false,
            "requiresCustomSSLChain": true,
            "shouldForceJSON": true,
            "shouldRecordAPIMock": false,
            "currentHosts": [
                "PublicAPI": "http://127.0.0.1:\(mocker.port)/"
            ],
            "enablePushReminderCooldownTimestamp": Date.distantFuture.timeIntervalSince1970,
            "enablePushReminderFirstPresented": true,
            "widgetPromoSavedFiltersHidden": true,
            "app2appStartupPromoIsHidded": true,
            "ssrBaseServer": "http://127.0.0.1:\(mocker.port)/"
        ]
    }

    var launchEnvironment: [String: String] {
        return [:]
    }

    override func setUp() {
        super.setUp()
        app.launchArguments.append("--resetDefaults")
        app.launchArguments.append("--recreateDB")
        app.launchArguments.append("--clearDocuments")

        let appServerPort: UInt16 = .random(in: 1_024 ..< UInt16.max)
        app.launchEnvironment["APP_SERVER_PORT"] = "\(appServerPort)"
        appClient.run(host: "127.0.0.1", port: appServerPort)
    }

    func launch(options: AppLaunchOptions) {
        continueAfterFailure = TestsLaunchParametersHelper.shouldCollectNewOrUnmatchedSnapshots
        executionTimeAllowance = 300

        var settings = appSettings
        settings.merge(options.overrideAppSettings, uniquingKeysWith: { _, second in second })

        let jsonData = try! JSONSerialization.data(withJSONObject: settings, options: [])

        app.launchEnvironment["APP_SETTINGS"] = jsonData.base64EncodedString()
        app.launchArguments.append("--UITests")

        if case let .deeplink(link) = options.launchType {
            app.launchEnvironment["LAUNCH_DEEPLINK_URL"] = link
        }

        var launchEnvironment = launchEnvironment

        if !options.userDefaults.isEmpty {
            let key = "STANDARD_USER_DEFAULTS"

            var userDefaults = options.userDefaults

            if let currentValue = launchEnvironment[key] {
                let defaults = try! JSONSerialization
                    .jsonObject(with: Data(base64Encoded: currentValue)!, options: []) as! [String: Any]

                userDefaults.merge(defaults, uniquingKeysWith: { first, _ in first })
            }

            launchEnvironment[key] = try! JSONSerialization
                .data(withJSONObject: userDefaults, options: [])
                .base64EncodedString()
        }

        for (key, value) in options.environment {
            app.launchEnvironment[key] = value
        }

        for (key, value) in launchEnvironment {
            app.launchEnvironment[key] = value
        }

        app.launch()
        app.activate()
        addAlertHandler()

        // this is neccessary to start UIInterruptionMonitor,
        // you may want to change that to tap() to save time,
        // but swipe prevents unwanted app interactions (like tapping on filters button for example)
        switch options.launchAction {
        case .swipeDown:
            app.swipeDown()
        case .tap:
            app.tap()
        case .none:
            break
        }
    }

    func launch() {
        launch(options: .default)
    }

    func launch(link: String) {
        launch(options: .init(launchType: .deeplink(link)))
    }

    override func tearDown() {
        mocker.stopMock()
        app.terminate()
        alertHandler.map { removeUIInterruptionMonitor($0) }
    }

    private func addAlertHandler() {
        XCTContext.runActivity(named: "Alert") { _ in
            self.alertHandler = self.addUIInterruptionMonitor(withDescription: "System Dialog") { alert -> Bool in
                var result = false
                self.steps.handleAlert(alert: alert, result: &result)
                return result
            }
        }
    }

    override func record(_ issue: XCTIssue) {
        var issue = issue
        let uiHierarchy = XCTAttachment(string: app.debugDescription)
        uiHierarchy.name = "UIHierarchy"
        issue.add(uiHierarchy)
        super.record(issue)
    }
}

extension BaseTest {
    func validateSnapshots(suiteName: String, accessibilityId: String, snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let screenshot = screenshotById(acessibilityId: accessibilityId)

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.01)
    }

    func screenshotById(acessibilityId: String) -> UIImage {
        return app.descendants(matching: .any).matching(identifier: acessibilityId).firstMatch.screenshot().image
    }
}

extension BaseTest {
    func waitForRequest(
        server: StubServer,
        timeout: TimeInterval,
        requestChecker: @escaping (Request) -> Bool
    ) {
        let expectation = XCTestExpectation(description: "Wait response")
        var index = 0

        let interceptorId = server.interceptRequest { request -> (Response?, Int) in
            let exptectedReqeust = requestChecker(request)
            if exptectedReqeust {
                expectation.fulfill()
            }
            return (nil, index)
        }

        wait(for: [expectation], timeout: timeout)
        index += 1
        server.removeIntercepter(interceptorId)
    }

    func expectationForRequest(requestChecker: @escaping (Request) -> Bool) -> XCTestExpectation {
        let expectation = XCTestExpectation(description: "Wait response")
        var index = 0

        mocker.server.interceptRequest { request -> (Response?, Int) in
            let expectedRequest = requestChecker(request)
            if expectedRequest {
                expectation.fulfill()
            }
            index += 1
            return (nil, index)
        }

        return expectation
    }

    func expectationForRequest(method: String, uri: String) -> XCTestExpectation {
        return expectationForRequest { req -> Bool in
            req.method == method && req.uri.lowercased() == uri.lowercased()
        }
    }

    func expectationForRequest<T: Message>(method: String, uri: String, requestChecker: @escaping (T) -> Bool = { _ in
        true
    }) -> XCTestExpectation {
        return expectationForRequest { req -> Bool in
            guard req.method == method, req.uri.lowercased() == uri.lowercased(),
                  let data = req.messageBody,
                  let request = try? T(jsonUTF8Data: data)
            else {
                return false
            }

            return requestChecker(request)
        }
    }
}

extension BaseTest: StepsContext {
    var analyticsEventService: AnalyticsEventService {
        AppApiAnalyticsEventService(appApi: appClient)
    }
}

extension BaseTest {
    var api: API {
        API(server: mocker.server)
    }
}

private struct AppApiAnalyticsEventService: AnalyticsEventService {
    let appApi: AppApi

    func shouldEventsBeReported(
        _ events: [(name: String, properties: [String: Any])],
        file: StaticString,
        line: UInt
    ) {
        let events = events.map { event in
            AnalyticsEvent(
                name: event.name,
                properties: event.properties.mapValues(EventProperty.init(fromRaw:))
            )
        }

        appApi.shouldEventsBeReported(events)
    }

}
