import XCTest
import Snapshots

final class ModelInfoSteps: BaseSteps {
    func onModelInfoScreen() -> ModelInfoScreen {
        return self.baseScreen.on(screen: ModelInfoScreen.self)
    }

    @discardableResult
    func openComplectationTab() -> Self {
        Step("Переключаемся на таб комплектаций") {
            self.onModelInfoScreen().complectationTab
                .coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.1))
                .tap()
        }

        return self
    }
}
