import XCTest
import Snapshots

final class DealerVASDescriptionSteps: BaseSteps {
    func onDealerVASDescriptionScreen() -> DealerVASDescriptionScreen {
        return self.baseScreen.on(screen: DealerVASDescriptionScreen.self)
    }

    func onDealerCabinetVASConfirmationScreen() -> DealerCabinetVASConfirmationScreen {
        return self.baseScreen.on(screen: DealerCabinetVASConfirmationScreen.self)
    }

    @discardableResult
    func shouldSeeTitleAndDescription(for vas: DealerVASContainingScreen.VASListItemType) -> Self {
        Step("Проверяем что есть заголовок и описание для васа '\(vas.rawValue)' на полноэкранном") {
            self.onDealerVASDescriptionScreen().find(by: vas.rawValue).firstMatch.shouldExist()
            self.onDealerVASDescriptionScreen().find(by: vas.rawValue).firstMatch.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeBottomButton(type: DealerVASDescriptionScreen.BottomButton) -> Self {
        Step("Проверяем что есть кнопка '\(type)' на полноэкранном") {
            self.onDealerVASDescriptionScreen().bottomButton(of: type).shouldExist()
        }

        return self
    }

    @discardableResult
    func tapOnBottomButton(type: DealerVASDescriptionScreen.BottomButton) -> Self {
        Step("Тапаем на кнопку '\(type)' на полноэкранном") {
            self.onDealerVASDescriptionScreen().bottomButton(of: type).tap()
        }

        return self
    }

    @discardableResult
    func tapOnCloseButton<Steps: BaseSteps>() -> Steps {
        Step("Тапаем на крестик на полноэкранном") {
            self.onDealerVASDescriptionScreen().closeButton.tap()
        }

        return Steps(context: self.context)
    }
}
