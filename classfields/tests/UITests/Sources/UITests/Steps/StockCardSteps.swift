//
//  StockCardSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 7/17/20.
//

import XCTest
import Snapshots

class StockCardSteps: BaseSteps {
    func onStockCardScreen() -> StockCardScreen {
        return baseScreen.on(screen: StockCardScreen.self)
    }

    @discardableResult
    func scrollToOffer(with offerId: String, maxSwipes: Int = 50) -> Self {
        let element = onStockCardScreen().stockOfferBody(for: offerId)
        onStockCardScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        return self
    }

    @discardableResult
    func scroll(to: String, maxSwipes: Int = 50) -> Self {
        let element: XCUIElement = onStockCardScreen().find(by: to).firstMatch
        onStockCardScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        return self
    }

    @discardableResult
    func scrollStartIL() -> Self {
        let element = onStockCardScreen().find(by: "startInfinityListingTitle").firstMatch
        onStockCardScreen().scrollTo(element: element, maxSwipes: 50)
        return self
    }

    @discardableResult
    func openOffer(with offerId: String, maxSwipes: Int = 50) -> SaleCardSteps {
        let element = onStockCardScreen().stockOfferBody(for: offerId)
        onStockCardScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        element.tap()
        return SaleCardSteps(context: context)
    }

    @discardableResult
    func tapOnComplectationPicker() -> OptionsFilterPickerSteps {
        Step("Тапаем на кнопку открытия пикера комплектаций") {
            self.onStockCardScreen().complectationPickerButton.tap()
        }

        return OptionsFilterPickerSteps(context: self.context)
    }

    @discardableResult
    func tapOnAboutModel() -> ModelInfoSteps {
        Step("Тапаем на кнопку открытия экрана 'О модели'") {
            self.onStockCardScreen().aboutModelButton.tap()
        }

        return ModelInfoSteps(context: self.context)
    }

    @discardableResult
    func shouldSeeEmptyResultsPlaceholder() -> Self {
        Step("Проверяем, что виден плейсхолдер с пустыми результатами") {
            self.onStockCardScreen().emptyResultsPlaceholder.shouldBeVisible()
        }

        return self
    }

    @discardableResult
    func checkEmptyResultsResetButton(isVisible: Bool) -> Self {
        Step("Проверяем, что \(isVisible ? "видна" : "не видна") кнопка сброса фильтров на плейсхолдере") {
            if isVisible {
                self.onStockCardScreen().emptyResultsResetButton.shouldExist()
            } else {
                self.onStockCardScreen().emptyResultsResetButton.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func tapEmptyResultsResetButton() -> Self {
        Step("Тапаем на кнопку сброса фильтров на плейсхолдере") {
            self.onStockCardScreen().emptyResultsResetButton.tap()
        }

        return self
    }

    @discardableResult
    func shouldSeeOffer(with offerID: String) -> Self {
        Step("Проверяем, что виден оффер с ID '\(offerID)'") {
            self.onStockCardScreen().stockOfferBody(for: offerID).shouldExist()
        }

        return self
    }

    func checkInfinityListingExist() -> Self {
        onStockCardScreen().scrollTo(element: onStockCardScreen().find(by: "startInfinityListingTitle").firstMatch)
        exist(selector: "startInfinityListingTitle")
        return self
    }

    func snapshotHeader() -> UIImage {
        return onStockCardScreen()
            .find(by: "stock_card_header_false_true").firstMatch
            .waitAndScreenshot().image
	}

    @discardableResult
    func swipeUp() -> Self {
        onStockCardScreen().swipe(.up)
        return self
    }

    @discardableResult
    func swipeDown() -> Self {
        onStockCardScreen().swipe(.down)
        return self
    }

    func scrollToFilterButton(_ type: StockCardScreen.FilterType) -> Self {
        onStockCardScreen().filtersScrollable.scrollTo(element: onStockCardScreen().filterButton(type), swipeDirection: .left)
        return self
    }

    func tapFilterButton(_ type: StockCardScreen.FilterType) -> Self {
        onStockCardScreen().filterButton(type).tap()
        return self
    }

    func openFullFilter() -> FiltersSteps {
        onStockCardScreen().filterButton(.all).tap()
        return self.as(FiltersSteps.self)
    }

    func snapshotParams() -> UIImage {
        return onStockCardScreen().paramsView.waitAndScreenshot().image
    }

    func snapshotParamsChips() -> UIImage {
        return onStockCardScreen().paramsChipsView.waitAndScreenshot().image
    }

    func snapshotOfferTitle(id: String) -> UIImage {
        return onStockCardScreen().offerTitle(id: id).waitAndScreenshot().image
    }
}
