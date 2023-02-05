import AutoMate
import XCTest

final class YandexLoginPage: PageObject {

    final class LoginField: PageObject {

        /// Ввести логин
        func typeText(_ text: String) {
            if !element.isSelected {
                element.tap()
            }
            element.typeText(text)
        }
    }

    final class PasswordField: PageObject {

        /// Ввести пароль
        func typeText(_ text: String) {
            if !element.isSelected {
                element.tap()
            }

            UIPasteboard.general.string = text

            element.longTap()

            let menuItemPaste = XCUIApplication().menuItems["Вставить"]
            menuItemPaste.tap()
        }
    }

    /// Кнопка "Войти"
    var next: XCUIElement {
        element
            .buttons["Войти"]
            .firstMatch
    }

    /// Поле для ввода логина
    var login: LoginField {
        let elem = element
            .textFields
            .firstMatch
        return LoginField(element: elem)
    }

    /// Поле для ввода пароля
    var password: PasswordField {
        let elem = element
            .secureTextFields
            .firstMatch
        return PasswordField(element: elem)
    }
}
