import XCTest
import Snapshots

final class AuthSteps: BaseSteps {
    @discardableResult
    func inputPhoneNumber(_ phone: String) -> AuthCodeSteps {
        step("Вводим номер телефона") {
            let textField = onAuthScreen().phoneInput.textFields.firstMatch
            textField.typeText(phone)
        }
        .as(AuthCodeSteps.self)
    }

    private func onAuthScreen() -> AuthScreen {
        self.baseScreen.on(screen: AuthScreen.self)
    }
}
