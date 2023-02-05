import MarketUITestMocks
import XCTest

final class CartPlusCellTestWithoutYandexPlusTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCartYandexPlusCell() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4195")
        Allure.addEpic("Корзина")
        Allure.addFeature("Кешбэк. Неплюсовик без баллов. Блок про баллы в корзине")
        Allure.addTitle("Текст в блоке про баллы в корзине")

        enable(toggles: FeatureNames.showPlus)

        var cartPage: CartPage!

        "Настраиваем стейт".ybm_run { _ in
            setupState()
        }

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackInCart")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Листаем до блока с Плюсом и сравниваем текст".ybm_run { _ in
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cartPage.plusSubscriptionCell.element)
            XCTAssertEqual(
                cartPage.plusSubscriptionCell.title.label,
                "Копите и тратьте баллы с Яндекс Плюсом"
            )
        }

        "Нажимаем на блок и переходим в Дом Плюса".ybm_run { _ in
            cartPage.plusSubscriptionCell.element.tap()

            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

    // MARK: - Private

    private func setupState() {
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
}
