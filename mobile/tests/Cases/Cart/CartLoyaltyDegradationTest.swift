import XCTest

final class CartLoyaltyDegradationTest: LocalMockTestCase {

    func testDiscountsUnavailablePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3323")
        Allure.addEpic("Корзина")
        Allure.addFeature("Попап недоступности скидок")
        Allure.addTitle("Проверяем отображение попапа в корзине")

        var root: RootPage!
        var cartPage: CartPage!
        var popupPage: DiscountsUnavailablePopupPage!

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_LoyaltyDegradation")
            mockStateManager?.addSuspended(filename: "POST_market_blue_v1_user_order_options_json")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
            mockStateManager?.deleteSuspended(filename: "POST_market_blue_v1_user_order_options_json")
        }

        "Проверяем отображение попапа".ybm_run { _ in
            popupPage = DiscountsUnavailablePopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.titleLabel, popupPage.messageLabel, popupPage.actionButton])

            XCTAssertEqual(popupPage.titleLabel.label, "Купоны и промокоды пока не работают")

            let expectedText = "По техническим причинам их сейчас не получится применить — пожалуйста, извините. " +
                "Вы можете оставить их до следующего заказа или попробовать ещё раз позже."
            XCTAssertEqual(popupPage.messageLabel.label, expectedText)

            popupPage.actionButton.tap()
        }

        "Проверяем отображение корзины".ybm_run { _ in
            wait(forVisibilityOf: cartPage.collectionView)

            XCTAssertFalse(cartPage.threshold.element.isVisible)

            XCTAssertFalse(cartPage.coins.title.isVisible)
            XCTAssertFalse(cartPage.coins.coinsItem(at: 0).element.isVisible)

            XCTAssertFalse(cartPage.promocode.element.isVisible)
        }
    }

}
