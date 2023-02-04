import XCTest
import Foundation
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuGarageForm AutoRuGarageCard AutoRuGarageWizard
final class GarageDreamCardTests: GarageCardBaseTests {

    private let cardId = "1783017034"
    private let sharedCardId = "683682330"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openDreamCar() {
        openGarageCard()
            .should(.titleLabel, .exist)
            .should(.paramsHeader, .exist)
            .should(.cardPhotos, .exist)
            .should(.reviewsAndArticlesHeaderButton, .exist)
            .should(.offersCountHeader, .exist)
            .should(.ratingHeader, .exist)
            .should(.serviceHeader, .exist)
            .should(.cheapingHeader, .exist)
            .should(.creditHeader, .exist)
        
            .scroll(to: .offersCell)
            .should(.offersCell, .exist)
            .scroll(to: .feedItem(0))
            .should(.feedItem(0), .exist)
            .scroll(to: .ratingCell)
            .should(.ratingCell, .exist)
            .scroll(to: .specialOffers)
            .should(.specialOffers, .exist)
            .scroll(to: .cheapingCell)
            .should(.cheapingCell, .exist)
            .scroll(to: .proavtoPromoCell)
            .should(.proavtoPromoCell, .exist)
            .scroll(to: .creditCell)
            .should(.creditCell, .exist)
    }

    func test_openFeed() {
        openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
    }

    func test_openOffer() {
        openGarageCard()
            .tap(.offersCountHeader)
            .should(.offersCell, .exist)
            .validateSnapshot()
            .tap(.offerItem)
            .should(provider: .saleCardScreen, .exist)
    }

    func test_openAllOffers() {
        let searchExpecation = api.search.cars
            .post(parameters: .parameters([
                .context("listing"),
                .page(1),
                .pageSize(20),
                .sort("fresh_relevance_1-desc")
            ]))
            .expect { params, _ in
                let containExpecatedFilter = params.catalogFilter.contains { filter in
                    filter.mark == "BMW" &&
                    filter.model == "3ER" &&
                    filter.generation == 21398591
                }

                guard params.damageGroup == .notBeaten,
                      params.customsStateGroup == .cleared,
                      params.stateGroup == .all,
                      containExpecatedFilter
                else { return .fail(reason: nil) }

                return .ok
            }

        openGarageCard()
            .tap(.offersCountHeader)
            .should(.offersCell, .exist)
            .tap(.allOffersButton)
            .should(provider: .saleListScreen, .exist)
            .wait(for: [searchExpecation], timeout: 5)
    }

