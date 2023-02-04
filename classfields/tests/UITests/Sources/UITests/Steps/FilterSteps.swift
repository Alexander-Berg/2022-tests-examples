//
//  FilterSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 03/12/2019.
//

import XCTest
import Snapshots

class FiltersSteps: BaseSteps {

    func onFiltersScreen() -> FiltersScreen {
        return baseScreen.on(screen: FiltersScreen.self)
    }

    @discardableResult
    func resetFilters() -> FiltersSteps {
        onFiltersScreen().resetButton.shouldExist(timeout: 10, message: "Кнопка Сбросить не найдена")
        onFiltersScreen().resetButton.tap()
        app.alerts.firstMatch.buttons["ОК"].tap()
        return self
    }

    @discardableResult
    func showResultsTap() -> SaleCardListSteps {
        onFiltersScreen().resultsButton.tap()
        return SaleCardListSteps(context: context)
    }

    @discardableResult
    func confirmFilters() -> FiltersSteps {
        onFiltersScreen().confirmFiltersButton.tap()
        return self
    }

    @discardableResult
    func tapRegionField() -> FiltersSteps {
        onFiltersScreen().regionField.tap()
        return self
    }

    @discardableResult
    func toggleExpandRegion(index: Int) -> FiltersSteps {
        onFiltersScreen().regionCell(index: index).shouldExist().tap()
        return self
    }

    @discardableResult
    func toggleSelectCity(regionIndex: Int, index: Int) -> FiltersSteps {
        onFiltersScreen().regionCityCell(regionIndex: regionIndex, index: index).shouldExist().tap()
        return self
    }

    func scrollToField(_ type: FiltersScreen.Field) -> Self {
        onFiltersScreen().scrollTo(element: onFiltersScreen().field(type))
        return self
    }

    func tapOnField(_ type: FiltersScreen.Field) -> Self {
        onFiltersScreen().field(type).tap()
        return self
    }

    enum RangeBorderType: String {
        case from = "от"
        case to = "до"
    }

    func enterToRangeInput(_ type: RangeBorderType, _ text: String) -> Self {
        onFiltersScreen().find(by: "TextRangeCellView_\(type.rawValue)").firstMatch.typeText(text)
        return self
    }

    func selectRange(_ type: RangeBorderType, _ text: String) -> Self {
        let pickerView: XCUIElement
        switch type {
        case .from:
            pickerView = app.pickerWheels.element(boundBy: 0)
        case .to:
            pickerView = app.pickerWheels.element(boundBy: 1)
        }
        while (pickerView.value as! String) != text {
            pickerView.adjust(toPickerWheelValue: text)
        }

        return self
    }
}
