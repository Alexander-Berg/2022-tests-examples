//
//  FiltersSteps.swift
//  UITests
//
//  Created by Alexey Salangin on 1/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
import YREFiltersModel
import YREAccessibilityIdentifiers
import YRETestsUtils

final class FiltersSteps {
    typealias Action = FiltersSubtests.Action
    typealias Category = FiltersSubtests.Category
    typealias RoomsTotal = FiltersSubtests.RoomsTotal
    typealias ApartmentType = FiltersSubtests.ApartmentType
    typealias RentTime = FiltersSubtests.RentTime
    typealias ObjectType = FiltersSubtests.ObjectType
    typealias GarageType = FiltersSubtests.GarageType
    typealias VillageOfferType = FiltersSubtests.VillageOfferType

    enum Identifiers {
        static let mainFiltersVC = "YREMainFilterViewController"
        static let submitButton = LegacyFiltersAccessibilityIdentifiers.submitButton
        static let footerView = LegacyFiltersAccessibilityIdentifiers.footer
        static let commuteCell = FiltersGeoIntentAccessibilityIdentifiers.commuteCellIdentifier
        static let geoIntentCell = FiltersGeoIntentAccessibilityIdentifiers.plainCellIdentifier
        static let drawGeoIntentCell = FiltersGeoIntentAccessibilityIdentifiers.drawCellIdentifier
        static let actionButton = "filters.actionButton"
        static let categoryButton = "filters.categoryButton"
        static let filtersAlert = "filters.alert"
        static let priceCell = "filters.cell.price"
        static let metroDistanceCell = "filters.cell.timeToMetro"
        static let tagsToIncludeCell = FiltersTagsAccessibilityIdentifiers.includeFilterCellIdentifier
        static let tagsToExcludeCell = FiltersTagsAccessibilityIdentifiers.excludeFilterCellIdentifier
        static let commercialTypeCell = "filters.cell.commercialType"
        static let onlySamoletCell = "filters.cell.onlySamolet"
        static let yandexRentCell = "filters.cell.yandexRent"
        static let developerCell = "filters.cell.developer"
        static let renovationCell = "filters.cell.renovation"
        static let conciergeBanner = LegacyFiltersAccessibilityIdentifiers.conciergeBanner

        static func roomsTotalButton(for roomsTotal: RoomsTotal) -> String {
            return "YRESegmentedControl-\(roomsTotal.buttonTitle)"
        }
    }

    // MARK: Private

    private lazy var filtersVC: XCUIElement = {
        return XCTContext.runActivity(named: "Ищем экран фильтров") { _ -> XCUIElement in
            return ElementsProvider.obtainElement(identifier: Identifiers.mainFiltersVC)
        }
    }()

    private lazy var filtersTable = self.filtersVC.tables.element
}

// MARK: - Common
extension FiltersSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана фильтров") { _ -> Void in
            self.filtersVC.yreEnsureExistsWithTimeout()
        }
        return self
    }


    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана фильтров") { _ -> Void in
            self.filtersVC.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func submitFilters() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Показать\" на экране фильтров") { _ -> Void in
            let submitButton = ElementsProvider.obtainButton(identifier: Identifiers.submitButton, in: self.filtersVC)
            submitButton
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return self
    }

    @discardableResult
    func dismissModallyPresented() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на кнопку \"Закрыть\" на экране фильтров") { _ -> Self in
            guard let closeButton = ElementsProvider.obtainBackButtonIfExists() else { return self }
            if closeButton.yreWaitForExistence(), closeButton.isHittable {
                closeButton.tap()
            }
            return self
        }
    }
}

