import MarketUITestMocks
import XCTest

final class CartThresholdsAuthTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testExpressWithDsbsThresholds() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Экспресс + ДСБС трешхолды")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartDSBSThresholdMore")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем трешхолд экспресс + дсбс > 699".ybm_run { _ in
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Бесплатная доставка заказа от 699 ₽ для тех, кто в Плюсе"
            )
        }
    }

    func testKgtThresholds() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("КГТ")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartKgtThreshold")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем трешхолд КГТ".ybm_run { _ in
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Ваш заказ крупногабаритный, поэтому доставка будет дороже, чем обычно\(String.ble_nonBreakingSpace)"
            )
        }
    }

    func testCouponThresholds() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Купон на доставку")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartCouponThresold")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем трешхолд с купоном".ybm_run { _ in
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Доставка товаров ниже будет бесплатной благодаря купону"
            )
        }
    }
}
