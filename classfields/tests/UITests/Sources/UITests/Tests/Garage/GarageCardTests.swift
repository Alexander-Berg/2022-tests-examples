import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf
import UIKit

/// @depends_on AutoRuGarageForm AutoRuGarageCard AutoRuGarageWizard
final class GarageCardTests: GarageCardBaseTests {
    func test_openCar() {
        let getCardExpectation = self.expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card/\(Self.garageCard.id)".lowercased()
            && req.method == "GET"
        }

        let reviewsRatingsExpectation = self.expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/reviews/auto/CARS/rating?mark=BMW&model=5ER&super_gen=0".lowercased()
            && req.method == "GET"
        }

        let featuresExpectation = self.expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/reviews/auto/features/CARS?mark=BMW&model=5ER".lowercased()
            && req.method == "GET"
        }

        self.launch()
        _ = self.openGarageCard()

        self.wait(
            for: [
                getCardExpectation,
                reviewsRatingsExpectation,
                featuresExpectation
            ],
            timeout: Self.requestTimeout
        )
    }

    func test_insertGovNumber() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked") {
                $0.card.cardTypeInfo.cardType = .exCar
            })

        api.garage.user.card.cardId("1955418404")
            .put
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        UIPasteboard.general.string = "O111oo11"
        launch()

        mainSteps
            .openTab(.garage)
            .as(GarageSteps.self)
            .should(provider: .garageCardScreen, .exist)
            .focus {
                $0.tap(.garageCars)
            }
            .should(provider: .garageListingScreen, .exist)
            .focus {
                $0.longTap(.gov_number_input)
                $0.tap("Вставить")
                $0.should(.addNextButton, .exist)
            }
    }

    func test_insertVin() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked") {
                $0.card.cardTypeInfo.cardType = .exCar
            })

        api.garage.user.card.cardId("1955418404")
            .put
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        UIPasteboard.general.string = "XTAF5015LE0773148"
        launch()

        mainSteps
            .openTab(.garage)
            .as(GarageSteps.self)
            .should(provider: .garageCardScreen, .exist)
            .focus {
                $0.tap(.garageCars)
            }
            .should(provider: .garageListingScreen, .exist)
            .focus {
                $0.tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
            .focus {
                $0.longTap(.vinInputField)
                $0.tap("Вставить")
                $0.should(.bottomButton(.search), .exist)
            }
    }

    func test_copyVINFromCard() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked") {
                var promoInfo = $0.card.partnerPromos[3].promocodeInfo
                promoInfo.promocode = ""
                promoInfo.poolID = "GARAGE"
                promoInfo.isPersonal = true
            })
        launch()
        openGarageCard()
            .longTap(.subtitleLabel)
            .tap("Скопировать")

        let VIN = "XTAF5015LE0773148"
        Step("Проверяем, что скопирован VIN '\(VIN)'") {
            let fromPasteboard = UIPasteboard.general.string ?? "<пусто>"
            XCTAssert(VIN == fromPasteboard, "В буфере обмена неверный VIN: \(fromPasteboard) вместо \(VIN)")
        }
    }

    func test_copyVINCurrentCarCharacteristic() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        api.garage.user.card.cardId("1955418404")
            .put
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .focus {
                $0.tap(.button("Изменить"))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.doubleTap(.vin, position: .init(x: 0, y: 0))
                $0.tap("Скопировать")
            }
    }

    func test_copyVINExCarCharacteristic() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked") {
                $0.card.cardTypeInfo.cardType = .exCar
            })

        api.garage.user.card.cardId("1955418404")
            .put
            .ok(mock: .file("garage_card_1955418404_manually_mocked") {
                $0.card.cardTypeInfo.cardType = .exCar
            })

        launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .focus {
                $0.tap(.button("Изменить"))
            }
            .should(provider: .garageFormScreen, .exist)
            .focus {
                $0.longTap(.vin, position: .init(x: 0, y: 0))
                $0.tap("Скопировать")
            }
    }

    func test_copyVINFromListing() {
        api.garage.user.card.cardId("1955418404")
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        api.garage.user.card.cardId("1955418404")
            .put
            .ok(mock: .file("garage_card_1955418404_manually_mocked"))

        launch()

        mainSteps
            .openTab(.garage)
            .as(GarageSteps.self)
            .should(provider: .garageCardScreen, .exist)
            .focus {
                $0.tap(.garageCars)
            }
            .should(provider: .garageListingScreen, .exist)
            .focus {
                $0.longTap(.vin)
                $0.tap("Скопировать")
            }
    }

    func test_shareGarageCard() {
        launch()
        openGarageCard()
            .tap(.moreButton)
            .tap(.shareButton)

        Step("Проверяем, что правильный заголовок сформировался в UIActivityViewController после нажатия на \"Поделиться\"") {
            let shareActivityTitle = "BMW 5 серии, 2018 в Гараже Авто.ру"
            app.staticTexts.containingText(shareActivityTitle).firstMatch.shouldExist()
        }
    }

    func test_partnerSuperPromoPopup() {
        launch()
        openGarageCard()
            .tap(.promo("Скидка до 50% на сезонный шиномонтаж"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.should(.logo, .exist)
                popUp.tap(.promoPopUpButton)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_partnerSuperPromoPromocodeNotPersonal() {
        launch()
        openGarageCard()
            .tap(.promo("Скидка 10% от Яндекс.Заправки по промокоду GARAGE"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.tap(.promoPopUpButton)
            }

        let promocode = "GARAGE"
        Step("Проверяем, что скопирован промокод '\(promocode)'") {
            let fromPasteboard = UIPasteboard.general.string ?? "<пусто>"
            XCTAssert(promocode == fromPasteboard, "В буфере обмена неверный промокод: \(fromPasteboard) вместо \(promocode)")
        }
    }
    
    func test_partnerSuperPromoPromocodePersonal() {
        api.garage.user.promos
            .get(parameters: [.page(1), .pageSize(10)])
            .ok(mock: .file("garage_card_promos", mutation: { promosResponse in
                var promoInfo = promosResponse.partnerPromos[4].promocodeInfo
                promoInfo.promocode = ""
                promoInfo.poolID = "GARAGE"
                promoInfo.isPersonal = true
                promosResponse.partnerPromos[4].promocodeInfo = promoInfo
                promosResponse.partnerPromos[4].promocode = ""
            }))
        
        api.garage.user.promos.acquirePromocode
            .post
            .ok(mock: .model { resp in
                resp.promocode = "GARAGE"
            })
        
        let acquirePromocodeExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased().contains("acquire_promocode")
        }
        
        launch()
        openGarageCard()
            .tap(.promo("Скидка 10% от Яндекс.Заправки по промокоду GARAGE"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.tap(.promoPopUpButton)
            }
            .wait(for: 1)

        let promocode = "GARAGE"
        Step("Проверяем, что скопирован промокод '\(promocode)'") {
            let fromPasteboard = UIPasteboard.general.string ?? "<пусто>"
            XCTAssert(promocode == fromPasteboard, "В буфере обмена неверный промокод: \(fromPasteboard) вместо \(promocode)")
        }
        
        wait(for: [acquirePromocodeExpectation], timeout: Self.requestTimeout)
    }

    func test_partnerSuperPromoJustURL() {
        launch()
        openGarageCard()
            .tap(.promo("Шины и диски"))
            .should(provider: .webViewPicker, .exist)
    }

    func test_partnerSuperPromoPopupDirectly() {
        launch()
        openGarageCard()
            .tap(.promo("Directly"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.tap(.promoPopUpButton)
            }
            .step("Проверяем, что открылся сафари") { _ in
                let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
                safari.shouldBeVisible()
            }
    }

    func test_partnerCommonPromoDirectlyURL() {
        launch()
        openGarageCard()
            .scroll(to: .specialOffers)
            .tap(.promo("Масла и автохимия"))
            .step("Проверяем, что открылся сафари") { _ in
                let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
                safari.shouldBeVisible()
            }
    }

    func test_partnerSuperPromoDirectlyURL() {
        launch()
        openGarageCard()
            .tap(.promo("Directly_no_popUp"))
            .step("Проверяем, что открылся сафари") { _ in
                let safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
                safari.shouldBeVisible()
            }
    }

    func test_partnerCommonPromoPopup() {
        launch()
        openGarageCard()
            .scroll(to: .specialOffers)
            .focus(on: .specialOffers) {
                $0.validateSnapshot()
            }
            .tap(.promo("Промо с popUp"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.tap(.promoPopUpButton)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_partnerPromoPopup_disclaimer() {
        launch()
        openGarageCard()
            .tap(.promo("Directly"))
            .should(provider: .garagePromoPopup, .exist)
            .focus { popUp in
                popUp.should(.disclaimer, .exist)
                    .focus { disclaimer in
                        disclaimer.rootElement.coordinate(withNormalizedOffset: .init(dx: 0.7, dy: 0.4)).tap()
                    }
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_addExtraParametersBannerShouldBeHiddenIfAllParametersAreAddedAndNoPrice() {
        addGetUserCardHandler(mutatingCard: { card in
            card.pricePredict.clearPredict()
        })

        self.launch()
        openGarageCard().shouldNotSeeAddExtraParametersBanner()
    }

    func test_addExtraParametersBannerWhenModificationIsEmptyAndNoPrice() {
        addGetUserCardHandler { card in
            card.vehicleInfo.carInfo.clearTechParam()
            card.vehicleInfo.carInfo.clearTechParamID()
            card.pricePredict.clearPredict()
        }

        self.launch()
        openGarageCard().shouldSeeAddExtraParametersBanner()
    }

    func test_addExtraParametersBannerWhenModificationIsEmptyAndPriceExists() {
        addGetUserCardHandler { card in
            card.vehicleInfo.carInfo.clearTechParam()
            card.vehicleInfo.carInfo.clearTechParamID()
            card.pricePredict.predict.market.price = 1_000_000
        }

        self.launch()
        openGarageCard().shouldNotSeeAddExtraParametersBanner()
    }

    func test_tapOnExtraParametersBannerShouldOpenForm() {
        addGetUserCardHandler { card in
            card.vehicleInfo.documents.clearPurchaseDate()
            card.pricePredict.clearPredict()
        }

        self.launch()
        self.openGarageCard()
            .tapOnContentsItem(description: "блок с предложением дозаполнить данные", title: "Рассчитайте стоимость автомобиля")
            .as(GarageFormSteps.self)
            .shouldSeeForm()
    }

    func test_noTaxIfnotEnoughData() {
        addGetUserCardHandler { card in
            card.tax.blockState.status = .notEnoughData
        }

        self.launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .scroll(to: .taxHeader, maxSwipes: 2)
            .should(.taxHeader, .be(.hidden))
    }

    func test_noTaxIfnotTaxInCard() {
        addGetUserCardHandler { card in
            card.clearTax()
        }

        self.launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .scroll(to: .taxCell, maxSwipes: 4)
            .should(.taxCell, .be(.hidden))
    }

    func test_haveTax() {
        addGetUserCardHandler { card in
            card.tax.blockState.status = .ok
        }

        self.launch()
        openGarageCard()
            .scroll(to: .taxHeader)
            .tap(.taxHeader)
            .should(.taxCell, .exist)
    }

    func test_priceStats() {
        addGetUserCardHandler { card in
            card.priceStats.priceDistribution = self.makePriceStats()
        }

        self.launch()
        mainSteps
            .openTab(.garage)
            .as(GarageCardSteps.self)
            .shouldSeePriceStats()
            .tapOnContentsAndCheckScroll(contentsTitle: "Средняя цена на Авто.ру", blockTitle: "Стоимость на Авто.ру")
            .checkPriceStats()
    }

    func test_tapPriceStats() {
        addGetUserCardHandler { card in
            card.priceStats.priceDistribution = self.makePriceStats()
        }

        self.launch()
        mainSteps
            .openTab(.garage)
            .as(GarageCardSteps.self)
            .shouldSeePriceStats()
            .tapOnPriceRow()
            .checkPriceStats()
    }

    func test_priceCheaping() {
        func price(_ value: Int32, age: Int32, diff: Int32) -> Auto_Api_Vin_AdditionalReportLayoutData.CheapeningGraphData.PriceWithAge {
            var priceWithAge = Auto_Api_Vin_AdditionalReportLayoutData.CheapeningGraphData.PriceWithAge()
            priceWithAge.price = .init(value)
            priceWithAge.age = .init(age)
            priceWithAge.pricePercentageDiff = .init(diff)
            return priceWithAge
        }

        addGetUserCardHandler { card in
            card.priceStats.cheapening.chartPoints = [
                price(2819194, age: 6, diff: 0),
                price(2505611, age: 7, diff: -11),
                price(2287759, age: 8, diff: -9)
            ]
            card.priceStats.cheapening.avgAnnualDiscountPercent = -9
        }

        self.launch()
        openGarageCard()
            .tapOnContentsAndCheckScroll(contentsTitle: "Потеря стоимости в год", blockTitle: "−9% в год")
            .checkCheapening()
    }

    func test_openExchangeListing() {
        addGetUserCardHandler { card in
            card.priceStats.priceDistribution = self.makePriceStats()
        }

        mocker.server.addHandler("GET /device/deeplink-parse?link=https://auto.ru/cars/all/?exchange_group=POSSIBLE&type=search") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_parse_deeplink_exchange_possible")
        }

        mocker.server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_search_cars_with_possible_exchange")
        }

        self.launch()
        openGarageCard()
            .tapOnContentsAndCheckScroll(contentsTitle: "Средняя цена на Авто.ру", blockTitle: "Стоимость на Авто.ру")
            .tapOnCheapeningBlockExchangeLink()
            .as(SaleCardListSteps.self)
            .checkIsVisible()
    }

    func test_openRecall() {
        mocker.server.addHandler("GET /garage/user/card/\(Self.garageCard.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard.id)_recalls", userAuthorized: true)
        }

        self.launch()
        let popupSnapshot = self.mainSteps
            .openTab(.garage)
            .as(GarageCardSteps.self)
            .shouldSeeRecalls()
            .tapRecalls()
            .tapCell(content: "Проникновение влаги в электронные компоненты")
            .snapshot()

        Snapshot.compareWithSnapshot(image: popupSnapshot)
    }

    func test_subscribeToRecalls() {
        let recallsId = "4338"

        let subscribeExpectation = api.recalls.userCards.cardId(recallsId)
            .subscription
            .put
            .expect()

        let unsubscribeExpectation = api.recalls.userCards.cardId(recallsId)
            .subscription
            .delete
            .expect()

        mocker.setForceLoginMode(.forceLoggedIn)
        mocker.mock_user(userEmailConfirmed: true)

        api.garage.user.card.cardId(Self.garageCard.id)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(Self.garageCard.id)_recalls"))

        api.recalls.userCards.cardId(recallsId)
            .subscription
            .delete
            .ok(mock: .model(.init()) { $0.status = .success })

        launch()
        openGarageCard()
            .shouldSeeRecalls()
            .tapRecalls()
            .should(provider: .garageCardScreen, .exist)
            .focus { screen in
                screen
                    .should(.recallsSwitch, .exist)
                    .tap(.recallsSwitch)
                    .wait(for: 1)
                    .tap(.recallsSwitch)
            }

        wait(for: [unsubscribeExpectation, subscribeExpectation], timeout: 5)
    }

    func test_passVerificationProvenOwner() {

        let uploadCarExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/media/upload_url?upload_data_type=CAR_PHOTO".lowercased()
            && req.method == "GET"
        }

        let uploadDocsExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/media/upload_url?upload_data_type=PROVEN_OWNER_DOCUMENTS_PHOTO".lowercased()
            && req.method == "GET"
        }
        uploadDocsExpectation.expectedFulfillmentCount = 3

        let putCardExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card/\(Self.garageCard.id)".lowercased()
            && req.method == "PUT"
        }

        putCardExpectation.expectedFulfillmentCount = 2

        launch()
        openGarageCard()
            .tap(.provenOwnerHeader)
            .should(.provenOwnerCell(.unverified), .exist)
            .tap(.passVerificationButton)
            .should(.provenOwnerPhotoController, .exist)
            .focus { $0.validateSnapshot() }
            .focus(on: .provenOwnerAddPhotoButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .scroll(to: .systemImage(1))
                    .tap(.systemImage(1))
                    .tap(.send)
            }
            .focus { _ in
                mainSteps.handleSystemAlertIfNeeded()
            }
            .focus(on: .provenOwnerContinueButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(.provenOwnerCell(.verifying), .exist)

        wait(
            for: [uploadCarExpectation, uploadDocsExpectation, putCardExpectation],
            timeout: Self.requestTimeout
        )
    }

    func test_retryUploadAndRetakePhotoProvenOnwer() {
        mocker.server.removedHandlers("PUT /garage/user/card/\(Self.garageCard.id)")
        mocker.server.addHandler("PUT /garage/user/card/\(Self.garageCard.id)") { _, _ in
            Response.badResponse(code: .externalServiceError)
        }

        let uploadCarExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/media/upload_url?upload_data_type=CAR_PHOTO".lowercased()
            && req.method == "GET"
        }
        uploadCarExpectation.expectedFulfillmentCount = 3

        let uploadDocsExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/media/upload_url?upload_data_type=PROVEN_OWNER_DOCUMENTS_PHOTO".lowercased()
            && req.method == "GET"
        }
        uploadDocsExpectation.expectedFulfillmentCount = 7

        let putCardExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/garage/user/card/\(Self.garageCard.id)".lowercased()
            && req.method == "PUT"
        }
        putCardExpectation.expectedFulfillmentCount = 3

        launch()
        openGarageCard()
            .tap(.provenOwnerHeader)
            .should(.provenOwnerCell(.unverified), .exist)
            .tap(.passVerificationButton)
            .should(.provenOwnerPhotoController, .exist)
            .focus(on: .provenOwnerAddPhotoButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .scroll(to: .systemImage(1))
                    .tap(.systemImage(1))
                    .tap(.send)
            }
            .should(.provenOwnerContinueButton, .exist)
            .focus(on: .provenOwnerContinueButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(.provenOwnerCell(.uploadFailed), .exist)
            .tap(.retakeProvenOwnerPhoto)
            .should(.provenOwnerPhotoController, .exist)
            .focus(on: .provenOwnerAddPhotoButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .scroll(to: .systemImage(1))
                    .tap(.systemImage(1))
                    .tap(.send)
            }
            .should(.provenOwnerContinueButton, .exist)
            .focus(on: .provenOwnerContinueButton) {
                $0.tap()
                mainSteps.handleSystemAlertIfNeeded()
                mainSteps.handleSystemAlertIfNeeded()
            }
            .should(.provenOwnerCell(.uploadFailed), .exist)
            .tap(.retryProvenOwnerUpload)
            .should(.supportButton, .exist)
            .tap(.supportButton)
            .should(.supportChat, .exist)

        wait(
            for: [uploadCarExpectation, uploadDocsExpectation, putCardExpectation],
            timeout: Self.requestTimeout
        )
    }

    func test_rejectedProvenOwner() {
        mocker.server.removedHandlers("GET /garage/user/card/\(Self.garageCard.id)")
        mocker.server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { response in
                response.card.id = Self.garageCard.id
                response.card.provenOwnerState.status = .failed
            }
        }

        launch()
        openGarageCard()
            .tap(.provenOwnerHeader)
            .should(.provenOwnerCell(.rejected), .exist)
            .should(.supportButton, .exist)
            .tap(.supportButton)
            .should(.supportChat, .exist)
    }

    func test_feed_openArticle() {
        launch()
        openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .tap(.feedItem(0))
            .should(provider: .webViewPicker, .exist)
    }

    func test_feed_openReview() {
        launch()
        openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .tap(.feedSegment(.reviews))
            .tap(.feedItem(0))
            .should(.reviewScreen, .exist)
    }

    func test_feed_loadMore() {
        let loadMoreExpectation = expectationForRequest { req -> Bool in
            req.uri.lowercased() == "/lenta/get-feed?content_amount=10&content_id=magazine_19530&source=ALL&user_id=user:47439675".lowercased()
            && req.method == "GET"
        }
        launch()
        let loadMore = openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .scroll(to: .loadMore)
            .tap(.loadMore)
            .scroll(to: .loadMore)
            .tap(.loadMore)

        wait(for: [loadMoreExpectation], timeout: Self.requestTimeout)

        loadMore
            .scroll(to: .feedItem(12))
    }

    func test_feed_rollUp() {
        mocker.server.addHandler("GET /lenta/get-feed?content_amount=10&source=ALL&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_getFeed_short", userAuthorized: true)
        }

        launch()
        openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .scroll(to: .loadMore)
            .tap(.loadMore)
            .scroll(to: .rollUp)
            .should(.rollUp, .exist)
            .tap(.rollUp)
            .should(.segmentControl, .exist)
            .scroll(to: .loadMore)
            .should(.loadMore, .exist)
    }

    func test_feed_emptyMagazineRequests() {
        mocker.server.addHandler("GET /lenta/get-feed?content_amount=10&source=ALL&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_review", userAuthorized: true)
        }

        mocker.server.addHandler("GET /lenta/get-feed?content_amount=10&source=MAGAZINE&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_empty", userAuthorized: true)
        }

        launch()
        openGarageCard()
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .tap(.feedSegment(.artciles))
            .should(.emptyFeed, .exist)
    }

    func test_feed_emptyReviews() {
        mocker.server.addHandler("GET /lenta/get-feed?content_amount=10&source=REVIEWS&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_empty", userAuthorized: true)
        }
        mocker.mock_makeReviewAuto()

        launch()
        openGarageCard()
            .scroll(to: .reviewsAndArticlesHeaderButton)
            .tap(.reviewsAndArticlesHeaderButton)
            .should(.feed, .exist)
            .tap(.feedSegment(.reviews))
            .should(.writeReview, .exist)
            .tap(.writeReview)
            .should(.reviewEditor, .exist)
    }

    func test_add_review() {
        let reviewExpectation = api.reviews.subject(.auto).post.expect()
        mocker.mock_makeReviewAuto()

        launch()
        openGarageCard()
            .scroll(to: .writeReviewHeader)
            .tap(.writeReviewHeader)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { editor in
                editor.tap(.closeButton)
            }
            .wait(for: [reviewExpectation])
    }

    func test_addInsuranceFromHeader() {
        launch()

        Step("Проверяем переходы к форме добавления страховки из боттомшита типа страховки")

        openGarageCard()
            .scroll(to: .addInsuranceHeader)
            .tap(.addInsuranceHeader)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.dismissButton)
            }
            .tap(.addInsuranceHeader)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.osago)
                    .should(provider: .navBar, .exist).focus { navBar in
                        navBar.should(.title, .match("ОСАГО"))
                        navBar.tap(.close)
                    }
            }
            .tap(.addInsuranceHeader)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.kasko)
                    .should(provider: .navBar, .exist).focus { navBar in
                        navBar.should(.title, .match("Каско"))
                        navBar.tap(.close)
                    }
            }
    }

    func test_insurancesBlock() {
        api.garage.user.card.cardId(Self.garageCard.id)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(Self.garageCard.id)_manually_mocked_filled") { response in
                response.card.insuranceInfo.insurances = self.makeInsurances()
            })

        launch()

        openGarageCard()
            .scroll(to: .insuranceHeader)
            .tap(.insuranceHeader)
            .should(.insurances, .exist)
            .focus(on: .insuranceRow(0), ofType: .insuranceRowCell) { row in
                row.should(.title, .contain("Другое"))
                row.should(.subtitle, .match("ЗАСТРАХУЙ БРАТУХУ"))
                row.should(.daysLeft, .match("10 000 дней"))
            }
            .focus(on: .insuranceRow(1), ofType: .insuranceRowCell) { row in
                row.should(.title, .contain("Каско"))
                row.should(.subtitle, .match("1234567890"))
                row.should(.daysLeft, .match("2 дня"))
            }
            .focus(on: .insuranceRow(2), ofType: .insuranceRowCell) { row in
                row.should(.title, .contain("ОСАГО"))
                row.should(.subtitle, .match("XXX 1234567890"))
                row.should(.daysLeft, .match("Истекает"))
            }
            .should(.insuranceRow(3), .be(.hidden))
            .tap(.insurancesMore)
            .focus(on: .insuranceRow(3), ofType: .insuranceRowCell) { row in
                row.should(.title, .contain("Другое"))
                row.should(.subtitle, .match("TINEK STRAHOVANIE"))
                row.should(.daysLeft, .match("Истёк"))
            }
            .tap(.insuranceAdd)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.should(.osago, .exist)
                menu.should(.kasko, .exist)
                menu.tap(.dismissButton)
            }
            .tap(.insuranceRow(0))
            .should(provider: .navBar, .exist).focus { navBar in
                navBar.should(.title, .match("Другой"))
                navBar.tap(.close)
            }
    }

    func test_cardOnSale() {
        api.garage.user.cards.post
            .ok(mock: .file("garage_cards_1955418404") { response in
                response.listing[0].offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.garage.user.card.cardId(Self.garageCard.id)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(Self.garageCard.id)_manually_mocked_filled") { response in
                response.card.offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.user.offers.category(.cars).offerID("123")
            .get
            .ok(mock: .model())

        launch()
        openGarageCard()
            .should(.onSaleLabel, .exist)
            .should(.showOnSaleOfferButton, .exist)
            .tap(.showOnSaleOfferButton)
            .should(provider: .userSaleCardScreen, .exist)
    }

    func test_cardOnSale_inactive() {
        api.garage.user.card.cardId(Self.garageCard.id)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(Self.garageCard.id)_manually_mocked_filled") { response in
                response.card.offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .inactive
                })
            })

        api.user.offers.category(.cars).offerID("123")
            .get
            .ok(mock: .model())

        launch()
        openGarageCard()
            .should(.onSaleLabel, .be(.hidden))
            .should(.showOnSaleOfferButton, .be(.hidden))
            .should(.sellButton, .exist)
            .tap(.sellButton)
            .should(provider: .userSaleCardScreen, .exist)
    }

    func test_cardOnSale_fromLabel() {
        api.garage.user.cards.post
            .ok(mock: .file("garage_cards_1955418404") { response in
                response.listing[0].offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.garage.user.card.cardId(Self.garageCard.id)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(Self.garageCard.id)_manually_mocked_filled") { response in
                response.card.offerInfo = .with({ offerInfo in
                    offerInfo.offerID = "123"
                    offerInfo.price = 1_000_000
                    offerInfo.status = .active
                })
            })

        api.user.offers.category(.cars).offerID("123")
            .get
            .ok(mock: .model())

        launch()
        openGarageCard()
            .should(.onSaleLabel, .exist)
            .should(.showOnSaleOfferButton, .exist)
            .tap(.onSaleLabel)
            .should(provider: .userSaleCardScreen, .exist)
    }

    func test_swipeBetweenCards() {
        api.garage.user.cards.post
            .ok(mock: .file("garage_cards_swipe_listing"))

        let cardIds = ["1346613894", "450232169", "1440172871", "1418287016", "1455667606", "1825623603", "1346613894", "1346613894", "1346613894"]
        cardIds.forEach { id in
            api.garage.user.card.cardId(id)
                .get(parameters: .wildcard)
                .ok(mock: .file("garage_card_1955418404_manually_mocked"))
        }

        let cardRequestExpectations = cardIds.map { id in
            api.garage.user.card.cardId(id).get(parameters: .wildcard).expect()
        }

        launch()
        openGarageCard()
            .should(provider: .garageCardScreen, .exist)
            .should(.pageControl(selectedIndex: 0), .exist)
            .focus {
                $0.validateSnapshot(snapshotId: "startSwipeDots")
            }
            .should(provider: .garageCardScreen, .exist)
            .focus { card in
                cardIds.enumerated().forEach { index, _ in
                    card
                        .should(.pageControl(selectedIndex: index), .exist)
                        .should(.cardPhotos, .exist)
                        .focus { photo in
                            photo.swipe(.left)
                        }
                }
            }
            .should(.pageControl(selectedIndex: 9), .exist)
            .focus {
                $0.validateSnapshot(snapshotId: "EndSwipeDots")
            }
            .wait(for: cardRequestExpectations)
    }

    func test_pickCarFromListing() {
        api.garage.user.cards.post
            .ok(mock: .file("garage_cards_swipe_listing"))

        let cardIds = ["1346613894", "450232169", "1440172871", "1418287016", "1455667606", "1825623603"]
        cardIds.forEach { id in
            api.garage.user.card.cardId(id)
                .get(parameters: .wildcard)
                .ok(mock: .file("garage_card_1955418404_manually_mocked"))
        }

        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .tap(.garageCars)
            .should(provider: .garageListingScreen, .exist)
            .focus { listing in
                listing
                    .tap(.carSnippet(2))
            }
            .should(provider: .garageCardScreen, .exist)
            .should(.pageControl(selectedIndex: 2), .exist)
        
    }

    // MARK: - Private

    private func addGetUserCardHandler(mutatingCard: @escaping (inout Auto_Api_Vin_Garage_Card) -> Void) {
        mocker.server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            guard var card = self.getFilledGarageCard()?.card else { return nil }
            mutatingCard(&card)
            var cardsResponse = Auto_Api_Vin_Garage_GetListingResponse()
            cardsResponse.status = .success
            cardsResponse.listing = [card]
            return Response.okResponse(message: cardsResponse, userAuthorized: true)
        }

        mocker.server.addHandler("GET /garage/user/card/\(Self.garageCard.id)") { (_, _) -> Response? in
            guard var response = self.getFilledGarageCard() else { return nil }
            mutatingCard(&response.card)
            return Response.okResponse(message: response, userAuthorized: true)
        }
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

    private func makePriceStats() -> Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData {
        func priceSegment(from: Int, to: Int, count: Int) -> Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData.PriceSegment {
            var segment = Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData.PriceSegment()
            segment.priceFrom = try! .init(jsonString: "\(from)")
            segment.priceTo = try! .init(jsonString: "\(to)")
            segment.count = try! .init(jsonString: "\(count)")
            return segment
        }

        var result = Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData()

        result.histogram = [
            priceSegment(from: 2250000, to: 2333333, count: 1),
            priceSegment(from: 2333333, to: 2416666, count: 0),
            priceSegment(from: 2416666, to: 2499999, count: 0),
            priceSegment(from: 2499999, to: 2583332, count: 0),
            priceSegment(from: 2583332, to: 2666665, count: 0),
            priceSegment(from: 2666665, to: 2749998, count: 1),
            priceSegment(from: 2749998, to: 2833331, count: 0),
            priceSegment(from: 2833331, to: 2916664, count: 0),
            priceSegment(from: 2916664, to: 3000000, count: 1)
        ]

        result.showSegments = [
            priceSegment(from: 2250000, to: 2499999, count: 1),
            priceSegment(from: 2499999, to: 2749998, count: 1),
            priceSegment(from: 2749998, to: 3000000, count: 1)
        ]

        result.predictedPrice = 2920000

        return result
    }

    private func makeInsurances() -> [Auto_Api_Vin_Garage_Insurance] {
        [
            .with { insurance in
                insurance.insuranceType = .osago
                insurance.isActual = true
                insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(60 * 60))
                insurance.serial = "XXX"
                insurance.number = "1234567890"
            },
            .with { insurance in
                insurance.insuranceType = .kasko
                insurance.isActual = true
                insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(60 * 60 * 24 * 2))
                insurance.number = "1234567890"
            },
            .with { insurance in
                insurance.insuranceType = .unknownInsurance
                insurance.isActual = true
                insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(60 * 60 * 24 * 10000))
                insurance.number = "ЗАСТРАХУЙ БРАТУХУ"
            },
            .with { insurance in
                insurance.insuranceType = .unknownInsurance
                insurance.isActual = false
                insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(-60 * 60 * 24))
                insurance.number = "TINEK STRAHOVANIE"
            }
        ]
    }
}

