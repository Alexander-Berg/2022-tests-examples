import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRu
class DeeplinksTests: BaseTest {
    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .mock_user()
            .startMock()
    }
    
    func testListingByMarkRegion() {
        server.addHandler("GET /device/deeplink-parse?link=https://auto.ru/moskovskaya_oblast/cars/jaguar/all/&type=search") { (_, _) in
            return Response.okResponse(fileName: "parse_ok")
        }

        let listingRequestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                if let data = request.messageBody, let response = try? Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: data) {
                    if response.catalogFilter.first?.mark == "JAGUAR" {
                        return true
                    }
                }
            }
            return false
        }

        launch(link: "https://auto.ru/moskovskaya_oblast/cars/jaguar/all/")
        wait(for: [listingRequestExpectation], timeout: 10)
    }

    func testAddOffer() {
        api.user.draft.category(.cars)
            .get
            .ok(
                mock: .file("draft_cars_ok") { model in
                    model.offer.state.stsUploadURL = "https://uploader.vertis.yandex.net:443/mock"
                })

        api.user.draft.category(.cars).offerId("8205248092971468456-9cf5a13f")
            .get
            .ok(
                mock: .file("draft_cars_ok") { model in
                    model.offer.state.stsUploadURL = "https://uploader.vertis.yandex.net:443/mock"
                })

        api.reference.catalog.category(.cars).suggest
            .get(parameters: [])
            .ok(mock: .file("catalog_CARS_suggest_ok"))

        api.user.draft.category(.cars).offerId("8205248092971468456-9cf5a13f")
            .put
            .ok(
                mock: .file("user_draft_CARS_8205248092971468456-9cf5a13f_ok") { model in
                    model.offer.state.stsUploadURL = "https://uploader.vertis.yandex.net:443/mock"
                })

        launch(link: "https://auto.ru/add/")
        let vinText = app.staticTexts.containingText("VIN").firstMatch

        XCTAssertTrue(vinText.waitForExistence(timeout: 3))
    }

    func testOpenUserOffer() {
        mocker.setForceLoginMode(.forceLoggedIn)
        let offerId = "1098105416-543819ea"

        server.addHandler("GET /user/offers/CARS/\(offerId) *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_user_offers_CARS_1098105416-543819ea_ok", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reports_test_user_offers_all_ok", userAuthorized: true)
        }

        launch(link: "https://m.auto.ru/user/offer/cars/1098105416-543819ea")
        let steps = SaleCardSteps(context: self)
        _ = steps.checkScreenLoaded()
    }

    func testLoginOpenUserOffer() {
        server.forceLoginMode = .forceLoggedOut
        launch(link: "https://m.auto.ru/user/offer/cars/1098105416-543819ea")
        app.staticTexts.matching(identifier: "Войдите, чтобы продолжить").firstMatch.shouldExist(timeout: 10)
    }

    func test_reviewFromOffer_new() {
        mocker.setForceLoginMode(.forceLoggedIn)
        let category = "cars"
        let offerId = "1"
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            return profile
        }()

        server.addHandler("GET /user/reviews *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_reviews_ok", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/\(category.uppercased())/offer/\(offerId)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "review_fromOffer_ok", userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=true") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        launch(link: "https://auto.ru/\(category)/reviews/add/\(offerId)/")
        app.staticTexts.matching(identifier: "Jeep Grand Cherokee").firstMatch.shouldExist(timeout: 10)
        app.staticTexts.matching(identifier: "Заполнено 9 из 13").firstMatch.shouldExist(timeout: 10)
    }

    func test_reviewFromOffer_alreadyHaveThisReview() {
        mocker.setForceLoginMode(.forceLoggedIn)
        let category = "cars"
        let offerId = "1"
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            return profile
        }()

        server.addHandler("GET /user/reviews *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_reviews_with_1_ok", userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=true") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        launch(link: "https://auto.ru/\(category)/reviews/add/\(offerId)/")
        app.staticTexts.matching(identifier: "Jeep Grand Cherokee").firstMatch.shouldExist(timeout: 10)
        app.staticTexts.matching(identifier: "Заполнено 9 из 13").firstMatch.shouldExist(timeout: 10)
    }

    func test_openProfile() {
        mocker.setForceLoginMode(.forceLoggedIn)
        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            profile.user.profile.autoru.fullName = "Test Name"
            return profile
        }()

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        launch(link: "https://auto.ru/my/profile")
        app.staticTexts.matching(identifier: "Test Name").firstMatch.shouldExist(timeout: 10)
    }

    func test_openLogin() {
        server.forceLoginMode = .preservingResponseState
        launch(link: "https://auto.ru/my/profile")
        app.staticTexts.matching(identifier: "Войдите, чтобы продолжить").firstMatch.shouldExist(timeout: 10)
    }

    func test_openDealerDeeplink() {
        mocker.setForceLoginMode(.preservingResponseState)

        let deeplink = "https://auto.ru/diler/cars/new/zvezda_stolici_kashirka_moskva_mercedes/mercedes/c_klasse/?year_from=2019&displacement_from=2000"

        server.addHandler("GET /device/deeplink-parse *") { request, _ in
            Response.okResponse(fileName: "zvezda_stolici_kashirka_moskva_mercedes_parse_deeplink")
        }

        server.addHandler("GET /salon/zvezda_stolici_kashirka_moskva_mercedes") { request, _ -> Response? in
            Response.okResponse(fileName: "zvezda_stolici_kashirka_moskva_mercedes_salon")
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=1&sort=fresh_relevance_1-desc") { request, _ in
            Response.okResponse(fileName: "first_new_car_from_dealer")
        }

        server.addHandler("POST /search/cars?context=listing&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc") { request, _ in
            Response.okResponse(fileName: "20_new_mercedes")
        }

        launch(link: deeplink)

        app.staticTexts.matching(identifier: "Звезда Столицы Mercedes-Benz Каширка").firstMatch.shouldExist(timeout: 1)

        DealerCardSteps(context: self).tapOnBackButton()

        app.staticTexts.matching(identifier: "5 346 предложений").firstMatch.shouldExist(timeout: 1)
        app.staticTexts.matching(identifier: "Mercedes-Benz G-Класс AMG II (W463)").firstMatch.shouldExist(timeout: 1)

        XCTAssertEqual("Новые", app.descendants(matching: .any).matching(identifier: "segmentControl").firstMatch.value as? String)
    }

    func test_promocodesDeeplinkUnauthorizedRequiresLogin() {
        let deeplink = "https://m.auto.ru/my/promo-codes/"

        mocker.setForceLoginMode(.forceLoggedOut)

        launch(link: deeplink)

        app.staticTexts.matching(identifier: "Войдите, чтобы продолжить").firstMatch.shouldExist(timeout: 10)
    }

    func test_promocodesDeeplinkAuthorizedShowsScreenSuccessfully() {
        let deeplink = "https://m.auto.ru/my/promo-codes/"

        mocker.setForceLoginMode(.forceLoggedIn)

        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            return profile
        }()

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        launch(link: deeplink)

        app.staticTexts.matching(identifier: "Кошелёк и промокоды").firstMatch.shouldExist(timeout: 1)
        app.descendants(matching: .any).matching(identifier: "activate_promocode_input").firstMatch.shouldExist(timeout: 1)
        app.descendants(matching: .any).matching(identifier: "activate_promocode_button").firstMatch.shouldExist(timeout: 1)
    }

    func test_promocodeEntryDeeplinkUnauthorizedRequiresLogin() {
        let deeplink = "https://m.auto.ru/my/promo-codes/apply/OLOLOSH"

        mocker.setForceLoginMode(.forceLoggedOut)

        launch(link: deeplink)

        app.staticTexts.matching(identifier: "Войдите, чтобы продолжить").firstMatch.shouldExist(timeout: 10)
    }

    func test_promocodeEntryDeeplinkAuthorizedShowsScreenSuccessfully() {
        let deeplink = "https://m.auto.ru/my/promo-codes/apply/OLOLOSH"

        mocker.setForceLoginMode(.forceLoggedIn)

        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            return profile
        }()

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        launch(link: deeplink)

        app.staticTexts.matching(identifier: "Кошелёк и промокоды").firstMatch.shouldExist(timeout: 1)
        app.descendants(matching: .any).matching(identifier: "activate_promocode_input").firstMatch.shouldExist(timeout: 1)
        app.descendants(matching: .any).matching(identifier: "activate_promocode_button").firstMatch.shouldExist(timeout: 1)
        app.descendants(matching: .any).matching(identifier: "OLOLOSH").firstMatch.shouldExist(timeout: 1)
    }

    func test_openCreditDeeplink() {
        mocker.stopMock()

        SharkMocker(server: server)
            .baseMock(offerId: "1098252972-99d8c274")
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        let deeplink = "https://auto.ru/my/credits/"
        launch(link: deeplink)
        app.staticTexts.matching(identifier: "Заполнить заявку").firstMatch.shouldExist()
    }

    func test_openGarageLanding() {
        api.garage.user.cards.post
            .ok(mock: .model(.init(), mutation: { model in
                model.status = .success
            }))
        let link = "autoru://app/garage/landing"

        mocker.setForceLoginMode(.forceLoggedIn)

        launch(
            options: .init(
                launchType: .deeplink(link),
                launchAction: .none
            )
        )

        shouldElementExist("garage_landing")
    }

    func test_openGarageLogout() {
        let link = "autoru://app/garage"

        mocker.setForceLoginMode(.forceLoggedOut)

        launch(
            options: .init(
                launchType: .deeplink(link),
                launchAction: .none
            )
        )

        shouldElementExist("garage_landing")
    }

    func test_openGarageLoginHasNotCar() {
        let link = "autoru://app/garage"

        mocker.setForceLoginMode(.forceLoggedIn)

        api.garage.user.cards
            .post
            .ok(mock: .model(.init()) { model in
                model.status = .success
            })

        launch(
            options: .init(
                launchType: .deeplink(link),
                launchAction: .none
            )
        )

        shouldElementExist("garage_landing")
    }

    func test_openGarageLoginHasCars() {
        let link = "autoru://app/garage"

        mocker.setForceLoginMode(.forceLoggedIn)

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_10"))

        launch(link: link)
        shouldElementExist("garage_card")
        shouldElementExist("XTT316300F1027573")
    }

    func test_openGarageCardWithId() {
        let link = "autoru://app/garage/458447563"

        mocker.setForceLoginMode(.forceLoggedIn)

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_10"))

        launch(link: link)
        shouldElementExist("garage_card")
        shouldElementExist("XTT316300F1027573")
    }

    func test_openGarageCardWithId_noAuth() {
        mocker.setForceLoginMode(.forceLoggedOut)

        let link = "autoru://app/garage/458447563"

        launch(link: link)
        shouldElementExist("garage_landing")
    }

    func test_openGarageCardWithUnknownId() {
        let link = "autoru://app/garage/00000000"

        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_10"))

        mocker.setForceLoginMode(.forceLoggedIn)

        launch(link: link)
        shouldElementExist("garage_card")
    }

    func test_openGarageLandingWithPromo_LogOut() {
        mocker.mock_garagePromos(targetPromoIds: ["renins_kasko_xl"], fileNames: ["garage_landing"])
        mocker.setForceLoginMode(.forceLoggedOut)

        launch(link: "autoru://app/garage/landing?promo=renins_kasko_xl")

        GarageLandingScreen_(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.promoPopUpButton) }
            .should(provider: .loginScreen, .exist)
    }

    func test_openGarageLandingWithPromo_LogIn() {
        api.garage
            .user
            .cards
            .post
            .ok(mock: .file("garage_cards_empty"))

        mocker.mock_garagePromos(targetPromoIds: ["renins_kasko_xl"], fileNames: ["garage_landing"])
        mocker.setForceLoginMode(.forceLoggedIn)

        launch(link: "autoru://app/garage/landing?promo=renins_kasko_xl")

        GarageLandingScreen_(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.promoPopUpButton) }
            .should(provider: .garageAddCarScreen, .exist)
    }

    func test_openGarageCardWithPromo() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_garagePromos()

        api.garage
            .user
            .cards
            .post
            .ok(mock: .file("garage_listing_with_promos"))

        api.garage
            .user
            .card
            .cardId("1995572707")
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_audi_100_newPromo"))

        let getCardExpectation = api.garage
            .user
            .card
            .cardId("1995572707")
            .get(parameters: .wildcard)
            .expect()

        launch(link: "autoru://app/garage/landing?promo=yandex_market")

        GarageCardScreen_(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)

        wait(for: [getCardExpectation], timeout: 5)
    }

    func test_openAllPromos_LogOut() {
        let targetPromoIds = ["yandex_market"]

        mocker
            .setForceLoginMode(.forceLoggedOut)
            .mock_garagePromos(targetPromoIds: targetPromoIds)

        let firstPageExpectation = api.garage.user.promos
            .get(parameters: [.page(1), .pageSize(10), .targetPromoIds(targetPromoIds)])
            .expect()

        let secondPageExpectation = api.garage.user.promos
            .get(parameters: [.page(2), .pageSize(10), .targetPromoIds(targetPromoIds)])
            .expect()

        launch(options: .init(launchType: .deeplink("autoru://app/garage/promo/all?promo=yandex_market"), launchAction: .none))

        GarageAllPromosScreen(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.closeButton) }
            .should(provider: .garageAllPromosScreen, .exist)
            .focus { promosScreen in
                promosScreen
                    .scroll(to: .commonPromo(title: "-20% на карты помощи на дороге от РАМК"), maxSwipes: 15)
                    .should(.commonPromo(title: "-20% на карты помощи на дороге от РАМК"), .exist)
                    .scroll(to: .bigPromo(title: "Автотовары со скидкой от Я.Маркета"), direction: .down, maxSwipes: 15)
                    .tap(.bigPromo(title: "Автотовары со скидкой от Я.Маркета"))

            }
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.promoPopUpButton) }
            .should(provider: .loginScreen, .exist)

        wait(for: [firstPageExpectation, secondPageExpectation], timeout: 3)
    }

    func test_openAllPromos_LogIn() {
        let targetPromoIds = ["yandex_market"]

        api.garage
            .user
            .cards
            .post
            .ok(mock: .file("garage_listing_with_promos"))

        api.garage
            .user
            .card
            .cardId("1995572707")
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_audi_100_newPromo"))

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_garagePromos(targetPromoIds: targetPromoIds)

        launch(link: "autoru://app/garage/promo/all?promo=yandex_market")

        GarageAllPromosScreen(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.crossButton)
            .should(provider: .garageCardScreen, .exist)
    }

    func test_openAllPromos_emptyGarage() {
        let targetPromoIds = ["yandex_market"]

        api.garage
            .user
            .cards
            .post
            .ok(mock: .file("garage_cards_empty"))

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_garagePromos(targetPromoIds: targetPromoIds)

        launch(options: .init(launchType: .deeplink("autoru://app/garage/promo/all?promo=yandex_market"), launchAction: .none))

        GarageAllPromosScreen(context: self).should(.exist)
            .should(provider: .garagePromoPopup, .exist)
            .focus { $0.tap(.promoPopUpButton) }
            .should(provider: .garageAddCarScreen, .exist)
    }

    func test_openAllPromos_EmptyPromos() {
        mocker
            .setForceLoginMode(.forceLoggedIn)

        api.garage
            .user
            .cards
            .post
            .ok(mock: .file("garage_listing_with_promos"))

        api.garage
            .user
            .card
            .cardId("1995572707")
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_audi_100_newPromo"))

        launch(link: "autoru://app/garage/promo/all?promo=yandex_market")
        GarageAllPromosScreen(context: self).should(.exist)
            .should(provider: .garageAllPromosScreen, .exist)
            .validateSnapshot()
            .focus { promosScreen in
                promosScreen
                    .should(.bottomButton(.goBack), .exist)
                    .tap(.bottomButton(.goBack))
            }
            .should(provider: .garageCardScreen, .exist)
    }

    private func shouldElementExist(_ element: String) {
        app.descendants(matching: .any)
            .matching(identifier: element)
            .firstMatch
            .shouldExist()
    }
}
