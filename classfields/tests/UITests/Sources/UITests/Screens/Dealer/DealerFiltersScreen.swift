import XCTest
import Snapshots

final class DealerFiltersScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var collectionView = findAll(.collectionView).firstMatch

    lazy var searchButton = find(by: "dealer.filters.btn.apply").firstMatch
    lazy var searchButtonSpinner = find(by: "dealer.filters.btn.apply.spinner").firstMatch

    lazy var categorySegment = find(by: "dealer.filters.segment.category").firstMatch
    lazy var emptySubcategoryField = optionalPlaceholder(name: "Раздел")

    lazy var defaultStatusPicker = find(by: "dealer.filters.picker.status").firstMatch
    lazy var statusPickerWithUserValue = optionalField(name: "Статус").firstMatch

    lazy var emptyMarkModelPicker = optionalPlaceholder(name: "Марки и модели").firstMatch
    lazy var filledMarkModelPicker = optionalField(name: "Марки и модели").firstMatch

    lazy var emptyPricePicker = optionalPlaceholder(name: "Цена, ₽")
    lazy var filledPricePicker = optionalField(name: "Цена, ₽")

    lazy var emptyVASPicker = optionalPlaceholder(name: "Объявления с услугами")
    lazy var vasPickerWithUserValue = optionalField(name: "Объявления с услугами")

    lazy var emptyVINPicker = optionalPlaceholder(name: "Проверки по VIN")
    lazy var vinPickerWithUserValue = optionalField(name: "Проверки по VIN")

    func conditionSegment(category: String) -> XCUIElement {
        return find(by: "dealer.filters.segment.condition.\(category)").firstMatch
    }

    func optionalPlaceholder(name: String) -> XCUIElement {
        return find(by: "dealer.filters.optional_placeholder.\(name)").firstMatch
    }

    func optionalField(name: String) -> XCUIElement {
        return find(by: "dealer.filters.optional_field.\(name)").firstMatch
    }
}

final class DealerFiltersSubcategoryPickerScreen: BaseScreen {
    lazy var doneButton = find(by: "Готово").firstMatch

    @discardableResult
    func option(named name: String) -> XCUIElement {
        return find(by: name).firstMatch
    }
}

final class DealerFiltersStatusPickerScreen: BaseScreen {
    lazy var allOption = find(by: "Все").firstMatch
    lazy var activeOption = find(by: "Активные").firstMatch
    lazy var pendingOption = find(by: "Ждут активации").firstMatch
    lazy var inactiveOption = find(by: "Неактивные").firstMatch
    lazy var blockedOption = find(by: "Заблокированные").firstMatch

    lazy var doneButton = find(by: "Готово").firstMatch
}

final class DealerFiltersPricePickerScreen: BaseScreen {
    lazy var fromOption = find(by: "from").firstMatch
    lazy var toOption = find(by: "to").firstMatch
    lazy var doneButton = find(by: "Готово").firstMatch

    @discardableResult
    func done() -> Self {
        Step("На пикере цены должны тапнуть `Готово`") {
            doneButton.tap()
        }

        return self
    }

    @discardableResult
    func enterLowerBound(_ value: Int) -> Self {
        Step("Должны ввести нижнюю границу цены") {
            let field = fromOption
            field.tap()
            field.typeText("\(value)")
        }

        return self
    }

    @discardableResult
    func enterUpperBound(_ value: Int) -> Self {
        Step("Должны ввести верхнюю границу цены") {
            let field = toOption
            field.tap()
            field.typeText("\(value)")
        }

        return self
    }

    func clearLowerBound() {
        Step("Должны очистить нижнюю границу цены") {
            fromOption.buttons["Очистить текст"].shouldExist().tap()
        }
    }

    func clearUpperBound() {
        Step("Должны очистить верхнюю границу цены") {
            toOption.buttons["Очистить текст"].shouldExist().tap()
        }
    }
}

final class DealerFiltersVASPickerScreen: BaseScreen {
    lazy var premiumOption = find(by: "Премиум").firstMatch
    lazy var freshOption = find(by: "Поднятые в поиске").firstMatch
    lazy var autoApplyOption = find(by: "С автоподнятием").firstMatch
    lazy var turboOption = find(by: "Турбо продажа").firstMatch
    lazy var specialOption = find(by: "Спецпредложение").firstMatch
    lazy var stickersOption = find(by: "Со стикерами").firstMatch
    lazy var alwaysFirstPageOption = find(by: "С автостратегиями").firstMatch

    lazy var doneButton = find(by: "Готово").firstMatch
}

final class DealerFiltersVINPickerScreen: BaseScreen {
    lazy var okOption = find(by: "Проверено").firstMatch
    lazy var untrustedOption = find(by: "Серые отчёты").firstMatch
    lazy var errorOption = find(by: "Красные отчёты").firstMatch
    lazy var noneOption = find(by: "Без отчёта").firstMatch

    lazy var doneButton = find(by: "Готово").firstMatch
}

final class DealerMarkModelPickerScreen: BaseScreen {
    lazy var searchTextField = findAll(.textField).firstMatch
    lazy var resetButton = findAll(.button)["modal_base.btn.left"].firstMatch
    lazy var doneButton = find(by: "Готово").firstMatch

    @discardableResult
    func arrow(mark: String) -> XCUIElement {
        let elem = find(by: "dealer.filters.mark_and_model.btn.arrow.\(mark)").firstMatch
        Step("Ищем стрелку для тогла марки `\(mark)`") {
            elem.shouldExist()
        }
        return elem
    }

    @discardableResult
    func checkbox(option: String, selected: Bool) -> XCUIElement {
        return find(by: "dealer.filters.mark_and_model.btn.checkbox.\(option)_\(selected)").firstMatch
    }

    @discardableResult
    func mark(_ name: String) -> XCUIElement {
        return find(by: "dealer.filters.mark_and_model.lbl.mark.\(name)").firstMatch
    }

    @discardableResult
    func model(_ name: String) -> XCUIElement {
        return find(by: "dealer.filters.mark_and_model.lbl.model.\(name)").firstMatch
    }

    @discardableResult
    func cellLabel(named name: String) -> XCUIElement {
        return find(by: name).firstMatch
    }
}
