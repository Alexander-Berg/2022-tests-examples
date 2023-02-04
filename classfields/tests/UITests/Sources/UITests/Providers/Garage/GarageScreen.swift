import XCTest
import Snapshots

typealias GarageScreen_ = GarageSteps

extension GarageScreen_: UIRootedElementProvider {
    static var rootElementID: String = "Гараж"
    static var rootElementName: String = "Гараж"

    enum Element {
        case logInLabel
        case garageCard(id: String)
        case dreamCarSnippet
        case exCarSnippet
        case addCarButton
        case snippet(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .logInLabel:
            return "Войти  "
        case let .garageCard(id):
            return "car_\(id)"
        case .dreamCarSnippet:
            return "Машина мечты"
        case .exCarSnippet:
            return "Бывшая"
        case .addCarButton:
            return "add_car"
        case .snippet(let title):
            return title
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
