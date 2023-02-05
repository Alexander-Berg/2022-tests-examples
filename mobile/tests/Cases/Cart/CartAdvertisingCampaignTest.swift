import MarketUITestMocks
import XCTest

final class CartAdvertisingCampaignTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    override func setUp() {
        super.setUp()
        enable(toggles: FeatureNames.showPlus)
    }

    func testThresholdVisibleLess3500() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4313")
        Allure.addEpic("Корзина")
        Allure.addFeature("Залогин. Трешхолд \"РК-500\"")
        Allure.addTitle("Корзина менее 3500 ₽")

        var cartPage: CartPage!

        setupAdvertisingState()

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем трешхолд рекламной компании".ybm_run { _ in
            cartPage.collectionView.swipe(to: .down, until: cartPage.advertisingCampaignThreshold.threshold.isVisible)
            XCTAssertEqual(
                cartPage.advertisingCampaignThreshold.descriptionText.element.label,
                "Ещё  баллов за 1й заказ если добавите товаров ещё на 1 830 ₽﻿﻿"
            )
        }
    }

    func testRestrictionsPopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4314")
        Allure.addEpic("Корзина")
        Allure.addFeature("Залогин. Трешхолд \"РК-500\"")
        Allure.addTitle("Нажатие на текст-ссылку \"Но есть ограничения\"")

        var cartPage: CartPage!
        var restrictionsPopup: CashbackAboutPage!

        setupAdvertisingState()

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Нажимаем на текст описания в трешхолде рекламной компании".ybm_run { _ in
            cartPage.collectionView
                .ybm_swipeCollectionView(toFullyReveal: cartPage.advertisingCampaignThreshold.element)

            restrictionsPopup = cartPage.advertisingCampaignThreshold.descriptionText.tap()
        }

        "Проверяем контент попапа".ybm_run { _ in
            let checkTexts = [
                "500 баллов за первый заказ в приложении от 3 500 ₽",
                "Кроме покупки БАДов, лекарств, стиков для испарителей и алкоголя. Баллы можно потратить на новый заказ.",
                "Узнать больше"
            ]
            for (index, text) in checkTexts.enumerated() {
                XCTAssertEqual(
                    restrictionsPopup.collectionView.cells.staticTexts.allElementsBoundByIndex[index].label,
                    text
                )
            }
        }
    }

    func testThresholdVisibleMore3500() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4316")
        Allure.addEpic("Корзина")
        Allure.addFeature("Залогин. Трешхолд \"РК-500\"")
        Allure.addTitle("Корзина более 3500 ₽")

        var cartPage: CartPage!

        setupAdvertisingState(lessThanThreshold: false)

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Проверяем трешхолд рекламной компании".ybm_run { _ in
            cartPage.collectionView
                .ybm_swipeCollectionView(toFullyReveal: cartPage.advertisingCampaignThreshold.element)

            XCTAssertTrue(cartPage.advertisingCampaignThreshold.threshold.isVisible)
            XCTAssertEqual(
                cartPage.advertisingCampaignThreshold.descriptionText.element.label,
                "Ещё  баллов за 1й заказ Баллы придут вместе с заказом﻿﻿"
            )
        }
    }

    func testRestrictionsPopupLinkClick() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4317")
        Allure.addEpic("Корзина")
        Allure.addFeature("Залогин. Трешхолд \"РК-500\". Попап \"500 баллов за первый заказ\"")
        Allure.addTitle("Нажатие на текст-ссылку \"Узнать больше\"")

        var cartPage: CartPage!
        var restrictionsPopup: CashbackAboutPage!

        setupAdvertisingState()

        "Открываем корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Нажимаем на текст описания в трешхолде рекламной компании".ybm_run { _ in
            cartPage.collectionView
                .ybm_swipeCollectionView(toFullyReveal: cartPage.advertisingCampaignThreshold.element)

            restrictionsPopup = cartPage.advertisingCampaignThreshold.descriptionText.tap()
        }

        "Нажимаем на ссылку и ждем открытия вебвью".ybm_run { _ in
            restrictionsPopup.collectionView.cells.staticTexts.element(withLabelMatching: "Узнать больше").tap()
            wait(forExistanceOf: WebViewPage.current.element)
        }
    }

    private func setupAdvertisingState(lessThanThreshold: Bool = true) {
        "Мокаем ручки".ybm_run { _ in

            if lessThanThreshold {
                mockStateManager?.pushState(bundleName: "CartSet_AdvertisingCampaign_lessThan3500")
            }

            var cartState = CartState()
            var orderState = OrdersState()
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            cartState.setUserOrdersState(with: .dropship)
            cartState.setCartStrategy(with: [.protein])
            orderState.setOrdersResolvers(
                mapper: .init(orders: [.init(status: .delivered)]),
                for: [.all]
            )

            var orderOptionsState = ResolveUserOrderOptions.UserOrderOptions.basic
            orderOptionsState.cashback = OrderOptionsCashback(
                welcomeCashback: OrderOptionsCashback.WelcomeCashback(
                    remainingMultiCartTotal: lessThanThreshold ? 1_830 : 0,
                    minMultiCartTotal: 3_500,
                    amount: 500
                )
            )
            cartState.setUserOrdersState(with: orderOptionsState)
            stateManager?.setState(newState: cartState)
            stateManager?.setState(newState: orderState)
            stateManager?.setState(newState: authState)
        }
    }

}
