import XCTest

final class ActivityListView: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "ActivityListView"
    static let rootElementName = "PopUp шаринг меню"

    enum Element {
        case copyButton
    }

    func find(element: Element) -> XCUIElement {
        switch element {
        case .copyButton:
            return rootElement.descendants(matching: .button).matching(
                NSPredicate(format: "label IN %@", ["Скопировать", "Copy"])
            ).firstMatch
        }
    }
}

