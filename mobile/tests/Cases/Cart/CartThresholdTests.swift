import MarketUITestMocks
import XCTest

final class CartThresholdsTests: LocalMockTestCase {

    func testExpressLessThreshold() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Экспресс трешхолды")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartExpressThresholdsLess")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forVisibilityOf: cartPage.element)
        }

        "Проверяем трешхолд экспресс < 699".ybm_run { _ in
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Добавьте товаров от продавца «ОГО! Онлайн-гипермаркет» на 50 ₽, и доставка будет стоить дешевле Добавить"
            )
        }
    }

    func testExpressMoreThreshold() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4413")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Унифицированные тарифы. Трешхолды")
        Allure.addTitle("Экспресс трешхолды")

        var root: RootPage!
        var cartPage: CartPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartExpressThresholdsLess")
            mockStateManager?.pushState(bundleName: "CartExpressThresholdMore")
        }

        "Настраиваем стейт кешбэка".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
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
                "Доставка ещё дешевле — для тех, кто в Плюсе"
            )
        }
    }
}