// MARK: - Geo
extension FiltersSteps {
    @discardableResult
    func tapOnCommute() -> ResetGeoIntentAlertSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Время на дорогу\"") { _ -> Void in
            let commuteCell = ElementsProvider.obtainElement(identifier: Identifiers.commuteCell, in: self.filtersVC)

            self.scrollToElement(element: commuteCell)

            commuteCell
                .yreEnsureExistsWithTimeout()
                .tap()
        }

        return ResetGeoIntentAlertSteps()
    }

    @discardableResult
    func tapOnPlainGeoIntentField() -> ResetGeoIntentAlertSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Гео\"") { _ -> Void in
            let geoIntentCell = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentCell)

            self.scrollToElement(element: geoIntentCell)

            geoIntentCell
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return ResetGeoIntentAlertSteps()
    }

    @discardableResult
    func tapOnDrawGeoIntentButton() -> ResetGeoIntentAlertSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Нарисовать область\"") { _ -> Void in
            let geoIntentCell = ElementsProvider.obtainElement(identifier: Identifiers.drawGeoIntentCell,
                                                               in: self.filtersVC)

            self.scrollToElement(element: geoIntentCell)

            geoIntentCell
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return ResetGeoIntentAlertSteps()
    }

    @discardableResult
    func tapOnMetroDistanceCell() -> FiltersMetroDistancePickerSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Время до метро\"") { _ -> Void in
            let metroDistanceCell = ElementsProvider.obtainElement(identifier: Identifiers.metroDistanceCell,
                                                                   in: self.filtersVC)

            self.scrollToElement(element: metroDistanceCell)

            metroDistanceCell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return FiltersMetroDistancePickerSteps()
    }

    @discardableResult
    func tapOnCommercialTypeCell() -> FiltersCommercialTypePickerSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Объект\" (тип коммерческой недвижимости)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: Identifiers.commercialTypeCell,
                in: self.filtersVC
            )

            self.scrollToElement(element: cell)

            cell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return FiltersCommercialTypePickerSteps()
    }

    @discardableResult
    func isPresetsCellPresented() -> FilterPresetsSteps {
        let presetsCell = ElementsProvider.obtainElement(
            identifier: FiltersPresetsAccessibilityIdentifiers.presetsCell,
            type: .cell,
            in: self.filtersTable
        )
        presetsCell.yreEnsureExists()
        return FilterPresetsSteps(presetsCell: presetsCell)
    }

    @discardableResult
    func isRecentSearchesCellNotPresented() -> Self {
        let recentSearchesCell = ElementsProvider.obtainElement(
            identifier: RecentSearchesAccessibilityIdentifiers.recentSearchesCell,
            type: .cell,
            in: self.filtersTable
        )
        recentSearchesCell.yreEnsureNotExists()
        return self
    }

    @discardableResult
    func isRecentSearchesCellPresented() -> RecentSearchesSteps {
        let recentSearchesCell = ElementsProvider.obtainElement(
            identifier: RecentSearchesAccessibilityIdentifiers.recentSearchesCell,
            type: .cell,
            in: self.filtersTable
        )
        recentSearchesCell.yreEnsureExists()
        return RecentSearchesSteps(recentSearchesCell: recentSearchesCell)
    }

    @discardableResult
    func isCommuteCellTitleEqual(to text: String) -> Self {
        XCTContext.runActivity(
            named: "Проверяем соответствие содержимого ячейку фильтра \"Время на дорогу\" значению '\(text)'"
        ) { _ -> Void in
            let commuteCell = ElementsProvider.obtainElement(identifier: Identifiers.commuteCell, in: self.filtersVC)
            commuteCell.staticTexts[text].yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isRentTimeEqual(to rentTime: RentTime) -> Self {
        XCTContext.runActivity(
            named: "Проверяем соответствие фильтра \"Длительность аренды\" значению '\(rentTime.readableName)'"
        ) { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: rentTime.accessibilityIdentifier, in: self.filtersVC)
            XCTAssertTrue(button.isSelected)
        }
        return self
    }

    @discardableResult
    func areRoomsTotalEqual(to roomsTotal: RoomsTotal...) -> Self {
        XCTContext.runActivity(
            named: "Проверяем соответствие фильтра \"Количество комнат\" значениям '\(roomsTotal)'"
        ) { _ -> Void in
            RoomsTotal.allCases.forEach {
                let button = ElementsProvider.obtainButton(
                    identifier: Identifiers.roomsTotalButton(for: $0),
                    in: self.filtersVC
                )

                let shouldBeSelected = roomsTotal.contains($0)
                XCTAssertEqual(button.isSelected, shouldBeSelected)
            }
        }
        return self
    }
}

