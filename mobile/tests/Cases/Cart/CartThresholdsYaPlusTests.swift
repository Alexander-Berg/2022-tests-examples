import MarketUITestMocks
import XCTest

final class CartThresholdsYaPlusTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testExpressWithDsbsThresholdsWithPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Экспресс + ДСБС трешхолды с плюсом")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartDSBSThresholdPlusMore")
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
                "Доставка товаров ниже будет бесплатной благодаря Плюсу"
            )
        }

        mockStateManager?.pushState(bundleName: "CartDSBSThresholdPlusLess")

        "Проверяем трешхолд экспресс + дсбс с плюсом < 699".ybm_run { _ in
            goToMorda(root: root)
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Еще 100 ₽, и товары ниже приедут бесплатно благодаря Плюсу"
            )
        }
    }

    func testExpressMoreThresholdWithPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Экспресс трешхолды. С плюсом")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartDSBSThresholdPlusMore") // для плюса
            mockStateManager?.pushState(bundleName: "CartExpressThresholdsLess")
            mockStateManager?.pushState(bundleName: "CartExpressThresholdMore")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.element)
        }

        "Проверяем трешхолд экспресс > 699".ybm_run { _ in
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Доставка будет дешевле благодаря Плюсу"
            )
        }
    }
}