    func test_offersEmpty() {
        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_dream_card", mutation: { response in
                response.card.clearListingOffers()
            }))

        openGarageCard()
            .should(.offersCountHeader, .be(.hidden))
            .scroll(to: .ratingCell)
            .should(.offersCell, .be(.hidden))
    }
    
    func test_openRatingCell() {
        api.reviews.subject(.auto).features.category(.CARS).snippet
            .get(parameters: .wildcard)
            .ok(mock: .file("reviews_auto_features_CARS_snippet"))
        
        openGarageCard()
            .tap(.ratingHeader)
            .should(.ratingCell, .exist)
            .tap(.ratingFeture(0))
            .should(provider: .reviewFeatureScreen, .exist)
            .focus { reviewFeatureScreen in
                reviewFeatureScreen.tap(.snippet("От самой машины впечатление очень положительное до сих пор, двигатель резвый"))
            }
            .should(provider: .reviewCardSreen, .exist)
    }

    func test_openPromos() {
        openGarageCard()
            .tap(.serviceHeader)
            .should(.specialOffers, .exist)
    }

    func test_openCheaping() {
        openGarageCard()
            .tap(.cheapingHeader)
            .should(.cheapingCell, .exist)
    }

    func test_openProavto() {
        openGarageCard()
            .scroll(to: .proavtoPromoCell)
            .tap(.proavtoPromoCellButton)
            .should(provider: .carReportStandAloneScreen, .exist)
    }

    func test_openSharedCard() {
        launchWithSharedCard()
            .should(provider: .garageCardScreen, .exist)
            .focus { card in
                card
                    .should(.titleLabel, .exist)
                    .should(.paramsHeader, .exist)
                    .should(.cardPhotos, .exist)
                    .should(.offersCountHeader, .exist)
                    .should(.ratingHeader, .exist)
                    .should(.cheapingHeader, .exist)
                    .should(.creditHeader, .exist)
                    .should(.goToOwnGarageButton("Перейти в свой гараж"), .exist)
                    .tap(.paramsHeader)
            }
            .should(provider: .garageParametersListScreen, .exist)
            .focus { $0.tap(.closeButton) }
            .should(provider: .garageCardScreen, .exist)
            .focus { card in
                card
                    .scroll(to: .offersCell)
                    .should(.offersCell, .exist)
                    .scroll(to: .ratingCell)
                    .should(.ratingCell, .exist)
                    .scroll(to: .cheapingCell)
                    .should(.cheapingCell, .exist)
                    .scroll(to: .proavtoPromoCell)
                    .should(.proavtoPromoCell, .exist)
                    .scroll(to: .creditCell)
                    .should(.creditCell, .exist)
            }
    }

    func test_shareDreamCar() {
        openGarageCard()
            .should(.paramsHeader, .exist)
            .tap(.moreButton)
            .tap(.shareButton)
            .should(provider: .activityList, .exist)
            .focus { list in
                list
                    .tap(.copyButton)
                    .copyBufferContains("https://auto.ru/garage/dreamcar/share/\(sharedCardId)")
            }
    }

    func test_formEdit() {
        let updateCardExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { cardRequest, _ in
                guard cardRequest.card.vehicleInfo.carInfo.engineType == "GASOLINE" &&
                cardRequest.card.vehicleInfo.registrationRegion.id == 90 else {
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
                    .should(.field("BMW"), .exist)
                    .should(.field("3 серии"), .exist)
                    .should(.field("VII (G2x)"), .exist)
                    .should(.field("Седан"), .exist)
                    // Дополнительные поля
                    .scroll(to: .field("Комплектация"))
                    .should(.field("Регион регистрации"), .exist)
                    .should(.garageFormSuggest, .exist)
                    .should(.field("Цвет"), .exist)
                    .should(.field("Двигатель"), .exist)
                    .should(.field("Привод"), .exist)
                    .should(.field("Коробка передач"), .exist)
                    .should(.field("Модификация"), .exist)
                    .should(.field("Комплектация"), .exist)

                    .tap(.saveButton)
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { formScreen in
                formScreen
                    .scroll(to: .field("Двигатель"))
                    .tap(.field("Двигатель"))
            }
            .should(provider: .modalPicker, .exist)
            .focus { $0.tap(.item("Бензин")) }
            .should(provider: .garageFormScreen, .exist)
            .focus { formScreen in
                formScreen
                    .scroll(to: .field("Регион регистрации"))
                    .tap(.field("Регион регистрации"))
            }
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in
                screen
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus { $0.tap(.saveButton) }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [updateCardExpectation], timeout: 5)
    }

    func test_changeModel() {
        let updateCardExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { cardRequest, _ in
                guard cardRequest.card.vehicleInfo.carInfo.mark == "AUDI",
                      cardRequest.card.vehicleInfo.carInfo.model == "100",
                      cardRequest.card.vehicleInfo.carInfo.superGenID == 7879464
                else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        openGarageCard()
            .tap(.changeParamsButton)
            .should(provider: .garageFormScreen, .exist)
            .focus { $0.tap(.field("Марка")) }
            .should(provider: .wizardMarkPicker, .exist)
            .focus { $0.tap(.wizardItem("Audi")) }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.scroll(to: .saveButton)
                $0.tap(.saveButton)
            }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.scroll(to: .field("Модель"))
                $0.tap(.field("Модель"))
            }
            .should(provider: .wizardModelPicker, .exist)
            .focus { $0.tap(.wizardItem("100")) }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.scroll(to: .field("Поколение"))
                $0.tap(.field("Поколение"))
            }
            .should(provider: .wizardGenerationPicker, .exist)
            .focus { $0.tap(.wizardItem("с 1990 по 1994, IV (C4)")) }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.scroll(to: .saveButton)
                $0.tap(.saveButton)
            }
            .should(provider: .garageCardScreen, .exist)
            .wait(for: [updateCardExpectation], timeout: 5)
    }

    func test_openCreditBannerPopup() {
        openGarageCard()
            .tap(.creditHeader)
            .should(provider: .creditBannerPopup, .exist)
            .focus{
                $0.wait(for: 1)
                $0.tap(.dismissButton)
            }
            .should(provider: .garageCardScreen, .exist)
            .focus { card in
                card
                    .scroll(to: .creditCell)
                    .should(.creditCell, .exist)
                    .tap(.button("Заполнить заявку"))
            }
            .should(provider: .creditBannerPopup, .exist)
    }

    func test_taxDreamCarCellChangeRegion() {
        let updateCardRegionExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { req, _ in
                guard req.card.vehicleInfo.registrationRegion.id == 90 else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        openGarageCard()
            .tap(.taxHeader)
            .should(.taxCell, .exist)
            .should(.taxRegion, .exist)
            .tap(.taxRegion)
            .should(provider: .regionPickerScreen, .exist)
            .focus { screen in
                screen
                    .validateSnapshot(of: "Сан-Франциско")
                    .tap(.pickerItem("Сан-Франциско"))
            }
            .wait(for: [updateCardRegionExpectation], timeout: 5)
            .should(provider: .garageCardScreen, .exist)
    }

    func test_taxDreamCarCellChangeModification() {
        api.garage.user.card.cardId(cardId)
                    .put
                    .ok(mock: .file("garage_dream_card") { response in
                        response.card.clearListingOffers()
                        response.card.partnerPromos = []
                    })

        let updateCardModificationExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { req, _ in
                guard req.card.vehicleInfo.carInfo.techParam.id == 21592423 else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        openGarageCard()
            .tap(.taxHeader)
            .should(.taxCell, .exist)
            .should(.taxModification, .exist)
            .tap(.taxModification)
            .should(provider: .modalPicker, .exist)
            .focus { screen in
                screen
                    .validateSnapshot()
                    .tap(.item("318d 2.0 л, 150 л.с., Дизель"))
            }
            .wait(for: [updateCardModificationExpectation], timeout: 5)
            .should(.taxCell, ofType: .garageCardScreen, .exist)
    }

    func test_noTaxIfnotEnoughData() {
        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_dream_card", mutation: { response in
                response.card.tax.blockState.status = .notEnoughData
            }))

        self.launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .should(.taxHeader, .be(.hidden))
    }

    func test_taxDreamCarCellChangeModificationAndBodyType() {
        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_dream_card", mutation: { response in
                response.card.vehicleInfo.carInfo.clearBodyType()
            }))

        let updateCardBodyTypeExpectation = api.garage.user.card
            .cardId(cardId)
            .put
            .expect { req, _ in
                guard req.card.vehicleInfo.carInfo.bodyType == "SEDAN" else {
                    return .fail(reason: nil)
                }
                return .ok
            }

        openGarageCard()
            .tap(.taxHeader)
            .should(.taxCell, .exist)
            .should(.taxModification, .exist)
            .tap(.taxModification)
            .should(provider: .modalPicker, .exist)
            .focus { screen in
                screen
                    .validateSnapshot()
                    .tap(.item("Седан"))
            }
            .wait(for: [updateCardBodyTypeExpectation], timeout: 5)
            .should(provider: .modalPicker, .exist)
            .focus { screen in
                screen.tap(.item("318d 2.0 л, 150 л.с., Дизель"))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    func test_priceEstimationCell() {
        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_dream_card_price_stats"))

        let searchExpecation = api.search.cars
            .post(parameters: .parameters([
                .context("listing"),
                .page(1),
                .pageSize(20),
                .sort("fresh_relevance_1-desc")
            ]))
            .expect { params, _ in
                let containExpecatedFilter = params.catalogFilter.contains { filter in
                    filter.mark == "BMW" &&
                    filter.model == "3ER" &&
                    filter.generation == 21398591
                }

                guard params.damageGroup == .notBeaten,
                      params.customsStateGroup == .cleared,
                      params.stateGroup == .all,
                      params.priceFrom == 3016666,
                      params.priceTo == 3383332,
                      containExpecatedFilter
                else { return .fail(reason: nil) }

                return .ok
            }

        openGarageCard()
            .should(.priceHeaderButton, .exist)
            .tap(.priceHeaderButton)
            .should(.priceStatsCell, .exist)
            .should(.priceStatsSegment(0), .exist)
            .should(.priceStatsSegment(1), .exist)
            .should(.priceStatsSegment(2), .exist)
            .tap(.priceStatsSegment(1))
            .should(provider: .saleListScreen, .exist)
            .wait(for: [searchExpecation])
    }

    private func setupServer() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_dream_car_listing"))

        api.garage.user.card.cardId(cardId)
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_dream_card"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_form_suggest_BMW_3"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_mark_suggest"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI")]))
            .ok(mock: .file("garage_models_suggest"))

        api.reference.catalog.category(.cars).suggest
            .get(parameters: .parameters([.mark("AUDI"), .model("100")]))
            .ok(mock: .file("garage_generation_suggest"))

        api.garage.user.card.cardId(cardId)
            .put
            .ok(mock: .file("garage_dream_card"))

        mocker.mock_sharkCreditProductList(products: [.tinkoff_1], appProduct: [])
        mocker.mock_sharkCreditApplicationActiveEmpty()
        mocker.mock_sharkCreditProductCalculator()
        mocker.mock_searchCars {
            .with { response in
                response.status = .success
            }
        }
        mocker.mock_searchOfferLocatorCounters(type: .cars)
        server.addHandler("GET /lenta/get-feed *") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }
    }

    override func openGarageCard() -> GarageCardScreen_ {
        let options = AppLaunchOptions(
            overrideAppSettings: ["dreamCarEstimateEnabled": true]
        )

        return launch(on: .mainScreen, options: options) { screen in
            screen.toggle(to: .garage)
            return screen.should(provider: .garageCardScreen, .exist)
                .should(provider: .garageCardScreen, .exist)
        }
    }

    private func launchWithSharedCard() -> GarageCardScreen_ {
        mocker.mock_publicGarageDreamCard("\(sharedCardId)")

        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/garage/dreamcar/share/\(sharedCardId)"),
            overrideAppSettings: ["dreamCarEstimateEnabled": true]
        )

        return launch(on: .garageCardScreen, options: options)
    }
}

extension UIElementProvider {
    @discardableResult
    func copyBufferContains(_ value: String) -> Self {
        guard let pastboardString = UIPasteboard.general.string else {
            XCTFail("Пустой буффер обмена")
            return self
        }

        XCTAssert(pastboardString.contains(value))
        return self
    }
}
