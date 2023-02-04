import Snapshots
import XCTest

class SharkLKSteps: BaseSteps {
    func openEditForm() -> Self {
        return step("Тапаем по кнопке редактировать в ЛК") {
            self.onMainScreen().find(by: "Редактировать").firstMatch.tap()
        }
    }

    @discardableResult
    func scrollTo(_ selector: String, maxSwipes: Int = 5, windowInsets: UIEdgeInsets = .zero) -> Self {
        let element: XCUIElement = onMainScreen().find(by: selector).firstMatch
        onMainScreen().scrollTo(element: element, maxSwipes: maxSwipes, windowInsets: windowInsets)
        return self
    }

    func tapCreditDescriptionLink() -> Self {
        // Приходится нажимать в конкретную точку экрана, так как там используется элемент, вычисляющий нажатие на конкретное слово по координате касания, то есть нужно нажимать в конкретную точку вьюхи, и не существует отдельного объекта, который можно просто нажать.
        XCUIApplication.make().coordinate(withNormalizedOffset: CGVector(dx: 0.15, dy: 0.8)).tap()
        return self
    }
}