extension Mocker {
    @discardableResult
    func mock_partsScreenRequests() -> Self {
        server.addHandler("GET /autoparts/offer/search *") { _, _ in
            Response.okResponse(fileName: "garage_autoparts", userAuthorized: true)
        }

        server.addMessageHandler("GET /device/deeplink-parse *") { _, _ in
            Auto_Api_DeeplinkParseResponse.with { response in
                response.partsSearchData.searchParams = [Auto_Api_PartsSearchData.SearchParam.with({ param in
                    param.key = "categoryId"
                    param.value = "2022"
                })]
                response.status = .success
            }
        }

        server.addHandler("GET /search/CARS/breadcrumbs *") { _, _ in
            Response.okResponse(fileName: "garage_card_search", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    func mockGarageCard() -> Self {
        mock_getSession()
        mock_user()
        server.forceLoginMode = .forceLoggedIn
        // TODO: переделать на новое api моков https://st.yandex-team.ru/AUTORUAPPS-19336
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok")
        }

        server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_cards_1955418404", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/rating *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/1955418404") { (_, _) -> Response? in
            let resp = Auto_Api_Vin_Garage_GetCardResponse(mockFile: "garage_card_1955418404_manually_mocked")
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }

        server.addHandler("GET /autoparts/offer/search *") { (_, _) -> Response? in
            Response.notFoundResponse()
        }

        server.addHandler("GET /reviews/auto/123") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_review_123", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest *") { _, _ in
            Response.okResponse(fileName: "garage_form_suggest_BMW2", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&source=ALL&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&source=MAGAZINE&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&source=REVIEWS&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_review", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&content_id=magazine_19530&source=ALL&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&content_id=magazine_19530&source=MAGAZINE&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_magazine", userAuthorized: true)
        }

        server.addHandler("GET /lenta/get-feed?content_amount=10&content_id=review_383928924357153578&source=REVIEWS&user_id=user:47439675") { _, _ in
            Response.okResponse(fileName: "lenta_get-feed_review", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/media/upload_url *") { _, _ in
            Response.okResponse(fileName: "garage_proven_owner_uploadURL", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/media/upload_url *") { _, _ in
            let mockResponse = ["upload_url": "http://127.0.0.1:\(port)/upload?sign=123"]
            let data = try! JSONEncoder().encode(mockResponse)
            return Response.responseWithStatus(body: data)
        }

        server.addMessageHandler("PUT /garage/user/card/1955418404") { _, _ -> Message in
            Auto_Api_Vin_Garage_UpdateCardResponse.with { response in
                response.card.id = "1955418404"
                response.card.cardTypeInfo.cardType = .currentCar
                response.status = .success
            }
        }

        server.addHandler("POST /upload *") { _, _ in
            Response.okResponse(fileName: "garage_uploader_mock", userAuthorized: true)
        }

        server.addMessageHandler("GET /chat/room/tech-support *") { _, _ in
            Auto_Api_RoomListingResponse.with { response in
                response.status = .success
            }
        }

        server.addHandler("GET /geo/suggest *") { _, _ in
            Response.okResponse(fileName: "garage_geo_suggest", userAuthorized: true)
        }

        mock_partsScreenRequests()
        mock_garageCardPromos()
        return self
    }
}
