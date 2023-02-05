import UIUtils
import XCTest

final class PublicationsPage: PageObject {

    // MARK: - Properties

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: PublicationsAccessibility.root).firstMatch
        return .init(element: element)
    }

    var questionButton: XCUIElement {
        element
            .firstMatch
            .buttons
            .element(withLabelContaining: "Вопросы")
    }

    var answersButton: XCUIElement {
        element
            .firstMatch
            .buttons
            .element(withLabelContaining: "Ответы")
    }

    // MARK: - Methods

    func tapQuestionButton() -> MyQuestionsPage {
        questionButton.tap()
        let element = XCUIApplication().any.matching(identifier: MyQuestionsAccessibility.root).firstMatch
        return MyQuestionsPage(element: element)
    }

    func tapAnswersButton() -> MyAnswersPage {
        answersButton.tap()
        let element = XCUIApplication().any.matching(identifier: MyAnswersAccessibility.root).firstMatch
        return MyAnswersPage(element: element)
    }

}
