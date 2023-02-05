import MarketUITestMocks
import XCTest

final class PharmaAnalogsTest: LocalMockTestCase {

    func testPharmaAnalogsSectionHasAnalogsGroups() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6077")
        Allure.addEpic("Секция аналогов на КМ")
        Allure.addFeature("Аналоги медицинских товаров")
        Allure
            .addTitle(
                "Проверяем секцию аналогов на КМ"
            )

        var skuPage: SKUPage!
        var analogsTabsPage: MedicineAnalogsTabsPage!
        var analogsOffersPage: MedicineAnalogsOffersPage!

        "Мокаем состояние".ybm_run { _ in
            setupStates()
        }

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.medicineReleaseGroupFeature
            )
        }

        "Открываем экран аналогов".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Скроллим к виджету аналогов".ybm_run { _ in
            wait(forVisibilityOf: skuPage.collectionView)
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.medicineAnalogsOffers.element)
            wait(forVisibilityOf: skuPage.medicineAnalogsTabs.element)
            wait(forVisibilityOf: skuPage.medicineAnalogsOffers.element)
            analogsTabsPage = skuPage.medicineAnalogsTabs
            analogsOffersPage = skuPage.medicineAnalogsOffers
        }

        "Проверяем наличие табов".ybm_run { _ in
            XCTAssertTrue(analogsTabsPage.element.isVisible)
        }

        "Проверяем что активен таб 'Все'".ybm_run { _ in
            XCTAssertEqual(analogsOffersPage.itemText(at: 0).label, "offerName_1")
            XCTAssertEqual(analogsOffersPage.itemText(at: 1).label, "offerName_2")
        }

        "Тапаем на таб 'Таблетки'".ybm_run { _ in
            analogsTabsPage.tabItem(at: 1).tap()
        }

        "Проверяем, что содержимое скроллбокса поменялось".ybm_run { _ in
            wait(forVisibilityOf: analogsOffersPage.itemText(at: 0))
            XCTAssertEqual(analogsOffersPage.itemText(at: 0).label, "offerName_3")
            XCTAssertEqual(analogsOffersPage.itemText(at: 1).label, "offerName_4")
        }

    }

}

// MARK: - Test state setup methods

private extension PharmaAnalogsTest {
    func setupStates() {
        setupExperiment()
        setupSkuState()
        setupPrimeState()
    }

    func setupExperiment() {
        var defaultState = DefaultState()

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента medicine_release_group_on_test".ybm_run { _ in
            defaultState.setExperiments(experiments: [.medicineReleaseGroupOnTest])
            stateManager?.setState(newState: defaultState)
        }
    }

    func setupSkuState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .pharmaCms)
        stateManager?.setState(newState: skuState)
    }

    func setupPrimeState() {
        var primeState = PrimeState()
        primeState.setPrimeSearchState(with: .pharma)
        stateManager?.setState(newState: primeState)
    }

}
