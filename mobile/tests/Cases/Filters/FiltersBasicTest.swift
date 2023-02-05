import MarketUITestMocks
import XCTest

final class FiltersBasicTest: LocalMockTestCase {

    func test_FilterBasic_QuickFiltersBlock() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5314")
        Allure.addEpic("Выдача")
        Allure.addFeature("Быстрые фильтры на выдаче")
        Allure.addTitle("Редизайн фильтров. Поисковая выдача. Четвертый этаж")

        let searchText = "iphone"
        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var expressCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var priceCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!
        var filterPopup: FilterPage!
        var filtersPage: FiltersPage!

        var feedState = FeedState()

        "Мокаем состояние".run {
            feedState.setSearchStateFAPI(mapper: .init(offers: FAPIOffer.all))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Быстрые фильтры отображаются и скролятся".run {
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
            quickFilterView.collectionView.swipe(to: .right, until: quickFilterView.filter(at: 4).element.isVisible)
            quickFilterView.collectionView.swipe(to: .left, until: quickFilterView.filter(at: 0).element.isVisible)
        }

        "Скроллим `Быстрые фильтры` до позиции и выбираем фильтр `Доставка за 2 часа`".run {
            expressCell = quickFilterView.filter(at: 1)
            quickFilterView.collectionView.swipe(to: .left, until: expressCell.element.isVisible)
            XCTAssertEqual(expressCell.element.staticTexts.firstMatch.label, "Доставка за 2 часа")
        }

        "Нажимаем на фильтр `Доставка за 2 часа`".run {
            expressCell.element.tap()
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Фильтр `Доставка за 2 часа` активен, количество выбранных фильтров — 1".run {
            expressCell = quickFilterView.filter(at: 0)
            wait(forVisibilityOf: expressCell.element)
            XCTAssertEqual(expressCell.element.staticTexts.firstMatch.label, "Доставка за 2 часа")
            XCTAssertTrue(expressCell.resetButton.isVisible)

            let filterBadge = feedPage.collectionView.headerFeedFilter.filterBadge
            XCTAssertEqual(filterBadge.label, "1")
        }

        "Сбрасываем фильтр `Доставка за 2 часа`".run {
            expressCell.resetButton.tap()
            wait(forVisibilityOf: quickFilterView.element)
            expressCell = quickFilterView.filter(at: 1)
            XCTAssertEqual(expressCell.element.staticTexts.firstMatch.label, "Доставка за 2 часа")
        }

        "Устанавливаем фильтр `Цена`".run {
            priceCell = quickFilterView.filter(at: 0)
            filterPopup = priceCell.tap()
            wait(forVisibilityOf: filterPopup.element)

            filterPopup.element.textFields.firstMatch.typeText("150")
            filterPopup.element.textFields.allElementsBoundByIndex[1].tap()
            filterPopup.element.textFields.allElementsBoundByIndex[1].typeText("2000")
            filterPopup.doneButton.tap()
            wait(forVisibilityOf: priceCell.element)
        }

        "Фильтр `Цена` активен, количество выбранных фильтров — 1".run {
            XCTAssertEqual(priceCell.element.staticTexts.firstMatch.label, "Цена")
            XCTAssertEqual(priceCell.element.staticTexts.allElementsBoundByIndex[1].label, "150 - 2 000")
            XCTAssertTrue(priceCell.resetButton.isVisible)

            let filterBadge = feedPage.collectionView.headerFeedFilter.filterBadge
            XCTAssertEqual(filterBadge.label, "1")
        }

        try "Выбираем фильтр экспресс-доставки на экране фильтров".run {
            let filtersButton = feedPage.collectionView.headerFeedFilter.filterButton
            filtersPage = filtersButton.tap()
            wait(forVisibilityOf: filtersPage.element)

            let expressFilter = try XCTUnwrap(filtersPage.snippet(named: "Доставка за 2 часа"))
            expressFilter.element.tap()
            _ = filtersPage.doneButton.tap()
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Фильтр `Доставка за 2 часа` активен, количество выбранных фильтров — 2".run {
            let filterBadge = feedPage.collectionView.headerFeedFilter.filterBadge
            XCTAssertEqual(filterBadge.label, "2")

            expressCell = quickFilterView.filter(at: 2)
            XCTAssertEqual(expressCell.element.staticTexts.firstMatch.label, "Доставка за 2 часа")
            XCTAssertTrue(expressCell.resetButton.isVisible)
        }

        "Нажимаем на крестик `сбросить все` в начале карусели фильтров".run {
            let resetCell = quickFilterView.filter(at: 0)
            resetCell.element.tap()
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Фильтр `Цена` не активен, выбранных фильтров нет".run {
            priceCell = quickFilterView.filter(at: 0)
            XCTAssertEqual(priceCell.element.staticTexts.firstMatch.label, "Цена")
            XCTAssertFalse(priceCell.resetButton.isVisible)

            let filterBadge = feedPage.collectionView.headerFeedFilter.filterBadge
            XCTAssertFalse(filterBadge.isVisible)
        }
    }

}
