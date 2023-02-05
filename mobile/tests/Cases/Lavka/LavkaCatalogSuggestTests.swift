import MarketUITestMocks
import XCTest

final class LavkaCatalogSuggestTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    override func setUp() {
        super.setUp()
        enable(
            toggles:
            FeatureNames.lavkaInMarket_v2,
            FeatureNames.lavkaInMarket_onboarding
        )
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
    }

    func testSearchZeroSuggest() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6227")
        Allure.addEpic("Лавка на Маркете")
        Allure.addTitle("Каталог. Нулевой Саджест")

        var catalog: CatalogPage!
        var search: SearchPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
            setupGroceriesStates()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Нажимаем на поисковую строку \"Найти товары\"".ybm_run { _ in
            wait(forVisibilityOf: catalog.searchButton.element)
            search = catalog.searchButton.tap()
        }

        "Проверяем текст на саджесте лавки".ybm_run { _ in
            XCTAssertTrue(search.zeroSuggestTitle.isVisible)
            XCTAssertTrue(search.zeroSuggestSubtitle.isVisible)
            XCTAssertEqual(search.zeroSuggestTitle.label, "Продукты")
            XCTAssertEqual(search.zeroSuggestSubtitle.label, "Быстрая доставка от 10 минут")
            search.zeroSuggestTitle.tap()
        }

        "Проверяем что tab \"Продукты\" открывается".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { TabBarPage.current.productTabItem.element.isSelected })
        }
    }

    // MARK: - Private

    private func setupGroceriesStates() {
        let lavkaState = LavkaState()
        stateManager?.setState(newState: lavkaState)
    }

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

    // MARK: - Private

    private var toggleInfo: String {
        let lavkaInMarketMainFeature = FeatureNames.lavkaInMarket_v2.lowercased()
        let lavkaInMarketOnboardingForGroceries = FeatureNames.lavkaInMarket_onboarding.lowercased()

        let toggleAdditionalInfo = [
            lavkaInMarketMainFeature: [
                "supportedRegions": [213],
                "forceEnabled": true
            ],
            lavkaInMarketOnboardingForGroceries: [
                "supportedRegions": ["213": 163_690]
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )
        else {
            return ""
        }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }
}
