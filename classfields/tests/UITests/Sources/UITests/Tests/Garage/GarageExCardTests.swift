//
//  GarageExCardTests.swift
//  UITests
//
//  Created by Igor Shamrin on 21.01.2022.
//

import XCTest
import Foundation
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuGarageForm AutoRuGarageCard AutoRuGarageWizard
final class GarageExCardTests: GarageCardBaseTests {
    private let cardId = "1783017034"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_ex_car_listing"))

        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_ex_car_card"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_form_suggest_BMW_3"))

        api.garage.user.card.cardId(cardId)
            .put
            .ok(mock: .file("garage_ex_car_card"))

        server.addHandler("GET /lenta/get-feed *") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }
    }

    func test_openExCar() {
        server.addHandler("GET /ios/makeXmlForGarage?garage_id=\(cardId)") {
            Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForOffer")
        }

        openGarageCard()
            .should(.titleLabel, .exist)
            .should(.paramsHeader, .exist)
            .should(.cardPhotos, .exist)
            .should(.reviewsAndArticlesHeaderButton, .exist)
            .should(.ratingHeader, .exist)
            .should(.reportHeader, .exist)
            .tap(.reportHeader)
            .should(.reportPreviewCell, .exist)
            .should(.ratingCell, .exist)
            .scroll(to: .feedItem(0))
            .should(.feedItem(0), .exist)
    }

    func test_formEdit() {
        let updateCardExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { cardRequest, _ in
                guard cardRequest.card.vehicleInfo.carInfo.engineType == "DIESEL" else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        openGarageCard()
            .tap(.changeParamsButton)
            .should(provider: .garageFormScreen, .exist)
            .focus { formScreen in
                formScreen
                    // Обязательные поля
                    .should(.field("RUMKE8938EV028756"), .exist)
                    .tap(.field("RUMKE8938EV028756"))
                    .should(.field("Volkswagen"), .exist)
                    .tap(.field("Volkswagen"))
                    .should(.field("Golf"), .exist)
                    .tap(.field("Golf"))
                    .should(.field("2010"), .exist)
                    .tap(.field("2010"))
                    .validateSnapshot(snapshotId: #function + "_1")
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { formScreen in
                formScreen
                    .scroll(to: .saveButton)
                    .validateSnapshot(snapshotId: #function + "_2")
                    .tap(.field("Двигатель"))
            }
            .should(provider: .modalPicker, .exist)
            .focus { $0.tap(.item("Дизель")) }

            .should(provider: .garageFormScreen, .exist)
            .focus { form in
                form
                    .scroll(to: .saveButton)
                    .tap(.saveButton)
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [updateCardExpectation], timeout: 5)
    }

    override func openGarageCard() -> GarageCardScreen_ {
        return launch(on: .mainScreen) { screen in
            screen.toggle(to: .garage)
            return screen.should(provider: .garageCardScreen, .exist)
                .should(provider: .garageCardScreen, .exist)
        }
    }
}
