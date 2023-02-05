import MarketUITestMocks
import XCTest

final class FeedHeaderTest: LocalMockTestCase {

    func test_FeedHeader_TypoDisclaimer() throws {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5312")
        Allure.addEpic("Выдача")
        Allure.addFeature("Исправление опечатки в поиске")
        Allure.addTitle("Редизайн фильтров. Поисковая выдача. Опечаточник")

        let searchText = "шакалад"
        let redirectText = "шоколад"
        let hid = 15_713_879
        let nid = 72_087

        var feedPage: FeedPage!

        var feedState = FeedState()

        // поиск с редиректом одним запросом
        enable(toggles: FeatureNames.searchPerOneRequest)
        // используем фапи-запрос, чтобы не мокать капи
        enable(toggles: FeatureNames.fapiNavigationTree)

        stateManager?.mockingStrategy = .dtoMock

        "Мокаем состояние".run {
            // мок ответа на первый запрос с редиректом в категорию
            feedState.setSearchOrUrlTransformState(
                mapper: .init(
                    offers: FAPIOffer.all,
                    spellchecker: [.init(id: searchText, raw: redirectText, highlighted: redirectText)],
                    intent: ResolveSearch.Intent.chocolates,
                    hid: hid,
                    nid: nid
                ),
                redirect: .init(
                    target: searchText,
                    hid: hid.string,
                    nid: nid.string
                )
            )
            // мок ответа на запрос поиска без уточнения категории
            feedState.setSearchStateFAPI(mapper: .init(offers: FAPIOffer.all))

            feedState.setNavigationTree(mapper: .init(nodes: [.chocolate]))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Проверяем исправление опечатки и категорию".run {
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, redirectText)
            XCTAssertEqual(feedPage.typoDisclaimer.label, "Запрос исправлен. Вернуть «\(searchText)»")
            XCTAssertEqual(feedPage.categoryTitle.label, "Шоколадные изделия")
        }

