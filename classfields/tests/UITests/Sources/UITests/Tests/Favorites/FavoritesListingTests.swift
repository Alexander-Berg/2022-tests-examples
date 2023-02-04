import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuFavoriteSaleList
final class FavoritesListingTests: FavoritesTests {
    func test_hasUpdatesAndSold() {
        currentFavoritesStub = "favs_offers_with_updates"

        launch()
        let steps = openFavorites()

        Step("Подменяем ответ с проданным автомобилем и тапаем по плашке, чтобы экран обновился") {
            currentFavoritesStub = "favs_offers_no_updates_sold"
        }

        steps.checkHasUpdatesView(title: "У 1 объявления изменилась цена")
            .checkHasTabbarUpdates()
            .checkSegmentHasUpdates(at: .offers)
            .hideUpdates()
            .checkHasUpdatesView(title: "2 объявления были сняты с продажи")
            .checkHasTabbarUpdates()
            .checkSegmentHasUpdates(at: .offers)
    }

    func test_hasBothSegmentUpdates() {
        currentFavoritesStub = "fav_offers_price_changed_2"
        currentSearchesStub = "fav_searches_1_bmw-5"

        server.addHandler("POST /search/saved/71c47b723742bcb0b79056767fdf32f3bc39c068/count?context=subscription") { (_, _) -> Response? in
            Response.okResponse(fileName: "fav_searches_1_bmw-5_2-new", userAuthorized: true)
        }

        launch()
        let steps = openFavorites()

        Step("Смотрим апдейты на двух табах") {}

        steps.checkHasUpdatesView(title: "У 2 объявлений изменилась цена")
            .checkHasTabbarUpdates()
            .checkSegmentHasUpdates(at: .offers)
            .checkSegmentHasUpdates(at: .searches)

        let control = steps.onFavoritesScreen().segmentControl

        Snapshot.compareWithSnapshot(
            image: control.screenshot().image,
            identifier: SnapshotIdentifier(
                suite: SnapshotIdentifier.suiteName(from: #file),
                identifier: "favs_segment_all_badges"
            )
        )

        steps
            .tapSegment(at: .searches)
            .checkSegmentHasUpdates(at: .searches)

        currentSearchesStub = nil
        currentFavoritesStub = "fav_offers_price_changed_0"

        steps
            .wait(for: 3)
            .tapSegment(at: .offers)
            .checkSegmentHasNoUpdates(at: .offers)
            .checkSegmentHasUpdates(at: .searches)

        steps
            .wait(for: 3)
            .tapSegment(at: .searches)
            .checkSegmentHasNoUpdates(at: .offers)
            .checkSegmentHasNoUpdates(at: .searches)
    }

    func test_searchesReloadedOnAppear() {
        currentFavoritesStub = "favs_no_offers"
        currentSearchesStub = "fav_searches_1_bmw-5"

        server.addHandler("POST /search/saved/71c47b723742bcb0b79056767fdf32f3bc39c068/count?context=subscription") { (_, _) -> Response? in
            Response.okResponse(fileName: "fav_searches_1_bmw-5_2-new", userAuthorized: true)
        }

        launch()
        let steps = openFavorites()

        steps
            .tapSegment(at: .searches)
            .checkSegmentHasUpdates(at: .searches)
            .tapSavedSearch(id: "71c47b723742bcb0b79056767fdf32f3bc39c068", index: 0)

        let reloadExp = expectation(description: "Должны перезапросить кол-во новых офферов после возврата на экран избранного")
        reloadExp.assertForOverFulfill = false

        server.addHandler("POST /search/saved/71c47b723742bcb0b79056767fdf32f3bc39c068/count?context=subscription") { (_, _) -> Response? in
            reloadExp.fulfill()
            return nil
        }

        steps
            .goBack()
            .wait(for: 1)
            .checkSegmentHasNoUpdates(at: .searches)

        wait(for: [reloadExp], timeout: 2)
    }

    func test_dealerSubscriptionSort() {
        currentSearchesStub = "fav-dealer_bmw"

        server.addHandler("GET /salon/by-dealer-id/20134432") { (_, _) -> Response? in
            return Response.okResponse(fileName: "fav-dealer_bmw-salon", userAuthorized: true)
        }

        launch()
        let steps = openFavorites()

        steps
            .tapSegment(at: .searches)
            .tapSavedSearch(id: "484d395436e4c4052be5fe76556bc828820424cc", index: 0)
            .wait(for: 1)

        let saleListSteps = steps.as(SaleCardListSteps.self)
        saleListSteps.checkContains(sorting: "По дате размещения")
    }

    func test_removeSold() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_favoriteSoldOffers()
            .mock_deleteInactiveOffersFromFavorite()

        launch()
        openFavorites()
            .focus(on: .offer(.custom("1098669850-51fb1239")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1239"))
            }
            .should(provider: .systemAlert, .exist)
            .focus { menu in
                menu.tap(.button("Удалить"))
            }
            .focus(on: .offer(.custom("1098503226-6b65e9cd")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098503226-6b65e9cd"))
            }
            .should(provider: .simpleButtonsModalMenu, .exist)
            .focus { menu in
                menu.tap(.button("Удалить все 3"))
            }
            .should(.noOffersLabel, .exist)
    }

    func test_removeSoldActiveSold() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_deleteInactiveOffersFromFavorite()
        api.user.favorites.category(._unknown("all")).get(parameters: .wildcard)
            .ok(mock: .file("favs_offers_no_updates_3_sold_1_active"))

        launch()
        openFavorites()
            .do {
                api.user.favorites.category(._unknown("all")).get(parameters: .wildcard)
                    .ok(mock: .file("favs_offers_no_updates_3_sold_1_active", mutation: { response in
                        response.offers.remove(at: 0)
                        response.notActiveOffersCount = 3
                    }))
            }
            .focus(on: .offer(.custom("1098669850-51fb1239")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1239"))
            }
            .should(provider: .systemAlert, .exist)
            .focus { menu in
                menu.tap(.button("Удалить"))
            }
            .do {
                api.user.favorites.category(._unknown("all")).get(parameters: .wildcard)
                    .ok(mock: .file("favs_offers_no_updates_3_sold_1_active", mutation: { response in
                        response.offers.remove(at: 0)
                        response.offers.remove(at: 0)
                        response.notActiveOffersCount = 2
                    }))
            }
            .focus(on: .offer(.custom("1098503226-6b65e9cd")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098503226-6b65e9cd"))
            }
            .focus(on: .offer(.custom("1098669850-51fb1238")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1238"))
            }
            .should(provider: .simpleButtonsModalMenu, .exist)
            .focus { menu in
                menu.tap(.button("Удалить все 2"))
            }
            .should(.noOffersLabel, .exist)
    }

    func test_removeSoldRefuseRemoveSold() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_favoriteSoldOffers()
            .mock_deleteInactiveOffersFromFavorite()

        launch()
        openFavorites()
            .wait(for: 2)
            .focus(on: .offer(.custom("1098669850-51fb1239")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1239"))
            }
            .should(provider: .systemAlert, .exist)
            .focus { menu in
                menu.tap(.button("Удалить"))
            }
            .focus(on: .offer(.custom("1098503226-6b65e9cd")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098503226-6b65e9cd"))
            }
            .tap()
            .focus(on: .offer(.custom("1098503226-6b65e9cd")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098503226-6b65e9cd"))
            }
            .focus(on: .offer(.custom("1098669850-51fb1238")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1238"))
            }
            .focus(on: .offer(.custom("1098669850-51fb1237")), ofType: .offerSnippet) { snippet in
                snippet.tap(.favoriteButton("1098669850-51fb1237"))
            }
            .should(.noOffersLabel, .exist)
    }
}
