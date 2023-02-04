final class UserReviewCloseAlert: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case cancel = "Отмена"
        case agree = "Да"
    }

    static let rootElementID = "userReviewCloseAlert"
    static let rootElementName = "Алерт при закрытии"
}
