//
//  CarReportsListSteps.swift
//  AutoRu
//
//  Created by Sergey An. Sergeev on 31.01.2021.
//

import XCTest
import Snapshots

class CarReportsListSteps: BaseSteps {
    func onScreen() -> CarfaxStandaloneCardScreen {
        return baseScreen.on(screen: CarfaxStandaloneCardScreen.self)
    }

    func onReportListScreen() -> CarReportsListScreen {
        return baseScreen.on(screen: CarReportsListScreen.self)
    }

    func tapOnSearch() -> CarReportSearchSteps {
        step("Тапаем на поиск по вин/грз в стендалоне") {
            self.onReportListScreen().searchField.tap()
        }
        .as(CarReportSearchSteps.self)
    }

    @discardableResult
    func checkHasSearchField() -> CarReportsListSteps {
        self.onReportListScreen().searchField.shouldExist()
        return self
    }

    func tapOnBuyReports() -> PaymentOptionsSteps<CarReportsListSteps> {
        Step("Тапаем покупку отчетов") {
            self.onReportListScreen()
                .buyReportsBundleButton
                .tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }

    @discardableResult
    func checkReportSnippetIsDisplayed(withVin vin: String) -> Self {
        step("Проверяем наличие сниппета отчета c VIN:\(vin) в списке  купленных") {
            self.onReportListScreen().reportSnippet(withVin: vin).shouldExist()
        }
    }

    func findVIN(_ vin: String) -> XCUIElement {
        return onScreen().find(by: vin).firstMatch
    }

    func snapshotVIN(_ vin: String) -> UIImage {
        return onScreen().find(by: vin).firstMatch.waitAndScreenshot().image
    }
}
