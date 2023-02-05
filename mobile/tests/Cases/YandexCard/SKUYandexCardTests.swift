import MarketUITestMocks
import XCTest

final class SKUYandexCardTests: YandexCardTests {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testYandexCardInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6135")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6141")
        Allure.addEpic("КТ")
        Allure.addFeature("YandexCard")
        Allure.addTitle("Информер об акции")

        var root: RootPage!
        var skuPage: SKUPage!
        var cashback: SKUPage.CashbackItem!

        setupFlags()

        "Настраиваем стейт".ybm_run { _ in
            stateManager?.setState(newState: makeState())
        }

        "Открываем карточку".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: root)
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            cashback = skuPage.cashback
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: cashback.text)
            XCTAssertEqual(cashback.text.label, " 28 и ещё  100 со Счётом в Яндексе")
        }

        "Переходим в сдк банка через детализацию ".ybm_run { _ in
            cashback.element.tap()

            let popupPage = CashbackDetailsAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            popupPage.closeButton.tap()

            let yanbankSDK = YandexBankPage.current
            wait(forVisibilityOf: yanbankSDK.element)
        }
    }

    private func makeState() -> SKUInfoState {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [
            .promoWithCashback("28"), .promoWithYandexCardCashback("100")
        ]
        config.cashbackDetails = [.defaultCashback, .yandexCardExtraCashback]

        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .custom(config))

        return skuState
    }
}
