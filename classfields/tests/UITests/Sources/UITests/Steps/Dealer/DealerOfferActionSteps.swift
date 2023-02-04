import XCTest
import Snapshots

final class DealerOfferActionSteps: BaseSteps {
    func onDealerOfferActionScreen() -> DealerOfferActionScreen {
        return self.baseScreen.on(screen: DealerOfferActionScreen.self)
    }

    func onDealerDeleteConfirmationScreen() -> DealerDeleteConfirmationScreen {
        return self.baseScreen.on(screen: DealerDeleteConfirmationScreen.self)
    }

    @discardableResult
    func tapOn(button: DealerOfferActionScreen.Button) -> Self {
        Step("Тапаем кнопку \"\(button.rawValue)\"") {
            self.onDealerOfferActionScreen().button(button).tap()
        }

        return self
    }

    @discardableResult
    func containsOnly(buttons: [DealerOfferActionScreen.Button]) -> Self {
        let description = buttons.map { $0.rawValue }.joined(separator: ", ")
        Step("Проверяем пункты в контекстном меню: \(description)") {
            for button in DealerOfferActionScreen.Button.allCases {
                let contains = buttons.contains(button)
                Step("Пункт \"\(button)\" \(contains ? "присутствует" : "отсутствует")") {
                    if contains {
                        self.onDealerOfferActionScreen().button(button).shouldExist()
                    } else {
                        self.onDealerOfferActionScreen().button(button).shouldNotExist()
                    }
                }
            }
        }

        return self
    }

    @discardableResult
    func tapOnConfirmationCancel() -> DealerSaleCardSteps {
        Step("Тапаем на Отмена в диалоге подтверждении") {
            self.onDealerDeleteConfirmationScreen().cancelButton.tap()
        }

        return DealerSaleCardSteps(context: self.context)
    }

    @discardableResult
    func tapOnConfirmationDelete() -> DealerCabinetSteps {
        Step("Тапаем на подтверждение в диалоге подтверждения") {
            self.onDealerDeleteConfirmationScreen().deleteButton.tap()
        }

        return DealerCabinetSteps(context: self.context)
    }

    @discardableResult
    func tapOnEdit() -> DealerSaleCardSteps {
        Step("Тапаем на `Редактировать` в контекстном меню") {
            self.onDealerOfferActionScreen().button(.edit).tap()
        }

        return DealerSaleCardSteps(context: self.context)
    }
}
