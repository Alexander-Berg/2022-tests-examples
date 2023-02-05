import MarketUITestMocks
import XCTest

final class CartItemDisclaimerTests: LocalMockTestCase {
    func testEstimatedDeliveryDisclaimer() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6026")
        Allure.addEpic("Корзина")
        Allure.addFeature("Товар с долгим сроком доставки")
        Allure.addTitle("Отображение бейджа 'Увеличенные сроки доставки'")

        var cartPage: CartPage!
        var cartState = CartState()
        enable(toggles: FeatureNames.cartRedesign)

        "Мокаем ручки".ybm_run { _ in
            cartState.setCartStrategy(with: [Constants.fapiOffer])
            cartState.setUserOrdersState(with: makeUserOrderOptions())
            stateManager?.setState(newState: cartState)
        }
        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            ybm_wait(forFulfillmentOf: {
                cartPage.element.isVisible
            })
        }
        "Проверяем наличие нотификации КГТ".ybm_run { _ in
            let cartItem = cartPage.cartItem(at: 0)
            ybm_wait(forFulfillmentOf: {
                cartItem.disclaimer.isVisible
            })
            XCTAssertEqual(
                cartItem.disclaimer.label,
                "﻿﻿Увеличенные сроки доставки"
            )
        }
    }
}

// MARK: - Helpers methods and nested Types

private extension CartItemDisclaimerTests {

    func makeUserOrderOptions() -> ResolveUserOrderOptions.UserOrderOptions {
        var firstItem = Item.basic
        firstItem.label = String(Constants.itemId)

        var cartItem = CartItem.withEstimatedDelivery
        cartItem.items = [firstItem]

        return ResolveUserOrderOptions.UserOrderOptions(
            region: .moscow,
            summary: .basic,
            shops: [cartItem]
        )
    }

    enum Constants {
        static let itemId = 111

        static let fapiOffer = modify(FAPIOffer.protein) {
            $0.cartItemInCartId = itemId
        }
    }
}
