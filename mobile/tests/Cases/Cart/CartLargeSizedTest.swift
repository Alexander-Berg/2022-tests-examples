import MarketUITestMocks
import XCTest

class CartLargeSizedTest: LocalMockTestCase {

    func testKgtPopupShortTextAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5549")
        Allure.addEpic("Корзина")
        Allure.addFeature("КГТ")
        Allure.addTitle("Показ попапа о доставке КГТ с коротким текстом")

        disable(toggles: FeatureNames.cartShowKgtPopupFullText)
        enable(toggles: FeatureNames.cartRedesign)

        let cartPage = openCartAndCheckThreshold()
        let kgtPopup: CartKgtPopupPage! = openKgtPopup(cartPage: cartPage)

        checkPopupAndClose(
            kgtPopup: kgtPopup,
            title: "Крупногабаритный заказ",
            bodyText: "Заказ считается крупногабаритным, если он тяжелее 30 кг",
            buttonTitle: "Понятно"
        )
    }

    func testKgtPopupAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5549")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2816")
        Allure.addEpic("Корзина")
        Allure.addFeature("КГТ")
        Allure.addTitle("Показ попапа о доставке КГТ")

        enable(toggles: FeatureNames.cartShowKgtPopupFullText)
        enable(toggles: FeatureNames.cartRedesign)

        let cartPage = openCartAndCheckThreshold()
        let kgtPopup: CartKgtPopupPage! = openKgtPopup(cartPage: cartPage)

        checkPopupAndClose(
            kgtPopup: kgtPopup,
            title: "Крупногабаритный заказ",
            bodyText: "Крупногабаритным считается заказ весом 30 кг и больше, объёмом 0,5 м³ и больше или с одной из сторон 200 см и больше",
            buttonTitle: "Понятно"
        )
    }

    // MARK: - Helpers methods

    private func openCartAndCheckThreshold() -> CartPage! {
        var cartPage: CartPage!
        var cartState = CartState()

        "Мокаем ручки".ybm_run { _ in
            cartState.setUserOrdersState(with: .largeSized)
            stateManager?.setState(newState: cartState)
        }
        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.element)
        }
        "Проверяем наличие нотификации КГТ".ybm_run { _ in
            wait(forVisibilityOf: cartPage.threshold.element)
            XCTAssertEqual(
                cartPage.threshold.deliveryText.label,
                "Ваш заказ крупногабаритный, поэтому доставка будет дороже, чем обычно\(String.ble_nonBreakingSpace)"
            )
        }
        return cartPage
    }

    private func openKgtPopup(cartPage: CartPage!) -> CartKgtPopupPage! {
        var kgtPopup: CartKgtPopupPage!

        "Нажимаем на нотификацию КГТ -> Открывается попап".ybm_run { _ in
            wait(forVisibilityOf: cartPage.threshold.deliveryText)
            cartPage.threshold.deliveryText.tap()

            let popupElement = XCUIApplication().otherElements[CartKgtPopupAccessibility.root]
            kgtPopup = CartKgtPopupPage(element: popupElement)
            wait(forVisibilityOf: kgtPopup.element)
        }
        return kgtPopup
    }

    private func checkPopupAndClose(
        kgtPopup: CartKgtPopupPage!,
        title: String,
        bodyText: String,
        buttonTitle: String
    ) {
        "Проверяем текст в попапе тайтл и кнопку".ybm_run { _ in
            XCTAssertEqual(
                kgtPopup.titleLabel.label,
                title
            )
            XCTAssertEqual(
                kgtPopup.bodyText.text,
                bodyText
            )
            XCTAssertEqual(
                kgtPopup.okButton.staticTexts.firstMatch.label,
                buttonTitle
            )
        }
        "Зыкрываем попап".ybm_run { _ in
            kgtPopup.okButton.tap()
            wait(forInvisibilityOf: kgtPopup.element)
        }
    }
}
