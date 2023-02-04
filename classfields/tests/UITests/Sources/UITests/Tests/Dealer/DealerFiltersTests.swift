import XCTest

/// @depends_on AutoRuDealerFilters
final class DealerFiltersTests: DealerBaseTest {
    // MARK: - Campaigns

    func test_categoryAllHasNoConditionOption() {
        Step("Для категории `Все` не должно быть выбора секции") {
            addCommonRequestHandler()
                .openFiltersScreen()
                .shouldBeNoConditionSegment()
        }
    }

    func test_categoryAutoHasConditionOption() {
        Step("Для категории `Легковые` должен быть выбор секции") {
            addCommonRequestHandler()
                .openFiltersScreen()
                .swipeCategorySegmentTo(index: 1)
                .shouldSeeConditionSegment(category: "cars", name: "Легковые")
        }
    }

    func test_categoryAutoHasNoConditionOption() {
        addMarkAndModelsHandler()
        let campaignsPath = "/dealer/campaigns"
        server.addHandler("GET \(campaignsPath)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_no_new_campaigns", userAuthorized: true)
        }

        launch()
        Step("Для категории `Легковые` не должно быть выбора секции") {
            openFiltersScreen()
                .swipeCategorySegmentTo(index: 1)
                .shouldBeNoConditionSegment()
        }
    }

    func test_trucksHasConditionAndSubcategory() {
        Step("Для категории `Комтранс` должнен быть выбор секции и подкатегории") {
            addCommonRequestHandler()
                .openFiltersScreen()
                .swipeCategorySegmentTo(index: 2)
                .shouldSeeConditionSegment(category: "trucks", name: "Комтранс")
                .shouldSeeEmptySubcategorySegment()
        }
    }

    func test_motoHasConditionAndSubcategory() {
        Step("Для категории `Мото` должнен быть выбор секции и подкатегории") {
            addCommonRequestHandler()
                .openFiltersScreen()
                .swipeCategorySegmentTo(index: 3)
                .shouldSeeConditionSegment(category: "moto", name: "Мото")
                .shouldSeeEmptySubcategorySegment()
        }
    }

    func test_onlyAutoHasVINFilter() {
        Step("Фильтр `Проверки по VIN` должен быть только для категорий `Все` и `Легковые`") {
            let steps = addCommonRequestHandler()
                .openFiltersScreen()

            Step("Для категории `Все` должно быть поле `Проверки по VIN`") {
                steps.shouldSeeEmptyVINPickerField()
            }
            Step("Для категории `Легковые` должно быть поле `Проверки по VIN`") {
                steps.swipeCategorySegmentTo(index: 1)
                    .shouldSeeEmptyVINPickerField()
            }
            Step("Для категории `Комтранс` не должно быть поля `Проверки по VIN`") {
                steps.swipeCategorySegmentTo(index: 2)
                    .shouldNotSeeVINPickerField()
            }
            Step("Для категории `Мото` не должно быть поля `Проверки по VIN`") {
                steps.swipeCategorySegmentTo(index: 3)
                    .shouldNotSeeVINPickerField()
            }
        }
    }

    // MARK: - Fields

    func test_hasCommonPickersOptions() {
        Step("На экране фильтров должны быть основные поля") {
            addCommonRequestHandler()
                .openFiltersScreen()
                .shouldSeeStatusPickerContent()
                .shouldSeePriceFromTo()
                .shouldSeeVASPickerContent()
                .shouldSeeVINPickerContent()
        }
    }

    // MARK: - Filters

    func test_applyMultipleFilters() {
        addCampaignsHandler()

        let byCarsExp = expectation(description: "Должны отфильтровать по категории `Легковые`")
        let byAllExp = expectation(description: "Должны отфильтровать по статусу `Все`")
        let byPriceFromExp = expectation(description: "Должны отфильтровать по `Цене от`")
        let byPriceToExp = expectation(description: "Должны отфильтровать по `Цене до`")
        let all = [byCarsExp, byAllExp, byPriceFromExp, byPriceToExp]
        all.forEach { $0.assertForOverFulfill = false }

        server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE") { (_, _) -> Response? in
            return Response.okResponse(fileName: "dealer_mark-models_all_filtered")
        }

        server.addHandler("GET /user/offers/cars/mark-models?status=ACTIVE") { (_, _) -> Response? in
            byCarsExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_filtered")
        }

        server.addHandler("GET /user/offers/cars/mark-models?") { (_, _) -> Response? in
            byAllExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_filtered")
        }

        server.addHandler("GET /user/offers/cars/mark-models?price_from=332600") { (_, _) -> Response? in
            byPriceFromExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_filtered")
        }

        server.addHandler("GET /user/offers/cars/mark-models?price_from=332600&price_to=454771") { (_, _) -> Response? in
            byPriceToExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_filtered")
        }

        let steps = addCampaignsHandler()
            .openFiltersScreen()

        Step("Фильтруем только по легковым") {
            steps.swipeCategorySegmentTo(index: 1)
            wait(for: [byCarsExp], timeout: 1)
        }

        Step("Фильтруем по статусу `Все`") {
            steps.selectStatusPickerOption(at: 0)
            wait(for: [byAllExp], timeout: 1)
        }

        Step("Фильтруем по цене от `332_600`") {
            steps.enterPriceLowerBound(332_600)
                .onPricePickerScreen()
                .done()
            wait(for: [byPriceFromExp], timeout: 1)
        }

        Step("Фильтруем по цене до `454_771`") {
            steps.enterPriceUpperBound(454_771)
                .onPricePickerScreen()
                .done()
            wait(for: [byPriceToExp], timeout: 1)
        }
    }

    // MARK: - Mark & Model

    func test_markAndModelDependsOnFilters() {
        let filterChangedExp = expectation(description: "Отфильтровали по VIN")
        filterChangedExp.assertForOverFulfill = false

        server.addHandler("GET /user/offers/all/mark-models?exclude_tag=vin_resolution_ok&exclude_tag=vin_resolution_untrusted&exclude_tag=vin_resolution_unknown&exclude_tag=vin_resolution_error&exclude_tag=vin_resolution_invalid&status=ACTIVE") { (_, _) -> Response? in
            filterChangedExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_vin_filtered", userAuthorized: true)
        }

        let steps = addCommonRequestHandler()
            .openFiltersScreen()

        Step("Смотрим, что у нас есть марка не из списка тех, которые будут отфильтрованы") {
            steps
                .openMarkAndModelPicker()
                .markModelPickerShouldHas(mark: "Lexus")
                .onMarkModelPickerScreen()
                .doneButton.tap()
        }

        Step("Применяем фильтр по VIN `Без отчёта`") {
            steps
                .openVINPicker()
                .onVINPickerScreen()
                .noneOption.tap()

            wait(for: [filterChangedExp], timeout: 2)
        }

        Step("В списке марок больше не должно быть `Lexus`") {
            steps
                .openMarkAndModelPicker()
                .markModelPickerShouldNotHas(mark: "Lexus")
                .onMarkModelPickerScreen()
                .doneButton.tap()
        }

        Step("В списке марок и моделей должно остаться `BMW`") {
            steps
                .openMarkAndModelPicker()
                .markModelPickerShouldHas(mark: "BMW")
                .onMarkModelPickerScreen()
                .doneButton.tap()
        }

        Step("В раскрытом списке BMW должны быть 4 серия, 5 серия") {
            steps
                .openMarkAndModelPicker()
                .expandMark(name: "BMW")
                .shouldSeeModels(elements: ["4 серия", "5 серия"])
        }
    }

    func test_markSelectionAltersFilters() {
        let filterChangedExp = expectation(description: "Отфильтровали по VIN")
        let filterByBMWModelOnlyApplied = expectation(description: "Должны отфильтровать только по `BMW 4 серия`")
        let filterByAllBMWModelsAppliedExp = expectation(description: "Должен учитываться фильтр по марке и модели")
        let all = [filterChangedExp, filterByBMWModelOnlyApplied, filterByAllBMWModelsAppliedExp]
        all.forEach { $0.assertForOverFulfill = false }

        server.addHandler("GET /user/offers/all/mark-models?exclude_tag=vin_resolution_ok&exclude_tag=vin_resolution_untrusted&exclude_tag=vin_resolution_unknown&exclude_tag=vin_resolution_error&exclude_tag=vin_resolution_invalid&status=ACTIVE") { (_, _) -> Response? in
            filterChangedExp.fulfill()
            return Response.okResponse(fileName: "dealer_mark-models_all_vin_filtered", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all/count?exclude_tag=vin_resolution_ok&exclude_tag=vin_resolution_untrusted&exclude_tag=vin_resolution_unknown&exclude_tag=vin_resolution_error&exclude_tag=vin_resolution_invalid&mark_model=BMW#4&status=ACTIVE") { (_, _) -> Response? in
            filterByBMWModelOnlyApplied.fulfill()
            return Response.okResponse(fileName: "dealer_offers_count_bmw_4er", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all/count?exclude_tag=vin_resolution_ok&exclude_tag=vin_resolution_untrusted&exclude_tag=vin_resolution_unknown&exclude_tag=vin_resolution_error&exclude_tag=vin_resolution_invalid&mark_model=BMW#4&mark_model=BMW#5ER&status=ACTIVE") { (_, _) -> Response? in
            filterByAllBMWModelsAppliedExp.fulfill()
            return Response.okResponse(fileName: "dealer_offers_count_all_bmw", userAuthorized: true)
        }

        let steps = addCommonRequestHandler()
            .openFiltersScreen()

        Step("Применяем фильтр по VIN `Без отчёта`") {
            steps
                .openVINPicker()
                .onVINPickerScreen()
                .noneOption.tap()

            wait(for: [filterChangedExp], timeout: 2)
        }

        Step("Раскрываем `BMW` и выбираем `4 серия`") {
            steps
                .openMarkAndModelPicker()
                .expandMark(name: "BMW")
                .toggleModel(name: "4 серия")
                .markModelPickerShouldHasModel("4 серия", selected: true)
                .onMarkModelPickerScreen()
                .doneButton.tap()

            wait(for: [filterByBMWModelOnlyApplied], timeout: 2)
        }

        Step("Фильтруем по всем моделям `BMW`") {
            steps
                .openMarkAndModelPicker()
                .toggleMark(name: "BMW")
                .markModelPickerShouldHasMark("BMW", selected: true)
                .onMarkModelPickerScreen()
                .doneButton.tap()

            wait(for: [filterByAllBMWModelsAppliedExp], timeout: 2)
        }

        Step("Проверяем кол-во результатов на кнопке поиска") {
            steps.resultsButtonTitleShouldMatch(text: "Показать 2 предложения")
        }
    }

    func test_markAndModelInteractions() {
        server.addHandler("GET /user/offers/all/mark-models *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_mark-models_all_vin_filtered", userAuthorized: true)
        }

        let steps = addCommonRequestHandler()
            .openFiltersScreen()

        Step("Фильтруем по VIN (Без отчёта)") {
            steps
                .openVINPicker()
                .onVINPickerScreen()
                .noneOption.tap()
        }

        Step("Проверяем, что кнопка сбросить неактивна") {
            steps
                .openMarkAndModelPicker()
                .wait(for: 1)
                .markModelResetButtonShouldBe(enabled: false)
        }

        Step("После выбора модели кнопка сброса должна стать активной") {
            steps
                .expandMark(name: "BMW")
                .toggleModel(name: "4 серия")
                .markModelPickerShouldHasModel("4 серия", selected: true)
                .markModelResetButtonShouldBe(enabled: true)
        }

        Step("После выбора модели, марка должна остаться неотмеченной") {
            steps.markModelPickerShouldHasMark("BMW", selected: false)
        }

        Step("Можем снять выделение с модели") {
            steps
                .toggleModel(name: "4 серия")
                .markModelPickerShouldHasModel("4 серия", selected: false)
                .markModelResetButtonShouldBe(enabled: false)
        }

        Step("Если отмечаем марку, должны выбраться все модели") {
            steps
                .toggleMark(name: "BMW")
                .markModelPickerShouldHasMark("BMW", selected: true)
                .markModelPickerShouldHasModel("4 серия", selected: true)
                .markModelPickerShouldHasModel("5 серия", selected: true)
                .markModelResetButtonShouldBe(enabled: true)
        }

        Step("Кнопка `сбросить` снимает выделение со всего") {
            steps
                .markModelResetButtonShouldBe(enabled: true)
                .markModelResetAll()
                .markModelPickerShouldHasMark("BMW", selected: false)
                .markModelPickerShouldHasModel("4 серия", selected: false)
                .markModelPickerShouldHasModel("5 серия", selected: false)
                .markModelResetButtonShouldBe(enabled: false)
        }
    }

    // MARK: - Mark and Model search

    func test_markAndModelSearchExpand() {
        let screen = addCommonRequestHandler()
            .openFiltersScreen()
            .openMarkAndModelPicker()

        Step("Провярем фильтрацию на пикере марки-модели") {
            screen.typeOnMarkModelPicker(text: "серия")

            Step("После фильтрации весь список должен быть развернут") {
                screen
                    .markModelPickerShouldHas(mark: "BMW")
                    .markModelPickerShouldHas(model: "4 серия")
                    .markModelPickerShouldHas(model: "5 серия")
            }
        }
    }

    func test_emptyMarkAndModelPicker() {
        let screen = addCommonRequestHandler()
            .openFiltersScreen()
            .openMarkAndModelPicker()

        Step("Проверяем, что есть плейсхолдер, когда ничего не найдено") {
            screen
                .typeOnMarkModelPicker(text: "WWW111XXX")
                .markModelPickerCheckHasNothingFoundTitle()
        }

        Step("В таком случае должна быть возможность сбросить запрос") {
            screen.markModelPickerResetSearchIfNothingFound()
        }

        Step("После сброса, поле поиска должно быть пустым") {
            screen.markModelPickerCheckHasEmptySearchQuery()
        }
    }

    // MARK: - All filters

    func test_allStatusFilter() {
        addCampaignsHandler()
        launch()
        let screen = mainSteps.openDealerCabinetTab()
            .waitForLoading()
            .tapOnFiltersButton()

        Step("Проверяем фильтрацию по `Статусу`") {
            screen.shouldSeeStatusPickerContent()
        }

        let categories = [
            (slug: "all", name: "Все"), (slug: "cars", name: "Легковые"),
            (slug: "trucks", name: "Комтранс"), (slug: "moto", name: "Мото")
        ]

        for (idx, category) in categories.enumerated() {
            Step("Фильтруем по статусу `Все` в категории \(category.name)") {
                let statusAllExp = expectation(description: "Должны отфильтровать по `Все`")
                statusAllExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/\(category.slug)/mark-models?") { (_, _) -> Response? in
                    statusAllExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
                }

                screen.selectStatusPickerOption(at: 0)
                wait(for: [statusAllExp], timeout: 1)
            }

            Step("Фильтруем по статусу `Активные` в категории \(category.name)") {
                let statusActiveExp = expectation(description: "Должны отфильтровать по `Активные`")
                statusActiveExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/\(category.slug)/mark-models?status=ACTIVE") { (_, _) -> Response? in
                    statusActiveExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
                }

                screen.selectStatusPickerOption(at: 1)
                wait(for: [statusActiveExp], timeout: 1)
            }

            Step("Фильтруем по статусу `Ждут активации` в категории \(category.name)") {
                let statusPendingExp = expectation(description: "Должны отфильтровать по `Ждут активации`")
                statusPendingExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/\(category.slug)/mark-models?status=NEED_ACTIVATION") { (_, _) -> Response? in
                    statusPendingExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
                }

                screen.selectStatusPickerOption(at: 2)
                wait(for: [statusPendingExp], timeout: 1)
            }

            Step("Фильтруем по статусу `Неактивные` в категории \(category.name)") {
                let statusInactiveExp = expectation(description: "Должны отфильтровать по `Неактивные`")
                statusInactiveExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/\(category.slug)/mark-models?status=INACTIVE") { (_, _) -> Response? in
                    statusInactiveExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
                }

                screen.selectStatusPickerOption(at: 3)
                wait(for: [statusInactiveExp], timeout: 1)
            }

            Step("Фильтруем по статусу `Заблокированные` в категории \(category.name)") {
                let statusBannedExp = expectation(description: "Должны отфильтровать по `Заблокированные`")
                statusBannedExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/\(category.slug)/mark-models?status=BANNED") { (_, _) -> Response? in
                    statusBannedExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
                }

                screen.selectStatusPickerOption(at: 4)
                wait(for: [statusBannedExp], timeout: 1)
            }

            if idx < categories.endIndex - 1 {
                Step("Выбираем следующую категорию: `\(categories[idx + 1].name)`") {
                    screen.onDealerFiltersScreen().categorySegment.gentleSwipe(.right)
                }
            }
        }
    }

    func test_allCategoryPricePicker() {
        // Нет смысла проверять для каждой категории, тк `test_allCategoryHasAllStatusOptions` проверит у себя,
        // Что фильтры применяются в отдельности для каждой категории.

        addCommonRequestHandler()

        launch()
        let screen = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .tapOnFiltersButton()

        Step("Проверяем фильтр по нижней границе цены") {
            let lowerBoundOnlyExp = expectation(description: "Должны отфильтровать по `Цена от`")
            lowerBoundOnlyExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?price_from=3333&status=ACTIVE") { (_, _) -> Response? in
                lowerBoundOnlyExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.enterPriceLowerBound(3333)
                .onPricePickerScreen()
                .doneButton.tap()

            wait(for: [lowerBoundOnlyExp], timeout: 1)
        }

        Step("Сбрасываем нижнюю границу") {
            screen.resetPriceLowerBound()
                .onPricePickerScreen()
                .doneButton.tap()
        }

        Step("Провярем фильтр по верхней границе цены") {
            let upperBoundOnlyExp = expectation(description: "Должны отфильтровать по `Цена до`")
            upperBoundOnlyExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?price_to=4444&status=ACTIVE") { (_, _) -> Response? in
                upperBoundOnlyExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.enterPriceUpperBound(4444)
                .onPricePickerScreen()
                .doneButton.tap()

            wait(for: [upperBoundOnlyExp], timeout: 1)
        }

        Step("Вводим другое значение `Цены от`, проверяем фильтр по двум границам") {
            let bothBoundsExp = expectation(description: "Должны отфильтровать по `Цена от` и `Цена до`")
            bothBoundsExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?price_from=1111&price_to=4444&status=ACTIVE") { (_, _) -> Response? in
                bothBoundsExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.enterPriceLowerBound(1111)
                .onPricePickerScreen()
                .doneButton.tap()

            wait(for: [bothBoundsExp], timeout: 1)
        }
    }

    func test_allCategoryVAS() {
        addCommonRequestHandler()
        launch()
        let screen = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .tapOnFiltersButton()

        Step("Проверяем фильтрацию по VAS `Премиум`") {
            let premiumExp = expectation(description: "Должны отфильтровать по `Премиум`")
            premiumExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?service=all_sale_premium&status=ACTIVE") { (_, _) -> Response? in
                premiumExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 0)
            wait(for: [premiumExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `Поднятые в поиске`") {
            let freshExp = expectation(description: "Должны отфильтровать по `Поднятые в поиске`")
            freshExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?service=all_sale_fresh&status=ACTIVE") { (_, _) -> Response? in
                freshExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 1)
            wait(for: [freshExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `С автоподнятием`") {
            let autoApplyExp = expectation(description: "Должны отфильтровать по `С автоподнятием`")
            autoApplyExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE&tag=service_auto_apply") { (_, _) -> Response? in
                autoApplyExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 2)
            wait(for: [autoApplyExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `Турбо продажа`") {
            let turboExp = expectation(description: "Должны отфильтровать по `Турбо продажа`")
            turboExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?service=package_turbo&status=ACTIVE") { (_, _) -> Response? in
                turboExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 3)
            wait(for: [turboExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `Спецпредложение`") {
            let specialExp = expectation(description: "Должны отфильтровать по `Спецпредложение`")
            specialExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?service=all_sale_special&status=ACTIVE") { (_, _) -> Response? in
                specialExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 4)
            wait(for: [specialExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `Со стикерами`") {
            let stickersExp = expectation(description: "Должны отфильтровать по `Со стикерами`")
            stickersExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?service=all_sale_badge&status=ACTIVE") { (_, _) -> Response? in
                stickersExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 5)
            wait(for: [stickersExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VAS `Со стикерами`") {
            let alwaysFirstPageExp = expectation(description: "Должны отфильтровать по `С автостратегиями`")
            alwaysFirstPageExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE&tag=autostrategy_always_at_first_page") { (_, _) -> Response? in
                alwaysFirstPageExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVASPickerOption(at: 6)
            wait(for: [alwaysFirstPageExp], timeout: 1)
        }
    }

    func test_allCategoryVIN() {
        addCommonRequestHandler()
        launch()
        let screen = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .tapOnFiltersButton()

        Step("Проверяем фильтрацию по VIN `Проверено`") {
            let okExp = expectation(description: "Должны отфильтровать `Проверено`")
            okExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE&tag=vin_resolution_ok") { (_, _) -> Response? in
                okExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVINPickerOption(at: 0)
            wait(for: [okExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VIN `Серые отчёты`") {
            let unkownUntrustedExp = expectation(description: "Должны отфильтровать `Серые отчёты`")
            unkownUntrustedExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE&tag=vin_resolution_untrusted&tag=vin_resolution_unknown") { (_, _) -> Response? in
                unkownUntrustedExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVINPickerOption(at: 1)
            wait(for: [unkownUntrustedExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VIN `Красные отчёты`") {
            let errorInvalidExp = expectation(description: "Должны отфильтровать `Красные отчёты`")
            errorInvalidExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?status=ACTIVE&tag=vin_resolution_error&tag=vin_resolution_invalid") { (_, _) -> Response? in
                errorInvalidExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVINPickerOption(at: 2)
            wait(for: [errorInvalidExp], timeout: 1)
        }

        Step("Проверяем фильтрацию по VIN `Без отчёта`") {
            let noInfoExp = expectation(description: "Должны отфильтровать по `Без отчёта`")
            noInfoExp.assertForOverFulfill = false

            server.addHandler("GET /user/offers/all/mark-models?exclude_tag=vin_resolution_ok&exclude_tag=vin_resolution_untrusted&exclude_tag=vin_resolution_unknown&exclude_tag=vin_resolution_error&exclude_tag=vin_resolution_invalid&status=ACTIVE") { (_, _) -> Response? in
                noInfoExp.fulfill()
                return Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
            }

            screen.selectVINPickerOption(at: 3)
            wait(for: [noInfoExp], timeout: 1)
        }
    }

    // MARK: - Moto & Trucks

    func test_commercialFilters() {
        let steps = addCommonRequestHandler()
            .openFiltersScreen()

        Step("Должны отфильтровать `Комтранс` по новым `Автопогрузчикам`") {
            steps.swipeCategorySegmentTo(index: 2)

            Step("Выбираем `Автопогрузчики`") {
                let activeAutoloaderExp = expectation(description: "Должны отфильтровать `Автопогрузчики`")
                activeAutoloaderExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/trucks/mark-models?status=ACTIVE&truck_category=autoloader") { (_, _) -> Response? in
                    activeAutoloaderExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_empty", userAuthorized: true)
                }

                steps.selectSubcategory(named: "Автопогрузчики")
                wait(for: [activeAutoloaderExp], timeout: 1)
            }

            Step("Выбираем раздел новые") {
                let activeNewAutoloaderExp = expectation(description: "Должны отфильтровать `новые`")
                activeNewAutoloaderExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/trucks/mark-models?section=new&status=ACTIVE&truck_category=autoloader") { (_, _) -> Response? in
                    activeNewAutoloaderExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_empty", userAuthorized: true)
                }

                steps.tapOnConditionSegment(category: "trucks", name: "Комтранс", at: 1)
                wait(for: [activeNewAutoloaderExp], timeout: 1)
            }
        }
    }

    func test_motoFilters() {
        let steps = addCommonRequestHandler()
            .openFiltersScreen()

        Step("Должны отфильтровать `Мото` по б/у `Мотовездеходам`") {
            steps.swipeCategorySegmentTo(index: 3)

            Step("Выбираем `Мотовездеходы`") {
                let activeAtvExp = expectation(description: "Должны отфильтровать `Мотовездеходы`")
                activeAtvExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/moto/mark-models?moto_category=atv&status=ACTIVE") { (_, _) -> Response? in
                    activeAtvExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_empty", userAuthorized: true)
                }

                steps.selectSubcategory(named: "Мотовездеходы")
                wait(for: [activeAtvExp], timeout: 1)
            }

            Step("Выбираем раздел б/у") {
                let activeUsedAtvExp = expectation(description: "Должны отфильтровать `б/у`")
                activeUsedAtvExp.assertForOverFulfill = false

                server.addHandler("GET /user/offers/moto/mark-models?moto_category=atv&section=used&status=ACTIVE") { (_, _) -> Response? in
                    activeUsedAtvExp.fulfill()
                    return Response.okResponse(fileName: "dealer_mark-models_empty", userAuthorized: true)
                }

                steps.tapOnConditionSegment(category: "moto", name: "Мото", at: 2)
                wait(for: [activeUsedAtvExp], timeout: 1)
            }
        }
    }

    // MARK: - Private

    @discardableResult
    private func openFiltersScreen() -> DealerFiltersSteps {
        launch()
        return mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .tapOnFiltersButton()
            .waitForLoading()
    }

    @discardableResult
    private func addCommonRequestHandler() -> Self {
        return addCampaignsHandler()
            .addMarkAndModelsHandler()
    }

    @discardableResult
    private func addCampaignsHandler() -> Self {
        let campaignsPath = "/dealer/campaigns"
        server.addHandler("GET \(campaignsPath)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_all_campaigns", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addMarkAndModelsHandler() -> Self {
        let markAndModelsPath = "/user/offers/all/mark-models?status=ACTIVE"
        server.addHandler("GET \(markAndModelsPath)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_mark-models_all", userAuthorized: true)
        }

        return self
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }
    }
}
