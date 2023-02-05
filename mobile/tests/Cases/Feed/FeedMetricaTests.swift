import MarketUITestMocks
import Metrics
import XCTest

// swiftlint:disable function_body_length
final class FeedMetricaTests: LocalMockTestCase {

    func testSearchFeedMetrica() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3110")
        Allure.addEpic("ПОИСКОВАЯ ВЫДАЧА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика на строку поиска")

        var root: RootPage!
        var morda: MordaPage!
        var search: SearchPage!
        var feed: FeedPage!
        var clarifyCategory: XCUIElement!

        "Мокаем состояние введенного текста \"i\"".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Search_Text_i")
        }

        "Открываем поисковый экран".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = root.tabBar.mordaPage
            search = morda.searchButton.tap()
        }

        "Вводим текст \"i\"".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            search.navigationBar.searchTextField.tap()
            search.navigationBar.searchTextField.typeText("i")
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_CHANGE-TEXT")
                    .with(params: ["text": "i", "type": "add"])
                    .isNotEmpty
            })
        }

        "Мокаем состояние введенного текста \"ip\"".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Search_Text_ip")
        }

        "Вводим текст \"ip\"".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            search.navigationBar.searchTextField.typeText("p")
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_CHANGE-TEXT")
                    .with(params: ["text": "ip", "type": "add"])
                    .isNotEmpty
            })
        }

        "Мокаем состояние введенного текста \"i\"".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Search_Text_i")
        }

        "Стираем текст до \"i\"".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            search.navigationBar.searchTextField.typeText(XCUIKeyboardKey.delete.rawValue)
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_CHANGE-TEXT")
                    .with(params: ["text": "i", "type": "del"])
                    .isNotEmpty
            })
        }

        "Мокаем состояние введенного текста \"красный\" и выдачу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SearchFeed_red")
        }

        "Вводим текст \"красный\" и переходим на выдачу".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            search.navigationBar.searchTextField.ybm_clearAndEnterText("красный" + "\n")

            ybm_wait(forFulfillmentOf: { FeedPage.current.element.isVisible })
            feed = FeedPage.current
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                let enterEvent = MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_ENTER")
                    .with(params: ["text": "красный"])
                    .first

                let visibleEvent = MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH_RESULTS_VISIBLE")
                    .with(params: [
                        "text": "красный",
                        "page": "1",
                        "referer": "SEARCH",
                        "countCategoryClarify": 15
                    ])
                    .first { event in
                        guard
                            let results = event.parameters["results"] as? [[String: Any]],
                            let resultAt2 = results[ble_safe: 2],
                            let skuId = resultAt2["skuId"] as? String,
                            let skuType = resultAt2["skuType"] as? String,
                            let price = resultAt2["price"] as? String,
                            let oldPrice = resultAt2["oldPrice"] as? String,
                            let hid = resultAt2["hid"] as? String,
                            let filterIds = event.parameters["filterIds"] as? [String],
                            let hidsCategoryClarify = event.parameters["hidsCategoryClarify"] as? [Int],
                            let countCategoryClarify = event.parameters["countCategoryClarify"] as? Int
                        else { return false }

                        return filterIds.isEmpty
                            && skuId == "100126177779"
                            && skuType == "market"
                            && price == "300"
                            && oldPrice == "400"
                            && hid == "10682532"
                            && hidsCategoryClarify.count == countCategoryClarify
                            && [10_682_647, 12_714_755, 90_689, 90_711].allSatisfy { hidsCategoryClarify.contains($0) }
                    }

                return enterEvent != nil && visibleEvent != nil
            })
        }

        "Свапаем уточнения категории".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            let indexPathOutsideBounds = IndexPath(item: 3, section: 0)
            let categoryOutsideBounds = feed.collectionView.categoriesCollectionView
                .cellElement(at: indexPathOutsideBounds)
            feed.collectionView.categoriesCollectionView.element.ybm_swipeCollectionView(
                to: .left,
                toFullyReveal: categoryOutsideBounds
            )
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica).with(name: "SEARCH_CLARIFY-CATEGORY_SCROLLBOX_SWIPE")
                    .isNotEmpty
            })
        }

        "Мокаем состояние категорийной выдачи \"Расскраски\"".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CategorialFeed_coloring")
        }

        "Уточняем категорию".ybm_run { _ in
            MetricRecorder.clear() // очищаем хранилище ивентов

            let indexPath = IndexPath(item: 2, section: 0)
            clarifyCategory = feed.collectionView.categoriesCollectionView.cellElement(at: indexPath)
            clarifyCategory.tap()
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "LIST_RESULTS_VISIBLE")
                    .with(params: [
                        "text": "красный",
                        "page": "1",
                        "referer": "SEARCH_RESULT",
                        "countCategoryClarify": 0,
                        "hid": 12_494_574,
                        "nid": 61_571,
                        "hasAdult": "false"
                    ])
                    .contains { event in
                        guard
                            let results = event.parameters["results"] as? [[String: Any]],
                            let resultAt1 = results[ble_safe: 1],
                            let skuId = resultAt1["skuId"] as? String,
                            let skuType = resultAt1["skuType"] as? String,
                            let price = resultAt1["price"] as? String,
                            let skuHid = resultAt1["hid"] as? String,
                            let filterIds = event.parameters["filterIds"] as? [String],
                            let hidsCategoryClarify = event.parameters["hidsCategoryClarify"] as? [Int],
                            let countCategoryClarify = event.parameters["countCategoryClarify"] as? Int
                        else { return false }

                        return filterIds.isEmpty
                            && skuId == "217265727"
                            && skuType == "market"
                            && price == "9999"
                            && skuHid == "7692671"
                            && countCategoryClarify == 0
                            && hidsCategoryClarify.count == countCategoryClarify
                    }
            })
        }
    }

    func testSearchFeedViewSearchResultsMetrics() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5216")
        Allure.addEpic("ПОИСКОВАЯ ВЫДАЧА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика просмотра результатов поиска")

        let search = "протеин"

        "Настраиваем стейт".ybm_run { _ in
            setupState()
        }

        "Открываем выдачу".ybm_run { _ in
            let feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.element)
        }

        "Проверяем отправку события просмотра результатов поиска в Firebase".ybm_run { _ in
            XCTAssertTrue(
                MetricRecorder.events(from: .firebase)
                    .with(name: "view_search_results")
                    .with(params: ["search_term": "протеин"])
                    .isNotEmpty
            )
        }

        try "Проверяем отправку события просмотра результатов поиска в Adjust".ybm_run { _ in
            let event = try XCTUnwrap(MetricRecorder.events(from: .adjust).with(name: "4cmnwd").first)

            XCTAssertEqual(try XCTUnwrap(event.parameters["success"] as? String), "1")
            XCTAssertEqual(try XCTUnwrap(event.parameters["content_type"] as? String), "product_group")
            XCTAssertEqual(try XCTUnwrap(event.parameters["fb_content_type"] as? String), "product")
            XCTAssertEqual(try XCTUnwrap(event.parameters["search_text"] as? String), "протеин")
            XCTAssertEqual(try XCTUnwrap(event.parameters["criteo_p"] as? String), "%5B%22100902560734%22%5D")

            let customerId = try XCTUnwrap(event.parameters["customer_id"] as? String)
            XCTAssertFalse(customerId.isEmpty)

            let items = try XCTUnwrap(event.parameters["items"] as? String)
            XCTAssertTrue(items.contains("'id':'100902560734'"))
            XCTAssertTrue(items.contains("'quantity':1"))

            let skuIds = try XCTUnwrap(event.parameters["sku_id"] as? String)
            XCTAssertTrue(skuIds.contains("100902560734"))
        }
    }

    func testSearchFeedViewItemListMetrics() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5217")
        Allure.addEpic("ПОИСКОВАЯ ВЫДАЧА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика просмотра списка товаров")

        let search = "протеин"

        "Настраиваем стейт".ybm_run { _ in
            setupState(redirect: true)
        }

        "Открываем выдачу".ybm_run { _ in
            let feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.element)
        }

        "Проверяем отправку события просмотра списка товаров в Firebase".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .firebase)
                    .with(name: "view_item_list")
                    .with(params: [
                        "item_list_id": "14247341",
                        "item_list_name": "протеин"
                    ]).count == 1
            }
        }

        try "Проверяем отправку события просмотра списка товаров в Adjust".ybm_run { _ in
            var event: MetricRecorderEvent?
            ybm_wait {
                let events = MetricRecorder.events(from: .adjust)
                    .with(name: "mlolli")
                    .with(params: [
                        "success": "1",
                        "category_id": "14247341",
                        "content_type": "product_group",
                        "fb_content_type": "product",
                        "search_text": "протеин",
                        "criteo_p": "%5B%22100902560734%22%5D"
                    ])
                event = events.first
                return events.count == 1
            }

            let customerId = try XCTUnwrap(event?.parameters["customer_id"] as? String)
            XCTAssertFalse(customerId.isEmpty)

            let items = try XCTUnwrap(event?.parameters["items"] as? String)
            XCTAssertTrue(items.contains("100902560734"))
        }
    }

    // MARK: - Helper Methods

    private func setupState(redirect: Bool = false) {
        var feedState = FeedState()

        feedState.setSearchOrRedirectState(
            mapper: .init(fromOffers: [.protein]),
            redirect: redirect ? .protein : nil
        )

        stateManager?.setState(newState: feedState)
    }
}
