//
//  GarageDreamCarTests.swift
//  Tests
//
//  Created by Igor Shamrin on 15.11.2021.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuGarageForm AutoRuGarageCard AutoRuGarageWizard
final class GarageAddDreamCarTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_addDreamCar() {
        let createCardExpectation = api.garage.user.card
            .post
            .expect { req, _ in
                let isCorrectBody: Bool = {
                    return
                        req.card.vehicleInfo.carInfo.mark == "AUDI" &&
                        req.card.vehicleInfo.carInfo.model == "100" &&
                        req.card.vehicleInfo.carInfo.superGenID == 7879464 &&
                        req.card.vehicleInfo.carInfo.bodyType == "SEDAN"
                }()

                return isCorrectBody ? .ok : .fail(reason: "incorrect garage card")
            }

        openAddCarScreen()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen.tap(.addDreamCarButton)
            }
            .should(provider: .wizardMarkPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Audi"))
            }
            .should(provider: .wizardModelPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("100"))
            }
            .should(provider: .wizardGenerationPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("с 1990 по 1994, IV (C4)"))
            }
            .should(provider: .wizardBodyTypePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Седан"))
            }
            .wait(for: 1)
            .should(provider: .garageCardScreen, .exist)

            .wait(
                for: [createCardExpectation],
                   timeout: 10
            )
    }

    func test_addDreamCarWithAutoBodyType() {
        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI"), .model("100"), ._unknown("super_gen", "7879464")]))
            .ok(mock: .file("garage_bodyType_suggest") {
                $0.carSuggest.bodyTypes.removeLast()
            })
        
        let createCardExpectation = api.garage.user.card
            .post
            .expect { req, _ in
                let isCorrectBody: Bool = {
                    return
                        req.card.vehicleInfo.carInfo.mark == "AUDI" &&
                        req.card.vehicleInfo.carInfo.model == "100" &&
                        req.card.vehicleInfo.carInfo.superGenID == 7879464
                }()

                return isCorrectBody ? .ok : .fail(reason: "incorrect garage card")
            }

        openAddCarScreen()
            .should(provider: .garageAddCarScreen, .exist)
            .focus { screen in
                screen.tap(.addDreamCarButton)
            }
            .should(provider: .wizardMarkPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Audi"))
            }
            .should(provider: .wizardModelPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("100"))
            }
            .should(provider: .wizardGenerationPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("с 1990 по 1994, IV (C4)"))
            }
            .should(provider: .garageCardScreen, .exist)

            .wait(
                for: [createCardExpectation],
                   timeout: 10
            )
    }

    private func openAddCarScreen() -> GarageLandingScreen_ {
        return launch(on: .mainScreen) { screen in
            screen.toggle(to: .garage)
            return screen.should(provider: .garageLanding, .exist)
                .focus { garage in
                    garage
                        .tapOnAddCarButton()
                }
        }
    }

    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mockGarageCard()
            .startMock()

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_empty"))

        api.garage.user.card
            .post
            .ok(mock: .file("garage_dream_car_response"))

        api.garage.user.card.cardId("1906310423")
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_mark_suggest"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI")]))
            .ok(mock: .file("garage_models_suggest"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI"), .model("100")]))
            .ok(mock: .file("garage_generation_suggest"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI"), .model("100"), ._unknown("super_gen", "7879464")]))
            .ok(mock: .file("garage_bodyType_suggest"))
    }
}
