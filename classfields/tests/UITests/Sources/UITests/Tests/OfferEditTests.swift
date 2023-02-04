//
//  OfferEditTests.swift
//  UITests
//
//  Created by Roman Bevza on 5/25/20.
//

import Foundation
import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRu AutoRuTradeIn
class OfferEditTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private static var needSetup: Bool = true

    private static var invalidDraftResponse: Auto_Api_DraftResponse = {
        let filePath = Bundle.resources.url(forResource: "offer_edit_get_invalid_draft", withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
        let response = body.flatMap { try? Auto_Api_DraftResponse(jsonUTF8Data: $0) }
        return response!
    }()

    private static var draftResponse: Auto_Api_DraftResponse = {
        let filePath = Bundle.resources.url(forResource: "offer_edit_get_draft", withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
        let response = body.flatMap { try? Auto_Api_DraftResponse(jsonUTF8Data: $0) }
        return response!
    }()

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("GET /reference/catalog/cars/all-options") { (request, _) -> Response? in
            return Response.okResponse(fileName: "offer_edit_get_equipment", userAuthorized: false)
        }

        try! server.start()
    }

    private func mockOfferEditBase(draftID: String, nds: Bool = false, description: String? = nil, processingOffer: Bool = false) {
        mocker
            .mock_base()
            .mock_user()
            .mock_wizardDraftCars(isPartial: true, nds: nds, description: description, processingOffer: processingOffer)
            .mock_wizardDraftCars(id: draftID, isPartial: true, nds: nds, description: description, processingOffer: processingOffer)
            .mock_wizardReferenceCatalogCars()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_putDraftCars(id: draftID)
            .mock_postDraftCarsPublish(id: draftID)
            .mock_activateUserOfferFrom(category: "cars", id: draftID)
    }

    private func launchWithOfferForm(overrideAppSettings: [String: Any] = [:], environment: [String: String] = [:]) -> OfferEditScreen_ {
        launch(
            on: .offerEditScreen,
            options: .init(
                launchType: .deeplink("https://auto.ru/add"),
                overrideAppSettings: overrideAppSettings,
                environment: environment
            )
        )
    }

    func test_invalidValueInDraft() {
        launch()

        server.addHandler("GET /reference/catalog/CARS/suggest *") { (request, index) -> Response? in
            if index <= 0 {
                return Response.badResponse(fileName: "offer_edit_get_suggestions", userAuthorized: false)
            } else {
                return Response.okResponse(fileName: "offer_edit_get_suggestions", userAuthorized: false)
            }
        }
        server.addHandler("GET /user/draft/CARS") { (request, index) -> Response? in
            if index <= 1 {
                return Response.responseWithStatus(body: try! OfferEditTests.invalidDraftResponse.jsonUTF8Data(), userAuthorized: false)
            } else {
                return Response.responseWithStatus(body: try! OfferEditTests.draftResponse.jsonUTF8Data(), userAuthorized: false)
            }
        }

        _ = mainSteps
            .openOffersTab()
            .tapAddOffer()
            .tapToCarsCategory()
            .notExist(selector: "Автомобиль")
            .shouldEventBeReported(
                "Открыть полную форму добавления",
                with: ["Категория": "Легковые", "Источник": "ЛК"]
            )
            .tap("Сбросить")
            .tap("ОК")
            .exist(selector: "Автомобиль")
    }

    func test_offerPublication_tradeInAvailable_afterVASTrap_autoProlongation() {
        // Если у оффера активируется автопродление на экране с вас-ловушкой, то нужно убедиться,
        // что дожидаемся закрытия ловушки перед показом трейдина, а не показываем его поверх после успешного
        // запроса на автопродление (для варианта экспа, когда трейдин перед ловушкой кейс неактуален)

        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: true)
            .mock_userOfferProductProlongable(offerId: draftID, product: "all_sale_activate")

        Step("Показ трейд-ина до вас-ловушки (выборка выключена) + автопродление на ловушке") { }

        let prolongationExpectation = expectationForRequest { req in
            req.method == "PUT"
            && req.uri.starts(with: "/user/offers/cars/\(draftID)/product/all_sale_activate/prolongable")
        }

        self.launchWithOfferForm()
            .step("Открываем форму и активируем оффер") {  form in
                form
                    .scroll(to: .activationButton, direction: .up)
                    .tap(.activationButton)
                    .wait(for: 1)
                    .shouldEventBeReported(
                        "Разместить объявление - успех",
                        with: ["draft_id": "1640897598478615376-2c0470c2",
                               "Платность": "reason_unknown",
                               "Категория": "cars",
                               "Источник": "форма"
                              ]
                    )
            }
            .should(provider: .vasTrapScreen, .exist)
            .focus({ vasTrap in
                vasTrap
                    .scroll(to: .doneButton)
                    .tap(.doneButton)
            })
            .wait(for: [prolongationExpectation])
            .should(provider: .tradeInPicker, .exist)
            .focus {
                $0.tap(.agreements)
                $0.tap(.applyButton)
            }
            .should(provider: .tradeInPicker, .be(.hidden))
    }

    func test_offerPublication_tradeInAvailable_withoutActivation() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        Step("Показ трейд-ина до вас-ловушки (выборка выключена)") { }

        self.launchWithOfferForm()
            .step("Открываем форму и жмем `Не публиковать сразу`") {  form in
                form
                    .scroll(to: .publishWithoutActivationButton, direction: .up)
                    .tap(.publishWithoutActivationButton)
            }
            .should(provider: .tradeInPicker, .exist)
            .focus({
                $0
                    .tap(.agreements)
                    .tap(.applyButton)
            })
            .should(provider: .tradeInPicker, .be(.hidden))
    }

    func test_offerPublication_tradeInAvailable_afterVASTrap() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferProductProlongable(offerId: draftID, product: "all_sale_activate")

        Step("Показ трейд-ина до вас-ловушки (выборка выключена)") { }

        let prolongationExpectation = expectationForRequest { req in
            req.method == "PUT"
            && req.uri.starts(with: "/user/offers/cars/\(draftID)/product/all_sale_activate/prolongable")
        }
        prolongationExpectation.isInverted = true

        let isAvailableExpectation = expectationForRequest { req in
            if req.method == "POST" && req.uri == "/trade-in/is_available" {
                guard let json = req.messageBodyString(),
                      let body = try? Auto_TradeInNotifier_Api_TradeInAvailableRequest(jsonString: json) else {
                          return false
                      }

                return body.offer.id == draftID
            }

            return false
        }

        let applyExpectation = expectationForRequest { req in
            if req.method == "PUT" && req.uri == "/trade-in/apply" {
                guard let json = req.messageBodyString(),
                      let body = try? Auto_TradeInNotifier_Api_TradeInApplyRequest(jsonString: json) else {
                          return false
                      }

                return body.offer.id == draftID && body.offer.tradeInInfo.tradeInType == .forMoney
            }

            return false
        }

        self.launchWithOfferForm()
            .step("Открываем форму и активируем оффер") {  form in
                form
                    .scroll(to: .activationButton, direction: .up)
                    .tap(.activationButton)
            }
            .should(provider: .vasTrapScreen, .exist)
            .focus({
                $0
                    .scroll(to: .doneButton)
                    .tap(.doneButton)
            })
            .wait(for: [isAvailableExpectation])
            .should(provider: .tradeInPicker, .exist)
            .focus {
                $0.focus(on: .agreements) { e in
                    e.validateSnapshot(snapshotId: "trade_in_agreements_unchecked")
                }
                .focus(on: .tradeInOption(.new), ofType: .tradeInOptionCell) { cell in
                    cell.should(.checkmark, .exist)
                }
                .focus(on: .tradeInOption(.used), ofType: .tradeInOptionCell) { cell in
                    cell.should(.checkmark, .be(.hidden))
                        .validateSnapshot(snapshotId: "trade_in_option_not_selected_used")
                }
                .focus(on: .tradeInOption(.money), ofType: .tradeInOptionCell) { cell in
                    cell.should(.checkmark, .be(.hidden))
                        .validateSnapshot(snapshotId: "trade_in_option_not_selected_money")
                        .tap()
                        .should(.checkmark, .exist)
                }
                .focus(on: .tradeInOption(.new)) { cell in
                    cell.validateSnapshot(snapshotId: "trade_in_option_not_selected_new")
                }
                .focus(on: .applyButton) { button in
                    button.validateSnapshot(snapshotId: "trade_in_apply_not_available")
                }
                .should(.agreementsValidation, .exist)
                .tap(.agreementsValidation)
                .focus(on: .agreements) { e in
                    e.validateSnapshot(snapshotId: "trade_in_agreements_checked")
                }
                .focus(on: .applyButton) { button in
                    button.validateSnapshot(snapshotId: "trade_in_apply_available")
                }
                .tap(.applyButton)
            }
            .should(provider: .snackbar, .exist)
            .should(provider: .tradeInPicker, .be(.hidden))
            .wait(for: [applyExpectation, prolongationExpectation])
    }

    func test_offerPublication_tradeInAvailable_beforeVASTrap() {
        let draftID = "1640897598478615376-2c0470c2"
        mockOfferEditBase(draftID: draftID)

        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferProductProlongable(offerId: draftID, product: "all_sale_activate")

        Step("Показ трейд-ина после вас-ловушки (выборка включена)") { }

        let isAvailableExpectation = expectationForRequest { req in
            if req.method == "POST" && req.uri == "/trade-in/is_available" {
                guard let json = req.messageBodyString(),
                      let body = try? Auto_TradeInNotifier_Api_TradeInAvailableRequest(jsonString: json) else {
                          return false
                      }

                return body.offer.id == draftID
            }

            return false
        }

        let prolongationExpectation = expectationForRequest { req in
            req.method == "PUT"
            && req.uri.starts(with: "/user/offers/cars/\(draftID)/product/all_sale_activate/prolongable")
        }
        prolongationExpectation.isInverted = true

        let applyExpectation = expectationForRequest { req in
            if req.method == "PUT" && req.uri == "/trade-in/apply" {
                guard let json = req.messageBodyString(),
                      let body = try? Auto_TradeInNotifier_Api_TradeInApplyRequest(jsonString: json) else {
                          return false
                      }

                return body.offer.id == draftID && body.offer.tradeInInfo.tradeInType == .forMoney
            }

            return false
        }

        self.launchWithOfferForm(overrideAppSettings: ["showTradeInFormFirst": true])
            .step("Открываем форму и активируем оффер") {  form in
                form
                    .scroll(to: .activationButton, direction: .up)
                    .tap(.activationButton)
            }
            .wait(for: [isAvailableExpectation], timeout: 10.0)
            .should(provider: .tradeInPicker, .exist)
            .focus { picker in
                picker
                    .focus(on: .agreements) { e in
                        e.validateSnapshot(snapshotId: "trade_in_agreements_unchecked")
                    }
                    .focus(on: .tradeInOption(.new), ofType: .tradeInOptionCell) { cell in
                        cell.should(.checkmark, .exist)
                    }
                    .focus(on: .tradeInOption(.used), ofType: .tradeInOptionCell) { cell in
                        cell.should(.checkmark, .be(.hidden))
                            .validateSnapshot(snapshotId: "trade_in_option_not_selected_used")
                    }
                    .focus(on: .tradeInOption(.money), ofType: .tradeInOptionCell) { cell in
                        cell.should(.checkmark, .be(.hidden))
                            .validateSnapshot(snapshotId: "trade_in_option_not_selected_money")
                            .tap()
                            .should(.checkmark, .exist)
                    }
                    .focus(on: .tradeInOption(.new)) { cell in
                        cell.validateSnapshot(snapshotId: "trade_in_option_not_selected_new")
                    }
                    .focus(on: .applyButton) { button in
                        button.validateSnapshot(snapshotId: "trade_in_apply_not_available")
                    }
                    .should(.agreementsValidation, .exist)
                    .tap(.agreementsValidation)
                    .focus(on: .agreements) { e in
                        e.validateSnapshot(snapshotId: "trade_in_agreements_checked")
                    }
                    .focus(on: .applyButton) { button in
                        button.validateSnapshot(snapshotId: "trade_in_apply_available")
                    }
                    .tap(.applyButton)
            }
            .should(provider: .tradeInPicker, .be(.hidden))
            .should(provider: .vasTrapScreen, .exist)

        wait(for: [applyExpectation, prolongationExpectation], timeout: 10.0)
    }

    func test_offerPublication_tradeInSkipped() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()

        let applyExpectation = expectationForRequest { req in
            req.method == "PUT" && req.uri == "/trade-in/apply"
        }
        applyExpectation.isInverted = true

        self.launchWithOfferForm()
            .step("Открываем форму и активируем оффер") {  form in
                form
                    .scroll(to: .activationButton, direction: .up)
                    .tap(.activationButton)
            }
            .should(provider: .tradeInPicker, .exist)
            .focus({
                $0.tap(.skipButton)
            })
            .should(provider: .tradeInPicker, .be(.hidden))

        wait(for: [applyExpectation], timeout: 2.0)
    }

    func test_offerPublication_tradeInUnavailable() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_tradeInIsAvailable(with: false)
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferProductProlongable(offerId: draftID, product: "all_sale_activate")

        let isAvailableExpectation = expectationForRequest { req in
            if req.method == "POST" && req.uri == "/trade-in/is_available" {
                guard let json = req.messageBodyString(),
                      let body = try? Auto_TradeInNotifier_Api_TradeInAvailableRequest(jsonString: json) else {
                          return false
                      }

                return body.offer.id == draftID
            }

            return false
        }

        self.launchWithOfferForm(overrideAppSettings: ["showTradeInFormFirst": true])
            .step("Открываем форму и активируем оффер") {  form in
                form
                    .scroll(to: .activationButton, direction: .up)
                    .tap(.activationButton)
            }
            .wait(for: [isAvailableExpectation], timeout: 10.0)
            .should(provider: .vasTrapScreen, .exist)
            .focus { vasTrap in
                vasTrap
                    .should(provider: .tradeInPicker, .be(.hidden))
                    .scroll(to: .doneButton)
                    .tap(.doneButton)
            }
    }

    func test_app2AppOptionShouldBeHiddenIfPhonesRedirectIsTurnedOff() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        self.launchWithOfferForm()
            .scroll(to: .app2appCalls)
            .should(.app2appCalls, .exist)
            .scroll(to: .phonesRedirect, ofType: .switchCell, direction: .down, maxSwipes: 1)
            .focus { cell in
                cell
                    .should(.switch, .be(.on))
                    .tap(.switch)
                    .should(.switch, .be(.off))
            }
            .should(.app2appCalls, .be(.hidden))
    }

    func test_disableApp2App() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        var _draft: Auto_Api_Offer?

        // override handler from `openOfferForm`
        server.addMessageHandler("PUT /user/draft/CARS/\(draftID)") { (draft: Auto_Api_Offer) -> Auto_Api_DraftResponse in
            _draft = draft

            var response: Auto_Api_DraftResponse = .init(mockFile: "GET user_draft_partial_wizard")

            response.offer = draft

            return response
        }

        let publishExpectation = XCTestExpectation(description: "publish expectation")

        server.addMessageHandler("POST /user/draft/CARS/\(draftID)/publish") { () -> Auto_Api_DraftResponse in
            publishExpectation.fulfill()
            return .init()
        }
        self.launchWithOfferForm()
            .scroll(to: .app2appCalls, ofType: .switchCell)
            .focus { cell in
                cell
                    .should(.switch, .be(.on))
                    .tap(.switch)
                    .should(.switch, .be(.off))
            }
            .scroll(to: .activationButton)
            .tap(.activationButton)
            .wait(for: [publishExpectation], timeout: 5)

        Step("Проверяем, что app2app звонки выключены")

        guard let draft = _draft else {
            XCTFail("no draft")
            return
        }

        XCTAssertTrue(draft.additionalInfo.app2AppCallsDisabled, "app2app calls should be disabled")
    }
    
    func test_draftWithActivatedNDS() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID, nds: true)
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        Step("Проверяем, что при включённом ранее НДС, поле показано и включено, а его help открывается.")

        self.launchWithOfferForm()
            .scroll(to: .nds, ofType: .switchCell)
            .tap(.nds)
            .should(.ndsHelp, .exist)
            .tap()
            .focus(on: .nds, ofType: .switchCell, { cell in
                cell
                    .should(.switch, .be(.on))
                    .tap(.switch)
                    .should(.switch, .be(.off))
            })
    }

    func test_draftNdsByDescription() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID, description: "НДС")
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferDescriptionParseOptions(isNds: true)

        Step("Проверяем, что при выключенном НДС, но наличии его в описании, поле показано и выключено.")

        self.launchWithOfferForm()
            .scroll(to: .nds, ofType: .switchCell)
            .focus({ cell in
                cell
                    .should(.switch, .be(.off))
            })
    }

    func test_draftPanoramaReshoot() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: "1640897598478615376-2c0470c2", description: "НДС", processingOffer: true)
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)
            .mock_userOfferDescriptionParseOptions(isNds: false)

        Step("Проверяем появление меню пересъёмки для обрабатываемой панорамы")
        self.launchWithOfferForm()
            .tap(.panoramaButton)
            .should(.panoramaReshootMenu, .exist)
            .should(.panoramaReshootButton, .exist)
    }

    func test_disableChatsAndPublishOffer() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker.mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        let steps = launchWithOfferForm()
            .scroll(to: .disableChats, ofType: .switchCell)
            .focus { cell in
                cell
                    .tap(.switch)
                    .wait(for: 1)
                    .shouldEventBeReported("Настройка Отключить чаты. Включение. Нажатие")
                    .tap(.switch)
                    .wait(for: 1)
                    .shouldEventBeReported("Настройка Отключить чаты. Выключение. Нажатие")
            }

        let expectation = api.user.draft.category(.cars).offerId("1640897598478615376-2c0470c2")
            .put
            .expect { offer, _ in
                offer.seller.chatsEnabled ? .ok : .fail(reason: nil)
            }

        steps
            .scroll(to: .activationButton)
            .tap(.activationButton)
            .wait(for: [expectation], timeout: 5)
    }

    func test_pickNoPtsOption() {
        let draftID = "1640897598478615376-2c0470c2"

        mockOfferEditBase(draftID: draftID)
        mocker
            .mock_userOffer(id: draftID, status: .active, hasActivationWithAutoprolongation: false)

        launchWithOfferForm()
            .scroll(to: .pts)
            .focus(on: .pts) { pts in
                pts.tap()
            }
            .should(provider: .modalPicker, .exist)
            .focus { picker in
                picker.tap(.item("Нет ПТС"))
            }
            .should(provider: .offerEditScreen, .exist)
            .should(.ownersNumber, .be(.hidden))
            .scroll(to: .customCleared, ofType: .switchCell)
            .focus({ cell in
                cell
                    .should(.switch, .be(.on))
            })
    }
}