// MARK: - Concierge banner
extension FiltersSteps {
    @discardableResult
    func isConciergeBannerExists() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие баннера Консьержа") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.conciergeBanner, in: self.filtersVC)
            cell.yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isConciergeBannerHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие баннера Консьержа") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.conciergeBanner)
            cell.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnConciergeBanner() -> Self {
        XCTContext.runActivity(named: "Скролим и нажимаем на баннер Консьержа") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.conciergeBanner)
            self.scrollToElement(element: cell, velocity: 0.5)
            cell
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }
}

// MARK: - Tags
extension FiltersSteps {
    @discardableResult
    func findTagsToIncludeCell() -> XCUIElement {
        let title = "Ищем наличие ячейки \"Искать в описании объявления\" на экране фильтров"
        return XCTContext.runActivity(named: title) { _ -> XCUIElement in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.tagsToIncludeCell, in: self.filtersVC)
            // @l-saveliy: this cell is bigger than others
            // scroll accurate to find it on screen
            self.scrollToElement(element: cell, velocity: 0.5)
            return cell
        }
    }

    @discardableResult
    func findTagsToExcludeCell() -> XCUIElement {
        let title = "Ищем наличие ячейки \"Не показывать объявления, если в описании\" на экране фильтров"
        return XCTContext.runActivity(named: title) { _ -> XCUIElement in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.tagsToExcludeCell, in: self.filtersVC)
            // @l-saveliy: this cell is bigger than others
            // scroll accurate to find it on screen
            self.scrollToElement(element: cell, velocity: 0.5)
            return cell
        }
    }

    @discardableResult
    func isTagsToIncludeCellPresented() -> FiltersTagsSteps {
        let title = "Проверяем наличие ячейки \"Искать в описании объявления\""
        XCTContext.runActivity(named: title) { _ -> Void in
            self.findTagsToIncludeCell().yreEnsureExistsWithTimeout()
        }
        return FiltersTagsSteps(cellIdentifier: Identifiers.tagsToIncludeCell)
    }

    @discardableResult
    func isTagsToExcludeCellPresented() -> FiltersTagsSteps {
        let title = "Проверяем наличие ячейки \"Не показывать объявления, если в описании\""
        XCTContext.runActivity(named: title) { _ -> Void in
            self.findTagsToExcludeCell().yreEnsureExistsWithTimeout()
        }
        return FiltersTagsSteps(cellIdentifier: Identifiers.tagsToExcludeCell)
    }
}

// MARK: - Main Panel Filters
extension FiltersSteps {
    @discardableResult
    func tapOnRoomsTotalButton(_ roomsTotal: RoomsTotal) -> Self {
        XCTContext.runActivity(
            named: "Нажимаем на кнопку '\(roomsTotal.buttonTitle)' фильтра \"Комнатность\""
        ) { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.roomsTotalButton(for: roomsTotal),
                                                       in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func tapOnRoomsTotalButtons(_ roomsTotals: [RoomsTotal]) -> Self {
        for roomsTotal in roomsTotals {
            self.tapOnRoomsTotalButton(roomsTotal)
        }
        return self
    }

    @discardableResult
    func ensureAction(equalTo action: Action) -> Self {
        XCTAssertTrue(self.action(equalTo: action))
        return self
    }

    func action(equalTo action: Action) -> Bool {
        return XCTContext.runActivity(
            named: "Сравниваем тип фильтра со значением '\(action.buttonTitle)'"
        ) { _ -> Bool in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.actionButton, in: self.filtersVC)
            button.yreEnsureExists()
            return button.label == action.buttonTitle
        }
    }

    @discardableResult
    func switchToAction(_ action: Action) -> Self {
        return XCTContext.runActivity(
            named: "Переключаемся на фильтр '\(action.buttonTitle)'"
        ) { _ -> Self in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.actionButton, in: self.filtersVC)
            button.yreEnsureExists().tap()

            guard button.title != action.buttonTitle else { return self }

            let alertSteps = AnyAlertSteps(elementType: .sheet, alertID: Identifiers.filtersAlert)
            alertSteps.screenIsPresented().tapOnButton(withID: action.buttonTitle).screenIsDismissed()

            button.yreEnsureExistsWithTimeout()
            XCTAssertEqual(button.label, action.buttonTitle)

            return self
        }
    }

