import XCTest
import Snapshots

final class CarReportSearchSteps: BaseSteps {

    func onScreen() -> CarfaxStandaloneScreen {
        return baseScreen.on(screen: CarfaxStandaloneScreen.self)
    }

    @discardableResult
    func typeInSearchBar(text: String) -> CarReportSearchSteps {
        step("Вводим в серчбаре текст '\(text)'") {
            onScreen().searchBar.typeText(text)
        }
    }

    @discardableResult
    func tapOnSearchButton() -> CarReportSearchSteps {
        step("Тапаем на поиск отчета") {
            onScreen().searchButton.tap()
        }
    }

    @discardableResult
    func tapOnBuySingleReportButton() -> PaymentOptionsSteps<CarReportSearchSteps> {
        step("Тапаем на покупку одного отчета") {
            onScreen().buySingleReportButton.shouldExist().tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }

    @discardableResult
    func checkModeratorWarning(visible: Bool) -> Self {
        step("Проверяем, что \(visible ? "видим" : "не видим") ворнинг для модератора") {
            let element = onScreen().findStaticText(by: "Отчёт под модератором")
            if visible {
                element.shouldExist()
            } else {
                element.shouldNotExist()
            }
        }
    }
}
