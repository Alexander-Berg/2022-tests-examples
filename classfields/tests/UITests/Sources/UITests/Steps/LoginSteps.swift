//
//  LoginSteps.swift
//  UITests
//
//  Created by Victor Orlovsky on 26/03/2019.
//

final class LoginSteps: BaseSteps {
    @discardableResult
    func dismissLoginIfNeeded() -> Self {
        if onLoginScreen().titleText.exists {
            onLoginScreen().closeButton.tap()
        }
        return self
    }

    @discardableResult
    func shouldSeeLoginScreen() -> Self {
        Step("Проверяем, что виден экран логина") {
            onLoginScreen().phoneInput.shouldExist()
            // add some checks
        }
        return self
    }

    @discardableResult
    func enterPhone(number: String = "79991112233") -> Self {
        step("Ввод телефона '\(number)'") {
            self.app.tap()
            self.app.typeText(number)
            self.wait(for: 1)
        }
    }

    @discardableResult
    func enterCode(_ code: String) -> Self {
        step("Ввод кода подтверждения '\(code)'") {
            self.app.tap()
            self.app.typeText(code)
            self.wait(for: 1)
        }
    }
}

extension LoginSteps: UIRootedElementProvider {
    static var rootElementID: String = "login_flow"
    static var rootElementName: String = "Форма логина"

    enum Element {
    }
}
