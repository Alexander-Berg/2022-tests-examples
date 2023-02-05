import MarketUITestMocks
import XCTest

final class CartPlusCNCTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    private let skuId = "100126177720"

    func testPlusIsInvisible() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4196")
        Allure.addEpic("Корзина")
        Allure.addFeature("Блок Плюса")
        Allure.addTitle("Проверяем, что в корзине с CNC товаром не появляется блок Плюса")

        var root: RootPage!
        var cartPage: CartPage!
        var skuPage: SKUPage!

        enable(
            toggles:
            FeatureNames.plusBenefits,
            FeatureNames.showPlus
        )

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartPlusCellCNCYandexPlus")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на КМ, добавляем товар в корзину".ybm_run { _ in
            open(market: .sku(skuId: skuId))
            skuPage = SKUPage.current
            wait(forVisibilityOf: skuPage.element)
            skuPage.element.swipeUp()
            XCTAssertTrue(skuPage.addToCartButton.element.isVisible)

            let button = skuPage.addToCartButton
            button.element.tap()
        }

        "Открываем корзину и проверяем, что отсутствует блок Плюса".ybm_run { _ in
            cartPage = goToCart(root: root)

            XCTAssertFalse(cartPage.plusSubscriptionCell.element.isVisible)
        }
    }
}
