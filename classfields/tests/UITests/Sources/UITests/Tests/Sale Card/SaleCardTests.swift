//
//  SaleCardTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 25.05.2020.
//

import XCTest
import AutoRuProtoModels
import Snapshots

/// @depends_on AutoRuSaleCard
final class SaleCardTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)
    lazy var mainSteps = MainSteps(context: self)

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["DRIVE_MOCK_PROMO"] = "drive_tests_promo"
        return env
    }

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: -

    func test_metric_show() {
        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1092222570-3203c7f6")))
            .shouldEventBeReported(
                "Просмотр объявления",
                with: ["категория": "Легковые", "Продавец": "Частник", "С транспортным налогом": 38400, "Источник": "Неизвестно", "Состояние": "Новая", "Тег": "Не_растаможен"]
            )
    }

    func test_ptsAndControllWeelAndCustomsStateShoudBeBold() {
        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1092222570-3203c7f6")))
            .scrollTo("CharacteristicBlock", windowInsets: .init(top: 0, left: 0, bottom: 56, right: 0))
        validateSnapshots(accessibilityId: "CharacteristicBlock", snapshotId: "CharacteristicBlockWithBold")
    }

    func test_NoPtsAndControllWeelAndCustomsState() {
        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scrollTo("CharacteristicBlock")
        validateSnapshots(accessibilityId: "CharacteristicBlock", snapshotId: "CharacteristicBlockWithoutBold")
    }

    func test_tradeInBlock() {
        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scrollTo("TradeInBlock")
        validateSnapshots(accessibilityId: "TradeInBlock", snapshotId: "TradeInBlock")
    }

    func test_warningBlockIcons() {

        addDefaultHistoryItems()

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1092222570-3203c7f6")))

        let blocksToCheck = ["customer_warning", "customer_warning_1", "customer_warning_2"]
        for block in blocksToCheck.reversed() {
            steps.scrollTo(block, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 56, right: 0))
            validateSnapshots(accessibilityId: block, snapshotId: "sale_card-warning_block_\(block)")
        }
    }

    func test_availableForBooking() {
        addDefaultHistoryItems()

        let expectation = expectationForRequest { (request) -> Bool in
            return request.uri == "/booking/terms/cars/1098230510-dd311329?category=cars&offerId=1098230510-dd311329"
        }
        Step("Проверяем кнопку брони") {}

        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            var model: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098230510-dd311329_ok")
            model.offer.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.with({ (booking: inout Auto_Api_AdditionalInfo.Booking) in
                booking.allowed = true
            })
            return Response.okResponse(message: model, userAuthorized: true)
        }

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))

        Step("Валидация кнопки забронировать") {
            steps.validateSnapshot(of: steps.onSaleCardScreen().bookButton)
        }
        steps.tapBookButton()
        wait(for: [expectation], timeout: 10)
    }

    func test_bookedNotMe() {
        addDefaultHistoryItems()

        Step("Проверяем бронь другим человеком") {}

        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_not_me", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1098230510-dd311329/related *") { (_, _) -> Response? in
            Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_not_me_related", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1098230510-dd311329/specials *") { (_, _) -> Response? in
            Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_not_me_specials", userAuthorized: true)
        }

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))

        Step("Есть плашка брони (до 20 июня)") {
            steps.checkHeaderHasOnly(labels: [
                "BMW 3 серия 320d xDrive VII (G2x), 2020",
                "Забронирован", "До 20 июня"
            ])
        }

        Step("Нет кнопки забронировать") {
            steps.onSaleCardScreen().bookButton.shouldNotExist()
        }

        Step("Есть предложения дня") {
            steps.onSaleCardScreen().find(by: "offers_of_the_day_title_1097446154-2ca35346").firstMatch.shouldExist()
            steps.onSaleCardScreen().find(by: "offers_of_the_day_1097446154-2ca35346").firstMatch.shouldExist()
        }

        Step("Тап по плашке открывает модалку") {
            steps.tapOnBookedBanner()
                .wait(for: 2)
                .hasTitle("Авто забронировано до 20 июня")
                .hasSubtitle("Другой пользователь оформил бронь на эту машину. Если сделка не состоится до 20 июня, вы сможете купить этот автомобиль. Или выбрать другой.")
        }
    }

    func test_bookedByMe() {
        addDefaultHistoryItems()

        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_by_me", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1098230510-dd311329/related *") { (_, _) -> Response? in
            Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_not_me_related", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1098230510-dd311329/specials *") { (_, _) -> Response? in
            Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_booked_not_me_specials", userAuthorized: true)
        }

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))

        Step("Есть плашка брони мной (до 20 июня)") {
            steps.checkHeaderHasOnly(labels: [
                "BMW 3 серия 320d xDrive VII (G2x), 2020",
                "от 2 742 020 ₽",
                "Без скидок 3 183 000 ₽",
                "Забронирован вами", "До 20 июня"
            ])
        }

        Step("Нет кнопки забронировать") {
            steps.onSaleCardScreen().bookButton.shouldNotExist()
        }

        Step("Нет предложений дня") {
            let screen = steps.onSaleCardScreen()

            screen.find(by: "offers_of_the_day_title_1097446154-2ca35346").firstMatch.shouldNotExist()
            screen.find(by: "offers_of_the_day_1097446154-2ca35346").firstMatch.shouldNotExist()
        }

        Step("Тап по плашке открывает модалку") {
            steps.tapOnBookedBanner()
                .hasTitle("Вы забронировали этот автомобиль до 20 июня")
                .hasSubtitle("Можно ехать в дилерский центр и выкупать машину. В автосалоне покажите смс с подтверждением бронирования, которое мы отправили на ваш номер. Для отмены бронирования свяжитесь с дилером или обратитесь в техподдержку.")
        }
    }

    // MARK: - Drive banners

    func test_hasSpecialDriveBanner() {
        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "history_drive_banners_audi_a3", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1099557904-ae455935") { (_, _) -> Response? in
            return Response.okResponse(fileName: "sale_card_drive_banner_audi_a3", userAuthorized: true)
        }

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1099557904-ae455935")))
            .scrollToDriveBanner()

        steps.validateSnapShot(
            accessibilityId: steps.onSaleCardScreen().driveBanner.identifier,
            snapshotId: "drive_banner_audi_a3"
        )
    }

    func test_inactiveHasNoBanner() {
        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "history_drive_banners_audi_a3_inactive", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1099557904-ae455935") { (_, _) -> Response? in
            return Response.okResponse(fileName: "sale_card_drive_banner_audi_a3_inactive", userAuthorized: true)
        }

        Step("Для неактивного оффера не должно быть банера") {
            launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1099557904-ae455935")))
                .scrollToTutorial()
                .onSaleCardScreen()
                .driveBanner.shouldNotExist()
        }
    }

    func test_canOpenBanner() {
        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "history_drive_banners_audi_a3", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1099557904-ae455935") { (_, _) -> Response? in
            return Response.okResponse(fileName: "sale_card_drive_banner_audi_a3", userAuthorized: true)
        }

        Step("По тапу должен открыться браузер") {
            launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1099557904-ae455935")))
                .scrollToDriveBanner()
                .onSaleCardScreen().driveBannerButton.tap()

            app.buttons["Done"].shouldExist()
        }
    }

    // MARK: - Same but new

    func test_snapshotSameButNewBlock() {
        server.forceLoginMode = .forceLoggedIn
        let offerId = "1103083947-e752630c"

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewHistory", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/\(offerId)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCardOffer", userAuthorized: true)
        }

        server.addHandler("GET /search/cars/context/same-but-new/\(offerId)?filter_groups=mm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewOffers", userAuthorized: true)
        }

        Step("Должны найти раздел с новыми машинами") {
            let image = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
                .snapshotSameButNewOffers()

            Snapshot.compareWithSnapshot(image: image)
        }
    }

    func test_canOpenSameMarkModelOffer() {
        server.forceLoginMode = .forceLoggedIn
        let offerId = "1103083947-e752630c"

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewHistory", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/\(offerId)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCardOffer", userAuthorized: true)
        }

        server.addHandler("GET /search/cars/context/same-but-new/\(offerId)?filter_groups=mm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewOffers", userAuthorized: true)
        }

        Step("Должны найти раздел с новыми машинами") {
            let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
                .scrollToSameButNewOffers()

            Step("Можем открыть оффер") {
                Snapshot.compareWithSnapshot(image: steps.tapSameMarkModelOffer(at: 0).snapshotHeader())
            }
        }
    }

    func test_canOpenListing() {
        server.forceLoginMode = .forceLoggedIn
        let offerId = "1103083947-e752630c"

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewHistory", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/\(offerId)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCardOffer", userAuthorized: true)
        }

        server.addHandler("GET /search/cars/context/same-but-new/\(offerId)?filter_groups=mm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewOffers_Multi", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCtxGroupByConfig_Multi", userAuthorized: true)
        }

        Step("Должны найти раздел с новыми машинами") {
            let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
                .scrollToSameButNewOffers()
                .tapSameMarkModelShowAll()

            Step("Можем открыть листинг") {
                Snapshot.compareWithSnapshot(image: steps.snapshotParams(), identifier: "\(#function)_params")
                Snapshot.compareWithSnapshot(image: steps.snapshotParamsChips(), identifier: "\(#function)_params_chips")
                Snapshot.compareWithSnapshot(image: steps.snapshotOfferTitle(id: "1101767234-8e142468"), identifier: "\(#function)_offer-title")
            }
        }
    }

    func test_canOpenGroupListing() {
        server.forceLoginMode = .forceLoggedIn
        let offerId = "1103083947-e752630c"

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewHistory", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/\(offerId)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCardOffer", userAuthorized: true)
        }

        server.addHandler("GET /search/cars/context/same-but-new/\(offerId)?filter_groups=mm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewOffers", userAuthorized: true)
        }

        server.addHandler("POST /search/CARS/equipment-filters") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewEqFilters", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=group_card&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCtxGroupByConfig", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=default&group_by=CONFIGURATION&page=1&page_size=1&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCtxGroupByConfig_2", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=default&group_by=COMPLECTATION_NAME&page=1&page_size=100&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCtxGroupByComplectation", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewCtxGroupCard", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=21473416") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewSubtree", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/cars/all-options") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewAllOptions", userAuthorized: true)
        }

        server.addHandler("POST /search/cars/context/premium-new-cars?page=1&page_size=10") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewPremiumNew", userAuthorized: true)
        }

        server.addHandler("POST /search/cars/mark-model-filters?geo_radius=200&rid=213&search_tag=match_applications") { (_, _) -> Response? in
            return Response.okResponse(fileName: "SameButNewMarkModelFilters", userAuthorized: true)
        }

        Step("Должны найти раздел с новыми машинами") {
            let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
                .scrollToSameButNewOffers()

            Step("Можем открыть листинг") {
                Snapshot.compareWithSnapshot(image: steps.tapSameMarkModelShowAll()
                    .wait(for: 1)
                    .snapshotHeader())
            }
        }
    }

    // MARK: - Покупка отчета

    func test_fullLicensePlateAfterReportBought() {
        let offerId = "1103337306-430cfce4"

        var boughtReport = false
        server.addHandler("GET /offer/CARS/\(offerId)") { (_, _) -> Response? in
            if boughtReport {
                return Response.okResponse(fileName: "ReportLicensePlateOfferWithPlate", userAuthorized: true)
            } else {
                return Response.okResponse(fileName: "ReportLicensePlateOffer", userAuthorized: true)
            }
        }

        mocker
            .mock_reportLayoutForOffer(bought: false, quotaLeft: 1)

        Step("Покупаем отчет") {
            let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
                .scrollToReportBuySingleButton(windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 96, right: 0))

            mocker.mock_reportLayoutForOffer(bought: true)
            boughtReport = true
            steps.tapReportBuySingleButton()
                .wait(for: 1)
            _ = steps.tapBack()

            Step("Ищем полный госномер") {
                steps.swipeDown().swipeDown().findLicensePlate("Т460ВХ797")
                steps.findVIN("Т460ВХ797")
            }
            Step("Ищем полный VIN") {
                steps.findVIN("WDB4632481X137079")
            }
        }
    }

    func test_fullLicensePlateAfterReportBoughtOnCard() {
        let offerId = "1103337306-430cfce4"

        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_reportLayoutForOffer(bought: false, quotaLeft: 1)
            .mock_reportLayoutForReport(bought: false, quotaLeft: 1)
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "ReportLicensePlateOffer")
            }

        Step("Покупаем отчет") {
            let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))

            let reportSteps = steps
                .scrollToReportBuySingleButton()
                .as(SaleCardReportPreviewSteps.self)
                .openFreeReport()

            mocker
                .mock_reportLayoutForOffer(bought: true, quotaLeft: 1)
                .mock_reportLayoutForReport(bought: true, quotaLeft: 1)
                .mock_offerCars(id: offerId) { response in
                    response = .init(mockFile: "ReportLicensePlateOfferWithPlate")
                }
            reportSteps.buyFullReport().tapBack()

            Step("Ищем полный госномер") {
                steps
                    .scroll(to: .сharacteristicCell, direction: .down)
                    .should(provider: .saleCardScreen, .exist)
                    .focus({ screen in
                        screen.findLicensePlate("Т460ВХ797")
                    })
                steps.findVIN("Т460ВХ797")
            }
            Step("Ищем полный VIN") {
                steps.findVIN("WDB4632481X137079")
            }
        }
    }

    func test_complainFromMenu() {
        let complainExpectation = expectationForRequest { req -> Bool in
            guard let data = req.messageBody,
                  let model = try? Auto_Api_ComplaintRequest(jsonUTF8Data: data),
                  model.placement == "offerCard_menu",
                  req.uri.lowercased() == "/offer/cars/1098230510-dd311329/complaints".lowercased(),
                  req.method == "POST"
            else { return false }

            return true
        }

        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .wait(for: 1)
            .tap(.moreButton)
            .should(provider: .actionsMenuPopup, .exist)
            .focus { popup in
                popup.tap(.complain)
            }
            .should(provider: .complainMenuPopup, .exist)
            .focus { popup in
                popup.tap(.didSale)
            }

        wait(for: [complainExpectation], timeout: 10)
    }

    func test_complain() {
        let complainExpectation = expectationForRequest { req -> Bool in
            guard let data = req.messageBody,
                  let model = try? Auto_Api_ComplaintRequest(jsonUTF8Data: data),
                  model.placement == "offerCard",
                  req.uri.lowercased() == "/offer/cars/1098230510-dd311329/complaints".lowercased(),
                  req.method == "POST"
            else { return false }

            return true
        }

        addDefaultHistoryItems()

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scroll(to: .complainButton, windowInsets: .init(top: 0, left: 0, bottom: 96, right: 0))
            .tap(.complainButton)
            .should(provider: .complainMenuPopup, .exist)
            .focus { popup in
                popup.tap(.didSale)
            }

        wait(for: [complainExpectation], timeout: 10)
    }

    func test_autoruOnlyBadge() {
        let offerId = "1092222570-3203c7f6"

        mocker
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "offer_CARS_1092222570-3203c7f6_ok")
                response.offer.tags.append("autoru_exclusive")
            }


        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen.tap(.autoruOnlyBadge)
            }
            .should(provider: .autoruOnlyPopupScreen, .exist)
            .focus { screen in
                screen.tap(.autoruOnlyDisclaimer)
            }
            .should(provider: .webViewPicker, .exist)
    }

    func test_openDealerCardFromDealerInfo() {
        let offerId = "1115379285-9e04f597"

        mocker
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "dealer_sale_card_used_ok")
            }
            .mock_salon(id: "20907211")

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .dealerListingButton)
                    .tap(.dealerListingButton)
            }
            .should(provider: .dealerСardScreen, .exist)
    }

    func test_requestCallBack() {
        let offerId = "1115379285-9e04f597"

        let requestPostWasCalled =
        api.offer.category(.cars).offerID(offerId).registerCallback.post.expect { request, _ in
            request.phone == "+79875643212" ? .ok : .fail(reason: "Номер юзера не отправился")
        }

        mocker
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "dealer_sale_card_used_ok")
            }
            .mock_registerCallback(offerId)

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .dealerListingButton)
                    .tap(.callBackAction)
            }
            .wait(for: [requestPostWasCalled])
    }

    func test_deliveryBannerMoreInfo() {
        let offerId = "1086440862-bbad2cac"
        
        mocker
            .mock_offerCars(id: offerId) { response in
                response = .init(mockFile: "offer_with_delivery_info")
            }
        
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .tap(.deliveryBanner)
            }
            .should(provider: .genericModalPopup, .exist)
            .focus { $0.validateSnapshot() }
    }

    // MARK: -

    private func setupServer() {

        mocker.mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    private func addDefaultHistoryItems() {
        server.addHandler("GET /offer/CARS/1092222570-3203c7f6") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1092222570-3203c7f6_ok", userAuthorized: true)
        }

        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_ok", userAuthorized: true)
        }

        server.addMessageHandler("POST /offer/cars/1098230510-dd311329/complaints") { _, _ in
            Auto_Api_SuccessResponse.with { response in
                response.status = .success
            }
        }
    }

    private func validateSnapshots(accessibilityId: String, snapshotId: String) {
        let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.02, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16))
    }
}
