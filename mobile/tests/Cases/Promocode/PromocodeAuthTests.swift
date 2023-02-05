import MarketUITestMocks
import XCTest

final class PromocodeAuthTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPromocodeAutoApplying() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3914")
        Allure.addEpic("Промокод")
        Allure.addFeature("Автоприменение промокода")

        var root: RootPage!
        var morda: MordaPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        let navigationBar = NavigationBarPage.current

        var defaultState = DefaultState()

        "Настраиваем FT".ybm_run { _ in
            enable(toggles: FeatureNames.promocodeAutoApplyingV2)
        }

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента promocode_auto_applying_enabled_test".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Experiments_Promocode_Autoapplying")
        }

        "Мокаем состояние".ybm_run { _ in
            defaultState.setBanners(banners: [])
            stateManager?.setState(newState: defaultState)
            mockStateManager?.pushState(bundleName: "Promocode_AutoApplying_Morda")
        }

        "Открываем морду".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Добавляем товар в корзину".ybm_run { _ in
            let widget = morda.historyWidget
            let snippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
            let button = snippet.addToCartButton.element
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: button)

            mockStateManager?.pushState(bundleName: "Promocode_AutoApplying_AddItemToCart")

            button.tap()
        }

        "Переходим в корзину".ybm_run { _ in

            mockStateManager?.pushState(bundleName: "Promocode_AutoApplying_ItemInCart")

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.element)
        }

        "Проверяем, что промокод применен".ybm_run { _ in
            let promocode = cartPage.promocode
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: promocode.element)

            XCTAssertEqual(promocode.input.text, "DAMIXA")

            let summary = cartPage.summary
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: summary.promocodeDiscount.element)

            XCTAssertEqual(summary.promocodeDiscount.details.label, "-194 ₽")
        }

        "Проверяем отображение скидки по промокоду в чекауте".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_AutoApplying_Checkout")

            checkoutPage = navigationBar.orderBarButton.tap()

            let promocodeDiscount = checkoutPage.summaryPromoCodeDiscountCell
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: promocodeDiscount.element)

            XCTAssertEqual(promocodeDiscount.details.label, "-194 ₽")
        }

        "Проверяем, что скидка по промокоду учтена на экране подтверждения заказа".ybm_run { _ in
            let paymentButton = checkoutPage.paymentButton
            checkoutPage.collectionView.ybm_swipeCollectionView(toFullyReveal: paymentButton.element)

            let finishMultiorderPage = paymentButton.tap()

            ybm_wait(forFulfillmentOf: {
                finishMultiorderPage.element.isVisible
            })

            XCTAssertEqual(finishMultiorderPage.paymentStatus(at: 0).text, "Оплата при получении 1 096 ₽")
        }
    }
}
