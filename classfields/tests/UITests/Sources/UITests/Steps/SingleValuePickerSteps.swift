import Foundation
import XCTest
import Snapshots

final class SingleValuePickerSteps: BaseSteps {
    @discardableResult
    func selectValue(title: String) -> Self {
        Step("Выбираем в пикере значение '\(title)'") {
            self.app.staticTexts[title].firstMatch.tap()
        }

        return self
    }
}
