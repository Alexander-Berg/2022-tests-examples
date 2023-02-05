import MarketUITestMocks
import XCTest

final class FeedBannersTest: LocalMockTestCase {

    func testBanner() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3265")
        Allure.addEpic("Выдача")
        Allure.addFeature("Баннеры")
        Allure.addTitle("Баннеры на выдаче")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!
        var feed: FeedPage!
        var banner: FeedPage.MediaSetSnippetCell!

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
            stateManager?.mockingStrategy = .bundleMock
            mockStateManager?.pushState(bundleName: "CatalogSet_ViewAllProducts")
            mockStateManager?.pushState(bundleName: "CatalogSet_FeedBanners")
        }

        "Переход в выдачу".ybm_run { _ in
            feed = subcategory.listHeader.showAllButton.goToFeed()
            ybm_wait(forFulfillmentOf: { feed.element.isVisible })
        }

        "Проверяем наличие баннера на выдаче".ybm_run { _ in
            banner = feed.mediaSetSnippetCell
            wait(forVisibilityOf: banner.element)
        }

        "Мокаем выдачу баннера".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Banner")
        }

        "Тапаем на баннер".ybm_run { _ in
            feed = banner.goToFeed()
            wait(forVisibilityOf: feed.element)
        }

        "Проверяем, что попали на отфильтрованную выдачу по производителю".ybm_run { _ in
            XCTAssertTrue(feed.navigationBar.title.isVisible)
            XCTAssertEqual(feed.navigationBar.title.label, "Мобильные телефоны")
        }

        "Возвращаемся обратно на выдачу с баннером, проверяем что баннер остался на месте".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()

            ybm_wait(forVisibilityOf: [feed.element, banner.element])

            XCTAssertEqual(feed.navigationBar.title.label, "Ноутбуки и аксессуары")
        }
    }
}

// MARK: - Private

private extension FeedBannersTest {

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
