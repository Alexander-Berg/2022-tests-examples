import MarketUITestMocks
import XCTest

class CheckoutLoyaltyDegradationTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testDiscountsUnavailablePopupOnCreatingOrder() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3330")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Попап недоступности скидок")
        Allure.addTitle("Проверяем отображение попапа в чекауте при создании заказа")

        var root: RootPage!
        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!
        var popupPage: DiscountsUnavailablePopupPage!

        disable(toggles: FeatureNames.cartRedesign)

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Checkout_LoyaltyDegradation_Order")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.threshold.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Выбираем адрес доставки".ybm_run { _ in
            let checkoutPresetSelectorPage = checkoutPage.addressChooserButton().tap()
            ybm_wait(forVisibilityOf: [checkoutPresetSelectorPage.element])

            checkoutPresetSelectorPage.doneButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Нажимаем на кнопку \"Оплатить\"".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.paymentButton.element)
            checkoutPage.paymentButton.tap()
        }

        "Проверяем отображение попапа".ybm_run { _ in
            popupPage = DiscountsUnavailablePopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.titleLabel, popupPage.messageLabel, popupPage.actionButton])

            XCTAssertEqual(popupPage.titleLabel.label, "Не удалось применить скидку")

            let expectedText =
                "Извините, что-то пошло не так.\nПопробуйте ещё раз позже или оставьте\nскидку для следующего заказа."

            XCTAssertEqual(popupPage.messageLabel.label, expectedText)

            popupPage.actionButton.tap()
        }

        "Проверяем что перешли в корзину".ybm_run { _ in
            ybm_wait(forVisibilityOf: [cartPage.element])
        }
    }
}
