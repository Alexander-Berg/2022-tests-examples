import MarketUITestMocks
import XCTest

final class CatalogGiftTest: LocalMockTestCase {

    func testGiftsInDepartmentsPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3020")
        Allure.addEpic("Каталог")
        Allure.addFeature("Акционные товары. Сетка департаментов")
        Allure.addTitle("Популярные товары")

        var catalog: CatalogPage!
        var recommendationCell: CatalogPage.RecommendationCell!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
            mockStateManager?.pushState(bundleName: "CatalogSet_Gifts")
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Скролим до предложений под департаментами".ybm_run { _ in
            recommendationCell = catalog.recommendationCell(at: IndexPath(item: 2, section: 1))
            ybm_wait(forFulfillmentOf: { recommendationCell.element.isVisible })
        }

        "Добавление товара с подарком в корзину".ybm_run { _ in
            let cartButton = recommendationCell.cartButton

            wait(forVisibilityOf: cartButton.element)
            cartButton.element.tap()

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }
    }

    func testGiftsInSubdepartmentsPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3021")
        Allure.addEpic("Каталог")
        Allure.addFeature("Акционные товары. Поддепартамент")
        Allure.addTitle("Самые популярные товары в этой категории")

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!
        var recommendationCell: SubcategoryPage.RecommendationCell!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
            mockStateManager?.pushState(bundleName: "CatalogSet_Gifts")
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в департамент".ybm_run { _ in
            let someDepartment = catalog.departmentCell(at: IndexPath(item: 2, section: 0))

            catalog.element.swipe(to: .down, untilVisible: someDepartment.element)
            wait(forVisibilityOf: someDepartment.element)

            subcategory = someDepartment.tap()

            wait(forVisibilityOf: subcategory.element)
            XCTAssertEqual(subcategory.header.title.label, "Электроника")
        }

        "Скролим до популярных товаров".ybm_run { _ in
            let indexPath = IndexPath(item: 2, section: 2)
            recommendationCell = subcategory.recommendationCell(index: indexPath)

            subcategory.element.ybm_swipeCollectionView(toFullyReveal: recommendationCell.element)

            wait(forVisibilityOf: recommendationCell.element)
        }

        "Добавление товара с подарком в корзину".ybm_run { _ in
            let cartButton = recommendationCell.cartButton
            ybm_wait(forFulfillmentOf: { cartButton.isVisible })
            subcategory.element.swipe(to: .down, untilVisible: cartButton)

            cartButton.tap()

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина2" })
        }
    }

}

private extension CatalogGiftTest {
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

        var navigationImagesState = CMSState()
        navigationImagesState.setCMSState(with: CMSState.CMSCollections.navigationImagesCollections)
        stateManager?.setState(
            newState: navigationImagesState,
            matchedBy: hasStringInBody("\"type\":\"mp_navigation_node_images\"")
        )
    }
}
