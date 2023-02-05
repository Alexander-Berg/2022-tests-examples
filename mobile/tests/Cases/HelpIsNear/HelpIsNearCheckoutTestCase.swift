import MarketUITestMocks
import XCTest

final class HelpIsNearCheckoutTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testHelpIsNearInOnCheckoutSummary() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4346")
        Allure.addEpic("Чекаут")
        Allure.addFeature("Помощь рядом")
        Allure.addTitle("Информация в блоке \"Итого\"")

        var root: RootPage!
        var cart: CartPage!
        var checkoutPage: CheckoutPage!
        var paymentMethodPage: CheckoutPaymentMethodPopupPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "NoHelpIsNearInCheckout")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Идем в корзину".ybm_run { _ in
            cart = goToCart(root: root)
            wait(forExistanceOf: cart.compactSummary.orderButton.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cart.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Смотрим что нет помощи рядом".ybm_run { _ in
            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.summaryTotalCell.element)
            wait(forInvisibilityOf: checkoutPage.summaryHelpIsNearCell.element)
        }

        "Обновляем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "HelpIsNearCheckout")
        }

        "Открываем способы оплаты".ybm_run { _ in
            checkoutPage.element.swipe(to: .up, untilVisible: checkoutPage.paymentMethodCell.element)
            paymentMethodPage = checkoutPage.paymentMethodCell.tap()
            wait(forVisibilityOf: paymentMethodPage.element)
        }

        "Меняем способ оплаты на способ с поддержкой \"Помощь рядом\"".ybm_run { _ in
            paymentMethodPage.selectPaymentMethod(with: "YANDEX")
            paymentMethodPage.continueButton.tap()

            wait(forVisibilityOf: checkoutPage.element)

            checkoutPage.element.swipe(to: .down, untilVisible: checkoutPage.summaryHelpIsNearCell.element)
            XCTAssertEqual(
                checkoutPage.summaryHelpIsNearCell.title.label,
                "И ещё в «Помощь рядом»"
            )

            XCTAssertEqual(
                checkoutPage.summaryHelpIsNearCell.details.label,
                "22 ₽"
            )
        }

    }
}
