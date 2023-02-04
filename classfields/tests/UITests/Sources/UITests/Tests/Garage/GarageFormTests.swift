import AutoRuProtoModels
import Snapshots
import SwiftProtobuf
import XCTest

final class GarageFormTests: BaseTest, KeyboardManaging {
    static let requestTimeout: TimeInterval = 10.0
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    // BMW, добавляем вручную
    static let garageCard1: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")
    // УАЗ, ищем по вину
    static let garageCard2: (vin: String, id: String) = ("XTT316300F1027573", "514745881")

    private lazy var mainSteps = MainSteps(context: self)

    static let garageCard: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")

    // ГРЗ для ручного добавления
    static let govNumberManually = "Y111KA164"
    static let govNumberManuallyRus = "У111КА164"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_showAddExtraParametersBannerWhenModificationIsEmpty() {
        addGetUserCardHandler { card in
            card.vehicleInfo.carInfo.clearTechParamID()
            card.vehicleInfo.carInfo.clearTechParam()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()
    }

    func test_showAddExtraParametersBannerWhenColorIsEmpty() {
        addGetUserCardHandler { card in
            card.vehicleInfo.clearColor()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()
    }

    func test_showAddExtraParametersBannerWhenOwnerCountIsEmpty() {
        addGetUserCardHandler { card in
            card.vehicleInfo.documents.ownersNumber = 0
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()
    }

    func test_showAddExtraParametersBannerWhenMileageIsEmpty() {
        addGetUserCardHandler { card in
            card.vehicleInfo.state.clearMileage()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()
    }

    func test_showAddExtraParametersBannerWhenPurchaseDateIsEmpty() {
        addGetUserCardHandler { card in
            card.vehicleInfo.documents.clearPurchaseDate()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()
    }

    func test_exCarSaleDateInvalid() {
        addGetUserCardHandler { card in
            card.cardTypeInfo.cardType = .exCar
            card.vehicleInfo.documents.purchaseDate.year = 2_022
        }
        let mockSource: MockSource<Auto_Api_Vin_Garage_UpdateCardRequest, Auto_Api_Vin_Garage_UpdateCardResponse> =
            .file("garage_user_card_put_incorrect_date_error")
        api.garage.user.card.cardId(GarageFormTests.garageCard.id)
            .put
            .error(mock: mockSource)

        launch()
        openGarageCard()
            .tapOnEditButton()
            .scroll(to: .saveButton)
            .should(provider: .garageFormScreen, .exist)
            .focus { screen in
                screen
                    .openDatePicker(field: .saleDate)
                    .select(components: "апрель", "2018")
                    .tapDone()
                screen
                    .tap(.saveButton)
            }
            .should(provider: .garageFormScreen, .exist)
    }

    func test_exCarSaleDateValid() {
        addGetUserCardHandler { card in
            card.cardTypeInfo.cardType = .exCar
        }
        addPutUserCardHandler()

        let updateGarageCardExpectation = api.garage.user.card.cardId(GarageFormTests.garageCard.id).put
            .expect { request, _ in
                (
                    request
                        .card.vehicleInfo.saleDate.year == 2_022 && request.card.vehicleInfo.saleDate.month == 4
                ) ? .ok :
                    .fail(reason: "Не изменилось поле")
            }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .scroll(to: .saveButton)
            .should(provider: .garageFormScreen, .exist)
            .focus { screen in
                screen
                    .openDatePicker(field: .saleDate)
                    .select(components: "апрель", "2022")
                    .tapDone()
                screen
                    .tap(.saveButton)
            }
            .wait(for: [updateGarageCardExpectation], timeout: 5)
            .should(provider: .garageCardScreen, .exist)
    }

    func test_shouldNotShowAddExtraParametersBannerWhenAllParametersFilled() {
        addGetUserCardHandler { _ in }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .shouldNotSeeAddExtraFieldsBanner()
    }

    func test_hideAddExtraParametersBannerWhenAllParametersAdded() {
        addGetUserCardHandler { card in
            card.vehicleInfo.clearColor()
        }

        launch()
        let formSteps = openGarageCard()
            .tapOnEditButton()
            .shouldSeeAddExtraFieldsBanner()

        formSteps.openSinglePicker(field: .color).selectValue(title: "Черный")

        formSteps.shouldNotSeeAddExtraFieldsBanner()
    }

    func test_addCarManually_withVIN() {
        let searchExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/vehicle_info/\(Self.garageCard1.vin)".lowercased() && req
                .method == "GET"
        }

        let createExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card".lowercased()
                && req.method == "POST"
                && Self.isValidCreateForm(
                    request: req,
                    mark: "BMW",
                    model: "5ER",
                    year: "2018",
                    generation: "20856169",
                    body: "SEDAN",
                    engine: "GASOLINE",
                    drive: "ALL_WHEEL_DRIVE",
                    transmission: "AUTOMATIC",
                    modification: "20918976",
                    complectation: "21385569",
                    colorHex: "040001",
                    vin: Self.garageCard1.vin,
                    mileage: "1500",
                    ownerCount: "4",
                    purchaseDate: (4, 2_019)
                )
        }

        launch()
        let steps = openGarageCard()
            .tap(.moreButton)
            .tap(.addCarButton)
            .as(GarageFormSteps.self)

        steps
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen.tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.garageCard1.vin.lowercased()) }
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addManually))
            }

        wait(for: [searchExpectation], timeout: Self.requestTimeout)

        let formSteps = steps.as(GarageFormSteps.self)
            .checkSaveButtonState(isEnabled: false)

        formSteps.openSinglePicker(field: .mark).selectValue(title: "BMW")
            .as(GarageFormSteps.self)
            .checkSaveButtonState(isEnabled: false)

        formSteps.openSinglePicker(field: .model).selectValue(title: "5 серии")
            .as(GarageFormSteps.self)
            .checkSaveButtonState(isEnabled: false)

        formSteps.checkFieldSnapshot(.generation, snapshot: "generation_disabled")
        formSteps.checkFieldSnapshot(.body, snapshot: "body_disabled")
        formSteps.checkFieldSnapshot(.engine, snapshot: "engine_disabled")
        formSteps.checkFieldSnapshot(.drive, snapshot: "drive_disabled")
        formSteps.checkFieldSnapshot(.transmission, snapshot: "transmission_disabled")
        formSteps.checkFieldSnapshot(.modification, snapshot: "modification_disabled")
        formSteps.checkFieldSnapshot(.complectation, snapshot: "complectation_disabled")

        formSteps.openSinglePicker(field: .year).selectValue(title: "2018")
            .as(GarageFormSteps.self)
            .checkSaveButtonState(isEnabled: true)

        formSteps
            .checkValue(
                field: .generation,
                text: "VII (G30/G31)"
            ) // поколение должно быть выбрано автоматом из 1 варианта
        formSteps.checkFieldSnapshot(.generation, snapshot: "generation_value")

        // body
        formSteps.checkFieldSnapshot(.body, snapshot: "body_empty")
        formSteps.openSinglePicker(field: .body).selectValue(title: "Седан")
        formSteps.checkValue(field: .body, text: "Седан")
        formSteps.checkFieldSnapshot(.body, snapshot: "body_value")

        // engine
        formSteps.checkFieldSnapshot(.engine, snapshot: "engine_empty")
        formSteps.openSinglePicker(field: .engine).selectValue(title: "Бензин")
        formSteps.checkValue(field: .engine, text: "Бензин")
        formSteps.checkFieldSnapshot(.engine, snapshot: "engine_value")

        // drive
        formSteps.checkFieldSnapshot(.drive, snapshot: "drive_empty")
        formSteps.openSinglePicker(field: .drive).selectValue(title: "Полный")
        formSteps.checkValue(field: .drive, text: "Полный")
        formSteps.checkFieldSnapshot(.drive, snapshot: "drive_value")

        // transmission
        formSteps
            .checkValue(
                field: .transmission,
                text: "Автоматическая"
            ) // Коробка передач должна быть выбрана автоматом из 1 варианта
        formSteps.checkFieldSnapshot(.transmission, snapshot: "transmission_value")

        // modification
        formSteps.checkFieldSnapshot(.modification, snapshot: "modification_empty")
        formSteps.openSinglePicker(field: .modification).selectValue(title: "530i xDrive 2.0 AT Бензин 249 л.с.")
        formSteps.checkValue(field: .modification, text: "530i xDrive 2.0 AT Бензин 249 л.с.")
        formSteps.checkFieldSnapshot(.modification, snapshot: "modification_value")

        // complectation
        formSteps.checkFieldSnapshot(.complectation, snapshot: "complectation_empty")
        formSteps.openSinglePicker(field: .complectation).selectValue(title: "530i xDrive M Sport")
        formSteps.checkValue(field: .complectation, text: "530i xDrive M Sport")
        formSteps.checkFieldSnapshot(.complectation, snapshot: "complectation_value")

        // color
        formSteps.openSinglePicker(field: .color).selectValue(title: "Черный")
        formSteps.checkValue(field: .color, text: "Черный")

        // mileage
        formSteps.tapAndTypeText(field: .mileage, text: "1500\n")
        formSteps.checkFieldSnapshot(.mileage, snapshot: "mileage_value")

        // owner count
        formSteps.openSinglePicker(field: .ownerCount).selectValue(title: "4+")
        formSteps.checkValue(field: .ownerCount, text: "4+")

        // purhcase date
        formSteps.openDatePicker(field: .purchaseDate)
            .select(components: "апрель", "2019")
            .tapDone()

        formSteps.checkValue(field: .purchaseDate, text: "апрель 2019")

        formSteps.checkText(field: .govNumber, text: "")
        formSteps.checkText(field: .vin, text: Self.garageCard1.vin)

        formSteps.tapOnSave()

        wait(for: [createExpectation], timeout: Self.requestTimeout)

        formSteps.as(GarageCardSteps.self)
            .shouldSeeCard()
    }

    func test_addCarManually_checkErrorMapping() {
        server.addHandler("POST /garage/user/card") { _, _ -> Response? in
            Response.badResponse(fileName: "garage_form_validation_error", userAuthorized: true)
        }

        launch()
        let formSteps = openGarageCard()
            .tap(.moreButton)
            .tap(.addCarButton)
            .as(GarageFormSteps.self)

        formSteps
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen.tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.garageCard1.vin.lowercased()) }
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addManually))
            }

        formSteps.openSinglePicker(field: .mark).selectValue(title: "BMW")
        formSteps.openSinglePicker(field: .model).selectValue(title: "5 серии")
        formSteps.openSinglePicker(field: .year).selectValue(title: "2018")
        formSteps.openSinglePicker(field: .color).selectValue(title: "Черный")
        formSteps.tapAndTypeText(field: .mileage, text: "1500\n")
        formSteps.openSinglePicker(field: .ownerCount).selectValue(title: "4+")
        formSteps.openDatePicker(field: .purchaseDate)
            .select(components: "апрель", "2019")
            .tapDone()

        formSteps.tapOnSave()

        formSteps.checkFieldSnapshot(.mark, snapshot: "garage_mark_validation_error")
            .checkFieldSnapshot(.year, snapshot: "garage_year_validation_error")
            .checkFieldSnapshot(.color, snapshot: "garage_color_validation_error")
            .checkFieldSnapshot(.vin, snapshot: "garage_vin_validation_error")
            .checkFieldSnapshot(.govNumber, snapshot: "garage_gov_number_validation_error")
            .checkFieldSnapshot(.modification, snapshot: "garage_modification_date_error")
            .checkFieldSnapshot(.mileage, snapshot: "garage_mileage_error")
            .checkFieldSnapshot(.ownerCount, snapshot: "garage_owner_count_error")
            .checkFieldSnapshot(.purchaseDate, snapshot: "garage_purchase_date_error")
    }

    func test_addCarManually_commonError() {
        server.addHandler("POST /garage/user/card") { _, _ -> Response? in
            Response.badResponse(fileName: "garage_form_validation_common_error", userAuthorized: true)
        }

        launch()
        let formSteps = openGarageCard()
            .tap(.moreButton)
            .tap(.addCarButton)
            .as(GarageFormSteps.self)

        formSteps
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen.tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.garageCard1.vin.lowercased()) }
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addManually))
            }

        formSteps.openSinglePicker(field: .mark).selectValue(title: "BMW")
        formSteps.openSinglePicker(field: .model).selectValue(title: "5 серии")
        formSteps.openSinglePicker(field: .year).selectValue(title: "2018")
        formSteps.openSinglePicker(field: .color).selectValue(title: "Черный")

        formSteps.tapOnSave()
            .checkFormErrorSnapshot("error")
    }

    func test_addCarManuallyWithGovNumberAndRetry() {
        let searchExpectation1 = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/vehicle_info/\(Self.govNumberManuallyRus)".lowercased() && req
                .method == "GET"
        }

        let searchExpectation2 = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/vehicle_info/\(Self.govNumberManuallyRus)".lowercased() && req
                .method == "GET"
        }

        let createExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card".lowercased()
                && req.method == "POST"
                && Self.isValidCreateForm(
                    request: req,
                    mark: "BMW",
                    model: "5ER",
                    year: "2018",
                    generation: "20856169",
                    colorHex: "040001",
                    govNumber: Self.govNumberManuallyRus
                )
        }

        server.addHandler("GET /garage/user/vehicle_info/\(Self.govNumberManuallyRus)") { _, _ -> Response? in
            Response.responseWith(code: "202", fileName: "garage_search_in_progress", userAuthorized: true)
        }

        launch()
        let steps = openGarageCard()
            .tap(.moreButton)
            .tap(.addCarButton)

        steps
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .tap(.govNumberView)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.govNumberManually.lowercased()) }
                    .tap(.continueButton)
            }

        Step("Делаем запрос на поиск -> получаем IN_PROGRESS -> ретраим") {
            self.wait(for: [searchExpectation1], timeout: Self.requestTimeout)

            self.server.addHandler("GET /garage/user/vehicle_info/\(Self.govNumberManuallyRus)") { _, _ -> Response? in
                Response.responseWith(code: "404", fileName: "garage_search_govnumber_not_found", userAuthorized: true)
            }

            self.wait(for: [searchExpectation2], timeout: Self.requestTimeout)
        }

        steps
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .wait(for: 3)
                    .should(.bottomButton(.addManually), .exist)
                    .tap(.bottomButton(.addManually))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { formScreen in
                formScreen
                    .checkSaveButtonState(isEnabled: false)

                formScreen.openSinglePicker(field: .mark).selectValue(title: "BMW")
                    .as(GarageFormSteps.self)
                    .checkSaveButtonState(isEnabled: false)

                formScreen.openSinglePicker(field: .model).selectValue(title: "5 серии")
                    .as(GarageFormSteps.self)
                    .checkSaveButtonState(isEnabled: false)

                formScreen.openSinglePicker(field: .year).selectValue(title: "2018")
                    .as(GarageFormSteps.self)
                    .checkSaveButtonState(isEnabled: true)

                formScreen.openSinglePicker(field: .color).selectValue(title: "Черный")
                    .as(GarageFormSteps.self)
                    .checkSaveButtonState(isEnabled: true)

                formScreen.checkText(field: .govNumber, text: Self.govNumberManuallyRus)
                formScreen.checkText(field: .vin, text: "")

                formScreen.tapOnSave()
            }

        wait(for: [createExpectation], timeout: Self.requestTimeout)
    }

    func test_updateCar() {
        mocker.mock_garagePromos()

        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard1.id)", userAuthorized: true)
        }
        api.geo.suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_geo_suggest"))

        let updateExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card/\(Self.garageCard1.id)".lowercased()
                && req.method == "PUT"
                && Self.isValidUpdateForm(
                    request: req,
                    mark: "AUDI",
                    model: "A3",
                    year: "2018",
                    generation: "20785010",
                    body: "SEDAN",
                    engine: "GASOLINE",
                    drive: "FORWARD_CONTROL",
                    transmission: "ROBOT",
                    modification: "20838794",
                    complectation: "20838807",
                    colorHex: "FAFBFB",
                    mileage: "1500",
                    ownerCount: "3",
                    purchaseDate: (4, 2_019)
                )
        }

        let reviewsRatingsExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased().starts(with: "/reviews/auto/CARS/rating?mark=AUDI&model=A3&super_gen=0".lowercased())
                && req.method == "GET"
        }

        let reviewsCounterExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased()
                .starts(with: "/reviews/auto/CARS/counter?category=CARS&mark=AUDI&model=A3".lowercased())
                && req.method == "GET"
        }

        let featuresExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased().starts(with: "/reviews/auto/features/CARS?mark=AUDI&model=A3".lowercased())
                && req.method == "GET"
        }

        // после начальной загрузки suggestions форма попытается сохранить исправленные поля. Игнорим этот кейс
        server.addHandler("PUT /garage/user/card/\(Self.garageCard1.id)") { _, _ -> Response? in
            Response.badResponse(code: .badRequest)
        }

        launch()
        let formSteps = openGarageCard()
            .shouldSeeCard()
            .tapOnEditButton()

        formSteps.openSinglePicker(field: .mark).selectValue(title: "Audi")
        formSteps.openSinglePicker(field: .model).selectValue(title: "A3")
        formSteps.openSinglePicker(field: .region).selectValue(title: "Сан-Франциско")
        formSteps.openSinglePicker(field: .year).selectValue(title: "2018")
        formSteps.openSinglePicker(field: .body).selectValue(title: "Седан")
        formSteps.openSinglePicker(field: .engine).selectValue(title: "Бензин")
        formSteps.openSinglePicker(field: .drive).selectValue(title: "Передний")
        formSteps.openSinglePicker(field: .transmission).selectValue(title: "Роботизированная")
        formSteps.openSinglePicker(field: .modification).selectValue(title: "2.0 АМТ Бензин 190 л.с.")
        formSteps.openSinglePicker(field: .complectation).selectValue(title: "Basis")
        formSteps.openSinglePicker(field: .color).selectValue(title: "Белый")
        formSteps.tapAndTypeText(field: .mileage, text: "1500\n")
        formSteps.openSinglePicker(field: .ownerCount).selectValue(title: "3")
        formSteps.openDatePicker(field: .purchaseDate)
            .select(components: "апрель", "2019")
            .tapDone()

        server.addHandler("PUT /garage/user/card/\(Self.garageCard1.id)") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard1.id)_audi", userAuthorized: true)
        }

        formSteps.tapOnSave()

        wait(
            for: [
                updateExpectation,
                reviewsRatingsExpectation,
                reviewsCounterExpectation,
                featuresExpectation
            ],
            timeout: Self.requestTimeout
        )
    }

    func test_updateCar_manuallyValidation() {
        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard1.id)", userAuthorized: true)
        }

        let invalidGovNumber = "Г123ОЗ77"
        let validGovNumber = "O123OА46"

        launch()
        openGarageCard()
            .shouldSeeCard()
            .tapOnEditButton()
            .as(GarageFormSteps.self)
            .tapAndTypeText(field: .govNumber, text: invalidGovNumber)
            .hideKeyboard(field: .govNumber)
            .checkSaveButtonState(isEnabled: false)
            .checkFieldSnapshot(.govNumber, snapshot: "garage_gov_number_client_validation_error")
            .clearText(field: .govNumber)
            .hideKeyboard(field: .govNumber)
            .tapAndTypeText(field: .govNumber, text: validGovNumber)
            .hideKeyboard(field: .govNumber)
            .checkFieldSnapshot(.govNumber, snapshot: "garage_gov_number_valid")
            .checkSaveButtonState(isEnabled: true)
            .tapOnSave()
    }

    func test_deleteCar() {
        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard1.id)", userAuthorized: true)
        }

        let deleteExpectation = expectationForRequest { req -> Bool in
            req.method == "DELETE" && req.uri.lowercased() == "/garage/user/card/\(Self.garageCard1.id)".lowercased()
        }

        launch()
        let steps = openGarageCard()
            .shouldSeeCard()
            .tapOnEditButton()
            .tapOnDelete()
            .handleAlert(alert: app.alerts.firstMatch, allowButtons: ["Удалить"])

        wait(for: [deleteExpectation], timeout: Self.requestTimeout)

        steps.as(GarageSteps.self)
            .checkPromoBanner()
            .shouldNotSeeGarageCard(id: Self.garageCard1.id)
    }

    func test_provenOwner() {
        launch()
        openGarageCard()
            .tapOnEditButton()
            .scroll(to: .provenOwnerCell)
            .tap(.passVerificationButton)
            .should(.provenOwnerPhotoController, .exist)
            .tap(.provenOwnerAddPhotoButton)
            .should(.attachmentPicker, .exist)
    }

    func test_createPanorama() {
        launch()
        openGarageCard()
            .tapOnEditButton()
            .tap(.createPanoramaButton)
            .should(.createPanoramaView, .exist)
    }

    func test_deleteFailedPanorama() {
        addGetUserCardHandler { card in
            card.vehicleInfo.exteriorPanorama.panorama.id = "123"
            card.vehicleInfo.exteriorPanorama.panorama.status = .failed
            card.vehicleInfo.exteriorPanorama.panorama.error = .init()
        }

        addPutUserCardHandler { card in
            card.vehicleInfo.exteriorPanorama.clearPanorama()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .should(.panoramaError, .exist)
            .tap(.panoramaError)
            .should(.panoramaPopUp, .exist)
            .tap(.delete)
            .should(.panoramaPopUp, .be(.hidden))
            .should(.createPanoramaButton, .exist)
    }

    func test_reshootFailedPanorama() {
        addGetUserCardHandler { card in
            card.vehicleInfo.exteriorPanorama.panorama.id = "123"
            card.vehicleInfo.exteriorPanorama.panorama.status = .failed
            card.vehicleInfo.exteriorPanorama.panorama.error = .init()
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .should(.panoramaError, .exist)
            .tap(.panoramaError)
            .should(.panoramaPopUp, .exist)
            .tap(.createNewPanoramaButton)
            .should(.createPanoramaView, .exist)
    }

    func test_reshootProcessingPanorama() {
        addGetUserCardHandler { card in
            card.vehicleInfo.exteriorPanorama.panorama.id = "123"
            card.vehicleInfo.exteriorPanorama.panorama.status = .processing
        }

        launch()
        openGarageCard()
            .tapOnEditButton()
            .should(.panoramaProcessing, .exist)
            .tap(.panoramaProcessing)
            .should(.panoramaReshootMenu, .exist)
    }

    private func openGarage() -> GarageSteps {
        mainSteps.openTab(.garage).as(GarageSteps.self)
    }

    private func openGarageCard() -> GarageCardSteps {
        openGarage().as(GarageCardSteps.self)
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { _, _ -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user *") { [weak self] _, _ in
            guard let strongSelf = self else {
                return Response.badResponse(code: Auto_Api_ErrorCode.badRequest)
            }
            return Response.responseWithStatus(body: try! strongSelf.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        server.addHandler("GET /session") { _, _ -> Response? in
            Response.okResponse(fileName: "session", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard.id)", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/rating *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/\(Self.garageCard.id)") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard.id)_manually_mocked", userAuthorized: true)
        }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=COUPE&engine_type=GASOLINE&gear_type=REAR_DRIVE&mark=BMW&model=2ER&super_gen=21006297&tech_param_id=21006573&transmission=AUTOMATIC&year=2018"
            ) { _, _ in
                Response.okResponse(fileName: "garage_form_suggest_BMW2", userAuthorized: true)
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=COUPE&engine_type=GASOLINE&gear_type=REAR_DRIVE&mark=BMW&model=2ER&super_gen=21006297&transmission=AUTOMATIC&year=2018"
            ) { _, _ in
                Response.okResponse(fileName: "garage_form_suggest_BMW2", userAuthorized: true)
            }

        server.addHandler("GET /garage/user/vehicle_info/\(Self.garageCard1.vin)") { _, _ -> Response? in
            Response.responseWith(code: "404", fileName: "garage_search_not_found", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/vehicle_info/\(Self.garageCard2.vin)") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_search_vin", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/card/identifier/\(Self.garageCard2.vin)") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_create_with_vin", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_mark", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_model", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_year", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER&year=2018") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_generation", userAuthorized: true)
        }

        server
            .addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER&super_gen=20856169&year=2018") { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_body", userAuthorized: true)
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&mark=BMW&model=5ER&super_gen=20856169&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_engine", userAuthorized: true)
            }

        server.addHandler(
            "GET /reference/catalog/CARS/suggest"
                + "?body_type=SEDAN&engine_type=GASOLINE&mark=BMW&model=5ER&super_gen=20856169&year=2018"
        ) { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_drive", userAuthorized: true)
        }

        server.addHandler(
            "GET /reference/catalog/CARS/suggest"
                +
                "?body_type=SEDAN&engine_type=GASOLINE&gear_type=ALL_WHEEL_DRIVE&mark=BMW&model=5ER&super_gen=20856169&year=2018"
        ) { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_transmission", userAuthorized: true)
        }

        server.addHandler(
            "GET /reference/catalog/CARS/suggest"
                +
                "?body_type=SEDAN&engine_type=GASOLINE&gear_type=ALL_WHEEL_DRIVE&mark=BMW&model=5ER&super_gen=20856169"
                + "&transmission=AUTOMATIC&year=2018"
        ) { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_modification", userAuthorized: true)
        }

        server.addHandler(
            "GET /reference/catalog/CARS/suggest"
                +
                "?body_type=SEDAN&engine_type=GASOLINE&gear_type=ALL_WHEEL_DRIVE&mark=BMW&model=5ER&super_gen=20856169"
                + "&tech_param_id=20918976&transmission=AUTOMATIC&year=2018"
        ) { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_complectation", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=AUDI") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_model_audi", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=AUDI&model=A3") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_year_audi", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=AUDI&model=A3&year=2018") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_generation_audi", userAuthorized: true)
        }

        server
            .addHandler("GET /reference/catalog/CARS/suggest?mark=AUDI&model=A3&super_gen=20785010&year=2018") { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_body_audi", userAuthorized: true)
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&mark=AUDI&model=A3&super_gen=20785010&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_engine_audi")
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&engine_type=GASOLINE&mark=AUDI&model=A3&super_gen=20785010&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_drive_audi")
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&mark=AUDI&model=A3&super_gen=20785010&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_transmission_audi")
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&mark=AUDI&model=A3&super_gen=20785010&transmission=ROBOT&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_modification_audi")
            }

        server
            .addHandler(
                "GET /reference/catalog/CARS/suggest?body_type=SEDAN&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&mark=AUDI&model=A3&super_gen=20785010&tech_param_id=20838794&transmission=ROBOT&year=2018"
            ) { _, _ -> Response? in
                Response.okResponse(fileName: "garage_form_suggest_complectation_audi")
            }

        server.addHandler("POST /garage/user/card") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_form_create", userAuthorized: true)
        }

        server.addHandler("DELETE /garage/user/card/\(Self.garageCard1.id)") { _, _ -> Response? in
            Response.okResponse(fileName: "success")
        }

        try! server.start()
    }

    private func addGetUserCardHandler(mutatingCard: @escaping (inout Auto_Api_Vin_Garage_Card) -> Void) {

        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            guard var card = self.getFilledGarageCard()?.card else { return nil }
            mutatingCard(&card)
            var cardsResponse = Auto_Api_Vin_Garage_GetListingResponse()
            cardsResponse.status = .success
            cardsResponse.listing = [card]
            return Response.okResponse(message: cardsResponse, userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/\(Self.garageCard.id)") { _, _ -> Response? in
            guard var response = self.getFilledGarageCard() else { return nil }
            mutatingCard(&response.card)
            return Response.okResponse(message: response, userAuthorized: true)
        }
    }

    private func addPutUserCardHandler(mutatingCard: ((inout Auto_Api_Vin_Garage_Card) -> Void)? = nil) {
        api.garage.user.card.cardId(Self.garageCard.id)
            .put
            .ok(mock: .file(
                "garage_card_\(Self.garageCard.id)_manually_mocked_filled",
                mutation: ({ resp in
                    var card = resp.card
                    mutatingCard?(&card)
                    resp.card = card
                })
            ))
    }

    private func getFilledGarageCard() -> Auto_Api_Vin_Garage_GetCardResponse? {
        do {
            let fileName = "garage_card_\(Self.garageCard.id)_manually_mocked_filled"
            guard let fileURL = Bundle.resources.url(forResource: fileName, withExtension: "json") else {
                XCTFail("unable to get \(fileName).json")
                return nil
            }
            let data = try Data(contentsOf: fileURL)
            return try Auto_Api_Vin_Garage_GetCardResponse(jsonUTF8Data: data)
        } catch {
            XCTFail("Unable to create response. \(error.localizedDescription)")
            return nil
        }
    }

    private static func isValidCreateForm(
        request: Request,
        mark: String,
        model: String,
        year: String,
        generation: String? = nil,
        body: String? = nil,
        engine: String? = nil,
        drive: String? = nil,
        transmission: String? = nil,
        modification: String? = nil,
        complectation: String? = nil,
        colorHex: String? = nil,
        vin: String? = nil,
        govNumber: String? = nil,
        mileage: String? = nil,
        ownerCount: String? = nil,
        purchaseDate: (month: Int, year: Int)? = nil
    ) -> Bool {
        guard let data = request.messageBody,
              let req = try? Auto_Api_Vin_Garage_CreateCardRequest(jsonUTF8Data: data)
        else {
            return false
        }

        if let vin = vin {
            if req.card.vehicleInfo.documents.vin != vin { return false }
        } else if let govNumber = govNumber {
            if req.card.vehicleInfo.documents.licensePlate != govNumber { return false }
        } else {
            assertionFailure("Pass VIN or gov number")
        }

        let helper = CompareHelper<Auto_Api_Vin_Garage_Vehicle>(object: req.card.vehicleInfo)
            .compare { $0.carInfo.mark == mark }
            .compare { $0.carInfo.model == model }
            .compare { year == "\($0.documents.year)" }
            .compare { generation ?? "0" == "\($0.carInfo.superGenID)" }
            .compare { body ?? "" == "\($0.carInfo.bodyType)" }
            .compare { engine ?? "" == "\($0.carInfo.engineType)" }
            .compare { drive ?? "" == "\($0.carInfo.drive)" }
            .compare { transmission ?? "" == "\($0.carInfo.transmission)" }
            .compare { modification ?? "0" == "\($0.carInfo.techParamID)" }
            .compare { complectation ?? "0" == "\($0.carInfo.complectationID)" }
            .compare { $0.color.id.lowercased() == colorHex?.lowercased() ?? "" }
            .compare { mileage ?? "0" == "\($0.state.mileage.value)" }
            .compare { ownerCount ?? "0" == "\($0.documents.ownersNumber)" }
            .compare { Int($0.documents.purchaseDate.month) == purchaseDate?.month ?? 0 }
            .compare { Int($0.documents.purchaseDate.year) == purchaseDate?.year ?? 0 }

        if !helper.failedConditions.isEmpty {
            debugPrint("failedConditions: \(helper.failedConditions)")
        }

        return helper.condition
    }

    private struct CompareHelper<Object> {
        let object: Object
        let condition: Bool
        let failedConditions: [UInt]

        init(object: Object, condition: Bool = true, failedConditions: [UInt] = []) {
            self.object = object
            self.condition = condition
            self.failedConditions = failedConditions
        }

        func compare(line: UInt = #line, _ block: (Object) -> Bool) -> Self {
            let result = block(object)
            var conditions = failedConditions
            if !result {
                conditions.append(line)
            }

            return CompareHelper(object: object, condition: condition && result, failedConditions: conditions)
        }
    }

    private static func isValidUpdateForm(
        request: Request,
        mark: String,
        model: String,
        year: String,
        generation: String? = nil,
        body: String? = nil,
        engine: String? = nil,
        drive: String? = nil,
        transmission: String? = nil,
        modification: String? = nil,
        complectation: String? = nil,
        colorHex: String? = nil,
        vin: String? = nil,
        govNumber: String? = nil,
        mileage: String? = nil,
        ownerCount: String? = nil,
        purchaseDate: (month: Int, year: Int)? = nil
    ) -> Bool {
        guard let data = request.messageBody,
              let req = try? Auto_Api_Vin_Garage_UpdateCardRequest(jsonUTF8Data: data)
        else {
            return false
        }

        let helper = CompareHelper<Auto_Api_Vin_Garage_Vehicle>(object: req.card.vehicleInfo)
            .compare { $0.carInfo.mark == mark }
            .compare { $0.carInfo.model == model }
            .compare { year == "\($0.documents.year)" }
            .compare { generation ?? "0" == "\($0.carInfo.superGenID)" }
            .compare { body ?? "" == "\($0.carInfo.bodyType)" }
            .compare { engine ?? "" == "\($0.carInfo.engineType)" }
            .compare { drive ?? "" == "\($0.carInfo.drive)" }
            .compare { transmission ?? "" == "\($0.carInfo.transmission)" }
            .compare { modification ?? "0" == "\($0.carInfo.techParamID)" }
            .compare { complectation ?? "0" == "\($0.carInfo.complectationID)" }
            .compare { $0.color.id.lowercased() == colorHex?.lowercased() ?? "" }
            .compare { mileage ?? "0" == "\($0.state.mileage.value)" }
            .compare { ownerCount ?? "0" == "\($0.documents.ownersNumber)" }
            .compare { Int($0.documents.purchaseDate.month) == purchaseDate?.month ?? 0 }
            .compare { Int($0.documents.purchaseDate.year) == purchaseDate?.year ?? 0 }

        if !helper.failedConditions.isEmpty {
            debugPrint("failedConditions: \(helper.failedConditions)")
        }

        return helper.condition
    }
}
