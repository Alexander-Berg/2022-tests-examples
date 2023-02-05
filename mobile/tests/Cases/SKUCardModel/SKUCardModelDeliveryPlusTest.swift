import MarketUITestMocks
import XCTest

final class SKUCardModelDeliveryPlusTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    // MARK: - Tests

    func testFreeDeliveryByPlusWithPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4178")
        Allure.addEpic("КМ")
        Allure.addFeature("Опции")
        Allure.addTitle("Проверяем отсутствие надписи 'С Яндекс Плюс доставка бесплатная' у плюсовика")

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = [
            FeatureNames.showPlus,
            FeatureNames.plusBenefits
        ].joined(separator: ",")

        var root: RootPage!

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDeliveryWithPlus")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка отсутствия блока с надписью 'С Яндекс Плюс доставка бесплатная'".ybm_run { _ in
            var skuPage: SKUPage!

            skuPage = goToDefaultSKUPage(root: root)
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)

            XCTAssertFalse(skuPage.freeDeliveryByYandexPlus.exists)

            goToMorda(root: root)
        }
    }

}
