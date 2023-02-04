import XCTest
import Snapshots
import UIKit

protocol _ContextProvider {
    var context: StepsContext { get }
    var rootElement: XCUIElement { get }
    var app: XCUIApplication { get }
}

protocol _InitializableContextProvider: _ContextProvider {
    init(context: StepsContext, root: XCUIElement?)
}

extension BaseSteps: _InitializableContextProvider { }

protocol UIElementProvider: _ContextProvider {
    associatedtype Element

    func find(element: Element) -> XCUIElement
    func name(of element: Element) -> String
    func identifier(of element: Element) -> String
}

protocol _UIRootedElementProvider: UIElementProvider {
    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement
    static var rootElementName: String { get }
}

protocol UIRootedElementProvider: _UIRootedElementProvider {
    static var rootElementID: String { get }
}

extension UIRootedElementProvider {
    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: Self.rootElementID).firstMatch
    }
}

extension UIRootedElementProvider {
    static func assertRootExists(in element: XCUIElement) {
        element.descendants(matching: .any).matching(identifier: Self.rootElementID).firstMatch.shouldExist()
    }
}

extension UIElementProvider {
    func name(of element: Element) -> String {
        "\(element)"
    }
}

extension UIElementProvider where Element: RawRepresentable, Element.RawValue == String {
    func identifier(of element: Element) -> String {
        element.rawValue
    }
}

extension UIElementProvider where Element == String {
    func identifier(of element: Element) -> String {
        element
    }
}

extension UIElementProvider {
    func find(element: Element) -> XCUIElement {
        let id = identifier(of: element)

        if id.contains("*") || id.contains("?") {
            return rootElement.descendants(matching: .any).matching(NSPredicate(format: "identifier like %@", id)).firstMatch
        }

        return rootElement.descendants(matching: .any).matching(identifier: id).firstMatch
    }

    func identifier(of element: Element) -> String {
        fatalError("\(#function) is not required to implement if find(element:) is implemented")
    }
}

enum UIElementState {
    enum BeState {
        case hidden
        case on
        case off
    }
    case be(BeState)
    case exist(timeout: TimeInterval)
    case contain(String)
    case match(String)

    static var exist: Self {
        .exist(timeout: XCUIElement.timeout)
    }
}

