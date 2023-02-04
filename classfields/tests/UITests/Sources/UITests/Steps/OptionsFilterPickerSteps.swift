import XCTest
import Snapshots

final class OptionsFilterPickerSteps: BaseSteps {
    func onOptionsFilterPickerScreen() -> OptionsFilterPickerScreen {
        return self.baseScreen.on(screen: OptionsFilterPickerScreen.self)
    }

    @discardableResult
    func tapOnComparisonButton() -> ComplectationComparisonSteps {
        Step("Тапаем на открытие пикера со сравнением комплектаций") {
            self.onOptionsFilterPickerScreen().comparisonButton.tap()
        }

        return ComplectationComparisonSteps(context: self.context)
    }
}
