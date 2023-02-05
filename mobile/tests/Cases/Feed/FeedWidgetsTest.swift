import MarketUITestMocks
import XCTest

final class FeedWidgetsTest: LocalMockTestCase {

    func testCatalogFeedWidgets() {
        /*
         Тест отображения сниппетов и добавление товар в корзину
         вынесен в CartRecommendationsWidgetAdapterTest для сокращения времени прогона тестов
         */
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3018")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3019")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3212")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3213")
        Allure.addEpic("Категорийная выдача")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем отображение скроллбоксов")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!
        var feed: FeedPage!

        // Вспомогательные функции
        func test(
            widget: FeedPage.CollectionView.CarouselCell,
            with title: String,
            in collectionView: FeedPage.CollectionView
        ) {
            collectionView.element.swipe(to: .down, untilExist: widget.element)

            XCTAssertEqual(widget.title.label, title)
            XCTAssertTrue(widget.collectionView.isVisible)
        }

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в подкатегорию".ybm_run { _ in
            let departmentPage = goToDepartament(fromCatalog: catalog)
            subcategory = goToSubcategory(fromDepartment: departmentPage)
        }

        "Мокаем состояние выдачи".ybm_run { _ in
            stateManager?.removeAllStates()

            var navigationTreeState = NavigationTreeState()
            navigationTreeState.setNavigationTreeState(.laptops)
            stateManager?.setState(newState: navigationTreeState)

            mockStateManager?.pushState(bundleName: "FeedSet_CatalogFeed_Gift")
        }

        "Переход в фид".ybm_run { _ in
            let cell = subcategory.subcategoryTreeCell(index: 2)
            feed = cell.goToFeed()
            ybm_wait(forFulfillmentOf: { feed.element.isVisible })
        }

        "Проверяем виджет \"Подобрали для вас\"".ybm_run { _ in
            let recommendedByHistoryWidget = feed.collectionView.recommendedByHistoryCell
            test(widget: recommendedByHistoryWidget, with: "Подобрали для вас", in: feed.collectionView)
        }
    }

    func testEmptyFeedWidgets() {
        /*
         Тест отображения сниппетов и добавления товара в корзину
         вынесен в CartRecommendationsWidgetAdapterTest для сокращения времени прогона тестов
         */
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3016")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3017")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3210")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3211")
        Allure.addEpic("Поисковая выдача без товаров")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем отображение скроллбоксов")

        // Вспомогательные функции
        func test(
            widget: FeedPage.CollectionView.CarouselCell,
            with title: String,
            in collectionView: FeedPage.CollectionView
        ) {
            collectionView.element.swipe(to: .down, untilExist: widget.element)

            XCTAssertEqual(widget.title.label, title)
            XCTAssertTrue(widget.collectionView.isVisible)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Empty")
        }

        var collectionView: FeedPage.CollectionView!

        "Открываем выдачу".ybm_run { _ in
            collectionView = goToFeed().collectionView
        }

        "Проверяем виджет \"Подобрали для вас\"".ybm_run { _ in
            let recommendedByHistoryWidget = collectionView.recommendedByHistoryCell
            test(widget: recommendedByHistoryWidget, with: "Подобрали для вас", in: collectionView)
        }
    }
}

// MARK: - Private

private extension FeedWidgetsTest {

    func setupCatalogCMSState() {
        stateManager?.mockingStrategy = .dtoMock

        var cmsState = CMSState()
        cmsState.setCMSState(with: CMSState.CMSCollections.rootCatalogCollections)
        stateManager?.setState(newState: cmsState)

        var electronicsDepartmentState = CMSState()
        electronicsDepartmentState.setCMSState(with: CMSState.CMSCollections.electronicsDepartamentCollections)
        stateManager?.setState(
            newState: electronicsDepartmentState,
            matchedBy: hasStringInBody("\"type\":\"mp_department_app\"")
        )

        var laptopAndAccessoriesCategoryState = CMSState()
        laptopAndAccessoriesCategoryState
            .setCMSState(with: CMSState.CMSCollections.laptopAndAccessoriesCategoryCollections)
        stateManager?.setState(
            newState: laptopAndAccessoriesCategoryState,
            matchedBy: hasStringInBody("\"type\":\"mp_navigation_node_app\"")
        )

        var navigationImagesState = CMSState()
        navigationImagesState.setCMSState(with: CMSState.CMSCollections.navigationImagesCollections)
        stateManager?.setState(
            newState: navigationImagesState,
            matchedBy: hasStringInBody("\"type\":\"mp_navigation_node_images\"")
        )
    }
}
