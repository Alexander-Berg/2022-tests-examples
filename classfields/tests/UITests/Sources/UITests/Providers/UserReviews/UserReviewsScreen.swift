import XCTest

final class UserReviewsScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case addReviewBigButton
        case addReviewButton
        case editReviewButton
        case deleteReviewButton
        case publishReviewButton
        case closeButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .addReviewBigButton: return "big_add_review_button"
        case .addReviewButton: return "add_review_button"
        case .editReviewButton: return "edit_user_review_button"
        case .deleteReviewButton: return "delete_user_review_button"
        case .publishReviewButton: return "publish_user_review_button"
        case .closeButton: return "nav_close_button"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .addReviewBigButton: return "Большая кнопка добавить отзыв"
        case .addReviewButton: return "Кнопка добавить отзыв"
        case .editReviewButton: return "Кнопка редактировать отзыв"
        case .deleteReviewButton: return "Кнопка удалить отзыв"
        case .publishReviewButton: return "Кнопка опубликовать отзыв"
        case .closeButton: return "nav_close_button"
        }
    }

    static let rootElementID = "userReviewsScreen"
    static let rootElementName = "Пустой экран отзывов пользователя"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

final class UserReviewAlert: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case no = "Нет"
        case yes = "Да"
    }

    static let rootElementID = "userReviewAlert"
    static let rootElementName = "Алерт после нажатия на удалить"
}

final class UserReviewErrorAlert: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case close = "Закрыть"
    }

    static let rootElementID = "userReviewErrorAlert"
    static let rootElementName = "Алерт c ошибкой"
}

final class UserReviewEditorScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case closeButton
        case saveButton
        case publishButton
        case userReviewContentEditorCell
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .closeButton: return "close_button"
        case .saveButton: return "save_button"
        case .publishButton: return "publish_button"
        case .userReviewContentEditorCell: return "content_editor"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .closeButton: return "Кнопка закрытия редактора"
        case .saveButton: return "Большая кнопка добавить отзыв"
        case .publishButton: return "Кнопка опубликовать отзыв"
        case .userReviewContentEditorCell: return "Элементы редактирования отзыва"
        }
    }

    static let rootElementID = "ReviewEditor"
    static let rootElementName = "Экран редактирования отзыва пользователя"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

final class UserReviewContentEditorCell: BaseSteps, UIElementProvider {
    enum Element {
        case titleReview
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .titleReview: return "titleReview"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .titleReview: return "Заголовок отзыва"
        }
    }
}
