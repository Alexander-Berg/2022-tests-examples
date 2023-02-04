//
//  EstimateTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 23.05.2022.
//

import AutoRuProtoModels
import XCTest
import Snapshots

final class EstimateTests: BaseTest, KeyboardManaging {
    private let offerID: String = "3019675754814267736-4e1a6193"
    private let govNumber = "a123aa77"
    private let govNumberRus = "А123АА77"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_addToGarageFromEstimation() {
        openSuperMenu()
            .tap(.price)
            .should(provider: .estimateFormScreen, .exist)
            .focus {
                $0
                    .scroll(to: .submitButton)
                    .tap(.submitButton)
            }
            .should(provider: .optionSelectPicker, .exist)
            .focus {
                $0
                    .tap(.option("option_0"))
            }
            .should(provider: .estimateFormScreen, .exist)
            .focus {
                $0
                    .tap(.submitButton)
            }
            .should(provider: .estimateResultScreen, .exist)
            .focus {
                $0
                    .tap(.addGarageButton)

            }
            .should(provider: .garageSearchScreen, .exist)
            .focus { screen in
                screen
                    .tap(.govNumberInputField)
                    .do { typeFromKeyboard(govNumber) }
                    .tap(.bottomButton(.search))
                    .tap(.bottomButton(.addToGarage))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    private func openSuperMenu() -> SuperMenuScreen {
        launchMain { screen in
            screen
                .toggle(to: \.favorites)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.superMenuButton) }
                .should(provider: .superMenuScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .startMock()

        api.garage.user.cards.post
            .ok(mock: .file("garage_cards_1955418404"))

        api.user.draft.category(.cars)
            .get
            .ok(mock: .file("user_draft_cars_get_ok"))

        api.user.draft.category(.cars).offerId(offerID)
            .put
            .ok(mock: .file("user_draft_cars_put_ok"))

        api.stats.predict
            .post
            .ok(mock: .file("stats_preditc_ok"))

        api.reference.catalog.category(.cars).suggest.get(parameters: .wildcard)
            .ok(mock: .file("catalog_CARS_suggest_ok"))

        api.garage.user.vehicleInfo.identifier(govNumberRus)
            .get
            .ok(mock: .file("garage_search_result_withoutCard"))

        api.garage.user.card.identifier.identifier(govNumberRus)
            .post
            .ok(mock: .file("garage_add_car_result"))
    }
}
