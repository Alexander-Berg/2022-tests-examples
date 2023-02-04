//
//  BaseTest.swift
//  UITests
//
//  Created by Victor Orlovsky on 25/03/2019.
//
import XCTest
import Snapshots

protocol StepsContext {
    var name: String { get }
    var screenshotIndex: Int { get set }
    var waiter: XCTWaiter { get }
    var analyticsEventService: AnalyticsEventService { get }
}

class BaseSteps {
    enum Const {
        static let timeout: TimeInterval = 10
    }

    private var screenshotIndex = 0
    private var downloadsFolder: URL = {
        let fm = FileManager.default
        let folder = fm.urls(for: .downloadsDirectory, in: .userDomainMask)[0]
        var isDirectory: ObjCBool = false
        if !(fm.fileExists(atPath: folder.path, isDirectory: &isDirectory) && isDirectory.boolValue) {
            try! fm.createDirectory(at: folder, withIntermediateDirectories: false, attributes: nil) }
        print(folder)
        return folder
    }()

    private var indexFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumIntegerDigits = 3
        formatter.minimumIntegerDigits = 3
        return formatter
    }()

    var app: XCUIApplication
    var rootElement: XCUIElement
    var baseScreen: BaseScreen
    var context: StepsContext

    required init(context: StepsContext, root: XCUIElement? = nil) {
        let app = XCUIApplication.make()
        self.app = app
        self.rootElement = root ?? app
        self.baseScreen = BaseScreen(app)
        self.context = context
    }

    static let allowSystemAlertButtons = ["Allow Tracking", "Allow While Using App", "Allow Access to All Photos", "Keep Current Selection", "Allow", "OK"]
    @discardableResult
    func handleSystemAlertIfNeeded(allowButtons: [String] = BaseSteps.allowSystemAlertButtons) -> Self {
        let alert = XCUIApplication(bundleIdentifier: "com.apple.springboard").alerts.firstMatch
        return handleAlert(alert: alert, allowButtons: allowButtons)
    }

    @discardableResult
    func handleAlert(alert: XCUIElement, allowButtons: [String] = BaseSteps.allowSystemAlertButtons) -> Self {
        var result = false
        return handleAlert(alert: alert, allowButtons: allowButtons, result: &result)
    }

    @discardableResult
    func handleAlert(alert: XCUIElement, allowButtons: [String] = BaseSteps.allowSystemAlertButtons, result: inout Bool) -> Self {
        result = false
        guard alert.isFullyVisible() else {
            return self
        }
        for buttonTitle in allowButtons {
            let allowButton = alert.buttons[buttonTitle]
            if allowButton.isFullyVisible() {
                allowButton.tap()
                result = true
                return self
            }
        }
        return self
    }

    @discardableResult
    func wait(for seconds: UInt32 = 3) -> Self {
        Step("Ждем \(seconds) сек.") {
            sleep(seconds)
        }
        return self
    }

    @discardableResult
    func waitForever() -> Self {
        repeat {
            sleep(20)
        } while true
    }

    func onMainScreen() -> MainScreen {
        return baseScreen.on(screen: MainScreen.self)
    }

    func onLoginScreen() -> LoginScreen {
        return baseScreen.on(screen: LoginScreen.self)
    }

    func onUserProfileScreen() -> UserProfileScreen {
        return baseScreen.on(screen: UserProfileScreen.self)
    }

    func onEmptyFavoritesScreen() -> EmptyFavoritesScreen {
        return baseScreen.on(screen: EmptyFavoritesScreen.self)
    }

    func `as`<Steps: BaseSteps>(_ type: Steps.Type) -> Steps {
        return Steps(context: self.context)
    }

    @discardableResult
    func tap(_ selector: String) -> BaseSteps {
        baseScreen.find(by: selector).firstMatch.tap()
        return self
    }

    func tapStatusBar() -> Self {
        XCUIApplication.make().coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.02)).tap()
        return self
    }

    @discardableResult
    func exist(selector: String) -> Self {
        let element = baseScreen.find(by: selector).firstMatch
        element.shouldExist(timeout: Const.timeout)
        return self
    }

    @discardableResult
    func exist(anyOf selectors: String...) -> Self {
        Step("Проверяем существование хотя бы одного элемента из \(selectors.joined(separator: ", "))")
        let elements = selectors.map { ($0, baseScreen.find(by: $0).firstMatch) }
        XCTAssertTrue(
            elements.contains(where: ({
                                        if $0.1.exists {
                                            Step("\($0.0) существует")
                                        }
                                        return $0.1.exists })) ||
            elements.contains(where: ({
                if $0.1.waitForExistence(timeout: Const.timeout) {
                    Step("\($0.0) существует")
                    return true
                } else {
                    return false
                }
            })), "")
        return self
    }

    @discardableResult
    func notExist(selector: String) -> Self {
        let element = baseScreen.find(by: selector).firstMatch
        element.shouldNotExist(timeout: Const.timeout)
        return self
    }

    @discardableResult
    func validateSnapshot(of identifier: String,
                          perPixelTolerance: Double = Snapshot.defaultPerPixelTolerance,
                          overallTolerance: Double = Snapshot.defaultOverallTolerance,
                          ignoreEdges: UIEdgeInsets = .zero,
                          file: StaticString = #filePath,
                          snapshotId: String = #function) -> Self {
        let element = app.descendants(matching: .any).matching(identifier: identifier).firstMatch
        return validateSnapshot(of: element,
                                perPixelTolerance: perPixelTolerance,
                                overallTolerance: overallTolerance,
                                ignoreEdges: ignoreEdges,
                                file: file,
                                snapshotId: snapshotId)
    }

    @discardableResult
    func validateSnapshot(of element: XCUIElement,
                          perPixelTolerance: Double = Snapshot.defaultPerPixelTolerance,
                          overallTolerance: Double = Snapshot.defaultOverallTolerance,
                          ignoreEdges: UIEdgeInsets = .zero,
                          file: StaticString = #filePath,
                          snapshotId: String = #function) -> Self {
        element.shouldExist()
        let screenshot = element.screenshot().image
        Snapshot.compareWithSnapshot(image: screenshot,
                                     identifier: snapshotId,
                                     perPixelTolerance: perPixelTolerance,
                                     ignoreEdges: ignoreEdges,
                                     file: file)
        return self
    }

    @discardableResult
    func step(_ title: String, step: () -> Void) -> Self {
        Step(title) { step() }
        return self
    }

    func stepToNext<StepType: BaseSteps>(_ title: String, element: () -> XCUIElement?) -> StepType {
        XCTContext.runActivity(named: title) { _ in
            StepType(context: context, root: element())
        }
    }
}
