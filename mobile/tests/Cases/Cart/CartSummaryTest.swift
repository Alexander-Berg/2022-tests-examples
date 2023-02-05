import MarketUITestMocks
import XCTest

final class CartSummaryTest: LocalMockTestCase {

    typealias Values = ResolveThresholdInfo.ThresholdInfo.Values

    func testCartSummaryClickAndCollect() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2772")
        Allure.addEpic("Корзина")
        Allure.addFeature("Саммари")
        Allure.addTitle("Проверяем, что для c&c товаров недоступен кредит и не отображается информация о доставке")

        var cart: CartPage!
        var defaultState = DefaultState()
        var cartState = CartState()

        "Настраиваем FT и мокаем startup для получения эксперимента all_tinkoff_credit_exp".ybm_run { _ in
            enable(toggles: FeatureNames.tinkoffCredit)
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            defaultState.setExperiments(experiments: [.tinkoffCreditExp])
        }

        "Настраиваем стейт корзины - трешхоллд".ybm_run { _ in
            let values = Values(
                info: .no_free_delivery,
                forReason: .regionWithoutThreshold
            )
            cartState.setThresholdInfoState(with: values)
            cartState.setUserOrdersState(with: .dropship)
        }

        "Мокаем ручки".ybm_run { _ in
            stateManager?.setState(newState: defaultState)
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Проверяем, что кредит недоступен".ybm_run { _ in
            cart.element.ybm_swipeCollectionView(toFullyReveal: cart.summary.totalPrice.element)
            XCTAssertFalse(cart.credit.creditInfo.element.exists)
            XCTAssertFalse(cart.credit.creditInfo.monthlyPayment.isVisible)
            XCTAssertFalse(cart.credit.creditInfo.buyInCreditButton.element.isVisible)
        }

        "Проверяем, что не отображается информация о доставке".ybm_run { _ in
            XCTAssertFalse(cart.summary.delivery.isVisible)
        }
    }

    func testSummaryWeightOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2822")
        Allure.addEpic("Корзина")
        Allure.addFeature("Саммари")
        Allure.addTitle("Проверяем информацию о весе заказа")

        var cart: CartPage!
        var cartState = CartState()

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт корзины - трешхоллд".ybm_run { _ in
            let values = Values(
                info: .free_with_more_items,
                forReason: .freeDeliveryByThreshold
            )
            cartState.setThresholdInfoState(with: values)
            cartState.setUserOrdersState(with: .dropship)
            cartState.setCartStrategy(with: [.protein])
        }

        "Мокаем ручки".ybm_run { _ in
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".ybm_run { _ in
            cart = goToCart()
        }

        let summary = cart.summary
        let item = cart.cartItem(at: 0)

        "Проверяем наличие \"Вес заказа\"".ybm_run { _ in
            cart.element.ybm_swipeCollectionView(toFullyReveal: summary.weight.element)
            XCTAssertEqual(summary.weight.title.label, "Вес заказа")
            XCTAssertEqual(summary.weight.details.label, "1 кг")
        }

        "Мок для удаления товара".ybm_run { _ in
            cartState.deleteItemsFromCartState()
            cartState.setCartStrategy(with: [])
            stateManager?.setState(newState: cartState)
        }

        "Проверяем, что вес заказа не отображается в пустой корзине".ybm_run { _ in
            cart.element.ybm_swipeCollectionView(to: .up, toFullyReveal: item.removeButton)
            item.removeButton.tap()
            wait(forInvisibilityOf: summary.weight.element)
        }
    }
}
