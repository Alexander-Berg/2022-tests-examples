import MarketUITestMocks
import XCTest

final class CartPaymentSystemCashbackTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testMastercardBlockExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5194")
        Allure.addEpic("Корзина")
        Allure.addFeature("MasterCard")
        Allure.addTitle("Блок в корзине для плюсовика")

        var root: RootPage!
        var cartPage: CartPage!
        var paymentSystemCashbackCell: CartPage.PaymentSystemCampaignCell!

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.mastercard_cashback_2021)
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем ручки".ybm_run { _ in
            stateManager?.setState(newState: makeCartState(nil, .basicMastercard))
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие блока и текст в нем".ybm_run { _ in
            paymentSystemCashbackCell = cartPage.paymentSystemCampaignCell
            cartPage.collectionView.swipe(to: .down, until: paymentSystemCashbackCell.element.isVisible)
            XCTAssertEqual(
                paymentSystemCashbackCell.title.label,
                "С Mastercard — ещё ﻿﻿ 231 балл\nОплатите заказ картой Mastercard® онлайн и получите ещё 10% баллами"
            )
        }

        "Нажимаем на блок и проверяем открытие условий акции".ybm_run { _ in
            paymentSystemCashbackCell.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testMirBlockExists() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6472")
        Allure.addEpic("Корзина")
        Allure.addFeature("МИР")
        Allure.addTitle("Блок в корзине для плюсовика")

        var root: RootPage!
        var cartPage: CartPage!
        var paymentSystemCashbackCell: CartPage.PaymentSystemCampaignCell!

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.mastercard_cashback_2021)
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем ручки".ybm_run { _ in
            stateManager?.setState(newState: makeCartState(nil, .basicMir))
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Проверяем наличие блока и текст в нем".ybm_run { _ in
            paymentSystemCashbackCell = cartPage.paymentSystemCampaignCell
            cartPage.collectionView.swipe(to: .down, until: paymentSystemCashbackCell.element.isVisible)
            XCTAssertEqual(
                paymentSystemCashbackCell.title.label,
                "С картой «Мир» — ещё ﻿﻿ 231 балл\nОплатите заказ картой «Мир» онлайн и получите ещё 10% баллами"
            )
        }

        "Нажимаем на блок и проверяем открытие условий акции".ybm_run { _ in
            paymentSystemCashbackCell.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testMastercardBlockDoesntShow() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5227")
        Allure.addEpic("Корзина")
        Allure.addFeature("MasterCard")
        Allure.addTitle("Блок в корзине после достижения лимита по акции")

        var root: RootPage!
        var cartPage: CartPage!

        "Настраиваем тоглы".ybm_run { _ in
            enable(toggles: FeatureNames.mastercard_cashback_2021)
            disable(toggles: FeatureNames.cartRedesign)
        }

        "Мокаем ручки".ybm_run { _ in
            stateManager?.setState(newState: makeCartState((123, .default)))
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем отсутствие блока".ybm_run { _ in
            let item = cartPage.cartItem(at: 1)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: item.element)
            XCTAssertFalse(cartPage.paymentSystemCampaignCell.element.isVisible)
        }
    }

    private func makeCartState(
        _ cashback: (Int, FAPIOffer)?,
        _ paymentSystemCashback: OrderOptionsCashback.PaymentSystemCashback? = nil
    ) -> CartState {
        var cartState = CartState()
        var userOrderOptions = ResolveUserOrderOptions.UserOrderOptions.basic
        var orderOptionsCashback = OrderOptionsCashback()
        if let cashback = cashback {
            orderOptionsCashback.addCashback(for: [cashback])
        }
        orderOptionsCashback.paymentSystemCashback = paymentSystemCashback
        userOrderOptions.cashback = orderOptionsCashback
        cartState.setUserOrdersState(with: userOrderOptions)
        return cartState
    }
}
