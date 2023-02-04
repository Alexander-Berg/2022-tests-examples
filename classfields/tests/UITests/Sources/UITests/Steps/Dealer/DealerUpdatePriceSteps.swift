import XCTest
import Snapshots

final class DealerUpdatePriceSteps: BaseSteps {
    func onDealerUpdatePriceScreen() -> DealerUpdatePriceScreen {
        return self.baseScreen.on(screen: DealerUpdatePriceScreen.self)
    }

    @discardableResult
    func shouldSeePicker() -> Self {
        Step("Проверяем, что есть пикер цены") {
            self.onDealerUpdatePriceScreen().title.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldBeFocusedOnEditField() -> Self {
        Step("Проверяем, что фокус на поле ввода цены") {
            let screen = self.onDealerUpdatePriceScreen()
            screen.app.keyboards.element.shouldExist()
            screen.editInput.shouldHaveKeyboardFocus()
        }

        return self
    }

    @discardableResult
    func tapOnResetAndCheckEditField() -> Self {
        Step("Тапаем на сбросить и проверяем, что сбросилось") {
            let screen = self.onDealerUpdatePriceScreen()
            screen.resetButton.tap()

            guard let value = screen.editInput.value as? String,
                  value.replacingOccurrences(of: " ", with: "").isEmpty else {
                XCTAssert(false, "Поле ввода текста не пустое")
                return
            }
        }

        return self
    }

    @discardableResult
    func tapOnDone() -> DealerSaleCardSteps {
        Step("Тапаем готово") {
            let screen = self.onDealerUpdatePriceScreen()
            screen.doneButton.tap()
        }

        return DealerSaleCardSteps(context: self.context)
    }

    @discardableResult
    func typePrice(value: String) -> Self {
        Step("Вводим цену '\(value)'") {
            let screen = self.onDealerUpdatePriceScreen()
            screen.editInput.typeText(value)
        }

        return self
    }
}
