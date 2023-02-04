//
//  GarageSearchTests.swift
//  Tests
//
//  Created by Igor Shamrin on 15.11.2021.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuGarageForm AutoRuGarageCard AutoRuGarageWizard
final class GarageSearchTests: BaseTest, KeyboardManaging {

    private lazy var mainSteps = MainSteps(context: self)

    private let govNumber = "a123aa77"
    private let govNumberRus = "А123АА77"

    private let govNumberUnknown = "a111aa77"
    private let govNumberRusUnknown = "А111АА77"

    private let vin = "rumke8938ev028756"
    private let cardId = "349151987"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_disclaimerExistIfNoCard() {
        api.garage.user.vehicleInfo.identifier(govNumber)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus {
                $0.scroll(to: .disclaimer)
                $0.tap(.disclaimer)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_disclaimerNotExistIfHasCard() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        openGarage()
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
            .focus {
                $0.should(.disclaimer, .be(.hidden))
            }
    }

    func test_addByExistingGovNum() {
        api.garage.user.vehicleInfo.identifier(govNumber)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.govNumberView)
                	.wait(for: 2)
                    .do { typeFromKeyboard(govNumber) }
                	.should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .should(.bottomButton(.addToGarage), .exist)
                    .validateSnapshot()
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_addByUnknownGovNum() {
        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.govNumberView)
                    .wait(for: 1)
                    .do { typeFromKeyboard(govNumberUnknown) }
                    .should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .wait(for: 1)
                    .validateSnapshot()
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Мы не нашли информацию об этом автомобиле, но вы можете его добавить по VIN или вручную, указав марку, модель и другие параметры."))
                    .should(.bottomButton(.addManually), .exist)
                    .tap(.bottomButton(.addManually))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { screen in
                screen.should(.field(govNumberRusUnknown), .exist)
            }
    }

