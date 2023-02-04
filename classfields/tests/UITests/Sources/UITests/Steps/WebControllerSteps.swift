import XCTest
import Snapshots

final class WebControllerSteps: BaseSteps {
    @discardableResult
    func shouldSeeWebController() -> Self {
        step("Проверяем, что показан контроллер с вебвью") {
            self.onWebControllerSteps().mainView.shouldExist()
        }
    }

    // MARK: - Private

    private func onWebControllerSteps() -> WebControllerScreen {
        return self.baseScreen.on(screen: WebControllerScreen.self)
    }
}