    @discardableResult
    func ensureCategory(equalTo category: Category) -> Self {
        XCTAssertTrue(self.category(equalTo: category))
        return self
    }

    func category(equalTo category: Category) -> Bool {
        return XCTContext.runActivity(
            named: "Сравниваем категорию фильтра со значением '\(category.buttonTitle)'"
        ) { _ -> Bool in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.categoryButton, in: self.filtersVC)
            button.yreEnsureExists()
            return button.label == category.buttonTitle
        }
    }

    @discardableResult
    func switchToCategory(_ category: Category) -> Self {
        return XCTContext.runActivity(
            named: "Переключаемся на категорию фильтра '\(category.buttonTitle)'"
        ) { _ -> Self in
            let button = ElementsProvider.obtainButton(identifier: Identifiers.categoryButton, in: self.filtersVC)
            button.yreEnsureExists().tap()

            guard button.title != category.buttonTitle else { return self }

            let alertSteps = AnyAlertSteps(elementType: .sheet, alertID: Identifiers.filtersAlert)
            alertSteps.screenIsPresented().tapOnButton(withID: category.buttonTitle).screenIsDismissed()

            button.yreEnsureExistsWithTimeout()
            XCTAssertEqual(button.label, category.buttonTitle)

            return self
        }
    }

    @discardableResult
    func tapOnPrice() -> FilterPricePickerSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Цена\"") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.priceCell, in: self.filtersTable)
            cell.yreEnsureExists().tap()
        }
        return FilterPricePickerSteps()
    }

    @discardableResult
    func priceParameter(hasValue value: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение фильтра \"Цена\" со значением '\(value)'") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.priceCell, in: self.filtersVC)
            cell.yreEnsureExistsWithTimeout()

            let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
            valueText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnApartmentTypeButton(_ apartmentType: ApartmentType) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент '\(apartmentType.readableName)' фильтра \"Тип квартиры\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: apartmentType.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func tapOnRentTimeButton(_ rentTime: RentTime) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент '\(rentTime.readableName)' фильтра \"Срок сдачи\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: rentTime.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func tapOnGarageTypeButton(_ garageType: GarageType) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент '\(garageType.readableName)' фильтра \"Тип гаража\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: garageType.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func tapOnObjectTypeButton(_ objectType: ObjectType) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент '\(objectType.readableName)' фильтра \"Тип объекта\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: objectType.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func tapOnVillageOfferTypeButton(_ villageOfferType: VillageOfferType) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент '\(villageOfferType.readableName)' фильтра \"Тип КП\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: villageOfferType.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExists().tap()
        }
        return self
    }

    @discardableResult
    func ensureObjectButtonExists(_  objectType: ObjectType) -> Self {
        ElementsProvider.obtainButton(
            identifier: objectType.accessibilityIdentifier,
            in: self.filtersVC
        )
        .yreEnsureExists()
        return self
    }

    @discardableResult
    func ensureObjectButtonNotExists(_  objectType: ObjectType) -> Self {
        ElementsProvider.obtainButton(
            identifier: objectType.accessibilityIdentifier,
            in: self.filtersVC
        )
        .yreEnsureNotExists()
        return self
    }

    @discardableResult
    func isApartmentTypeEqual(to value: ApartmentType) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение фильтра \"Тип квартиры\" со значением '\(value.readableName)'") { _ -> Void in
            let button = ElementsProvider.obtainElement(identifier: value.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExistsWithTimeout()

            XCTAssertTrue(button.isSelected)
        }
        return self
    }

    @discardableResult
    func isObjectTypeEqual(to value: ObjectType) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение фильтра \"Тип объекта\" со значением '\(value.readableName)'") { _ -> Void in
            let button = ElementsProvider.obtainElement(identifier: value.accessibilityIdentifier, in: self.filtersVC)
            button.yreEnsureExistsWithTimeout()

            XCTAssertTrue(button.isSelected)
        }
        return self
    }
}

