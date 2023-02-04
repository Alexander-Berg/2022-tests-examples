import XCTest
import Snapshots

final class SwitchCell: BaseSteps, UIElementProvider {
    enum Element {
        case `switch`
    }

    func find(element: Element) -> XCUIElement {
        switch element {
        case .switch:
            return rootElement.switches.element
        }
    }
}
