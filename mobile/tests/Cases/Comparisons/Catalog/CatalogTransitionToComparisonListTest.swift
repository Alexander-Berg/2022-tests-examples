import MarketUITestMocks
import XCTest

class CatalogTransitionToComparisonListTest: LocalMockTestCase {

    func testTransitionToComparisonList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3529")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Каталог")
        Allure.addTitle("Добавление из категорийной выдачи")

        var catalog: CatalogPage!
        var equipmentSubcategoryPage: SubcategoryPage!
        var medicalEquipmentFeedPage: FeedPage!
        var comparisonPage: ComparisonPage!

        "Мокаем состояние каталога".ybm_run { _ in
            setupCatalogCMSState()

            var comparisonState = ComparisonState()
            comparisonState.addItemToComparison()
            stateManager?.setState(newState: comparisonState)
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в подкатегорию".ybm_run { _ in
            let equipmentDepartmentPage = goToDepartament(fromCatalog: catalog)
            equipmentSubcategoryPage = goToSubcategory(fromDepartment: equipmentDepartmentPage)
        }

        "Мокаем состояние выдачи и сравнения".ybm_run { _ in
            stateManager?.removeAllStates()

            var navigationTreeState = NavigationTreeState()
            navigationTreeState.setNavigationTreeState(.alcotest)
            stateManager?.setState(newState: navigationTreeState)

            mockStateManager?.pushState(bundleName: "Comparisons_CatalogTransitionToComparisonList")
        }

        "Переход на категорийную выдачу".ybm_run { _ in
            let cell = equipmentSubcategoryPage.subcategoryTreeCell(index: 2)
            medicalEquipmentFeedPage = cell.goToFeed()
        }

        let snippetPage = medicalEquipmentFeedPage.collectionView.cellPage(at: 0)

        "Проверяем кнопку сравнения на первой карточке товара".ybm_run { _ in
            medicalEquipmentFeedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            wait(forVisibilityOf: snippetPage.comparsionButton)
            XCTAssertFalse(snippetPage.comparsionButton.isSelected)
        }

        "Тапаем на кнопку и переходим в список сравнения".ybm_run { _ in
            snippetPage.comparsionButton.tap()

            let popup = AddToComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.titleLabel)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")

            comparisonPage = popup.tap()
            wait(forExistanceOf: comparisonPage.element)
        }

        "Проверяем данные в КМ".ybm_run { _ in
            let cellPage = comparisonPage.collectionView.modelCell(with: 0)
            wait(forExistanceOf: cellPage.element)
            wait(forExistanceOf: cellPage.photo.element)
            wait(forExistanceOf: cellPage.title.element)
            XCTAssertEqual(cellPage.title.element.label, "Алкотестер Inspector AT750")
        }
    }
}

// MARK: - Private

private extension CatalogTransitionToComparisonListTest {

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
