import XCTest

final class SKUCardModelServerErrorsTest: LocalMockTestCase {

    func testShouldHandleFapiUnexpectedError() {
        Allure.addEpic("КМ")
        Allure.addFeature("Ошибки")
        Allure.addTitle("Проверяем, что при Unexpected ошибке от фапи приложение не падает")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента FAPI_SKU_METHOD_IOS_TEST".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_SKU_FAPI")
        }

        "Мокаем ответ ручки FAPI resolveSKU с UnexpectedError".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_SKUFapiUnexpectedError")
        }

        var root: RootPage!
        var skuPage: SKUPage!

        "Открываем SKU".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: root)
        }

        "Проверяем, что отобразилась урезенная версия SKU".ybm_run { _ in
            wait(forVisibilityOf: skuPage.element)
            wait(forInvisibilityOf: skuPage.navigationBar.wishlistButton)
            wait(forInvisibilityOf: skuPage.navigationBar.comparisonButton)
        }
    }
}
