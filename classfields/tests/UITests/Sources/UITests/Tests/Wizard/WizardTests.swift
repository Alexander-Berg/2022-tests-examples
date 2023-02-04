//
//  WizardTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/23/20.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRu AutoRuTradeIn
class WizardTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private let suiteName = SnapshotIdentifier.suiteName(from: #file)

    private static let draftId = "1640897598478615376-2c0470c2"

    override func setUp() {
        super.setUp()

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }
    
    func test_fullWizard_tradeInAfterVASTrap() {
        // Попап трейдина тестируется в тестах на форму

        launch()

        let steps = traverseWizard()

        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: Self.draftId, status: .active)

        let isAvailableExpectation = expectationForRequest { req in
            req.method == "POST" && req.uri == "/trade-in/is_available"
        }

        steps
            .should(provider: .vasTrapScreen, .exist)
            .focus { vasTrap in
                vasTrap
                    .scroll(to: .doneButton, maxSwipes: 20)
                    .tap(.doneButton)
            }

        wait(for: [isAvailableExpectation], timeout: 10.0)

        steps
            .should(provider: .tradeInPicker, .exist)
            .focus { tradeIn in
                tradeIn
                    .tap(.agreements)
                    .tap(.applyButton)
            }
            .should(provider: .tradeInPicker, .be(.hidden))
    }

    func test_fullWizard_tradeInBeforeVASTrap() {
        // Попап трейдина тестируется в тестах на форму

        launch(options: .init(launchType: .default, overrideAppSettings: ["showTradeInFormFirst": true]))

        let isAvailableExpectation = expectationForRequest { req in
            req.method == "POST" && req.uri == "/trade-in/is_available"
        }

        let steps = traverseWizard()

        wait(for: [isAvailableExpectation], timeout: 10.0)

        mocker
            .mock_tradeInIsAvailable(with: true)
            .mock_statsPredict()
            .mock_tradeInApply()
            .mock_userOffer(id: Self.draftId, status: .active)

        steps
            .should(provider: .tradeInPicker, .exist)
            .focus { tradeIn in
                tradeIn
                    .tap(.agreements)
                    .tap(.applyButton)
            }
            .should(provider: .vasTrapScreen, .exist)
    }

    func test_openVinRecognitionScreen() {
        mocker
            .mock_base()
            .mock_user()
            .mock_wizardDraftCars()
            .mock_wizardDraftCars(id: Self.draftId)
            .mock_wizardReferenceCatalogCars()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_wizardPUTEmptyDraftCars(id: Self.draftId)
            .mock_wizardUserAuthData()

        launch(options: .init(launchType: .default, overrideAppSettings: [
            "enableSkipButtonOnVinRecognitionScreen": true,
            "autoRecognizeVin": NSNull()]))

        mainSteps
            .openOffersTab()
            .tapAddOffer()
            .tapToCarsCategoryForWizard()
            .handleSystemAlertIfNeeded()
            .validateSnapshot(of: "VinRecognizerViewController", snapshotId: "VinRecognizerViewControllerScreen")
            .tapEnterManually()
            .exist(selector: "VIN / № кузова")
            .tapVinCameraButton()
            .validateSnapshot(of: "VinRecognizerViewController", snapshotId: "VinRecognizerViewControllerScreen")
    }

    func test_disableApp2appCalls() {
        launch()

        let activationExpectation = XCTestExpectation(description: "activation")
        server.interceptRequest("POST /user/offers/CARS/1640897598478615376-2c0470c2/activate") { _ in
            activationExpectation.fulfill()
        }

        var _draft: Auto_Api_Offer?

        server.interceptRequest("PUT /user/draft/CARS/1640897598478615376-2c0470c2") { (draft: Auto_Api_Offer) in
            _draft = draft
        }

        _ = traverseWizard(onPhonesStep: { picker in
            _ = picker
                .step("Проверяем, что при выключении защиты номера опция app2app скрывается") { $0
                    .scroll(to: .enableApp2AppCallCell)
                    .should(.enableApp2AppCallCell, .exist)
                    .scroll(to: .protectPhoneCell, ofType: .switchCell)
                    .focus { cell in
                        cell
                            .should(.switch, .be(.on))
                            .tap(.switch)
                            .should(.switch, .be(.off))
                    }
                    .should(.enableApp2AppCallCell, .be(.hidden))
                }
                .step("Включаем защиту номера обратно") { $0
                    .focus(on: .protectPhoneCell, ofType: .switchCell) { $0
                        .tap(.switch)
                    }
                }
                .scroll(to: .enableApp2AppCallCell, ofType: .switchCell)
                .focus { cell in
                    cell
                        .should(.switch, .be(.on))
                        .tap(.switch)
                        .should(.switch, .be(.off))
                }

            return false
        })

        wait(for: [activationExpectation], timeout: 5)

        guard let draft = _draft else {
            XCTFail("no draft")

            return
        }

        Step("Проверяем, что app2app звонки отключены")
        XCTAssertTrue(draft.additionalInfo.app2AppCallsDisabled, "app2app calls should be disabled")
    }

    func test_askMicForApp2AppIfMicStatusNotDetermined_granted() {
        var phonesStepCompleted = false

        let app2appExpectation = api.user.draft.category(.cars).offerId("1640897598478615376-2c0470c2").put
            .expect { draft, _ in
                guard phonesStepCompleted else { return .skip }

                return draft.additionalInfo.app2AppCallsDisabled
                ? .fail(reason: "app2app should be enabled")
                : .ok
            }

        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.App2AppAskMic())
        api.device.hello.post.ok(mock: experiments.toMockSource())

        launch(
            options: .init(
                environment: [
                    "app2AppFakeMicStatus": "notDetermined"
                ]
            )
        )

        _ = traverseWizard(onPhonesStep: { picker in
            _ = picker
                .scroll(to: .enableApp2AppCallCell, ofType: .switchCell)
                .focus { cell in
                    cell.should(.switch, .be(.on))
                }
                .should(provider: .wizardScreen, .exist)
                .focus { screen in
                    screen.tap(.continueButton)
                }
                .should(provider: .sellerCallPermissionIntroScreen, .exist)
                .focus { screen in
                    screen.tap(.allowButton)
                }
                .should(provider: .systemAlert, .exist)
                .focus { alert in
                    alert.tap(.button("OK"))
                }

            phonesStepCompleted = true
            return true
        })

        _ = XCTWaiter.wait(for: [app2appExpectation], timeout: 1)
    }

    func test_askMicForApp2AppIfMicStatusNotDetermined_denied() {
        var phonesStepCompleted = false

        let app2appExpectation = api.user.draft.category(.cars).offerId("1640897598478615376-2c0470c2").put
            .expect { draft, _ in
                guard phonesStepCompleted else { return .skip }

                return draft.additionalInfo.app2AppCallsDisabled
                ? .ok
                : .fail(reason: "app2app should be disabled")
            }

        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.App2AppAskMic())
        api.device.hello.post.ok(mock: experiments.toMockSource())

        launch(
            options: .init(
                environment: [
                    "app2AppFakeMicStatus": "notDetermined"
                ]
            )
        )

        _ = traverseWizard(onPhonesStep: { picker in
            _ = picker
                .scroll(to: .enableApp2AppCallCell, ofType: .switchCell)
                .focus { cell in
                    cell.should(.switch, .be(.on))
                }
                .should(provider: .wizardScreen, .exist)
                .focus { screen in
                    screen.tap(.continueButton)
                }
                .should(provider: .sellerCallPermissionIntroScreen, .exist)
                .focus { screen in
                    screen.tap(.allowButton)
                }
                .should(provider: .systemAlert, .exist)
                .focus { alert in
                    alert.tap(.button("Cancel"))
                }
                .base
                .scroll(to: .enableApp2AppCallCell, ofType: .switchCell)
                .focus { cell in
                    cell.should(.switch, .be(.off))
                }

            phonesStepCompleted = true
            return true
        })

        _ = XCTWaiter.wait(for: [app2appExpectation], timeout: 1)
    }

    func test_fullWizard() {
        fullWizardInternal()
    }
    
    // Тестировщики хотят два полных прогона для охвата разных выборов опций по кейсам (то есть дойти до финала с разными вариантами). Будет постепенно всё больше отличаться от базового по мере доработки всё новых случаев. Где можно, лишние действия будут убираться.
    func test_fullWizardAlternative() {
        fullWizardInternal(fasterVersion: true, isBroken: false)
    }

    private func fullWizardInternal(fasterVersion: Bool = false, isBroken: Bool = true) {
        launch(options: .init(launchType: .default, overrideAppSettings: [
            "autoRecognizeVin": NSNull()]))

        mocker
            .mock_base()
            .mock_user()
            .mock_wizardDraftCars()
            .mock_wizardDraftCars(id: Self.draftId)
            .mock_wizardReferenceCatalogCars()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_wizardPUTEmptyDraftCars(id: Self.draftId)
            .mock_wizardPhotoUpload()
            .mock_wizardUserAuthData()
            .mock_tradeInIsAvailable(with: false)
            .mock_userOfferDescriptionParseOptions(isNds: true)
            .mock_statsPredict()

        var steps: WizardSteps!

        Step("Открываем визард") {
            steps = mainSteps
                .openOffersTab()
                .tapAddOffer()
                .tapToCarsCategoryForWizard()
                .wait(for: 1)
                .shouldEventBeReported(
                    "Открыть визард добавления",
                    with: ["Категория": "Легковые", "Источник": "ЛК"]
                )
        }

        Step("Пропускаем алерт, если нужно") {
            steps.handleSystemAlertIfNeeded(allowButtons: ["OK"])
        }

        Step("Вводим и проверяем VIN") {
            steps = steps
                .tapEnterManually()
                .tapEnterVin()
                .typeInActiveField("qqqqqqqqqqqqqqq")
                .typeSpecialsInActiveField("more")
                .typeInActiveField("11111")// Тут суммарно вбивается больше, чем может быть в поле, проверяя автокоррекцию.
                .checkElementExistWithText("QQQQQQQQQQQQQQQ11")
                .tapSkip()
                .wait(for: 1)
        }

        Step("Вводим и проверяем госномер") {
            steps = steps
                .typeInActiveField("a")
                .typeSpecialsInActiveField("more")
                .typeInActiveField("555")
                .typeSpecialsInActiveField("more")
                .typeInActiveField("aa")
                .typeSpecialsInActiveField("more")
                .typeInActiveField("11111")// Пытаемся вбить больше, чем нужно.
                .wait(for: 1)

            validateSnapshots(suiteName: suiteName, accessibilityId: "app.views.gov_number", snapshotId: "test_GovNumber()")
        }

        Step("Проходим визард") {
            steps = steps
                .tapNext()
                .tapBMW()
                .tapBMW6()
                .tap2020()
                .tapRestyle()
                .tapBody()
                .tapEngine()
                .tapWheels()
                .tapTransmission()
                .tapModification()
                .tapColor()
                .tapPTS()
                .tapOwner()
                .tapSkip()
        }

        Step("Выбираем и проверяем фотки") {
            steps = steps
                .checkElementExistWithPartText("Удостоверьтесь, что\u{00a0}госномер")
                .tapAddPhoto()
                .handleSystemAlertIfNeeded()
                .addPhoto()
            if fasterVersion {
                steps = steps
                    .tapNext()
            } else {
                steps = steps
                    .checkElementExistWithPartText("Мы не\u{00a0}распознали")
                    .tapAddPhotoInList()
                    .addPhoto(index: 1)
                    .wait(for: 1)
                    .tapAddPhotoInList()
                    .addPhoto(index: 2)
                    .checkElementExistWithPartText("Мы не\u{00a0}распознали", not: true)
                    .checkElementExistWithText("Фото: 3/40")
                    .tapManualPhotoOrder()
                    .checkElementExistWithText("Задать порядок фотографий самостоятельно.")
                    .tapRemoveFirstPhotoInList()
                    .checkElementExistWithText("Фото: 2/40")
                    .tapNext()
                    .wait(for: 1)
                    .tapBackToPhoto()
                    .wait(for: 1)
                    .tapNext()
            }
        }

        let description = "Qwerty"
        let brokenText = "Требует ремонта"
        let id = "1640897598478615376-2c0470c2"
        Step("Проходим остальные поля") {
            steps = steps
                .checkDescriptionScreenshot(name: "wizard_description_empty_state")
                .checkDescriptionScreenContent()
                .typeInActiveField(description)
                .checkDescriptionScreenshot(name: "wizard_description_full_state")
                .tapNext()
                .typeInActiveField("10000000")
                .checkDistanceScreenContent(distance: "1000000", tapSwitch: isBroken)
                .tapNext()
                .tapClose()

            steps
                .tapContinue()
                .tapNext()
                .tapSkip()
                .chackNextButtonDontExist()
                .typeInActiveField("10000000500")// Снова вбиваем больше.
                .checkPureTextExist("100 000 005")
                .checkPriceScreenContent()
                .checkPureTextExist("861 000")
                .tapNext()
                .wait(for: 1)
        }

        api.user.offers.category(._unknown("all"))
            .get(parameters: [
                .page(1),
                .pageSize(10),
                .withDailyCounters(true)
            ])
            .ok(mock: .file("GET user_offers_all_external_Panoramas_no_poi", mutation: { model in
                var offer = model.offers[0]
                offer.id = id
                offer.description_p = description
                offer.state.condition = isBroken ? .broken : .ok
                model.offers = [offer]
            }))

        api.user.offers.category(.cars).offerID(id).edit
            .post
            .ok(mock: .file("POST user_offers_CARS_DRAFT_1101389279-dccb254c", mutation: { model in
                var offer = model.offer
                offer.id = id
                offer.description_p = description
                offer.state.condition = isBroken ? .broken : .ok
                model.offer = offer
            }))

        mocker
            .mock_wizardDraftCars(id: id, isPublished: true, description: description, isBroken: isBroken)
            .mock_activateUserOfferFrom(category: "cars", id: id)
            .mock_userOffer(id: id, description: description, isBroken: isBroken)
            .mock_userOfferDescriptionParseOptions(isNds: false)

        Step("Публикуем оффер") {
            steps = steps
                .publishNow()
                .tapFinish()
                .shouldEventBeReported(
                    "Разместить объявление - успех",
                    with: ["Категория": "cars",
                           "Платность": "free",
                           "draft_id": "1640897598478615376-2c0470c2",
                           "Источник": "визард"
                          ]
                )
        }

        steps
            .should(provider: .vasTrapScreen, .exist)
            .focus { vasTrap in
                vasTrap
                    .scroll(to: .doneButton, maxSwipes: 20)
                    .tap(.doneButton)
            }
        steps
            .should(provider: .userSaleListScreen, .exist)
            .focus({ userSaleScreen in
                userSaleScreen.openOffer(offerId: id)
                    .should(provider: .safeDealOnboardingPopup, .exist)
                    .focus { popup in
                        popup
                            .tap(.understand)
                    }
                    .scroll(to: .saleDescription)
            })
        steps
            .exist(selector: description)
        if isBroken {
            steps
                .exist(selector: brokenText)
        }
    }

    // MARK: - Private

    private func traverseWizard(
            onPhonesStep: (WizardPhonesPicker) -> Bool = { _ in false }
        ) -> WizardSteps {
            mocker
                .mock_base()
                .mock_user()
                .mock_wizardDraftCars()
                .mock_wizardDraftCars(id: Self.draftId)
                .mock_wizardReferenceCatalogCars()
                .mock_wizardReferenceCatalogCarsSuggest()
                .mock_wizardPUTEmptyDraftCars(id: Self.draftId)
                .mock_wizardPhotoUpload()
                .mock_wizardUserAuthData()

            var steps: WizardSteps!

            Step("Открываем визард") {
                steps = mainSteps
                    .openOffersTab()
                    .tapAddOffer()
                    .tapToCarsCategoryForWizard()
            }

            Step("Пропускаем алерт, если нужно") {
                steps.handleSystemAlertIfNeeded(allowButtons: ["OK"])
            }

            Step("Вводим VIN") {
                steps = steps
                    .tapSkip()
                    .wait(for: 1)
            }

            Step("Проходим визард") {
                steps = steps
                    .tapNext()
                    .tapBMW()
                    .tapBMW6()
                    .tap2020()
                    .tapRestyle()
                    .tapBody()
                    .tapEngine()
                    .tapWheels()
                    .tapTransmission()
                    .tapModification()
                    .tapColor()
                    .tapPTS()
                    .tapOwner()
                    .tapSkip()
            }

            Step("Выбираем фотки") {
                steps = steps
                    .tapAddPhoto()
                    .handleSystemAlertIfNeeded()
                    .addPhoto()
                    .tapNext()
            }

            Step("Проходим остальные поля") {
                steps = steps
                    .wait(for: 2)// Из-за подъёма клавиатуры может не сработать нажатие кнопки, если не подождать завершения анимации.
                    .tapSkip()
                    .typeInActiveField("1111")
                    .tapNext()
                    .tapClose()

                let performedNextStep = onPhonesStep(steps.as(WizardPhonesPicker.self))

                if !performedNextStep {
                    _ = steps.tapContinue()
                }

                steps
                    .tapNext()
                    .tapSkip()
                    .typeInActiveField("1111")
                    .tapNext()
                    .wait(for: 1)
            }

            mocker
                .mock_wizardDraftCars(id: "1640897598478615376-2c0470c2", isPublished: true)
                .mock_activateUserOfferFrom(category: "cars", id: "1640897598478615376-2c0470c2")

            Step("Публикуем оффер") {
                steps = steps
                    .publishNow()
                    .tapFinish()
            }

            return steps
    }
}
