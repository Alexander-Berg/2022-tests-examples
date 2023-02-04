import XCTest
import Snapshots

final class AuthCodeSteps: BaseSteps {
    @discardableResult
    func inputCode(_ code: String) -> Self {
        step("Вводим код") {
            let textField = onAuthCodeScreen().codeInput.textFields.firstMatch
            textField.typeText(code)
        }
    }

    @discardableResult
    func shouldSeeInvalidCodeError() -> Self {
        let message = "Неверный код подтверждения"
        return step("Проверяем наличие текста '\(message)'") {
            onAuthCodeScreen().find(by: message).firstMatch.shouldExist()
        }
    }

    private func onAuthCodeScreen() -> AuthCodeScreen {
        self.baseScreen.on(screen: AuthCodeScreen.self)
    }
}