    func test_addByInvalidGovNum() {
        api.garage.user.vehicleInfo.identifier(govNumberRusUnknown.uppercased())
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage, Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_gn_fail", mutation: ({
                        $0.error = .licensePlateInvalid
                    })))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.govNumberView)
                    .wait(for: 1)
                    .do { typeFromKeyboard(govNumberUnknown) }
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .wait(for: 1)
                    .validateSnapshot()
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Мы не нашли информацию об этом автомобиле. Если вы уверены, что не ошиблись при вводе госномера, то попробуйте добавить автомобиль по VIN или повторите поиск по госномеру ещё раз."))
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .should(.bottomButton(.retry), .exist)
                    .tap(.bottomButton(.addByVin))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { $0.should(.vinInputField, .exist) }
    }

    func test_vinInvalid() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage,Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_vin_not_found", mutation: ({
                        $0.error = .vinCodeInvalid
                    }))
            )

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Удивительно, но мы ничего не знаем о вашем автомобиле. Если вы уверены, что не ошиблись в номере VIN, то попробуйте найти автомобиль ещё раз или вернитесь через несколько дней."))
                    .validateSnapshot(snapshotId: #function + "_searchResult")
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .should(.bottomButton(.retry), .exist)
            }
    }

    func test_badRequest() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage,Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_vin_not_found", mutation: ({
                        $0.error = .badRequest
                    }))
            )

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Кажется, вы указали некорректные данные для поиска автомобиля. Скорректируйте введённый номер и повторите поиск."))
                    .validateSnapshot(snapshotId: #function + "_searchResult")
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .should(.bottomButton(.retry), .exist)
            }
    }

    func test_addByUnknownGovNumTryVIN() {
        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.govNumberView)
                    .wait(for: 1)
                    .do { typeFromKeyboard(govNumberUnknown) }
                    .should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Мы не нашли информацию об этом автомобиле, но вы можете его добавить по VIN или вручную, указав марку, модель и другие параметры."))
                    .should(.bottomButton(.addByVin), .exist)
                    .should(.bottomButton(.addManually), .exist)
                    .tap(.bottomButton(.addByVin))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { $0.should(.vinInputField, .exist) }
    }

    func test_deleteGovNumberFromSearchScreen() {
        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.govNumberView)
                    .wait(for: 1)
                    .do { typeFromKeyboard(govNumberUnknown) }
                    .should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .should(.govNumberInputField, .exist)
                    .tap(.govNumberInputField)
                    .do { deleteStringFromKeyboard(govNumberUnknown) }
                    .should(.bottomButton(.search), .be(.hidden))
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .do { typeFromKeyboard(govNumberUnknown) }
                    .should(.bottomButton(.search), .exist)
            }
    }

    func test_addByVinAndChangeRegion_hasNotCard() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        api.geo.suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_geo_suggest"))

        let expectation = api.garage.user.card.identifier.identifier(vin)
            .post
            .expect { req, _ in
                return req.registrationRegionID == 90 ? .ok : .fail(reason: nil)
            }

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .wait(for: 1)
                    .validateSnapshot()
                    .tap(.selectedRegion)
            }
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in
                screen
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [expectation], timeout: 10)
    }

    func test_addByGovNumberAndChangeRegion_hasNotCard() {
        api.garage.user.vehicleInfo.identifier(govNumberRus)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        api.geo.suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_geo_suggest"))

        let expectation = api.garage.user.card.identifier.identifier(govNumberRus)
            .post
            .expect { req, _ in
                return req.registrationRegionID == 90 ? .ok : .fail(reason: nil)
            }

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .tap(.govNumberView)
                    .wait(for: 2)
                    .do { typeFromKeyboard(govNumber) }
                    .should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .wait(for: 3)
                    .validateSnapshot()
                    .tap(.selectedRegion)
            }
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in 
                screen
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [expectation], timeout: 10)
    }

    func test_addByGovNumberAndChangeRegion_hasCard() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        api.garage.user.card.cardId(cardId)
            .put
            .ok(mock: .file("garage_search_result_withCard"))

        api.garage.user.vehicleInfo.identifier(govNumberRus)
            .get
            .ok(mock: .file("garage_search_result_withCard"))

        api.geo.suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_geo_suggest"))

        let expectation = api.garage.user.card.cardId(cardId)
            .put
            .expect { req, _ in
                return req.card.vehicleInfo.registrationRegion.id == 90 ? .ok : .fail(reason: nil)
            }

        openGarage()
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .tap(.govNumberView)
                    .wait(for: 2)
                    .do { typeFromKeyboard(govNumber) }
                    .should(.continueButton, .exist)
                    .tap(.continueButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .should(.selectedRegion, .exist)
                    .tap(.selectedRegion)
            }
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in
                screen
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.bottomButton(.goToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [expectation], timeout: 10)
    }

    func test_addByVinAndChangeRegion_hasCard() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        api.garage.user.card.cardId(cardId)
            .put
            .ok(mock: .file("garage_search_result_withCard"))

        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withCard"))

        api.geo.suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_geo_suggest"))

        let expectation = api.garage.user.card.cardId(cardId)
            .put
            .expect { req, _ in
                return req.card.vehicleInfo.registrationRegion.id == 90 ? .ok : .fail(reason: nil)
            }

        openGarage()
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .wait(for: 1)
                    .tap(.selectedRegion)
            }
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in
                screen
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.bottomButton(.goToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [expectation], timeout: 10)
    }

    func test_addByVin_withCard() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withCard"))

        openGarage()
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.goToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_addByVin_withoutCard() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_vinNotFound() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage,Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_vin_not_found")
            )

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .validateSnapshot(snapshotId: #function + "_empty")
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .should(.hintLabel, .exist)
                    .should(.hintLabel, .match("Удивительно, но мы ничего не знаем о вашей машине. Если уверены, что не ошиблись в номере VIN, просто добавьте машину вручную."))
                    .validateSnapshot(snapshotId: #function + "_searchResult")
                    .tap(.bottomButton(.addManually))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { screen in
                screen
                    .should(.field("RUMKE8938EV028756"), .exist)
            }
    }

    func test_searchVin_serverError() {
        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .wait(for: 1)
                    .validateSnapshot()
            }
    }

    func test_addByVin_withCamera() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .should(.continueButton, .be(.hidden))
                    .tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.cameraButton)
                    .handleSystemAlertIfNeeded()
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .wait(for: 1)
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_addExCar() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .addExCarButton)
                    .tap(.addExCarButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_addExCar_notFound() {
        api.garage.user.vehicleInfo.identifier(vin)
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage,Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_vin_not_found")
            )

        openAddCarScreenFromLanding()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .addExCarButton)
                    .tap(.addExCarButton)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(vin) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .should(.bottomButton(.retry), .exist)
                    .validateSnapshot()
            }
    }

    func test_openAddScreenFromListing() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_1955418404"))

        openGarage()
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
    }

    private func openAddCarScreenFromLanding() -> AddCarScreen {
        return launch(on: .mainScreen) { screen in
            screen.toggle(to: .garage)

            return screen.should(provider: .garageLanding, .exist)
                .focus { focused in
                    focused.tap(.addToGarageHeaderButton)
                }
                .should(provider: .garageAddCarScreen, .exist)
        }
    }

    private func openGarage() -> GarageCardScreen_ {
        return launch(on: .mainScreen) { screen in
            screen.toggle(to: .garage)
            return screen.should(provider: .garageCardScreen, .exist)
        }
    }

    private func setupServer() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_empty"))

        api.garage.user.card
            .post
            .ok(mock: .file("garage_dream_car_response"))

        api.garage.user.vehicleInfo.identifier(govNumberRus.uppercased())
            .get
            .ok(mock: .file("garage_search_gn"))

        api.garage.user.card.cardId(cardId)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_get_card_response"))

        api.garage.user.vehicleInfo.identifier(govNumberRusUnknown.uppercased())
            .get
            .error(
                status: ._404,
                mock: MockSource<StubProtobufMessage, Auto_Api_Vin_Garage_GetVehicleInfoResponse>
                    .file("garage_search_gn_fail"))

        api.garage.user.card.identifier.identifier(vin)
            .post
            .ok(mock: .file("garage_add_car_result"))

        api.garage.user.card.identifier.identifier(govNumberRus)
            .post
            .ok(mock: .file("garage_add_car_result"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_form_suggest_BMW2"))

        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_user()
            .startMock()
    }
}
