import XCTest

final class WriteCommentPage: PageObject {

    class SendButton: PageObject {
        func tap() -> OpinionDetailsPage {
            element.tap()
            let opinionPage = XCUIApplication().otherElements[CommentsAccessibility.opinionView]
            return OpinionDetailsPage(element: opinionPage)
        }
    }

    class CommentNavigationBar: NavigationBarPage {
        override var closeButton: XCUIElement {
            element.buttons.matching(identifier: CommentsAccessibility.closeCommentViewButton).firstMatch
        }
    }

    var sendButton: SendButton {
        let el = element.buttons.matching(identifier: CommentsAccessibility.sendCommentButton).firstMatch
        return SendButton(element: el)
    }

    var inputText: XCUIElement {
        element.textViews.matching(identifier: CommentsAccessibility.inputCommentTextView).firstMatch
    }

    var symbolsLeftLabel: XCUIElement {
        element.staticTexts.firstMatch
    }

    var navigationBar: CommentNavigationBar {
        CommentNavigationBar(element: NavigationBarPage.current.element)
    }
}
