import XCTest
import Snapshots

final class DealerFiltersSteps: BaseSteps {
    // MARK: - Common

    @discardableResult
    func resultsButtonTitleShouldMatch(text: String) -> Self {
        Step("На экране фильтров у кнопки поиска должен быть тайтл `\(text)`") {
            let screen = self.onDealerFiltersScreen()
            screen.searchButton.shouldExist()
            screen.searchButton
                .staticTexts.matching(identifier: text)
                .firstMatch.shouldExist(timeout: 10)
        }

        return self
    }

    @discardableResult
    func shouldSeeCommonContent() -> Self {
        Step("На экране фильтров должен быть сегмент выбора категории ТС") {
            let screen = self.onDealerFiltersScreen()
            screen.categorySegment.shouldExist()
        }
        Step("На экране фильтров должно быть поле с выбором марки-модели") {
            let screen = self.onDealerFiltersScreen()
            screen.emptyMarkModelPicker.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldBeNoConditionSegment() -> Self {
        Step("На экране фильтров не должно быть выбора секции ТС") {
            let screen = self.onDealerFiltersScreen()
            screen.app.staticTexts["Новые"].shouldNotExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeEmptySubcategorySegment() -> Self {
        Step("На экране фильтров должно быть пустое поле подкатегории") {
            let screen = self.onDealerFiltersScreen()
            screen.emptySubcategoryField.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeConditionSegment(category: String, name: String) -> Self {
        Step("Должен быть сегмент выбора секции для категории `\(name)`") {
            let screen = self.onDealerFiltersScreen()
            screen.conditionSegment(category: category).shouldExist()
        }

        return self
    }

    // MARK: - Fields

    @discardableResult
    func shouldSeeStatusPickerContent() -> Self {
        Step("На экране фильтров должно быть поле `Статус`") {
            self.onDealerFiltersScreen().defaultStatusPicker.tap()
            let picker = self.onStatusPickerScreen()

            Step("В пикере статуса должен быть вариант `Все`") {
                picker.allOption.shouldExist()
            }
            Step("В пикере статуса должен быть вариант `Активные`") {
                picker.activeOption.shouldExist()
            }
            Step("В пикере статуса должен быть вариант `Ждут активации`") {
                picker.pendingOption.shouldExist()
            }
            Step("В пикере статуса должен быть вариант `Неактивные`") {
                picker.inactiveOption.shouldExist()
            }
            Step("В пикере статуса должен быть вариант `Заблокированные`") {
                picker.blockedOption.shouldExist()
            }
            Step("На пикере статуса должна быть кнопка `Готово`") {
                picker.doneButton.tap()
            }
        }

        return self
    }

    @discardableResult
    func shouldSeeModels(elements: [String]) -> Self {
        let options = elements.joined(separator: ", ")
        Step("На пикере марки-модели должны увидеть марки `\(options)`") {
            let screen = self.onMarkModelPickerScreen()
            for elem in elements {
                Step("Проверяем наличие \(elem)") {
                    screen.model(elem).shouldExist()
                }
            }
        }

        return self
    }

    @discardableResult
    func shouldSeePriceFromTo() -> Self {
        Step("На экране фильтров должно быть поле `Цена`") {
            self.onDealerFiltersScreen().emptyPricePicker.tap()
            let picker = self.onPricePickerScreen()

            Step("На пикере цены должно быть поле `Цена от`") {
                picker.fromOption.shouldExist()
            }
            Step("На пикере цены должно быть поле `Цена до`") {
                picker.toOption.shouldExist()
            }
            Step("На пикере цены должна быть кнопка `Готово`") {
                picker.doneButton.tap()
            }
        }

        return self
    }

    @discardableResult
    func shouldSeeVASPickerContent() -> Self {
        Step("На экране фильтров должно быть поле `Объявления с услугами`") {
            self.onDealerFiltersScreen().emptyVASPicker.tap()
            let picker = self.onVASPickerScreen()

            Step("В пикере VAS должен быть вариант `Премиум`") {
                picker.premiumOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `Поднятые в поиске`") {
                picker.freshOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `С автоподнятием`") {
                picker.autoApplyOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `Турбо продажа`") {
                picker.turboOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `Спецпредложение`") {
                picker.specialOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `Со стикерами`") {
                picker.stickersOption.shouldExist()
            }
            Step("В пикере VAS должен быть вариант `С автостратегиями`") {
                picker.alwaysFirstPageOption.shouldExist()
            }
            Step("На пикере VAS должна быть кнопка `Готово`") {
                picker.doneButton.tap()
            }
        }

        return self
    }

    @discardableResult
    func shouldSeeVINPickerContent() -> Self {
        Step("На экране фильтров должно быть поле `Проверки по VIN`") {
            self.onDealerFiltersScreen().emptyVINPicker.tap()
            let picker = self.onVINPickerScreen()

            Step("В пикере VIN должен быть вариант `Проверено`") {
                picker.okOption.shouldExist()
            }
            Step("В пикере VIN должен быть вариант `Серые отчёты`") {
                picker.untrustedOption.shouldExist()
            }
            Step("В пикере VIN должен быть вариант `Красные отчёты`") {
                picker.errorOption.shouldExist()
            }
            Step("В пикере VIN должен быть вариант `Без отчёта`") {
                picker.noneOption.shouldExist()
            }
            Step("На пикере VIN должна быть кнопка `Готово`") {
                picker.doneButton.tap()
            }
        }

        return self
    }

    // MARK: - Fields

    @discardableResult
    func shouldSeeEmptyVINPickerField() -> Self {
        Step("На экране фильтров должно быть пустое поле `Проверки по VIN`") {
            self.onDealerFiltersScreen()
                .emptyVINPicker.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldNotSeeVINPickerField() -> Self {
        Step("На экране фильтров не должно быть поля `Проверки по VIN`") {
            let screen = self.onDealerFiltersScreen()
            screen.emptyVINPicker.shouldNotExist()
            screen.vinPickerWithUserValue.shouldNotExist()
        }

        return self
    }

    // MARK: - Actions

    @discardableResult
    func swipeCategorySegmentTo(index: Int) -> Self {
        Step("Свайпаем сегмент категории ТС к пункту \(index + 1)") {
            let segment = self.onDealerFiltersScreen().categorySegment
            for _ in 0 ..< index {
                segment.gentleSwipe(.right)
            }
        }

        return self
    }

    @discardableResult
    func selectSubcategory(named name: String) -> Self {
        Step("Можем выбрать подкатегорию \(name)") {
            let screen = self.onDealerFiltersScreen()

            Step("Ищем поле `Раздел`") {
                if screen.optionalPlaceholder(name: "Раздел").exists {
                    screen.optionalPlaceholder(name: "Раздел").tap()
                } else {
                    screen.emptySubcategoryField.tap()
                }
            }

            let picker = self.onSubcategoryPickerScreen()
            picker.option(named: name).tap()

            Step("Пикер должен был закрыться") {
                picker.doneButton.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func tapOnConditionSegment(category: String, name: String, at index: Int) -> Self {
        Step("В сегменте `Состояние` должны выбрать вариант \(index + 1)") {
            let segment = self.onDealerFiltersScreen()
                .conditionSegment(category: category)

            Step("Ищем сегмент для категории \(name)") {
                segment.shouldExist()
            }

            for _ in 0 ..< index {
                segment.gentleSwipe(.right)
            }
        }

        return self
    }

    @discardableResult
    func selectStatusPickerOption(at index: Int) -> Self {
        Step("Должны выбрать пункт \(index + 1) в фильтре по статусу") {
            let screen = self.onDealerFiltersScreen()
            Step("Ищем поле `Статус`") {
                if screen.statusPickerWithUserValue.exists {
                    screen.statusPickerWithUserValue.tap()
                } else {
                    screen.defaultStatusPicker.tap()
                }
            }
            let picker = self.onStatusPickerScreen()
            let options: [XCUIElement] = [
                picker.allOption, picker.activeOption, picker.pendingOption,
                picker.inactiveOption, picker.blockedOption
            ]
            XCTAssertLessThan(index, options.count, "Индекс пункта опции статуса должен быть в пределах кол-ва опций")

            options[index].tap()

            Step("Пикер должен был закрыться") {
                picker.doneButton.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func openMarkAndModelPicker() -> Self {
        Step("Должны открыть пикер марки-модели") {
            let screen = self.onDealerFiltersScreen()

            Step("Ищем поле `Марки и модели`") {
                if screen.filledMarkModelPicker.exists {
                    screen.filledMarkModelPicker.tap()
                } else {
                    screen.emptyMarkModelPicker.tap()
                }
            }
        }

        return self
    }

    @discardableResult
    func expandMark(name: String) -> Self {
        Step("Должны раскрыть марку `\(name)`") {
            self.onMarkModelPickerScreen()
                .arrow(mark: name)
                .shouldExist().tap()
        }

        return self
    }

    @discardableResult
    func toggleMark(name: String) -> Self {
        Step("Должны тапнуть по марке `\(name)`") {
            self.onMarkModelPickerScreen()
                .mark(name)
                .shouldExist().tap()
        }

        return self
    }

    @discardableResult
    func toggleModel(name: String) -> Self {
        Step("Должны тапнуть по модели `\(name)`") {
            self.onMarkModelPickerScreen()
                .model(name)
                .shouldExist().tap()
        }

        return self
    }

    @discardableResult
    func typeOnMarkModelPicker(text: String) -> Self {
        Step("Набираем `\(text)` в пикере марки-модели") {
            let input = self.onMarkModelPickerScreen().searchTextField

            Step("Должен быть серч-бар") {
                input.shouldExist()
            }

            input.tap()
            input.typeText(text)
        }

        return self
    }

    @discardableResult
    func markModelPickerCheckHasNothingFoundTitle() -> Self {
        Step("Ищем лейбл `Ничего не нашлось`") {
            app.descendants(matching: .staticText)
                .matching(identifier: "Ничего не нашлось")
                .firstMatch.shouldExist()
        }

        return self
    }

    @discardableResult
    func markModelPickerResetSearchIfNothingFound() -> Self {
        let clearSearchLabel = app.descendants(matching: .staticText)
            .matching(identifier: "Очистить запрос")
            .firstMatch

        Step("Ищем кнопку `Очистить запрос`") {
            clearSearchLabel.shouldExist()
        }

        clearSearchLabel.tap()

        return self
    }

    @discardableResult
    func markModelPickerCheckHasEmptySearchQuery() -> Self {
        Step("Проверяем, что поле поиска пустое") {
            let searchTextField = onMarkModelPickerScreen()
                .searchTextField

            Step("Должен быть только плейсхолдер") {
                XCTAssertEqual(searchTextField.placeholderValue, searchTextField.value as? String)
                XCTAssertEqual(searchTextField.placeholderValue, "Марки и модели")
            }
        }

        return self
    }

    @discardableResult
    func enterPriceLowerBound(_ value: Int) -> Self {
        Step("Должны открыть пикер ввода цены, указать нижнюю границу") {
            let screen = self.onDealerFiltersScreen()

            Step("Должны найти поле `Цена`") {
                if screen.emptyPricePicker.waitForExistence(timeout: 2) {
                    screen.emptyPricePicker.tap()
                } else {
                    screen.filledPricePicker.tap()
                }
            }

            self.onPricePickerScreen()
                .enterLowerBound(value)
        }

        return self
    }

    @discardableResult
    func enterPriceUpperBound(_ value: Int) -> Self {
        Step("Должны открыть пикер ввода цены, указать верхнюю границу") {
            let screen = self.onDealerFiltersScreen()

            Step("Должны найти поле `Цена`") {
                if screen.emptyPricePicker.waitForExistence(timeout: 2) {
                    screen.emptyPricePicker.tap()
                } else {
                    screen.filledPricePicker.tap()
                }
            }

            self.onPricePickerScreen()
                .enterUpperBound(value)
        }

        return self
    }

    @discardableResult
    func resetPriceLowerBound() -> Self {
        Step("Должны открыть пикер ввода цены, сбросить нижнюю границу") {
            let screen = self.onDealerFiltersScreen()

            Step("Должны найти поле `Цена`") {
                if screen.emptyPricePicker.waitForExistence(timeout: 2) {
                    screen.emptyPricePicker.tap()
                } else {
                    screen.filledPricePicker.tap()
                }
            }

            let picker = self.onPricePickerScreen()
            picker.clearLowerBound()

            let textField = picker.fromOption
                .textFields.firstMatch

            Step("Проверяем, что цена сбросилась") {
                textField.shouldExist()
                XCTAssertEqual(textField.value as? String, "от")
            }
        }

        return self
    }

    @discardableResult
    func resetUpperLowerBound() -> Self {
        Step("Должны открыть пикер ввода цены, сбросить верхнюю границу") {
            let screen = self.onDealerFiltersScreen()

            Step("Должны найти поле `Цена`") {
                if screen.emptyPricePicker.waitForExistence(timeout: 2) {
                    screen.emptyPricePicker.tap()
                } else {
                    screen.filledPricePicker.tap()
                }
            }

            let picker = self.onPricePickerScreen()
            picker.clearUpperBound()

            let textField = picker.toOption
                .textFields.firstMatch

            Step("Проверяем, что цена сбросилась") {
                textField.shouldExist()
                XCTAssertEqual(textField.value as? String, "до")
            }
        }

        return self
    }

    @discardableResult
    func selectVASPickerOption(at index: Int) -> Self {
        Step("Должны открыть пикер VAS и выбрать вариант \(index + 1)") {
            let screen = self.onDealerFiltersScreen()

            Step("Ищем поле `Объявления с услугами`") {
                if screen.vasPickerWithUserValue.exists {
                    screen.vasPickerWithUserValue.tap()
                } else {
                    screen.emptyVASPicker.tap()
                }
            }

            let picker = self.onVASPickerScreen()

            let options: [XCUIElement] = [
                picker.premiumOption, picker.freshOption, picker.autoApplyOption,
                picker.turboOption, picker.specialOption, picker.stickersOption,
                picker.alwaysFirstPageOption
            ]

            options[index].tap()

            Step("Пикер должен был закрыться") {
                picker.doneButton.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func selectVINPickerOption(at index: Int) -> Self {
        Step("Должны открыть пикер VIN и выбрать вариант \(index + 1)") {
            let screen = self.onDealerFiltersScreen()

            Step("Ищем поле `Проверки по VIN`") {
                if screen.vinPickerWithUserValue.exists {
                    screen.vinPickerWithUserValue.tap()
                } else {
                    screen.emptyVINPicker.tap()
                }
            }

            let picker = self.onVINPickerScreen()

            let options: [XCUIElement] = [
                picker.okOption, picker.untrustedOption,
                picker.errorOption, picker.noneOption
            ]

            options[index].tap()
            picker.doneButton.shouldNotExist()

            Step("Пикер должен был закрыться") {
                picker.doneButton.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func openVINPicker() -> Self {
        Step("Должны открыть пикер `Проверки по VIN`") {
            let screen = self.onDealerFiltersScreen()

            Step("Ищем поле `Проверки по VIN`") {
                if screen.vinPickerWithUserValue.exists {
                    screen.vinPickerWithUserValue.tap()
                } else {
                    screen.emptyVINPicker.tap()
                }
            }
        }

        return self
    }

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем загрузки экрана фильтров") {
            self.onDealerFiltersScreen()
                .searchButtonSpinner
                .shouldNotExist(timeout: 5)
        }

        return self
    }

    // MARK: - Pickers

    @discardableResult
    func markModelPickerShouldHas(mark: String) -> Self {
        Step("Должны найти марку `\(mark)`") {
            self.onMarkModelPickerScreen()
                .mark(mark).shouldExist()
        }

        return self
    }

    @discardableResult
    func markModelPickerShouldHas(model: String) -> Self {
        Step("Должны найти модель `\(model)`") {
            self.onMarkModelPickerScreen()
                .model(model).shouldExist()
        }

        return self
    }

    @discardableResult
    func markModelPickerShouldNotHas(mark: String) -> Self {
        Step("Не должны найти марку `\(mark)`") {
            self.onMarkModelPickerScreen()
                .mark(mark).shouldNotExist()
        }

        return self
    }

    @discardableResult
    func markModelPickerShouldHasMark(_ name: String, selected: Bool) -> Self {
        let title = selected ? "отмеченной" : "не выбранной"
        Step("Марка `\(name)` должна быть \(title)") {
            self.onMarkModelPickerScreen()
                .checkbox(option: name, selected: selected).shouldExist()
        }

        return self
    }

    @discardableResult
    func markModelPickerShouldHasModel(_ name: String, selected: Bool) -> Self {
        let title = selected ? "отмеченной" : "не выбранной"
        Step("Модель `\(name)` должна быть \(title)") {
            self.onMarkModelPickerScreen()
                .checkbox(option: name, selected: selected).shouldExist()
        }

        return self
    }

    @discardableResult
    func markModelResetButtonShouldBe(enabled: Bool) -> Self {
        let title = enabled ? "активна" : "неактивна"
        Step("Проверяем, что кнопка сбросить \(title)") {
            self.onMarkModelPickerScreen()
                .resetButton.shouldExist()
                .shouldBe(enabled: enabled)
        }

        return self
    }

    @discardableResult
    func markModelResetAll() -> Self {
        Step("Тапаем сбросить на пикере марки-модели") {
            self.onMarkModelPickerScreen()
                .resetButton.shouldExist()
                .tap()
        }

        return self
    }

    // MARK: - Screens

    func onDealerFiltersScreen() -> DealerFiltersScreen {
        return baseScreen.on(screen: DealerFiltersScreen.self)
    }

    func onSubcategoryPickerScreen() -> DealerFiltersSubcategoryPickerScreen {
        return baseScreen.on(screen: DealerFiltersSubcategoryPickerScreen.self)
    }

    func onStatusPickerScreen() -> DealerFiltersStatusPickerScreen {
        return baseScreen.on(screen: DealerFiltersStatusPickerScreen.self)
    }

    func onPricePickerScreen() -> DealerFiltersPricePickerScreen {
        return baseScreen.on(screen: DealerFiltersPricePickerScreen.self)
    }

    func onVASPickerScreen() -> DealerFiltersVASPickerScreen {
        return baseScreen.on(screen: DealerFiltersVASPickerScreen.self)
    }

    func onVINPickerScreen() -> DealerFiltersVINPickerScreen {
        return baseScreen.on(screen: DealerFiltersVINPickerScreen.self)
    }

    func onMarkModelPickerScreen() -> DealerMarkModelPickerScreen {
        return baseScreen.on(screen: DealerMarkModelPickerScreen.self)
    }
}
