import XCTest
import Snapshots

final class ComplainMenuPopup: BaseSteps, UIRootedElementProvider {
    static var rootElementID: String = "complaint_alert"
    static var rootElementName: String = "Меню с причиной жалобы"

    enum Element: String {
        case didSale = "Продано"
    }
}
