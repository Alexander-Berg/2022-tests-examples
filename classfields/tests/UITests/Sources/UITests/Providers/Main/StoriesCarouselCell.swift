import Foundation

final class StoriesCarouselCell: BaseSteps, UIElementProvider {
    enum Element {
        case story(index: Int)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .story(let index): return "story_\(index)"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .story(let id): return "История с индексом \(id)"
        }
    }
}
