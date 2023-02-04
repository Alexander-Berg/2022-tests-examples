import XCTest
import Snapshots

typealias VASTrapScreen_ = VASTrapSteps

extension VASTrapScreen_: UIRootedElementProvider {
    enum Element: String {
        case doneButton = "Готово"
    }

    static let rootElementID = "VASTrapViewController"
    static let rootElementName = "Продайте автомобиль быстрее"
}

