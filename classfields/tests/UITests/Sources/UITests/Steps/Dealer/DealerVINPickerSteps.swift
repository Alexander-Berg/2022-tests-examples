import XCTest
import Snapshots

final class DealerVINPickerSteps: BaseSteps {
    @discardableResult
    func shouldSeeCommonContent() -> Self {
        Step("На экране VIN пикера должно быть поле ввода") {
            let screen = self.onDealerVINPickerScreen()
            Step("Ищем поле ввода") {
                screen.input.shouldExist()
            }
        }

        return self
    }

    // MARK: - Actions

    @discardableResult
    func skip() -> DealerFormSteps {
        Step("Можем пропустить VIN пикер без заполнения") {
            let screen = self.onDealerVINPickerScreen()
            let button = screen.skipButton

            Step("Ищем кнопку `Пропустить`") {
                button.shouldExist()
            }

            button.tap()
        }

        return DealerFormSteps(context: context)
    }

    // MARK: - Screens

    func onDealerVINPickerScreen() -> DealerVINPickerScreen {
        return baseScreen.on(screen: DealerVINPickerScreen.self)
    }
}
