import XCTest
import Snapshots

final class DealerVASDescriptionScreen: BaseScreen {
    lazy var closeButton = find(by: "close_button").firstMatch

    func bottomButton(of type: BottomButton) -> XCUIElement {
        return find(by: type.rawValue).firstMatch
    }

    enum BottomButton: String, CustomStringConvertible {
        case activate = "action_button_activate"
        case deactivate = "action_button_deactivate"
        case activated = "action_button_activated"

        var description: String {
            switch self {
            case .activate:
                return "Подключить"
            case .deactivate:
                return "Отключить"
            case .activated:
                return "Подключено"
            }
        }
    }
}
