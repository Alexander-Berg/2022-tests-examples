import XCTest
import Snapshots

final class UserOfferStatBubble: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case viewCounter = "viewCount"
        case positionCounter = "positionCount"
    }

    func name(of element: Element) -> String {
        switch element {
        case .viewCounter: return "Счётчик показов"
        case .positionCounter: return "Счётчик позиции"
        }
    }

    static let rootElementID = "UserOfferStatView"
    static let rootElementName = "Баббл статистики по офферу"
}
