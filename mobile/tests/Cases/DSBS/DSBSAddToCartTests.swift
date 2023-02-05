import MarketUITestMocks
import XCTest

final class DSBSAddToCartTests: LocalMockTestCase {

    func testAddToCartFromCatalog() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4137")
        Allure.addEpic("DSBS. КО.")
        Allure.addFeature("Добавление в корзину через каталог")

        enable(
            toggles:
            FeatureNames.fapiNavigationTree
        )

        var catalog: CatalogPage!
        var subcategoryPage: SubcategoryPage!
        var feed: FeedPage!
        var snippet: FeedSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в подкатегорию".ybm_run { _ in
            let departmentPage = goToDepartament(fromCatalog: catalog)
            subcategoryPage = goToSubcategory(fromDepartment: departmentPage)
        }

        "Мокаем состояние выдачи".ybm_run { _ in
            stateManager?.mockingStrategy = .bundleMock
            mockStateManager?.pushState(bundleName: "DSBSSearchResults")
            mockStateManager?.pushState(bundleName: "DSBSAddToCartFromCatalog")
        }

        "Переход в фид ручек".ybm_run { _ in
            let cell = subcategoryPage.subcategoryTreeCell(index: 2)
            feed = cell.goToFeed()
        }

        "Нажимаем добавить в корзину".ybm_run { _ in
            snippet = feed.collectionView.cellPage(at: 0)

            snippet.addToCartButton.element.tap()

            ybm_wait(forFulfillmentOf: {
                snippet.addToCartButton.minusButton.isVisible == true &&
                    snippet.addToCartButton.plusButton.isVisible == true
            })

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
        }

        "Проверяем сниппет".ybm_run { _ in
            XCTAssertTrue(snippet.currentPrice.isVisible)
            XCTAssertTrue(snippet.titleLabel.isVisible)
            XCTAssertTrue(snippet.imageView.isVisible)
            XCTAssertTrue(snippet.wishListButton.isVisible)
            XCTAssertTrue(snippet.comparsionButton.isVisible)
        }

    }

    func testAddToCartFromSearchNoMskuHasModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4138")
        Allure.addEpic("DSBS. КО.")
        Allure.addFeature("Добавление в корзину через поиск")
        Allure.addTitle("Без msku, но с моделью")

        var root: RootPage!

        mockStateManager?.pushState(bundleName: "DSBSSearchResults")

        addSearchMatchRule(
            id: "0",
            searchQuery: "Внешний аккумулятор Samsung NRG Power 5000 мАч, 2.1A серый"
        )

        "Открываем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            checkSnippet(
                root: root,
                query: "Внешний аккумулятор Samsung NRG Power 5000 мАч, 2.1A серый",
                shouldCheckCartButton: true
            )
        }
    }

    func testAddToCartFromSearchHasMsku() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4138")
        Allure.addEpic("DSBS. КО.")
        Allure.addFeature("Добавление в корзину через поиск")
        Allure.addTitle("С msku")

        var root: RootPage!

        mockStateManager?.pushState(bundleName: "DSBSSearchResults")

        addSearchMatchRule(
            id: "2",
            searchQuery: "Электрическая варочная поверхность Samsung C61R1CAMST/BWT черный"
        )

        "Открываем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            checkSnippet(
                root: root,
                query: "Электрическая варочная поверхность Samsung C61R1CAMST/BWT черный"
            )
        }
    }

    func testAddToCartFromSearchNoMskuNoModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4138")
        Allure.addEpic("DSBS. КО.")
        Allure.addFeature("Добавление в корзину через поиск")
        Allure.addTitle("Без msku и без модели")

        var root: RootPage!

        mockStateManager?.pushState(bundleName: "DSBSSearchResults")

        addSearchMatchRule(
            id: "1",
            searchQuery: "Чехол Dbramante1928 Copenhagen Slim для Galaxy S20+, кожа коричневый"
        )

        "Открываем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            checkSnippet(
                root: root,
                query: "Чехол Dbramante1928 Copenhagen Slim для Galaxy S20+, кожа коричневый"
            )
        }
    }
}

// MARK: - Private

private extension DSBSAddToCartTests {

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

    private func checkSnippet(root: RootPage, query: String, shouldCheckCartButton: Bool = false) {
        "Проверяем сниппет".ybm_run { _ in
            let feed = goToFeed(root: root, with: query)
            wait(forExistanceOf: feed.element)

            let snippet = feed.collectionView.cellPage(at: 0)
            feed.element.ybm_swipeCollectionView(toFullyReveal: snippet.element)

            XCTAssertTrue(snippet.currentPrice.isVisible)
            XCTAssertTrue(snippet.titleLabel.isVisible)
            XCTAssertTrue(snippet.imageView.isVisible)
            XCTAssertTrue(snippet.addToCartButton.element.isVisible)

            if shouldCheckCartButton {
                mockStateManager?.pushState(bundleName: "DSBSAddToCart")

                snippet.addToCartButton.element.tap()

                ybm_wait(forFulfillmentOf: {
                    snippet.addToCartButton.minusButton.isVisible == true &&
                        snippet.addToCartButton.plusButton.isVisible == true
                })

                ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
            }
        }
    }

    private func addSearchMatchRule(id: String, searchQuery: String) {
        let rule = MockMatchRule(
            id: id,
            matchFunction:
            isPOSTRequest &&
                isFAPIRequest &&
                hasExactFAPIResolvers(["resolveSearchOrRedirect"]) &&
                hasStringInBody("\"text\":\"\(searchQuery)\""),
            mockName: "POST_api_v1_resolveSearchOrRedirect_\(id)"
        )

        mockServer?.addRule(rule)
    }
}
