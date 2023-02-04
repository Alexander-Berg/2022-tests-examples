import XCTest
import Snapshots

class GenericModalPopup: BaseSteps, UIRootedElementProvider {
    static var rootElementID: String { "ModalViewControllerHost" }
    static var rootElementName: String { "Модальный попап" }

    enum Element {
        case dismissButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .dismissButton:
            return "dismiss_modal_button"
        }
    }
}
