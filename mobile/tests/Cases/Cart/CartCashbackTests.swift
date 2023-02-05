import MarketUITestMocks
import XCTest

final class CartCashbackTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    /*
     Тест отображения информации о плюсе в корзине
     */
    func testCashbackInProductSnippet() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3582")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3583")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3584")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Кешбэк. Бейдж на сниппете товара")
        Allure.addTitle("На сниппете товара отображается бейдж кешбэка “Х баллов на Плюс”")

        var cartPage: CartPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт".ybm_run { _ in

            var authState = UserAuthState()
            var cartState = CartState()

            authState.setPlusBalanceState(.noMarketCashback)

            var item1 = FAPIOffer.default1
            item1.cartItemInCartId = 555
            var item2 = FAPIOffer.default2
            item2.cartItemInCartId = 666
            cartState.setCartStrategy(with: [
                item1,
                item2
            ])

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.addCashback(
                for: [
                    (value: 20, offer: item1),
                    (value: 49, offer: item2)
                ]
            )

            cartState.setUserOrdersState(with: orderOptionsState)

            cartState.setThresholdInfoState(with: .init(
                info: .no_free_delivery,
                forReason: .regionWithoutThreshold
            ))

            stateManager?.setState(newState: authState)
            stateManager?.setState(newState: cartState)
        }

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackInCart")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Смотрим что за товары полагается кешбэк".ybm_run { _ in
            XCTAssertEqual(cartPage.cartItem(at: 0).cashback.label, " 20 баллов")
            XCTAssertEqual(cartPage.cartItem(at: 1).cashback.label, " 49 баллов")
        }

        "Смотрим что в итого выводится кешбэк".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalCashback.element)
            XCTAssertEqual(cartPage.summary.totalCashback.title.label, "Вернётся на Плюс")
            XCTAssertEqual(cartPage.summary.totalCashback.details.label, " 69")
        }

        "Проверяем тайтл попапа".ybm_run { _ in
            cartPage.summary.totalCashback.element.tap()
            let popup = CashbackAboutPage.current
            wait(forVisibilityOf: popup.element)
            XCTAssertEqual(popup.title.label, "Кешбэк от Яндекс Плюса")
        }
    }
}
