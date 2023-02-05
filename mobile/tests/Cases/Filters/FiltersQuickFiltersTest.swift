import MarketUITestMocks
import XCTest

final class FiltersQuickFiltersChooseTest: LocalMockTestCase {

    // MARK: - Constants

    let allFilterOptions = [
        "Сегодня",
        "Сегодня или завтра",
        "До 3 дней",
        "1-2 часа",
        "Любой",
        "Со склада Яндекс.Маркета"
    ]

    let deliveryOptionFilterOptionLabels = [
        "1-2 часа",
        "Сегодня",
        "Сегодня или завтра",
        "До 5 дней",
        "Любой"
    ]

    // MARK: - Tests

    func testTapQuickFilter() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4365")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4364")
        Allure.addEpic("Фильтры")
        Allure.addFeature("Быстрые фильтры")
        Allure.addTitle("На поисковой выдаче по тапу фильтр меняет цвет и переносится на 1 место")

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var saleCell: XCUIElement!

        let search = "Мобильные телефоны"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_QuickFiltersMobile")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Отображается выдача".ybm_run { _ in
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Скидки`".ybm_run { _ in
            let indexPath = IndexPath(item: 6, section: 0)
            saleCell = quickFilterView.cellElement(at: indexPath)
            quickFilterView.collectionView.ybm_swipeCollectionView(to: .left, toFullyReveal: saleCell)
        }

        "Мокаем запрос выдачи по фильтру".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_QuickFiltersMobileOnSale")
        }

        "Тапаем на фильтр `Скидки`".ybm_run { _ in
            wait(forVisibilityOf: saleCell)
            XCTAssertEqual(saleCell.staticTexts.firstMatch.label, "Скидки")
            saleCell.tap()
        }

        "Фильтр `Скидки` первый в быстрых фильтрах".ybm_run { _ in
            let indexPath = IndexPath(item: 0, section: 0)
            saleCell = quickFilterView.cellElement(at: indexPath)
            quickFilterView.collectionView.ybm_swipeCollectionView(to: .right, toFullyReveal: saleCell)
            wait(forVisibilityOf: saleCell)
            XCTAssertEqual(saleCell.staticTexts.firstMatch.label, "Скидки")
        }
    }

    func testDeliveryIntervalQuickFilter() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4532")
        Allure.addEpic("Фильтры")
        Allure.addFeature("Быстрые фильтры")
        Allure.addTitle(
            """
            Есть фильтр по сроку доставки, нет фильтра 'Со склада Яндекс.Маркета'.
            Отображается фильтр по сроку доставки в быстрых фильтрах.
            """
        )

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var deliveryIntervalCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!

        let search = "ps 5"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_DeliveryIntervalQuickFilters2")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Срок доставки курьером`".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 2)
            quickFilterView.collectionView.swipe(to: .right, until: deliveryIntervalCell.element.isVisible)
            XCTAssertEqual(deliveryIntervalCell.element.staticTexts.firstMatch.label, "Срок доставки курьером")
        }

        "Нажимаем на фильтр 'Срок доставки курьером'".ybm_run { _ in
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)

            ["Сегодня", "Сегодня или завтра", "До 3 дней", "1-2 часа", "Любой"].forEach {
                XCTAssertEqual(filterPopup.snippet(named: $0)?.element.exists, true)
            }
            XCTAssertNil(filterPopup.snippet(named: "Со склада Яндекс.Маркета")?.element)
        }
    }

    func testMarketWarehouseQuickFilterWithoutDeliveryInterval() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4533")
        Allure.addEpic("Фильтры")
        Allure.addFeature("Быстрые фильтры")
        Allure.addTitle(
            """
            Есть фильтр 'Со склада Яндекс.Маркета', нет фильтра по сроку доставки.
            Отображается фильтр 'Со склада Яндекс.Маркета' в быстрых фильтрах.
            """
        )

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var marketWarehouse: XCUIElement!

        let search = "ps 5"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_MarketWarehouseQuickFilter")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Со склада маркета`".ybm_run { _ in
            let indexPath = IndexPath(item: 7, section: 0)
            marketWarehouse = quickFilterView.cellElement(at: indexPath)
            quickFilterView.collectionView.swipe(to: .right, until: marketWarehouse.isVisible)
            XCTAssertEqual(marketWarehouse.staticTexts.firstMatch.label, "Со склада Яндекс.Маркета")
        }
    }

    func testMarketWarehouseQuickFilterWithDeliveryInterval() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4534")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4537")
        Allure.addEpic("Фильтры")
        Allure.addFeature("Быстрые фильтры")
        Allure.addTitle(
            """
            Есть и фильтр 'Со склада Яндекс.Маркета', и фильтр по сроку доставки.
            Фильтр по сроку доставки содержит под собой оба фильтра.
            """
        )

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var deliveryIntervalCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!

        let search = "ps 5"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_DeliveryIntervalQuickFilters")
            mockStateManager?.pushState(bundleName: "FiltersSet_WarehouseWithDeliveryIntervalQuickFilters")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Срок доставки курьером`".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 3)
            quickFilterView.collectionView.swipe(to: .right, until: deliveryIntervalCell.element.isVisible)
            XCTAssertEqual(deliveryIntervalCell.element.staticTexts.firstMatch.label, "Срок доставки курьером")
        }

        try "Нажимаем на фильтр 'Срок доставки курьером' и выбираем со склада Яндекса".ybm_run { _ in
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)

            let switchElement = try XCTUnwrap(filterPopup.snippet(named: "Со склада Яндекс.Маркета"))

            allFilterOptions.forEach {
                XCTAssertEqual(filterPopup.snippet(named: $0)?.element.exists, true)
            }

            mockStateManager?.pushState(bundleName: "FiltersSet_ActiveMarketWarehouseQuickFilter")

            XCTAssertEqual(switchElement.switchElem.text, "0")
            switchElement.element.tap()
            ybm_wait(forFulfillmentOf: {
                filterPopup.loadingPage.element.isVisible == false
            })
            XCTAssertEqual(switchElement.switchElem.text, "1")

            filterPopup.doneButton.tap()
            wait(forVisibilityOf: feedPage.element)
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Срок доставки курьером`".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 1)
            quickFilterView.collectionView.swipe(to: .left, until: deliveryIntervalCell.element.isVisible)
            XCTAssertEqual(deliveryIntervalCell.element.staticTexts.firstMatch.label, "Срок доставки курьером")
        }

        try "Нажимаем на фильтр 'Со склада Яндекс.Макерта' и выбираем доставку сегодня или завтра".ybm_run { _ in
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)

            let switchElement = try XCTUnwrap(filterPopup.snippet(named: "Со склада Яндекс.Маркета"))

            ["Сегодня или завтра", "Любой", "Со склада Яндекс.Маркета"].forEach {
                XCTAssertEqual(filterPopup.snippet(named: $0)?.element.exists, true)
            }

            ["Сегодня", "До 3 дней", "Доставка за 1-2 часа"].forEach {
                XCTAssertNil(filterPopup.snippet(named: $0))
            }

            mockStateManager?.pushState(bundleName: "FiltersSet_WarehouseWithDeliveryIntervalQuickFilters")

            switchElement.element.tap()
            ybm_wait(forFulfillmentOf: {
                filterPopup.loadingPage.element.isVisible == false
            })
            XCTAssertEqual(switchElement.switchElem.text, "0")

            allFilterOptions.forEach {
                XCTAssertEqual(filterPopup.snippet(named: $0)?.element.exists, true)
            }

            let todayOption = try XCTUnwrap(filterPopup.snippet(named: "Сегодня или завтра")?.element)

            mockStateManager?.pushState(bundleName: "FiltersSet_ActiveDeliveryIntervalQuickFilter")
            todayOption.tap()

            wait(forVisibilityOf: filterPopup.doneButton)
            filterPopup.doneButton.tap()
            wait(forVisibilityOf: feedPage.element)
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Проверяем, что фильтр по сроку доставки активен".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 1)
            wait(forVisibilityOf: deliveryIntervalCell.element)
            XCTAssertEqual(
                deliveryIntervalCell.element.staticTexts.allElementsBoundByIndex[1].label,
                "Сегодня или завтра"
            )
        }

        try "Нажимаем на фильтр по сроку доставки и включаем тоггл со склада".ybm_run { _ in
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)
            let switchElement = try XCTUnwrap(filterPopup.snippet(named: "Со склада Яндекс.Маркета"))

            mockStateManager?.pushState(bundleName: "FiltersSet_WarehouseWithDeliveryActiveQuickFilter")

            switchElement.element.tap()
            ybm_wait(forFulfillmentOf: {
                filterPopup.loadingPage.element.isVisible == false
            })
            XCTAssertEqual(switchElement.switchElem.text, "1")

            filterPopup.doneButton.tap()
            wait(forVisibilityOf: feedPage.element)
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Проверяем, что фильтр с выбранными опциями доставки активен".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 1)
            wait(forVisibilityOf: deliveryIntervalCell.element)
            XCTAssertEqual(
                deliveryIntervalCell.element.staticTexts.allElementsBoundByIndex[1].label,
                "Сегодня или завтра, +1"
            )
        }
    }

    func testDeliveryIntervalQuickFilterDisableOptions() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4535")
        Allure.addEpic("Фильтры")
        Allure.addFeature("Быстрые фильтры")
        Allure.addTitle("При применении фильтра 'со склада Яндекс.Маркета' недоступные значения скрываются.")

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var deliveryIntervalCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!

        let search = "ps 5"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FiltersSet_DeliveryIntervalQuickFilters")
            mockStateManager?.pushState(bundleName: "FiltersSet_WarehouseWithDeliveryIntervalQuickFilters")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: search)
            wait(forVisibilityOf: feedPage.collectionView.element)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Срок доставки курьером`".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 3)
            quickFilterView.collectionView.swipe(to: .right, until: deliveryIntervalCell.element.isVisible)
            XCTAssertEqual(deliveryIntervalCell.element.staticTexts.firstMatch.label, "Срок доставки курьером")
        }

        try "Нажимаем на фильтр 'Срок доставки курьером' и выбираем со склада Яндекса".ybm_run { _ in
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)

            allFilterOptions.forEach {
                XCTAssertEqual(filterPopup.snippet(named: $0)?.element.exists, true)
            }

            mockStateManager?.pushState(bundleName: "FiltersSet_DeliverIntervalQuickFilterTodayActive")
            let todayOption = try XCTUnwrap(filterPopup.snippet(named: "Сегодня"))

            todayOption.element.tap()
            ybm_wait(forFulfillmentOf: {
                filterPopup.loadingPage.element.isVisible == false
            })
        }

        try "Проверяем, что тоггл неактивен".ybm_run { _ in
            let switchElement = try XCTUnwrap(filterPopup.snippet(named: "Со склада Яндекс.Маркета"))
            switchElement.element.tap()
            XCTAssertEqual(switchElement.switchElem.text, "0")
        }
    }

    func testDeliveryOptionIntervalsInCommonContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6357")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6361")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6362")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6363")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6364")

        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Смешанная выдача. Опция 1-2ч")
        Allure.addTitle(
            """
            На смешанной выдаче после поиска есть фильтр "Срок доставки" в быстрых фильтрах
            - 1-2 часа
            - сегодня
            - сегодня или завтра
            - до 5 дней
            - любой
            """
        )

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var deliveryIntervalCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!

        let search = "смартфон"

        "Мокаем поиск".ybm_run { _ in
            setupFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Открываем попап фильтра \"Срок доставки\"".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 2)
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)
        }

        deliveryOptionFilterOptionLabels.enumerated().forEach { index, optionName in
            ("Тапаем по опции " + optionName + " в попапе").ybm_run { _ in
                XCTAssertEqual(
                    filterPopup.snippets.dropFirst(index).first?.name.label
                        .trimmingCharacters(in: .whitespacesAndNewlines),
                    optionName
                )
                filterPopup = filterPopup.snippets.dropFirst(index).first?.tap()
            }

            checkFilterOptions(page: filterPopup)

            ("Проверяем выбор опции " + optionName).ybm_run { _ in
                XCTAssertEqual(
                    filterPopup.snippets.dropFirst(index).first?.checkBox.identifier,
                    FiltersSnippetAccessibility.checkBoxSelected
                )
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filterPopup.doneButton.label, "Показать 2 товара")
            }
        }
    }

    func testDeliveryOptionIntervalsInExpressContext() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6358")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6365")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6366")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6367")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6368")
        Allure.addEpic("Фильтр Срок доставки")
        Allure.addFeature("Смешанная выдача. Опция 1-2ч")
        Allure.addTitle(
            """
            На смешанной выдаче после поиска есть фильтр "Срок доставки" в быстрых фильтрах
            - 1-2 часа
            - сегодня
            - сегодня или завтра
            - до 5 дней
            - любой
            """
        )

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var deliveryIntervalCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!

        let search = "смартфон"

        "Настраиваем стейт".ybm_run { _ in
            setupExpressState()
        }

        feedPage = openExpressFeed(search: search)

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Открываем попап фильтра \"Срок доставки\"".ybm_run { _ in
            deliveryIntervalCell = quickFilterView.filter(at: 2)
            filterPopup = deliveryIntervalCell.tap()
            wait(forVisibilityOf: filterPopup.element)
        }

        deliveryOptionFilterOptionLabels.enumerated().forEach { index, optionName in
            ("Тапаем по опции " + optionName + " в попапе").ybm_run { _ in
                XCTAssertEqual(
                    filterPopup.snippets.dropFirst(index).first?.name.label
                        .trimmingCharacters(in: .whitespacesAndNewlines),
                    optionName
                )
                filterPopup = filterPopup.snippets.dropFirst(index).first?.tap()
            }

            checkFilterOptions(page: filterPopup)

            ("Проверяем выбор опции " + optionName).ybm_run { _ in
                XCTAssertEqual(
                    filterPopup.snippets.dropFirst(index).first?.checkBox.identifier,
                    FiltersSnippetAccessibility.checkBoxSelected
                )
            }

            "Проверяем кнопку".ybm_run { _ in
                XCTAssertEqual(filterPopup.doneButton.label, "Показать 2 товара")
            }
        }
    }
}

// MARK: - Helper methods

private extension FiltersQuickFiltersChooseTest {
    func setupFeedState() {
        stateManager?.mockingStrategy = .dtoMock
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchOrRedirectState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        stateManager?.setState(newState: feedState)
    }

    func setupExpressState() {
        stateManager?.mockingStrategy = .dtoMock
        var feedState = FeedState()
        feedState.setSearchOrRedirectState(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fromOffers: Constants.feedOffers)
        )
        stateManager?.setState(newState: feedState)

        var cmsState = CMSState()
        cmsState.setCMSState(with: CMSState.CMSCollections.expressCollections)
        stateManager?.setState(newState: cmsState)

        let expressState = ExpressState()
        stateManager?.setState(newState: expressState)

        var authState = UserAuthState()
        authState.setAddressesState(addresses: [.default])
        authState.setPlusBalanceState(.withZeroMarketCashback)
        stateManager?.setState(newState: authState)
    }

    func checkFilterOptions(page filterPage: FilterPage) {
        let filterOptions = deliveryOptionFilterOptionLabels + ["Со склада Яндекса", "С доставкой Яндекса"]
        "Проверяем список опций".ybm_run { _ in
            filterPage.snippets.enumerated().forEach { index, filterSnippet in
                XCTAssertEqual(
                    filterSnippet.name.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    filterOptions[index]
                )
            }
        }
    }

    func openExpressFeed(search: String) -> FeedPage {
        var feedPage: FeedPage!
        "Открываем экспресс-выдачу".ybm_run { _ in
            let expressPage: ExpressPage!
            expressPage = goToExpress()
            wait(forVisibilityOf: expressPage.collectionView)

            let searchPage = expressPage.goToSearch()

            ybm_wait(forVisibilityOf: [searchPage.navigationBar.searchTextField])

            searchPage.navigationBar.searchTextField.tap()
            searchPage.navigationBar.searchTextField.typeText(search + "\n")

            let feedElement = XCUIApplication().otherElements[FeedAccessibility.root]
            feedPage = FeedPage(element: feedElement)
            ybm_wait(forVisibilityOf: [feedElement])
        }
        return feedPage
    }
}

// MARK: - Nested Types

private extension FiltersQuickFiltersChooseTest {

    enum Constants {
        static let feedOffers = [
            CAPIOffer.protein,
            CAPIOffer.protein1
        ]
    }
}