extension UIElementProvider {
    @discardableResult
    func should<Provider>(
        _ element: Element,
        ofType type: UIElementProviderWitness<Provider>,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Self, Provider>
    where Provider: UIElementProvider, Provider: _InitializableContextProvider
    {
        let xcuiElement = find(element: element)
        check(xcuiElement, satisfyState, name(of: element))
        return ImplicitlyFocusedElementProvider(base: self, focused: Provider(context: context, root: xcuiElement))
    }

    @discardableResult
    func should(
        _ element: Element,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Self, AnyElementProvider> {
        should(element, ofType: UIElementProviderWitness<AnyElementProvider>(), satisfyState)
    }

    @discardableResult
    func should<Provider>(
        provider: UIElementProviderWitness<Provider>,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Self, Provider>
    where Provider: _UIRootedElementProvider, Provider: _InitializableContextProvider
    {
        let element = Provider.findRoot(in: app, parent: rootElement)
        check(element, satisfyState, Provider.rootElementName)
        return ImplicitlyFocusedElementProvider(base: self, focused: Provider(context: context, root: element))
    }

    @discardableResult
    func tap(_ element: Element) -> Self {
        Step("Тапаем на `\(name(of: element))`") {
            find(element: element).tap()
        }
        return self
    }

    @discardableResult
    func tap() -> Self {
        Step("Тапаем на элемент") {
            rootElement.tap()
        }
        return self
    }

    @discardableResult
    func longTap() -> Self {
        Step("Долгий тап на элемент") {
            rootElement.press(forDuration: 1.0)
        }
        return self
    }

    @discardableResult
    func longTap(_ element: Element, position: CGPoint? = nil) -> Self {
        Step("Долгий тап на `\(name(of: element))`") {
            let element = find(element: element)
            if let position = position {
                let cooridnate = element
                    .coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
                    .withOffset(CGVector(dx: position.x, dy: position.y))
                cooridnate.press(forDuration: 1.0)
            } else {
                element.press(forDuration: 1.0)
            }

        }
        return self
    }

    @discardableResult
    func doubleTap(_ element: Element, position: CGPoint? = nil) -> Self {
        Step("Долгий тап на `\(name(of: element))`") {
            let element = find(element: element)
            if let position = position {
                let cooridnate = element
                    .coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
                    .withOffset(CGVector(dx: position.x, dy: position.y))
                cooridnate.doubleTap()
            } else {
                element.doubleTap()
            }

        }
        return self
    }
    
    @discardableResult
    func adjustWheel(to value: String, in element: Element, wheelIndex: Int = 0) -> Self {
        Step("Выставляем значение \(value) на барабане `\(name(of: element))`") {
            find(element: element).adjustWheel(index: wheelIndex, value: value)
        }
        return self
    }

    @discardableResult
    func type(_ text: String) -> Self {
        Step("Печатаем текст `\(text)`") {
            rootElement.typeText(text)
        }
        return self
    }

    @discardableResult
    func clearText(in element: Element) -> Self {
        Step("Стираем текст в элементе `\(name(of: element))`") {
            find(element: element).clearText()
        }
        return self
    }

    @discardableResult
    func type(_ text: String, in element: Element) -> Self {
        Step("Печатаем текст `\(text)` в элементе `\(name(of: element))`") {
            find(element: element).typeText(text)
        }
        return self
    }

    @discardableResult
    func step<Result>(_ name: String, perform: (Self) -> Result) -> Result {
        XCTContext.runActivity(named: name) { _ in perform(self) }
    }

    @discardableResult
    func log(_ action: String) -> Self {
        XCTContext.runActivity(named: action) { _ in }
        return self
    }

    @discardableResult
    func `do`(_ action: () -> Void) -> Self {
        action()
        return self
    }

    fileprivate func check(_ xcuiElement: XCUIElement, _ satisfyState: UIElementState, _ name: String) {
        switch satisfyState {
        case .be(.hidden):
            Step("Проверяем, что элемент `\(name)` не виден") {
                xcuiElement.shouldNotExist()
            }

        case .be(.on):
            Step("Проверяем, что элемент `\(name)` включен") {
                XCTAssertEqual(xcuiElement.value as? String, "1")
            }

        case .be(.off):
            Step("Проверяем, что элемент `\(name)` выключен") {
                XCTAssertEqual(xcuiElement.value as? String, "0")
            }

        case let .exist(timeout: timeout):
            Step("Проверяем существование элемента `\(name)`") {
                xcuiElement.shouldExist(timeout: timeout)
            }

        case let .contain(value):
            Step("Проверяем, что текст элемента содержит `\(value)`") {
                XCTAssertTrue(xcuiElement.label.replacingOccurrences(of: String.nbsp, with: " ").contains(value.replacingOccurrences(of: String.nbsp, with: " ")))
            }

        case let .match(value):
            Step("Проверяем, что текст элемента точно совпадает с `\(value)`") {
                let elementValue = xcuiElement.elementType == .textField ? xcuiElement.value as? String : xcuiElement.label
                let element = elementValue?.replacingOccurrences(of: String.nbsp, with: " ")

                let test = value.replacingOccurrences(of: String.nbsp, with: " ")
                XCTAssertEqual(element, test, "Значение `\(String(describing: element))` не совпадает с ожидаемым")
            }
        }
    }
}

extension UIElementProvider {
    @discardableResult
    func scroll<Provider>(
        to element: Element,
        ofType type: UIElementProviderWitness<Provider>,
        direction: XCUIElement.SwipeDirection = .up,
        maxSwipes: Int = 10,
        windowInsets: UIEdgeInsets = .zero
    ) -> ImplicitlyFocusedElementProvider<Self, Provider>
    where Provider: UIElementProvider, Provider: _InitializableContextProvider
    {
        let xcuiElement = find(element: element)

        Step("Скроллим к `\(name(of: element))`") {
            rootElement.scrollTo(element: xcuiElement, swipeDirection: direction, maxSwipes: maxSwipes, windowInsets: windowInsets)
        }

        return ImplicitlyFocusedElementProvider(base: self, focused: Provider(context: context, root: xcuiElement))
    }

    @discardableResult
    func scroll(
        to element: Element,
        direction: XCUIElement.SwipeDirection = .up,
        maxSwipes: Int = 10,
        windowInsets: UIEdgeInsets = .zero
    ) -> ImplicitlyFocusedElementProvider<Self, AnyElementProvider> {
        scroll(to: element, ofType: UIElementProviderWitness<AnyElementProvider>(), direction: direction, maxSwipes: maxSwipes, windowInsets: windowInsets)
    }

    @discardableResult
    func swipe(_ direction: XCUIElement.SwipeDirection = .up, velocity: XCUIGestureVelocity = .default) -> Self {
        Step("Скроллим \(direction)...") {
            rootElement.swipe(direction: direction, velocity: velocity)
        }
        return self
    }
}

extension UIElementProvider {
    @discardableResult
    func validateSnapshot(
        perPixelTolerance: Double = Snapshot.defaultPerPixelTolerance,
        overallTolerance: Double = Snapshot.defaultOverallTolerance,
        ignoreEdges: UIEdgeInsets = .zero,
        file: StaticString = #filePath,
        snapshotId: String = #function
    ) -> Self {
        Step("Сравниваем скриншот с `\(snapshotId)`") {
            rootElement.shouldExist()
            let screenshot = rootElement.screenshot().image
            Snapshot.compareWithSnapshot(
                image: screenshot,
                identifier: snapshotId,
                perPixelTolerance: perPixelTolerance,
                ignoreEdges: ignoreEdges,
                file: file
            )
        }
        return self
    }
}

extension UIElementProvider {
    @discardableResult
    func shouldEventBeReported(
        _ name: String,
        with properties: [String: Any] = [:],
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        Step("Проверяем логгирование события \(name)") {
            context.analyticsEventService.shouldEventsBeReported([(name, properties)], file: file, line: line)
        }

        return self
    }
}

extension UIElementProvider {
    @discardableResult
    func focus<Provider>(
        on element: Element,
        ofType type: UIElementProviderWitness<Provider>,
        _ action: (Provider) -> Void
    ) -> Self
    where Provider: UIElementProvider, Provider: _InitializableContextProvider
    {
        should(element, ofType: type, .exist).focus(action).base
    }

    @discardableResult
    func focus(on element: Element, _ action: (AnyElementProvider) -> Void) -> Self {
        should(element, .exist).focus(action).base
    }
}

struct ImplicitlyFocusedElementProvider<Base: UIElementProvider, Focused: UIElementProvider>: UIElementProvider {
    typealias Element = Base.Element

    let base: Base
    fileprivate let focused: Focused

    init(base: Base, focused: Focused) {
        self.base = base
        self.focused = focused
    }

    var rootElement: XCUIElement {
        base.rootElement
    }

    var app: XCUIApplication {
        base.app
    }

    var context: StepsContext {
        base.context
    }

    func find(element: Element) -> XCUIElement {
        base.find(element: element)
    }

    func name(of element: Element) -> String {
        base.name(of: element)
    }

    func identifier(of element: Base.Element) -> String {
        base.identifier(of: element)
    }

    @discardableResult
    func focus(_ action: (Focused) -> Void) -> Self {
        action(focused)
        return self
    }
}

extension ImplicitlyFocusedElementProvider {
    @discardableResult
    func should<Provider>(
        _ element: Element,
        ofType type: UIElementProviderWitness<Provider>,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Base, Provider>
    where Provider: UIElementProvider, Provider: _InitializableContextProvider
    {
        base.should(element, ofType: type, satisfyState)
    }

    @discardableResult
    func should(
        _ element: Element,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Base, AnyElementProvider> {
        base.should(element, satisfyState)

    }

    @discardableResult
    func should<Provider>(
        provider: UIElementProviderWitness<Provider>,
        _ satisfyState: UIElementState
    ) -> ImplicitlyFocusedElementProvider<Base, Provider>
    where Provider: _UIRootedElementProvider, Provider: _InitializableContextProvider
    {
        base.should(provider: provider, satisfyState)
    }
}

struct AnyElementProvider: UIElementProvider, _InitializableContextProvider {
    typealias Element = String

    let context: StepsContext
    let rootElement: XCUIElement
    let app: XCUIApplication

    init(context: StepsContext, root: XCUIElement?) {
        self.context = context
        self.app = XCUIApplication.make()
        self.rootElement = root ?? app
    }
}

struct UIElementProviderWitness<T: UIElementProvider> {}

extension UIElementProvider {
    @discardableResult
    func wait(for seconds: UInt32 = 3) -> Self {
        Step("Ждем \(seconds) сек.") {
            sleep(seconds)
        }
        return self
    }

    @discardableResult
    func waitForever() -> Self {
        Step("Ждем бесконечно") {
            repeat {
                sleep(20)
            } while true
        }
        return self
    }

    @discardableResult
    func wait(for expectations: [XCTestExpectation], timeout: TimeInterval = 1.0) -> Self {
        Step("Ждем заполнения ожиданий") {
            let result = XCTWaiter.wait(for: expectations, timeout: timeout)

            if result == .timedOut {
                XCTFail("Таймаут ожиданий - \(expectations)")
            }
        }
        return self
    }
}

extension BaseTest {
    func launch<Base: _InitializableContextProvider & _UIRootedElementProvider>(
        on provider: UIElementProviderWitness<Base>,
        options: AppLaunchOptions = .default
    ) -> Base {
        launch(options: options)

        return Base(context: self, root: Base.findRoot(in: app, parent: app))
    }

    func launchMain(options: AppLaunchOptions = .default) -> UIElementProviderHost<MainScreen_, TransportScreen> {
        launch(options: options)

        typealias Host = UIElementProviderHost<MainScreen_, TransportScreen>
        return Host(context: self, root: Host.findRoot(in: app, parent: app))
    }

    func launchMain<NewBase, Focused: UIElementProvider>(
        options: AppLaunchOptions = .default,
        _ afterLaunch: (UIElementProviderHost<MainScreen_, TransportScreen>) -> ImplicitlyFocusedElementProvider<NewBase, Focused>
    ) -> Focused {
        launch(options: options)

        typealias Host = UIElementProviderHost<MainScreen_, TransportScreen>

        let entryProvider = Host(context: self, root: Host.findRoot(in: app, parent: app))
        return afterLaunch(entryProvider).focused
    }

    func launch<Base: _InitializableContextProvider & _UIRootedElementProvider, Focused: UIElementProvider>(
        on provider: UIElementProviderWitness<Base>,
        options: AppLaunchOptions = .default,
        _ afterLaunch: (Base) -> ImplicitlyFocusedElementProvider<Base, Focused>
    ) -> Focused {
        launch(options: options)

        let entryProvider = Base(context: self, root: Base.findRoot(in: app, parent: app))
        return afterLaunch(entryProvider).focused
    }
}

extension UIRootedElementProvider {
    @discardableResult
    func should(_ satisfyState: UIElementState) -> Self {
        let element = Self.findRoot(in: app, parent: rootElement)
        check(element, satisfyState, Self.rootElementName)
        return self
    }
}
