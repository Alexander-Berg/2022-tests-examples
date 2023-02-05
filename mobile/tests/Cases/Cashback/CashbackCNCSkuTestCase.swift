import MarketUITestMocks
import XCTest

final class CashbackCNCSkuTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testPlusToastInvisible() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4239")
        Allure.addEpic("КМ")
        Allure.addFeature("Кешбэк для плюсовика")
        Allure.addTitle("Проверка, что тост про накопленные баллы не появляется")

        var root: RootPage!

        enable(toggles: FeatureNames.plusBenefits)

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackCNCSku")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переход на КМ, проверка отсутствия тоста про накопленные баллы".ybm_run { _ in
            let skuPage = goToDefaultSKUPage(root: root)
            skuPage.element.swipeUp()

            XCTAssertFalse(BuyForCashbackPopupPage.currentPopup.element.isVisible)
        }
    }
}
