import MarketUITestMocks
import XCTest

final class CartYandexCardTests: YandexCardTests {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testYandexCardBlock() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6254")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6255")
        Allure.addEpic("Корзина")
        Allure.addFeature("YandexCard")
        Allure.addTitle("Информер об акции")

        var root: RootPage!
        var cartPage: CartPage!
        var yandexCardCell: XCUIElement!

        setupFlags()

        "Настраиваем стейт".ybm_run { _ in
            stateManager?.setState(newState: makeCartState())
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем наличие блока".ybm_run { _ in
            yandexCardCell = cartPage.yandexCard.cell
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: yandexCardCell)
            XCTAssertEqual(
                yandexCardCell.textViews.firstMatch.label,
                "Со Счётом в Яндексе — ещё ﻿﻿ 500 баллов\nОплатите заказ со Счёта в Яндексе онлайн и получите ещё 5% баллами"
            )
        }

        "Нажимаем на блок и ждем появление шторки банка".ybm_run { _ in
            yandexCardCell.tap()
            let yanbankSDK = YandexBankPage.current
            wait(forVisibilityOf: yanbankSDK.element)
        }
    }

    private func makeCartState() -> CartState {
        var cartState = CartState()
        var userOrderOptions = ResolveUserOrderOptions.UserOrderOptions.basic
        var orderOptionsCashback = OrderOptionsCashback()
        orderOptionsCashback.yandexCardCashback = .basic
        userOrderOptions.cashback = orderOptionsCashback
        cartState.setUserOrdersState(with: userOrderOptions)
        return cartState
    }
}
