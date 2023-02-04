//
//  NewDeeplinksTest.swift
//  UITests
//
//  Created by Roman Bevza on 10/15/20.
//

import Foundation

/// @depends_on AutoRuSaleList AutoRuComparisonsList
final class NewDeeplinksTests: BaseTest {

    // MARK: - Tests
    func test_getBestPriceLink() {
        setupAndStartServer { server in
            advancedMockReproducer.setup(server: server, mockFolderName: "GetBestOffersDeeplink")
            server.forceLoginMode = .forceLoggedIn
        }

        let requestExpectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/match-applications".lowercased()
        }
        let steps = GetBestPriceSteps(context: self)
        launch(link: "https://m.auto.ru/moskva/cars/new/get_best_price")

        steps
            .tapSendButton()
            .checkErrorAlert(message: "Заполните марку автомобиля")
            .pickMark("Acura")
            .tapSendButton()

        wait(for: [requestExpectation], timeout: 5)
        steps.validateSuccessHUD()
    }

    // Comparision Deeplinks
    func test_openEmptyOffersComparison() {
        // https://auto.ru/compare ведёт на офферы, но их может не быть и должны остаться на списке

        setupAndStartServer { server in
            server.forceLoginMode = .forceLoggedIn

            server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_favorites")
            }

            server.addHandler("GET /user/compare/cars/offers *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_offers_0")
            }

            server.addHandler("GET /user/compare/cars/models *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_models_deeplink")
            }
        }

        launch(link: "https://auto.ru/compare")

        // Экран сравнений
        sleep(2)
        app.staticTexts.matching(identifier: "Объявления из избранного").firstMatch.shouldExist(timeout: 10)
    }

    func test_openComparison() {
        openOffersComparison(link: "https://auto.ru/compare")
    }

    func test_openOffersComparison() {
        openOffersComparison(link: "https://auto.ru/compare-offers")
    }

    func test_openModelsComparison() {
        setupAndStartServer { server in
            server.forceLoginMode = .forceLoggedIn

            server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_favorites")
            }

            server.addHandler("GET /user/compare/cars/offers *") {    (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_offers_0")
            }

            server.addHandler("GET /user/compare/cars/models *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_models_deeplink")
            }
        }

        launch(link: "https://auto.ru/compare-models")

        // Экран сравнения моделей
        app.staticTexts.matching(identifier: "Сравнение").firstMatch.shouldExist(timeout: 10)
        app.staticTexts.matching(identifier: "3 модели").firstMatch.shouldExist(timeout: 10)
    }

    func test_openListingAllCarsAndCheckMarketingParameters() {
        launch(
            on: .saleListScreen,
            options: AppLaunchOptions(
                launchType: .deeplink("https://m.auto.ru/cars/all/?year_from=2011&from=yandex-m-gl&utm_source=yandex-m_list_service&utm_medium=cpm&utm_campaign=q2_ymls_r1_subtitle&utm_content=mbwrdvzrst-34312_r1&utm_term=s-probegom-do-5-let_mbwrdvzrst-34312")
            )
        )
        .wait(for: 1)
        .shouldEventBeReported(
            "Deeplink. Открыть листинг по марке и модели",
            with: ["utm_content": "mbwrdvzrst-34312_r1",
                   "utm_term": "s-probegom-do-5-let_mbwrdvzrst-34312",
                   "utm_medium": "cpm", "utm_source": "yandex-m_list_service",
                   "from": "yandex-m-gl",
                   "utm_campaign": "q2_ymls_r1_subtitle"
                  ]
        )
    }

    func test_openListingMototsiklyAndCheckMarketingParameters() {
        launch(
            on: .saleListScreen,
            options: AppLaunchOptions(
                launchType: .deeplink("autoru://mototsikly/used/sale/harley_davidson/vrscr/2840107-faba7814/?utm_source=24auto&utm_medium=CHPkrsk&utm_campaign=post&utm_content=2307")
            )
        )
        .wait(for: 1)
        .shouldEventBeReported(
            "Deeplink. Открыть карточку объявления",
            with: ["utm_source": "24auto",
                   "utm_content": "2307",
                   "utm_campaign": "post",
                   "utm_medium": "CHPkrsk"
                  ]
        )
    }

    func test_openListingLandRoverAndCheckMarketingParameters() {
        launch(
            on: .saleListScreen,
            options: AppLaunchOptions(
                launchType: .deeplink("https://auto.ru/cars/used/sale/land_rover/freelander/1102286023-b7e86c0c/?utm_source=24auto&utm_medium=CHPkrsk&utm_campaign=post&utm_content=2307")
            )
        )
        .wait(for: 1)
        .shouldEventBeReported(
            "Deeplink. Открыть карточку объявления",
            with: ["utm_source": "24auto",
                   "utm_medium": "CHPkrsk",
                   "utm_campaign": "post",
                   "utm_content" : "2307"
                  ]
        )
    }

    // MARK: - Private

    private func openOffersComparison(link: String) {
        setupAndStartServer { server in
            server.forceLoginMode = .forceLoggedIn

            server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_favorites")
            }

            server.addHandler("GET /user/compare/cars/offers *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_offers_6")
            }

            server.addHandler("GET /user/compare/cars/models *") { (_, _) -> Response? in
                return Response.okResponse(fileName: "comparison_models_deeplink")
            }
        }

        launch(link: link)

        // Экран сравнения офферов
        app.staticTexts.matching(identifier: "Сравнение").firstMatch.shouldExist(timeout: 10)
        app.staticTexts.matching(identifier: "6 объявлений").firstMatch.shouldExist(timeout: 10)
    }

    private func setupAndStartServer(_ action: (StubServer) -> Void) {
        action(server)
        try! server.start()
    }
}
