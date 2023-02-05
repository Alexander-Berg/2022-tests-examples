import MarketUITestMocks
import XCTest

class CatalogAddToComparsionListTest: LocalMockTestCase {

    func testAddAndRemoveFromComparsionList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3538")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Каталог")
        Allure.addTitle("Удаление из категорийной выдачи")

        var catalog: CatalogPage!
        var subcategoryPage: SubcategoryPage!
        var hooversFeedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
            mockStateManager?.pushState(bundleName: "Comparsions_CatalogAddToListStart")
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в подкатегорию".ybm_run { _ in
            let departmentPage = goToDepartament(fromCatalog: catalog)
            subcategoryPage = goToSubcategory(fromDepartment: departmentPage)
        }

        "Мокаем состояние выдачи и сравнения".ybm_run { _ in
            stateManager?.mockingStrategy = .bundleMock
            mockStateManager?.pushState(bundleName: "Comparsions_CatalogAddToList")
        }

        "Переход в фид пылесосов".ybm_run { _ in
            let cell = subcategoryPage.subcategoryTreeCell(index: 2)
            hooversFeedPage = cell.goToFeed()
        }

        "Ищем карточку товара".ybm_run { _ in
            snippetPage = hooversFeedPage.collectionView.cellPage(at: 0)
            hooversFeedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
        }

        "Проверяем кнопку сравнения на первой карточке товара".ybm_run { _ in
            XCTAssertTrue(snippetPage.comparsionButton.isVisible)
            XCTAssertTrue(snippetPage.comparsionButton.isSelected)
        }

        "Тапаем на кнопку, проверяем тост, возвращаем товар".ybm_run { _ in
            snippetPage.comparsionButton.tap()
            hooversFeedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.comparsionButton)
            XCTAssertFalse(snippetPage.comparsionButton.isSelected)

            let popup = RemoveFromComparsionToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар удалён")

            popup.actionButton.tap()
        }

        "Проверяем, что товар вернулся".ybm_run { _ in
            let popup = AddToComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")

            XCTAssertTrue(snippetPage.comparsionButton.isSelected)
        }
    }
}

// MARK: - Private

private extension CatalogAddToComparsionListTest {
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
