import XCTest
import Snapshots

final class HealthScorePopupSteps: BaseSteps {
    @discardableResult
    func tapOnBuyReportButton() -> HealthScorePopupPaymentModalSteps {
        Step("Тапаем на кнопку покупки отчета") {
            self.baseScreen.find(by: "report_purchase_button").firstMatch.tap()
        }

        return HealthScorePopupPaymentModalSteps(context: context, source: self)
    }

    @discardableResult
    func tapOnSeeReportButton() -> CarReportPreviewSteps {
        step("Тапаем на кнопку показа отчета") {
            self.baseScreen.find(by: "Смотреть полный отчёт").firstMatch.tap()
        }
        .as(CarReportPreviewSteps.self)
    }
}

final class HealthScorePopupPaymentModalScreen: ModalScreen { }

final class HealthScorePopupPaymentModalSteps: ModalSteps<HealthScorePopupSteps, HealthScorePopupPaymentModalScreen> {
    @discardableResult
    func shouldSeePaymentModal() -> Self {
        step("Проверяем, что показана модалка оплаты") {
            self.onModalScreen().findContainedText(by: "Оплата").firstMatch.shouldExist()
        }
    }
}
