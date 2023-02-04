import XCTest
import Snapshots

final class StylizedButton: BaseSteps, UIElementProvider {
    enum Element {
        case title
        case subtitle
    }

    func name(of element: Element) -> String {
        switch element {
        case .title: return "Заголовок кнопки"
        case .subtitle: return "Подзаголовок кнопки"
        }
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .title: return "button_title"
        case .subtitle: return "button_subtitle"
        }
    }
}