// MARK: - House filters
extension FiltersSteps {
    @discardableResult
    func tapOnDeveloper() -> FiltersSuggestsListPickerSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Застройщик\"") { _ in
            let cell = self.filtersVC
                .descendants(matching: .any)
                .element(matching: NSPredicate(format: "identifier LIKE[cd] 'filters.cell.*developer'"))

            self.filtersVC.scrollToElement(element: cell, direction: .up)

            cell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }

        return FiltersSuggestsListPickerSteps()
    }

    @discardableResult
    func tapOnBuildingSeries() -> FiltersSuggestsListPickerSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку фильтра \"Серия дома\"") { _ in
            let cell = ElementsProvider.obtainElement(identifier: "filters.cell.buildingSeriesId", in: self.filtersVC)
                .yreEnsureExistsWithTimeout()

            self.filtersVC.scrollToElement(element: cell, direction: .up)

            cell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }

        return FiltersSuggestsListPickerSteps()
    }
}

// MARK: - Common Helpers
extension FiltersSteps {
    @discardableResult
    func isCellPresented(accessibilityIdentifier: String) -> Self {
        let cell = ElementsProvider.obtainElement(
            identifier: accessibilityIdentifier,
            in: self.filtersVC
        )

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
        return self
    }

    @discardableResult
    func tapOnBoolParameterCell(accessibilityIdentifier: String) -> Self {
        let cell = ElementsProvider.obtainElement(
            identifier: accessibilityIdentifier,
            in: self.filtersVC
        )

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
            .tap()
        return self
    }

    func boolParameterValue(cellAccessibilityIdentifier: String) -> Bool? {
        let cell = ElementsProvider.obtainElement(
            identifier: cellAccessibilityIdentifier,
            in: self.filtersVC
        )

        self.scrollToElement(element: cell)

        if cell.yreWaitForExistence() {
            let switchValue = cell.descendants(matching: .switch).element(boundBy: 0).value as? String
            if switchValue == "1" {
                return true
            }
            if switchValue == "0" {
                return false
            }
        }

        return nil
    }

    @discardableResult
    func isBoolParameterEnabled(cellAccessibilityIdentifier: String) -> Self {
        let value = self.boolParameterValue(cellAccessibilityIdentifier: cellAccessibilityIdentifier)
        XCTAssertEqual(value, true)

        return self
    }

    @discardableResult
    func isBoolParameterDisabled(cellAccessibilityIdentifier: String) -> Self {
        let value = self.boolParameterValue(cellAccessibilityIdentifier: cellAccessibilityIdentifier)
        XCTAssertEqual(value, false)

        return self
    }

    @discardableResult
    func tapOnBoolParameterCell(containing text: String) -> Self {
        let cellPredicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        let cell = self.filtersVC.cells.containing(cellPredicate).element

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
            .tap()
        return self
    }

    @discardableResult
    func isCellPresented(containing text: String) -> Self {
        let cellPredicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        let cell = self.filtersVC.cells.containing(cellPredicate).element

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
        return self
    }

