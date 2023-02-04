import AutoRuProtoModels
import XCTest
import Snapshots

/// @depends_on AutoRuDealerForm
final class DealerFormTests: DealerBaseTest {
    private let suiteName = SnapshotIdentifier.suiteName(from: #file)
    // MARK: - VIN Picker

    func test_trucksHasNoVINPicker() {
        addCampaignsHandler()
            .addTrucksDraftRequestHandler()
            .addNewTrucksEmptyDraftHandler()
            .addUsedTrucksEmptyDraftHandler()
            .addTrucksEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Комтранс` при добавлении не должно быть шага с VIN пикером") {}

        Step("Добавляем `Комтранс новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Комтранс новые")

            Step("Проверяем, что открылась сама форма размещения, а не VIN пикер") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }

        Step("Закрываем форму") {
            steps.as(DealerFormSteps.self).close()
        }

        Step("Проверяем для `Комтранс с пробегом") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Комтранс с пробегом")

            Step("Проверяем, что открылась сама форма размещения, а не VIN пикер") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }
    }

    func test_motoHasNoVINPicker() {
        addCampaignsHandler()
            .addMotoDraftRequestHandler()
            .addNewMotoEmptyDraftHandler()
            .addUsedMotoEmptyDraftHandler()
            .addMotoEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Мото` при добавлении не должно быть шага с VIN пикером") {}

        Step("Добавляем `Мото новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Мото новые")

            Step("Проверяем, что открылась сама форма размещения, а не VIN пикер") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }

        Step("Закрываем форму") {
            steps.as(DealerFormSteps.self).close()
        }

        Step("Проверяем для `Мото с пробегом`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Мото с пробегом")

            Step("Проверяем, что открылась сама форма размещения, а не VIN пикер") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }
    }

    func test_emptyDraftHasVINPicker() {
        addCampaignsHandler()
            .addCarsEmptyDraftRequestHandler()
            .addNewCarsEmptyDraftHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Легковые` и пустом черновике, должен быть шаг с VIN пикером") {}

        Step("Добавляем `Легковые новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")

            Step("Проверяем, что открылся VIN пикер") {
                steps
                    .as(DealerVINPickerSteps.self)
                    .wait(for: 1)
                    .shouldSeeCommonContent()
            }

            Step("Пропускаем ввод VIN") {
                steps.as(DealerVINPickerSteps.self).skip()
            }

            Step("Проверяем, что открылась форма размещения") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }
    }

    func test_nonEmptyDraftHasNoVINPicker() {
        addCampaignsHandler()
            .addCarsNonEmptyDraftRequestHandler()
            .addNewCarsNonEmptyDraftHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Легковые` и частично заполненном черновике, сразу должна открыться форма размещения") {}

        Step("Добавляем `Легковые новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")

            Step("Проверяем, что сразу открылась форма размещения") {
                Step("Ищем ячейку с пикером медиа") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .headerView.shouldExist()
                }
            }
        }
    }

    // MARK: - Publish buttons

    func test_publishButton() {
        Step("При успешном размещении, должны послать запрос на активацию") {}

        addCampaignsHandler()
            .addMotoDraftRequestHandler()
            .addNewMotoEmptyDraftHandler()
            .addMotoEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Открываем форму размещения для `Мото новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Мото новые")
                .onDealerFormScreen()
                .headerView.shouldExist()
        }

        let form = steps.as(DealerFormSteps.self)
        form.scrollToPublish()

        let publishButton = form.onDealerFormScreen().publishButton

        Step("Проверяем, что нашли кнопку `Опубликовать`") {
            publishButton.shouldExist()
        }

        Step("Пробуем опубликовать") {
            let publishExpectation = expectation(description: "Должны опубликовать черновик")
            let activateExpectation = expectation(description: "Должны активировать черновик")
            publishExpectation.assertForOverFulfill = false
            activateExpectation.assertForOverFulfill = false

            server.addHandler("POST /user/offers/MOTO/7802053328698736611-6516e533/activate") { (_, _) -> Response? in
                activateExpectation.fulfill()
                let filePath = Bundle.resources.url(forResource: "dealer_form_used_cars_draft_publish", withExtension: "json")
                let body: Data? = filePath.flatMap { try? Data(contentsOf: $0) }
                return Response.responseWithStatus(body: body, protoName: "auto.api.ErrorResponse", userAuthorized: true, status: "HTTP/1.1 402 Payment Required")
            }

            server.addHandler("POST /user/draft/MOTO/7802053328698736611-6516e533/publish") { (_, _) -> Response? in
                publishExpectation.fulfill()
                return Response.okResponse(fileName: "dealer_form_new_moto_empty_draft", userAuthorized: true)
            }

            publishButton.tap()
            wait(for: [publishExpectation, activateExpectation], timeout: 2)
        }
    }

    func test_publishHiddenButton() {
        Step("Если выбираем `Не публиковать сразу`, то не должно быть запроса на активацию") {}

        addCampaignsHandler()
            .addMotoDraftRequestHandler()
            .addMotoEmptyDraftSuggestHandler()

        let publishHiddenInModelExpectation = expectation(description: "Должен быть PUT со скрытой публикацией")
        publishHiddenInModelExpectation.assertForOverFulfill = false
        server.addHandler("PUT /user/draft/moto/7802053328698736611-6516e533") { (req: Request, _) -> Response? in
            if let body = req.messageBody,
               let model = try? Auto_Api_Offer(jsonUTF8Data: body),
               model.additionalInfo.hidden {
                publishHiddenInModelExpectation.fulfill()
            }
            return Response.okResponse(fileName: "dealer_form_new_moto_empty_draft", userAuthorized: true)
        }

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Открываем форму размещения для `Мото новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Мото новые")
                .onDealerFormScreen()
                .headerView.shouldExist()
        }

        let form = steps.as(DealerFormSteps.self)
        form.scrollToPublishHidden()

        let publishButton = form.onDealerFormScreen().publishHiddenButton

        Step("Проверяем, что нашли кнопку `Не публиковать сразу`") {
            publishButton.shouldExist()
        }

        Step("Пробуем продолжить") {
            let publishExpectation = expectation(description: "Должны опубликовать черновик")
            let activateNotCalledExpectation = expectation(description: "Не должны пытаться активировать черновик")

            publishExpectation.assertForOverFulfill = false
            activateNotCalledExpectation.assertForOverFulfill = false
            activateNotCalledExpectation.isInverted = true

            server.addHandler("POST /user/offers/MOTO/7802053328698736611-6516e533/activate") { (_, _) -> Response? in
                activateNotCalledExpectation.fulfill()
                return Response.okResponse(fileName: "dealer_form_new_moto_empty_draft", userAuthorized: true)
            }

            server.addHandler("POST /user/draft/MOTO/7802053328698736611-6516e533/publish") { (_, _) -> Response? in
                publishExpectation.fulfill()
                return Response.okResponse(fileName: "dealer_form_new_moto_empty_draft", userAuthorized: true)
            }

            publishButton.tap()
            wait(for: [publishExpectation, activateNotCalledExpectation, publishHiddenInModelExpectation], timeout: 2)
        }
    }

    // MARK: - Publish

    func test_unfilledPublishScrollsToTop() {
        Step("Если пытаемся опубликовать черновик с ошибками, должны подскроллить к первому полю с ошибкой") {}

        addCampaignsHandler()
            .addCarsNonEmptyDraftRequestHandler()
            .addNewCarsNonEmptyDraftWithErrorsHandler()
            .addNewCarsPublishNonEmptyDraftWithErrorsHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Открываем форму размещения для `Легковые новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")
                .onDealerFormScreen()
                .headerView.shouldExist()
        }

        let form = steps.as(DealerFormSteps.self)
        form.scrollToPublishHidden()

        let publishButton = form.onDealerFormScreen().publishHiddenButton

        Step("Проверяем, что нашли кнопку `Не публиковать сразу`") {
            publishButton.shouldExist()
        }

        Step("Публикуем") {
            publishButton.tap()
        }

        Step("Ищем поле с ошибкой") {
            form.onDealerFormScreen()
                .autoMarkField
                .has(name: "Марка", error: "Марка не указана")
        }
    }

    func test_successPublishOpensCard() {
        addCampaignsHandler()
            .addUserCardsFilledDraftHandler()
            .addUserCardsFilledDraftUpdateHandler()
            .addUserCardsFilledDraftPublishHandler()
            .addUserCardsFilledDraftActivateHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Открываем форму размещения `Легковые с пробегом`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые с пробегом")

            Step("Пропускаем шаг с VIN") {
                steps
                    .as(DealerVINPickerSteps.self)
                    .wait(for: 1)
                    .shouldSeeCommonContent()
                    .skip()
                    .onDealerFormScreen()
                    .headerView.shouldExist()
            }
        }

        let form = steps.as(DealerFormSteps.self)
        form.scrollToPublishHidden()

        let publishButton = form.onDealerFormScreen().publishHiddenButton

        Step("Проверяем, что нашли кнопку `Не публиковать сразу`") {
            publishButton.shouldExist()
        }

        Step("После успешной публикации, должны еще раз запросить оффер, чтобы обновить данные") {
            let requestActiveOfferExp = expectation(description: "Должны были запросить оффер")

            server.addHandler("GET /user/offers/CARS/1098044212-0abb6f7b") { (_, _) -> Response? in
                requestActiveOfferExp.fulfill()
                return Response.okResponse(fileName: "dealer_form_active_offer", userAuthorized: true)
            }

            publishButton.tap()
            wait(for: [requestActiveOfferExp], timeout: 1)
        }

        Step("Проверяем, что мы на карточке и цена изменилась") {
            steps
                .as(DealerSaleCardSteps.self)
                .checkPrice(value: "4 377 000 ₽")
        }
    }

    // MARK: - Form

    func test_formNewCars() {
        addCampaignsHandler()
            .addFormNewCarsDraftHandler()
            .addFormNewCarsOfferHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Открываем форму размещения `Легковые с пробегом`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")
                .onDealerFormScreen()
                .headerView.shouldExist()
        }

        let form = steps.as(DealerFormSteps.self)
            .onDealerFormScreen()

        form.autoMarkField.equals(title: "Марка", value: "BMW")
        form.autoModelField.equals(title: "Модель", value: "3 серия")
        form.autoYearField.equals(title: "Год выпуска", value: "2018")
        form.autoGenerationField.equals(title: "Поколение", value: "с 2018 по 2020, VII (G2x)")
        form.autoBodyTypeField.equals(title: "Тип кузова", value: "Седан")
        form.autoEngineTypeField.equals(title: "Двигатель", value: "Дизель")
        form.autoGearTypeField.equals(title: "Привод", value: "Полный")
        form.autoTransmissionTypeField.equals(title: "Коробка передач", value: "Автоматическая")

        form.scrollableElement.gentleSwipe(.up)
        form.autoModificationField.equals(title: "Модификация", value: "320d xDrive 2.0 AT Дизель 190 л.с.")
        form.autoColorField.equals(title: "Цвет", value: "Серебристый")
        form.autoComplectationField.equals(title: "Комплектация", value: "320d xDrive M Sport Pure")
        form.autoAvailabilityField.equals(title: "Наличие", value: "На заказ")
        form.autoMileageField.notExists(name: "Пробег")

        form.scrollableElement.gentleSwipe(.up)
        form.notRegisteredInRuField.switched(name: "Авто не на учёте в РФ", on: true)
        form.licensePlateField.notExists(name: "Пикер гос. номера")
        form.vinField.hasTextField(name: "VIN или номер кузова", placeholder: "VIN или номер кузова")
        form.stsField.notExists(name: "Свидетельство о регистрации (СТС)")
        form.ptsField.notExists(name: "ПТС (оригинал/дубликат)")
        form.ownersNumberField.notExists(name: "Владельцев по ПТС")
        form.purchaseDateField.notExists(name: "Дата приобретения")
        form.warrantyValidField.notExists(name: "На гарантии")
        form.warrantyEndDateField.notExists(name: "Дата окончания гарантии")
        form.aruOnlyField.switched(name: "Только на Авто.ру", on: true)
        form.offerTextField.equals(title: "Описание", value: "Описание нового авто")

        form.scrollableElement.gentleSwipe(.up)
        form.modelOptionsField.equals(title: "Указать комплектацию", value: "58")
        form.isRightWheelField.switched(name: "Правый руль", on: false)
        form.customsNotClearedField.switched(name: "Не растаможен", on: true)
        form.isAutogasField.switched(name: "Газобаллонное оборудование", on: false)
        form.isBeatenField.notExists(name: "Битый или не находу")
        form.damagePickerField.notExists(name: "Пикера повреждений")

        form.scrollableElement.gentleSwipe(.up)
        form.priceCurrencyField.notExists(name: "Валюта цены")

        form.priceField.hasTextField(title: "Цена, ₽", value: "1 882 524")
        form.withNDSField.exists(name: "Цена с НДС")
        form.creditDiscountField.hasTextField(title: "Скидка по кредиту, ₽", value: "326 189")
        form.insuranceDiscountField.hasTextField(title: "Скидка по КАСКО, ₽", value: "21 530")
        form.tradeInDiscountField.hasTextField(title: "Скидка по трейд-ин, ₽", value: "750 290")
        form.scrollableElement.gentleSwipe(.up)
        form.maxDiscountField.hasTextField(title: "Максимальная скидка, ₽", value: "1 037 426")
        form.canExchangeField.notExists(name: "Возможен обмен")
    }

    func test_formUsedCars() {
        addCampaignsHandler()
            .addFormUsedCarsDraftHandler()
            .addFormUsedCarsOfferHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Легковые с пробегом")
            .wait(for: 1)
            .onDealerFormScreen()

        form.autoMarkField.equals(title: "Марка", value: "BMW")
        form.autoModelField.equals(title: "Модель", value: "3 серия")
        form.autoYearField.equals(title: "Год выпуска", value: "2018")
        form.autoGenerationField.equals(title: "Поколение", value: "с 2018 по 2020, VII (G2x)")
        form.autoBodyTypeField.equals(title: "Тип кузова", value: "Седан")
        form.autoEngineTypeField.equals(title: "Двигатель", value: "Бензин")
        form.autoGearTypeField.equals(title: "Привод", value: "Задний")
        form.autoTransmissionTypeField.equals(title: "Коробка передач", value: "Автоматическая")

        form.scrollableElement.gentleSwipe(.up)
        form.autoModificationField.equals(title: "Модификация", value: "330i 2.0 AT Бензин 258 л.с.")
        form.autoColorField.equals(title: "Цвет", value: "Серый")
        form.autoComplectationField.equals(title: "Комплектация", value: "330i Luxury Line")
        form.autoAvailabilityField.equals(title: "Наличие", value: "На заказ")
        form.autoMileageField.hasTextField(title: "Пробег, км", value: "666")

        form.scrollableElement.gentleSwipe(.up)
        form.notRegisteredInRuField.switched(name: "Авто не на учёте в РФ", on: false)

        steps
            .as(DealerFormSteps.self)
            .checkLicensePlate(plate: "Е123КХ", region: "77")

        form.vinField.hasTextField(name: "VIN или номер кузова", placeholder: "VIN или номер кузова")
        form.stsField.hasTextField(name: "Свидетельство о регистрации (СТС)", placeholder: "Свидетельство о регистрации (СТС)")

        form.scrollableElement.gentleSwipe(.up)
        form.ptsField.equals(title: "ПТС (оригинал/дубликат)", value: "Оригинал")
        form.ownersNumberField.equals(title: "Владельцев по ПТС", value: "2")
        form.purchaseDateField.equals(title: "Дата приобретения", value: "май 2018")
        form.warrantyValidField.switched(name: "На гарантии", on: true)
        form.warrantyEndDateField.equals(title: "Дата окончания гарантии", value: "май 2022")
        form.aruOnlyField.switched(name: "Только на Авто.ру", on: true)

        form.scrollableElement.gentleSwipe(.up)
        form.offerTextField.equals(title: "Описание", value: "Описание авто с пробегом")
        form.scrollableElement.gentleSwipe(.up)
        form.modelOptionsField.equals(title: "Указать комплектацию", value: "50")
        form.isRightWheelField.switched(name: "Правый руль", on: false)
        form.customsNotClearedField.switched(name: "Не растаможен", on: false)
        form.isAutogasField.switched(name: "Газобаллонное оборудование", on: false)
        form.isBeatenField.switched(name: "Битый или не находу", on: false)

        form.scrollableElement.gentleSwipe(.up)
        form.damagePickerField.exists(name: "Пикера повреждений")

        form.scrollableElement.gentleSwipe(.up)
        form.priceCurrencyField.notExists(name: "Валюта")
        form.priceField.hasTextField(title: "Цена, ₽", value: "4 220 171")
        form.withNDSField.exists(name: "Цена с НДС")
        form.creditDiscountField.exists(name: "Скидка по кредиту")
        form.insuranceDiscountField.exists(name: "Скидка по КАСКО")
        form.scrollableElement.gentleSwipe(.up)
        form.tradeInDiscountField.exists(name: "Скидка по трейд-ин")
        form.maxDiscountField.exists(name: "Максимальная скидка")

        form.canExchangeField.switched(name: "Возможен обмен", on: true)
    }

    func test_formNewCarsFromUsedDraft() {
        addCampaignsHandler()
            .addFormUsedCarsDraftHandler()
            .addFormUsedCarsOfferHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Легковые новые")
            .wait(for: 1)
            .onDealerFormScreen()

        form.autoMarkField.equals(title: "Марка", value: "BMW")
        form.autoModelField.equals(title: "Модель", value: "3 серия")
        form.autoYearField.equals(title: "Год выпуска", value: "2018")
        form.autoGenerationField.equals(title: "Поколение", value: "с 2018 по 2020, VII (G2x)")
        form.autoBodyTypeField.equals(title: "Тип кузова", value: "Седан")
        form.autoEngineTypeField.equals(title: "Двигатель", value: "Бензин")
        form.autoGearTypeField.equals(title: "Привод", value: "Задний")
        form.autoTransmissionTypeField.equals(title: "Коробка передач", value: "Автоматическая")

        form.scrollableElement.gentleSwipe(.up)
        form.autoModificationField.equals(title: "Модификация", value: "330i 2.0 AT Бензин 258 л.с.")
        form.autoColorField.equals(title: "Цвет", value: "Серый")
        form.autoComplectationField.equals(title: "Комплектация", value: "330i Luxury Line")
        form.autoAvailabilityField.equals(title: "Наличие", value: "На заказ")
        form.autoMileageField.shouldNotExist()

        form.scrollableElement.gentleSwipe(.up)
        form.notRegisteredInRuField.switched(name: "Авто не на учёте в РФ", on: false)
        form.licensePlateField.shouldNotExist()
        form.vinField.hasTextField(name: "VIN или номер кузова", placeholder: "VIN или номер кузова")
        form.stsField.hasTextField(name: "Свидетельство о регистрации (СТС)", placeholder: "Свидетельство о регистрации (СТС)")
        form.ptsField.notExists(name: "ПТС (оригинал/дубликат)")
        form.ownersNumberField.notExists(name: "Владельцев по ПТС")
        form.purchaseDateField.notExists(name: "Дата приобретения")
        form.warrantyValidField.notExists(name: "На гарантии")
        form.warrantyEndDateField.notExists(name: "Дата окончания гарантии")
        form.aruOnlyField.switched(name: "Только на Авто.ру", on: true)
        form.offerTextField.equals(title: "Описание", value: "Описание авто с пробегом")

        form.scrollableElement.gentleSwipe(.up)
        form.modelOptionsField.equals(title: "Указать комплектацию", value: "50")
        form.isRightWheelField.switched(name: "Правый руль", on: false)
        form.customsNotClearedField.switched(name: "Не растаможен", on: false)
        form.isAutogasField.switched(name: "Газобаллонное оборудование", on: false)
        form.isBeatenField.notExists(name: "Битый или не находу")
        form.damagePickerField.notExists(name: "Пикера повреждений")

        form.scrollableElement.gentleSwipe(.up)
        form.priceCurrencyField.notExists(name: "Валюта")

        form.priceField.hasTextField(title: "Цена, ₽", value: "4 220 171")
        form.withNDSField.exists(name: "Цена с НДС")
        form.creditDiscountField.hasTextField(name: "Скидка по кредиту", placeholder: "Скидка по кредиту, ₽")
        form.insuranceDiscountField.hasTextField(name: "Скидка по КАСКО", placeholder: "Скидка по КАСКО, ₽")

        form.scrollableElement.gentleSwipe(.up)
        form.tradeInDiscountField.hasTextField(name: "Скидка по трейд-ин", placeholder: "Скидка по трейд-ин, ₽")
        form.maxDiscountField.hasTextField(name: "Максимальная скидка", placeholder: "Максимальная скидка, ₽")
        form.canExchangeField.notExists(name: "Возможен обмен")
    }

    func test_formUsedCarsFromNewDraft() {
        addCampaignsHandler()
            .addFormNewCarsDraftHandler()
            .addFormNewCarsOfferHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Легковые с пробегом")
            .wait(for: 1)
            .onDealerFormScreen()

        form.autoMarkField.equals(title: "Марка", value: "BMW")
        form.autoModelField.equals(title: "Модель", value: "3 серия")
        form.autoYearField.equals(title: "Год выпуска", value: "2018")
        form.autoGenerationField.equals(title: "Поколение", value: "с 2018 по 2020, VII (G2x)")
        form.autoBodyTypeField.equals(title: "Тип кузова", value: "Седан")
        form.autoEngineTypeField.equals(title: "Двигатель", value: "Дизель")
        form.autoGearTypeField.equals(title: "Привод", value: "Полный")
        form.autoTransmissionTypeField.equals(title: "Коробка передач", value: "Автоматическая")

        form.scrollableElement.gentleSwipe(.up)
        form.autoModificationField.equals(title: "Модификация", value: "320d xDrive 2.0 AT Дизель 190 л.с.")
        form.autoColorField.equals(title: "Цвет", value: "Серебристый")
        form.autoComplectationField.equals(title: "Комплектация", value: "320d xDrive M Sport Pure")
        form.autoAvailabilityField.equals(title: "Наличие", value: "На заказ")
        form.autoMileageField.hasTextField(name: "Пробег", placeholder: "Пробег, км")

        form.scrollableElement.gentleSwipe(.up)
        form.notRegisteredInRuField.switched(name: "Авто не на учёте в РФ", on: true)
        form.licensePlateField.notExists(name: "Пикер гос. номера")
        form.vinField.hasTextField(name: "VIN или номер кузова", placeholder: "VIN или номер кузова")
        form.stsField.notExists(name: "Свидетельство о регистрации (СТС)")
        form.ptsField.hasPicker(name: "ПТС (оригинал/дубликат)", placeholder: "ПТС (оригинал/дубликат)")
        form.ownersNumberField.hasPicker(name: "Владельцев по ПТС", placeholder: "Владельцев по ПТС")

        form.scrollableElement.gentleSwipe(.up)
        form.purchaseDateField.hasPicker(name: "Дата приобретения", placeholder: "Дата приобретения")
        form.warrantyValidField.switched(name: "На гарантии", on: false)
        form.warrantyEndDateField.notExists(name: "Дата окончания гарантии")
        form.aruOnlyField.switched(name: "Только на Авто.ру", on: true)
        form.offerTextField.equals(title: "Описание", value: "Описание нового авто")

        form.scrollableElement.gentleSwipe(.up)
        form.modelOptionsField.equals(title: "Указать комплектацию", value: "58")
        form.isRightWheelField.switched(name: "Правый руль", on: false)
        form.customsNotClearedField.switched(name: "Не растаможен", on: true)
        form.isAutogasField.switched(name: "Газобаллонное оборудование", on: false)
        form.isBeatenField.switched(name: "Битый или не на ходу", on: false)

        form.scrollableElement.gentleSwipe(.up)
        form.damagePickerField.exists(name: "Пикер повреждений")

        form.scrollableElement.gentleSwipe(.up)
        form.priceCurrencyField.shouldNotExist()
        form.priceField.hasTextField(title: "Цена, ₽", value: "1 882 524")
        form.withNDSField.exists(name: "Цена с НДС")
        form.creditDiscountField.exists(name: "Скидка по кредиту")
        form.insuranceDiscountField.exists(name: "Скидка по КАСКО")

        form.scrollableElement.gentleSwipe(.up)
        form.tradeInDiscountField.exists(name: "Скидка по трейд-ин")
        form.maxDiscountField.exists(name: "Максимальная скидка")
        form.canExchangeField.switched(name: "Возможен обмен", on: false)
    }

    func test_newTrucksDiscountFields() {
        addCampaignsHandler()
            .addTrucksDraftRequestHandler()
            .addNewTrucksEmptyDraftHandler()
            .addTrucksEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Комтранс новые")
            .wait(for: 1)
            .onDealerFormScreen()

        Step("Для категории `Комтранс` должны быть поля указания скидки") {}

        Step("Ищем поля скидки") {
            form.scrollTo(
                element: form.maxDiscountField,
                maxSwipes: 5
            )
            form.creditDiscountField.hasTextField(name: "Скидка по кредиту", placeholder: "Скидка по кредиту, ₽")
            form.insuranceDiscountField.hasTextField(name: "Скидка по КАСКО", placeholder: "Скидка по КАСКО, ₽")
            form.tradeInDiscountField.hasTextField(name: "Скидка по трейд-ин", placeholder: "Скидка по трейд-ин, ₽")
            form.leasingDiscountField.hasTextField(name: "Скидка по лизингу", placeholder: "Скидка по лизингу, ₽")
            form.maxDiscountField.hasTextField(name: "Максимальная скидка", placeholder: "Максимальная скидка, ₽")
        }
    }

    func test_newTrucksHasNDSSwitch() {
        addCampaignsHandler()
            .addTrucksDraftRequestHandler()
            .addNewTrucksEmptyDraftHandler()
            .addTrucksEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Комтранс новые")
            .wait(for: 1)
            .onDealerFormScreen()

        Step("Для категории `Комтранс` должен быть свитч НДС") {}

        Step("Ищем свитч") {
            form.scrollTo(
                element: form.withNDSField,
                maxSwipes: 3
            )
            form.withNDSField.switched(name: "Цена с НДС", on: false)
        }
    }

    func test_usedTrucksHasNDSSwitch() {
        addCampaignsHandler()
            .addTrucksDraftRequestHandler()
            .addUsedTrucksEmptyDraftHandler()
            .addTrucksEmptyDraftSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Комтранс с пробегом")
            .wait(for: 1)
            .onDealerFormScreen()

        Step("Для категории `Комтранс` должен быть свитч НДС") {}

        Step("Ищем свитч") {
            form.scrollTo(
                element: form.withNDSField,
                maxSwipes: 3
            )
            form.withNDSField.switched(name: "Цена с НДС", on: false)
        }
    }

    func test_formHasNoComplectationFieldWithEmptySuggest() {
        addCampaignsHandler()
            .addFormNewCarsEmptyComplectationDraftHandler()
            .addFormNewCarsEmptyComplectationOfferHandler()
            .addFormNewCarsEmptyComplectationSuggestHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        let form = steps
            .tapOnAddButton()
            .waitForLoading()
            .tapOption(named: "Легковые новые")
            .wait(for: 1)
            .onDealerFormScreen()

        Step("Скроллим к полю `Цвет`") {
            form.scrollTo(element: form.autoColorField)
        }

        Step("Проверяем, что нет поля комплектации") {
            form.autoAvailabilityField.exists(name: "Наличие")
            form.autoComplectationField.notExists(name: "Комплектация")
        }
    }

    // MARK: - Panoramas
    func test_PanoramasEmptyCommonScreen() {
        addCampaignsHandler()
            .addCarsNonEmptyDraftRequestHandler()
            .addNewCarsNonEmptyDraftHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Легковые` и частично заполненном черновике, сразу должна открыться форма размещения") {}

        Step("Добавляем `Легковые новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")

            Step("Проверяем, что сразу открылась форма размещения") {
                Step("Ищем ячейку с экраном панорам") {
                    steps
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .emptyPanoramaView.tap()
                }
            }
        }

        steps
            .as(DealerInteriorExteriorPanoramaSteps.self)
            .checkHelpBothPanoramas()
            .checkAddExteriorPanorama()
            .checkAddInteriorPanorama()
    }

    func test_PanoramasInteriorExteriorCommonScreen() {
        addCampaignsHandler()
            .addCarsNonEmptyDraftRequestHandler()
            .addNewCarsPanoramaDraftHandler()

        launch()
        let steps = mainSteps
            .openDealerCabinetTab()
            .waitForLoading()

        Step("Для категории `Легковые` и частично заполненном черновике, сразу должна открыться форма размещения") {}

        Step("Добавляем `Легковые новые`") {
            steps
                .tapOnAddButton()
                .waitForLoading()
                .tapOption(named: "Легковые новые")

            Step("Проверяем, что сразу открылась форма размещения") {
                Step("Ищем ячейку с экраном панорам") {
                    steps
                        .wait(for: 2)
                        .as(DealerFormSteps.self)
                        .onDealerFormScreen()
                        .bothPanoramaView.tap()
                }
            }
        }

        steps
            .as(DealerInteriorExteriorPanoramaSteps.self)
            .checkExteriorPanoramaMenu()
            .checkInteriorPanoramaMenu()
            .handleSystemAlertIfNeeded(allowButtons: ["Allow Access to All Photos"])
            .closeInteriorPanoramaMenu()

        steps.wait(for: 1)
    }

    // MARK: - Private

    @discardableResult
    private func openForm() -> DealerNewListingCategoryPickerSteps {
        return mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
            .tapOnAddButton()
            .waitForLoading()
    }

    @discardableResult
    private func addCampaignsHandler() -> Self {
        server.addHandler("GET /dealer/campaigns") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_all_campaigns", userAuthorized: true)
        }

        return self
    }

    // MARK: - Cars

    @discardableResult
    private func addCarsEmptyDraftRequestHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_draft_req_response", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addCarsNonEmptyDraftRequestHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_non_default_draft_req_response", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsNonEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_non_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsPanoramaDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_panoramas_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsNonEmptyDraftWithErrorsHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_non_empty_draft_with_errors", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsPublishNonEmptyDraftWithErrorsHandler() -> Self {
        server.addHandler("POST /user/draft/CARS/3242459595742638998-1992143a/publish") { (_, _) -> Response? in
            Response.responseWith(
                errorModel: "auto.api.OfferValidationErrorsResponse",
                code: "422 Unprocessable Entity",
                fileName: "dealer_form_new_cars_publish_errors",
                userAuthorized: true
            )
        }

        return self
    }

    @discardableResult
    private func addUsedCarsEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_cars_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewCarsInStockEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/3242459595742638998-1992143a") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_in_stock_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addCarsEmptyDraftSuggestHandler() -> Self {
        server.addHandler("GET /reference/catalog/CARS/suggest *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_empty_draft_suggest", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUserCardsFilledDraftHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_cars_filled_draft_req", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUserCardsFilledDraftUpdateHandler() -> Self {
        server.addHandler("PUT /user/draft/CARS/8221591163132341463-0928c044") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_cars_filled_offer_resp", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUserCardsFilledDraftPublishHandler() -> Self {
        server.addHandler("POST /user/draft/CARS/8221591163132341463-0928c044/publish") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_cars_filled_offer_publ", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUserCardsFilledDraftActivateHandler() -> Self {
        server.addHandler("POST /user/offers/CARS/1098044212-0abb6f7b/activate") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_activate_ok", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUserCardsFilledDraftActiveOfferHandler() -> Self {
        server.addHandler("GET /user/offers/CARS/1098044212-0abb6f7b") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_active_offer", userAuthorized: true)
        }

        return self
    }

    // MARK: - Form

    @discardableResult
    private func addFormNewCarsDraftHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_filled_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addFormNewCarsOfferHandler() -> Self {
        server.addHandler("GET /user/offers/CARS/2847309743481178659-bf9756db") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_filled_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addFormUsedCarsDraftHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_filled_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addFormUsedCarsOfferHandler() -> Self {
        server.addHandler("GET /user/offers/CARS/2847309743481178659-bf9756db") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_filled_draft", userAuthorized: true)
        }

        return self
    }

    // MARK: Complectation

    @discardableResult
    private func addFormNewCarsEmptyComplectationDraftHandler() -> Self {
        server.addHandler("GET /user/draft/CARS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_filled_no_complectation_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addFormNewCarsEmptyComplectationOfferHandler() -> Self {
        server.addHandler("GET /user/offers/CARS/4625756787987535285-fd52eae0") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_filled_no_complectation_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addFormNewCarsEmptyComplectationSuggestHandler() -> Self {
        server.addHandler("GET /reference/catalog/CARS/suggest *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_cars_no_complectation_suggest", userAuthorized: true)
        }

        return self
    }

    // MARK: - Moto

    @discardableResult
    private func addMotoDraftRequestHandler() -> Self {
        server.addHandler("GET /user/draft/MOTO") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_moto_draft_req_response", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewMotoEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/moto/7802053328698736611-6516e533") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_moto_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUsedMotoEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/MOTO/7802053328698736611-6516e533") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_moto_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addMotoEmptyDraftSuggestHandler() -> Self {
        server.addHandler("GET /reference/catalog/MOTO/suggest") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_moto_empty_draft_suggest", userAuthorized: true)
        }

        return self
    }

    // MARK: - Trucks

    @discardableResult
    private func addTrucksDraftRequestHandler() -> Self {
        server.addHandler("GET /user/draft/TRUCKS") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_trucks_draft_req_response", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addNewTrucksEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/TRUCKS/5754971087428499094-838c4fe5") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_new_trucks_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addUsedTrucksEmptyDraftHandler() -> Self {
        server.addHandler("PUT /user/draft/TRUCKS/5754971087428499094-838c4fe5") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_used_trucks_empty_draft", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    private func addTrucksEmptyDraftSuggestHandler() -> Self {
        server.addHandler("GET /reference/catalog/TRUCKS/suggest") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_form_trucks_empty_draft_suggest", userAuthorized: true)
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

private extension XCUIElement {
    func equals(title: String, value: String) {
        Step("Проверяем значение `\(value)` у поля `\(title)`") {
            let labels = staticTexts
            XCTAssertEqual(labels.count, 2)
            XCTAssertEqual(labels.element(boundBy: 0).label, title)
            XCTAssertEqual(labels.element(boundBy: 1).label, value)
        }
    }

    func switched(name: String, on: Bool) {
        let title: String = on ? "Выбран" : "Не выбран"
        Step("Проверяем, что свитчер `\(name)` \(title)") {
            let switcher = switches.firstMatch
            XCTAssertEqual(switcher.value as? String, on ? "1" : "0")
        }
    }

    func hasTextField(name: String, placeholder: String) {
        Step("Проверяем плейсхолдер `\(placeholder)` у поля `\(name)`") {
            let fields = textFields
            XCTAssertEqual(fields.count, 1)
            XCTAssertEqual(fields.firstMatch.placeholderValue, placeholder)
        }
    }

    func hasTextField(title: String, value: String) {
        Step("Проверяем значение `\(value)` у поля `\(title)`") {
            let labels = staticTexts
            let fields = textFields
            XCTAssertEqual(labels.count, 1)
            XCTAssertEqual(fields.count, 1)

            XCTAssertEqual(labels.firstMatch.label, title)
            XCTAssertEqual(fields.firstMatch.value as? String, value)
        }
    }

    func hasPicker(name: String, placeholder: String) {
        Step("Проверяем плейсхолдер `\(placeholder)` у пикера `\(name)`") {
            let labels = staticTexts
            XCTAssertEqual(labels.count, 1)
            XCTAssertEqual(labels.firstMatch.label, placeholder)
        }
    }

    func has(name: String, error: String) {
        Step("Проверяем поле `\(title)` на ошибку `\(error)`") {
            let labels = staticTexts
            labels[error].shouldExist()
        }
    }

    func notExists(name: String) {
        Step("Проверяем, что поля `\(name)` нет") { self.shouldNotExist() }
    }

    func exists(name: String) {
        Step("Проверяем наличие поля `\(name)`") { self.shouldExist() }
    }
}
