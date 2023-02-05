import XCTest

final class QnAFormPage: PageObject {

    static var current: QnAFormPage {
        let element = XCUIApplication().any.matching(identifier: QnAFormAccessibility.root).firstMatch
        return QnAFormPage(element: element)
    }

    /// Шапка
    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    /// Кнопка "Отправить"
    var submitButton: XCUIElement {
        element
            .buttons
            .matching(identifier: QnAFormAccessibility.submitButton)
            .firstMatch
    }

    var descriptionLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: QnAFormAccessibility.descriptionLabel)
            .firstMatch
    }

    var charactersCountLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: QnAFormAccessibility.charactersCountLabel)
            .firstMatch
    }

    var inputTextView: TextViewPage {
        let elem = element
            .textViews
            .matching(identifier: QnAFormAccessibility.inputTextView)
            .firstMatch
        return TextViewPage(element: elem)
    }

}

// MARK: - Nested Types

extension QnAFormPage {

    final class NavigationBar: NavigationBarPage {
        override var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: QnAFormAccessibility.barTitle)
                .firstMatch
        }

        override var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: QnAFormAccessibility.closeBarButton)
                .firstMatch
        }
    }

    final class TextViewPage: PageObject {

        func typeText(_ text: String) {
            element.tap()

            UIPasteboard.general.string = text

            element.longTap()

            let menuItemPaste = XCUIApplication().menuItems["Вставить"]
            menuItemPaste.tap()
        }

        func clearText() {
            element.doubleTap()

            let menuItemCut = XCUIApplication().menuItems["Вырезать"]
            menuItemCut.tap()
        }
    }
}
