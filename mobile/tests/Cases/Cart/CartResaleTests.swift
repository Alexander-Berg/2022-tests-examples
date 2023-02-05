import MarketUITestMocks
import XCTest

final class CartResaleTest: LocalMockTestCase {

    func testResale() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6304")
        Allure.addEpic("Корзина")
        Allure.addFeature("Ресейл")
        Allure.addTitle("Признак ресейла для Б/У товара в корзине")

        enable(toggles: FeatureNames.cartRedesign)
        var cartState = CartState()
        var cartPage: CartPage!

        "Настраиваем стейт".run {
            enableResale()
            stateManager?.mockingStrategy = .dtoMock
            cartState.setCartStrategy(with: .init(offers: [.resaleProtein], useAvailableCount: true))
            stateManager?.setState(newState: cartState)
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            ybm_wait(forFulfillmentOf: { cartPage.element.isVisible })
        }

        "Проверяем признак ресейла".run {
            let firstItem = cartPage.cartItem(with: 0)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: firstItem.cartButtonRedesign.element)
            XCTAssertEqual(cartPage.cartItem(at: 0).supplier.label, "Ресейл﻿﻿•﻿﻿Яндекс.Маркет")
        }
    }

    private func enableResale() {
        var defaultState = DefaultState()
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
        enable(toggles: FeatureNames.marketResaleGoods)
        defaultState.setExperiments(experiments: [.resaleExperiment])
        stateManager?.setState(newState: defaultState)
    }
}
