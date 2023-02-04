import XCTest

final class ReviewCommentsScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case comment
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .comment: return "comment_view"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .comment: return "Комментарий"
        }
    }

    static let rootElementID = "review_comments_screen"
    static let rootElementName = "Экран с комментариями к отзыву"
}