    @discardableResult
    func isCellNotPresented(containing text: String) -> Self {
        let cellPredicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        self.filtersVC
            .cells
            .containing(cellPredicate)
            .element
            .yreEnsureNotExists()

        return self
    }

    @discardableResult
    func openNumberRangePicker(for parameter: FiltersSubtests.NumberRangeParameter) -> FilterNumberRangePickerSteps {
        let activityTitle = "Открываем пикер диапазона для параметра \"\(parameter.readableName)\""
        XCTContext.runActivity(named: activityTitle) { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: parameter.accessibilityIdentifier,
                in: self.filtersVC
            )

            self.scrollToElement(element: cell)

            cell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureVisible()
                .yreEnsureHittableWithTimeout()
                .tap()
        }
        return FilterNumberRangePickerSteps()
    }

    @discardableResult
    func openSingleSelectionPicker(for parameter: FiltersSubtests.SingleSelectParameter) -> FilterSingleSelectionPickerSteps {
        let activityTitle = "Открываем пикер единичного выбора для параметра \"\(parameter.readableName)\""
        XCTContext.runActivity(named: activityTitle) { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: parameter.accessibilityIdentifier,
                in: self.filtersVC
            )

            self.scrollToElement(element: cell)

            cell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureVisible()
                .yreEnsureHittableWithTimeout()
                .tap()
        }

        return FilterSingleSelectionPickerSteps(parameter.accessibilityIdentifier)
    }

    @discardableResult
    func openMultipleSelectionPicker(for parameter: FiltersSubtests.MultipleSelectParameter) -> FilterMultipleSelectionPickerSteps {
        let activityTitle = "Открываем пикер множественного выбора для параметра \"\(parameter.readableName)\""
        XCTContext.runActivity(named: activityTitle) { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: parameter.accessibilityIdentifier,
                in: self.filtersVC
            )

            self.scrollToElement(element: cell)

            cell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureVisible()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }

        return FilterMultipleSelectionPickerSteps()
    }

    @discardableResult
    func compareWithCellScreenshot(cellID: String, screenshotID: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом элемента фильтра") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: cellID,
                in: self.filtersVC
            )
            let screenshot = cell.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(
                image: screenshot,
                identifier: screenshotID,
                // Ignore the bottom separator.
                // For some Filter configurations a cell may be at the bottom of a Table Section - so no separator.
                // If the same cell is in the middle for another Filter configuration - it will have a separator.
                ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 1, right: 0)
            )
        }
        return self
    }

    @discardableResult
    func singleSelectParameter(with accessibilityIdentifier: String, hasValue value: String) -> Self {
        let cell = ElementsProvider.obtainElement(
            identifier: accessibilityIdentifier,
            in: self.filtersVC
        )

        cell.yreEnsureExistsWithTimeout()

        let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
        valueText.yreEnsureExistsWithTimeout()

        return self
    }

    @discardableResult
    func multipleSelectParameter(with accessibilityIdentifier: String, hasValue value: String) -> Self {
        let cell = ElementsProvider.obtainElement(
            identifier: accessibilityIdentifier,
            in: self.filtersVC
        )

        cell.yreEnsureExistsWithTimeout()

        let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
        valueText.yreEnsureExistsWithTimeout()

        return self
    }

    private func scrollToElement(
        element: XCUIElement,
        velocity: CGFloat = 1.0,
        swipeLimits: UInt = 5
    ) {
        XCTContext.runActivity(named: "Скроллим к элементу") { _ -> Void in
            element.yreEnsureExistsWithTimeout()
            
            let table = self.filtersTable
            let footer = ElementsProvider.obtainElement(
                identifier: Identifiers.footerView,
                in: self.filtersVC
            )

            table.scroll(
                to: element,
                adjustInteractionFrame: { $0.yreSubtract(footer.frame, from: .maxYEdge) },
                velocity: velocity,
                swipeLimits: swipeLimits
            )
        }
    }
}
