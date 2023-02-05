import MarketUITestMocks
import XCTest

class CatalogDepartamentTest: LocalMockTestCase {

    func testPictures() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-964")
        Allure.addEpic("Каталог")
        Allure.addFeature("Департаменты")
        Allure.addTitle("Картинки в департаментах")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        let departmentData = generateDepartmentData()

        "Проверяем отображение департаментов с картинками и именами".ybm_run { _ in
            for (index, element) in departmentData.enumerated() {
                let indexPath = IndexPath(
                    item: index + Constants.topSeparatorsCount,
                    section: 0
                )
                let cell = catalog.departmentCell(at: indexPath)
                XCTAssertEqual(element, cell.title.label)
                XCTAssertTrue(cell.image.exists)
            }
        }

        "Переход в департамент: отображается заголовок, картинка и название у категории".ybm_run { _ in
            // тап по любому департаменту в каталоге
            let tapIndex = 0

            subcategory = goToDepartament(
                fromCatalog: catalog,
                atIndex: tapIndex + Constants.topSeparatorsCount
            )

            XCTAssertEqual(subcategory.header.title.label, departmentData[tapIndex])

            let subcategoryCell = subcategory.subcategoryTreeCell(index: 1)

            XCTAssertTrue(subcategoryCell.title.exists)
            XCTAssertTrue(subcategoryCell.image.exists)

            ybm_wait(forFulfillmentOf: { subcategoryCell.element.isVisible })
            subcategoryCell.element.tap()
        }

        "Переход в подкатегорию: отображается заголовок, название у  категории"
            .ybm_run { _ in
                subcategory = SubcategoryPage.current
                ybm_wait(forFulfillmentOf: { subcategory.element.isVisible })

                XCTAssertEqual(subcategory.listHeader.title.label, "Ноутбуки и аксессуары")

                let subcategoryCell = subcategory.subcategoryTreeCell(index: 2)
                XCTAssertTrue(subcategoryCell.title.exists)
            }
    }

    func testBackButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-970")
        Allure.addEpic("Каталог")
        Allure.addFeature("Департаменты")
        Allure.addTitle("Переход назад")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в 1-ый департамент".ybm_run { _ in
            subcategory = goToDepartament(fromCatalog: catalog)
        }

        "Проверка кнопки назад: наличие, переход в рутовую категорию".ybm_run { _ in
            let backButton = subcategory.backButton
            ybm_wait(forFulfillmentOf: { backButton.isVisible })

            backButton.tap()

            ybm_wait(forFulfillmentOf: { catalog.element.isVisible })
        }
    }

    func testSearch() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-980")
        Allure.addEpic("Каталог")
        Allure.addFeature("Департаменты")
        Allure.addTitle("Переход к поиску")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в 1-ый департамент".ybm_run { _ in
            subcategory = goToDepartament(fromCatalog: catalog)
        }

        "Проверка кнопки поиска: наличие, переход в поиск".ybm_run { _ in
            let searchButton = subcategory.searchButton
            ybm_wait(forFulfillmentOf: { searchButton.element.isVisible })

            let searchPage = searchButton.tap()
            ybm_wait(forFulfillmentOf: { searchPage.element.exists })
        }
    }

    func testViewAllProductsUnderTheTitle() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3264")
        Allure.addEpic("Каталог")
        Allure.addFeature("Департаменты")
        Allure.addTitle("Кнопка \"все\" справа от тайтла категории")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!
        var feed: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в департамент".ybm_run { _ in
            let indexPath = IndexPath(item: Constants.topSeparatorsCount, section: 0)
            let department = catalog.departmentCell(at: indexPath)
            ybm_wait(forFulfillmentOf: { department.element.isVisible })

            subcategory = department.tap()

            ybm_wait(forFulfillmentOf: { subcategory.element.isVisible })
        }

        "Переход в категорию".ybm_run { _ in
            let subcategoryCell = subcategory.subcategoryTreeCell(index: 1)
            ybm_wait(forFulfillmentOf: { subcategoryCell.element.isVisible })

            subcategoryCell.element.tap()

            subcategory = SubcategoryPage.current
            ybm_wait(forFulfillmentOf: { subcategory.element.isVisible })
        }

        "Мокаем состояние поисковой выдачи".ybm_run { _ in
            stateManager?.mockingStrategy = .bundleMock
            mockStateManager?.pushState(bundleName: "CatalogSet_ViewAllProducts")
        }

        "Тапаем на кнопку \"все\" справа от тайтла, переходим на выдачу".ybm_run { _ in
            feed = subcategory.listHeader.showAllButton.goToFeed()
            ybm_wait(forFulfillmentOf: { feed.element.isVisible })

            XCTAssertEqual(feed.navigationBar.title.label, "Ноутбуки и аксессуары")
        }

        "Отображаются найденные товары: проверка карточки первого товара".ybm_run { _ in
            let snippetPage = feed.collectionView.cellPage(at: 0)
            feed.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.element.isVisible)
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            XCTAssertTrue(snippetPage.titleLabel.isVisible)
            XCTAssertTrue(snippetPage.imageView.isVisible)
            XCTAssertTrue(snippetPage.currentPrice.isVisible)
            XCTAssertTrue(snippetPage.ratingLabel.isVisible)
            XCTAssertTrue(snippetPage.wishListButton.isVisible)
            XCTAssertTrue(snippetPage.reasonsToBuyRecomendations.element.isVisible)
        }
    }
}

// MARK: - Private

private extension CatalogDepartamentTest {

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

    func generateDepartmentData() -> [String] {
        [
            "Электроника",
            "Компьютерная техника",
            "Бытовая техника"
        ]
    }
}

// MARK: - Nested types

private extension CatalogDepartamentTest {
    enum Constants {
        static let topSeparatorsCount = 1
    }
}