        "Отменяем исправление".run {
            feedPage.typoDisclaimer.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, searchText)
            XCTAssertFalse(feedPage.typoDisclaimer.exists)
        }

        "Возвращаемся назад, проверяем исправление опечатки".run {
            feedPage.navigationBar.backButton.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, redirectText)
            XCTAssertEqual(feedPage.typoDisclaimer.label, "Запрос исправлен. Вернуть «\(searchText)»")
        }

        "Переходим в подкатегорию".run {
            feedPage.collectionView.categoriesCollectionView.element.cells.firstMatch.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Проверяем запрос, название категории и остутствие исправления".run {
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, redirectText)
            XCTAssertEqual(feedPage.categoryTitle.label, "Шоколадная плитка")
            XCTAssertFalse(feedPage.typoDisclaimer.exists)
        }
    }

    func test_FeedHeader_CategoryRefinement() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5310")
        Allure.addEpic("Выдача")
        Allure.addFeature("Уточнение категории")
        Allure.addTitle("Редизайн фильтров. Поисковая выдача. Переход в категорию через карусель категорий")

        let searchText = "шоколад"

        var feedPage: FeedPage!
        var categoriesCollection: FeedPage.CollectionView.CategoriesCollectionView!

        var feedState = FeedState()

        // поиск с редиректом одним запросом
        enable(toggles: FeatureNames.searchPerOneRequest)

        stateManager?.mockingStrategy = .dtoMock

        "Мокаем состояние".run {
            // мок ответа на первый запрос без редиректа в категорию
            feedState.setSearchOrUrlTransformState(
                mapper: .init(
                    offers: FAPIOffer.all,
                    intent: ResolveSearch.Intent.chocolates
                ),
                redirect: nil
            )
            // мок ответа на запрос поиска после уточнения категории
            feedState.setSearchStateFAPI(mapper: .init(offers: FAPIOffer.all))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Варианты категорий отображаются и скролятся".run {
            categoriesCollection = feedPage.collectionView.categoriesCollectionView
            XCTAssertTrue(categoriesCollection.element.isVisible)
            categoriesCollection.collectionView.swipe(
                to: .right,
                until: categoriesCollection.category(atIndex: 3).isVisible
            )
            categoriesCollection.collectionView.swipe(
                to: .left,
                until: categoriesCollection.category(atIndex: 0).isVisible
            )
        }

        "Переходим на уточненную категорию".run {
            let category = categoriesCollection.category(atIndex: 0)
            category.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Проверяем запрос, название категории и остутствие кнопки искать везде".run {
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, searchText)
            XCTAssertEqual(feedPage.categoryTitle.label, "Шоколадная плитка")
            XCTAssertEqual(feedPage.resultsCountLabel.label, "6")
            XCTAssertFalse(feedPage.redirectButton.isVisible)
            XCTAssertTrue(feedPage.collectionView.headerFeedFilter.element.isVisible)
        }
    }

    func test_FeedHeader_ResetCategory() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5311")
        Allure.addEpic("Выдача")
        Allure.addFeature("Отмена редиректа в категорию")
        Allure.addTitle("Редизайн фильтров. Поисковая выдача. «Искать везде»")

        let searchText = "шоколад"
        let hid = 15_713_879
        let nid = 72_087

        var feedPage: FeedPage!
        var feedState = FeedState()

        // поиск с редиректом одним запросом
        enable(toggles: FeatureNames.searchPerOneRequest)
        // используем фапи-запрос, чтобы не мокать капи
        enable(toggles: FeatureNames.fapiNavigationTree)

        stateManager?.mockingStrategy = .dtoMock

        "Мокаем состояние".run {
            // мок ответа на первый запрос с редиректом в категорию
            feedState.setSearchOrUrlTransformState(
                mapper: .init(
                    offers: FAPIOffer.all,
                    hid: hid,
                    nid: nid
                ),
                redirect: .init(
                    target: searchText,
                    hid: hid.string,
                    nid: nid.string
                )
            )
            // мок ответа на запрос поиска без уточнения категории
            feedState.setSearchStateFAPI(
                mapper: .init(
                    offers: FAPIOffer.all,
                    intent: ResolveSearch.Intent.chocolates
                )
            )

            feedState.setNavigationTree(mapper: .init(nodes: [.chocolate]))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Проверяем запрос, название категории и наличие кнопки искать везде".run {
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, searchText)
            XCTAssertEqual(feedPage.categoryTitle.label, "Шоколадные изделия")
            XCTAssertEqual(feedPage.resultsCountLabel.label, "6")
            XCTAssertTrue(feedPage.redirectButton.isVisible)
            XCTAssertTrue(feedPage.collectionView.headerFeedFilter.element.isVisible)
        }

        "Сбрасываем редирект в категорию".run {
            feedPage.redirectButton.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Проверяем запрос, отстутсвие названия категории и кнопки искать везде".run {
            XCTAssertEqual(feedPage.navigationBar.searchedTextButton.label, searchText)
            XCTAssertFalse(feedPage.categoryTitle.isVisible)
            XCTAssertFalse(feedPage.redirectButton.isVisible)
            XCTAssertTrue(feedPage.collectionView.categoriesCollectionView.element.isVisible)
            XCTAssertTrue(feedPage.collectionView.headerFeedFilter.element.isVisible)
        }
    }

    func test_FeedHeader_SortAndFilters() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5313")
        Allure.addEpic("Выдача")
        Allure.addFeature("Сортировка и экран фильтров")
        Allure.addTitle("Редизайн фильтров. Поисковая выдача. Третий этаж")

        let searchText = "шоколад"

        var feedPage: FeedPage!
        var feedState = FeedState()

        stateManager?.mockingStrategy = .dtoMock

        "Мокаем состояние".run {
            feedState.setSearchStateFAPI(mapper: .init(offers: FAPIOffer.all))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Выбираем сортировку".run {
            feedPage.collectionView.headerFeedFilter.sortButton.tap()
            let dicountsFirst = app.buttons.element(withLabelMatching: "сначала со скидками")
            dicountsFirst.tap()
            XCTAssertEqual(feedPage.collectionView.headerFeedFilter.sortButton.label, "Сначала со скидками")
        }

        "Проверяем открытие экрана фильтров".run {
            let filtersPage = feedPage.collectionView.headerFeedFilter.filterButton.tap()
            ybm_wait(forVisibilityOf: [filtersPage.element])
            filtersPage.closeButton.tap()
        }
    }
}
