import MarketUITestMocks
import XCTest

final class GlobalNoticeSectionTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func test_OpenCheckoutWithAllConditions_CheckGlobalNoticeShown() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3949")
        Allure.addEpic("Буст брендированных ПВЗ")
        Allure.addFeature("Оповещение о бесплатной доставке в чекауте")
        Allure.addTitle("Пользователь авторизован, заказ на нужную сумму")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        enable(toggles: FeatureNames.marketBrandedOutletDeliveryDisclaimer)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
        disable(toggles: FeatureNames.deliveryDelayDisclaimer, FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "GlobalNotice_AuthorizedUserEnoughTotal")
        }

        "Переходим корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Ждем прогрузки нотификашки и проверяем ее видимость".ybm_run { _ in
            ybm_wait(forVisibilityOf: [checkoutPage.globalNoticeSection])
            XCTAssertTrue(checkoutPage.globalNoticeSection.isVisible)
        }
    }

    func test_OpenCheckoutWithNotEnoughTotal_CheckGlobalNoticeNotShown() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3956")
        Allure.addEpic("Буст брендированных ПВЗ")
        Allure.addFeature("Оповещение о бесплатной доставке в чекауте")
        Allure.addTitle("Пользователь авторизован, суммы заказа недостаточно")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        enable(toggles: FeatureNames.marketBrandedOutletDeliveryDisclaimer)
        disable(toggles: FeatureNames.cartRedesign)

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
        disable(toggles: FeatureNames.deliveryDelayDisclaimer)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "GlobalNotice_AuthorizedUserNotEnoughTotal")
        }

        "Переходим корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Ждем некоторое время чтобы убедиться что нотификашка не показывается после запроса total".ybm_run { _ in
            checkoutPage.globalNoticeSection.shouldNotExist()
        }
    }

    func test_OpenCheckoutWithDeliveryDelayToggleEnabled_CheckGlobalNoticeShown() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4339")
        Allure.addEpic("Уведомление о расширении сроков доставки")
        Allure.addFeature("Срок доставки увеличен")
        Allure.addTitle("Отображается нотификашка")

        var cartPage: CartPage!
        var checkoutPage: CheckoutPage!

        enable(toggles: FeatureNames.deliveryDelayDisclaimer)
        disable(toggles: FeatureNames.cartRedesign)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "GlobalNotice_AuthorizedUserEnoughTotal")
        }

        "Переходим корзину".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            cartPage = goToCart(root: root)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cartPage.compactSummary.orderButton.tap()
            ybm_wait(forVisibilityOf: [checkoutPage.element])
        }

        "Ждем прогрузки нотификашки и проверяем ее видимость".ybm_run { _ in
            ybm_wait(forVisibilityOf: [checkoutPage.globalNoticeSection])
            XCTAssertTrue(checkoutPage.globalNoticeSection.isVisible)
        }
    }

    private var toggleInfo: String {
        let name = FeatureNames
            .marketBrandedOutletDeliveryDisclaimer.lowercased()
        let brandedOutletInfo = [
            name: [
                "title": "Самовывоз может быть бесплатным",
                "subtitle": "Из любого пункта выдачи или постамата Яндекс.Маркета при заказе от 699 ₽",
                "threshold": 699
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: brandedOutletInfo,
            options: .prettyPrinted
        )
        else {
            return ""
        }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }
}
