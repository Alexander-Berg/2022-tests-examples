import XCTest
import Snapshots

final class GeoRadiusBubblesCell: BaseSteps, UIRootedElementProvider {
    enum Element {
        case currentRegionBubble // == .radiusBubble(0)
        case radiusBubble(Int)
        case wholeRussiaBubble // == .radiusBubble(1100)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .currentRegionBubble: return "GeoRadiusBubble_\(0)"
        case .radiusBubble(let radius): return "GeoRadiusBubble_\(radius)"
        case .wholeRussiaBubble: return "GeoRadiusBubble_\(1100)"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .currentRegionBubble: return "Чипс текущего региона"
        case .radiusBubble(let radius): return "Чипс радиуса +\(radius)"
        case .wholeRussiaBubble: return "Чипс Россия"
        }
    }

    static let rootElementID = "GeoRadiusBubbles"
    static let rootElementName = "Чипсы с радиусами бесконечного листинга"
}

final class GeoRadiusBubbleItem: BaseSteps, UIElementProvider {
    enum Element {
        case title
        case counter
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .title: return "GeoRadiusBubble_title"
        case .counter: return "GeoRadiusBubble_counter"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .title: return "Заголовок чипса"
        case .counter: return "Счетчик офферов на чипсе"
        }
    }

    @discardableResult
    func shouldBeActive(_ value: Bool) -> Self {
        step("Проверяем, что чипс выбран \(value ? "активным" : "неактивным")") {
            rootElement.descendants(matching: .any)
                .matching(identifier: "GeoRadiusBubble_background_\(value)")
                .firstMatch
                .shouldExist()
        }
    }
}
