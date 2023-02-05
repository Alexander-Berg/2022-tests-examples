import XCTest

final class PromocodeTests: LocalMockTestCase {

    func testPromocodeApplyingError() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3911")
        Allure.addEpic("Промокод")
        Allure.addFeature("Применение промокода к неакционному товару")

        var cartPage: CartPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_Cart_Error")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Применяем промокод".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Promocode_Cart_ApplyingError")

            let promocode = cartPage.promocode
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: promocode.element)

            promocode.input.tap()
            promocode.input.typeText("WIN15")
            promocode.applyButton.tap()
        }

        "Проверяем отображение ошибки".ybm_run { _ in
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)

            let errorMessage = "Промокод введён некорректно или уже не действует"
            XCTAssertEqual(popup.text.label, errorMessage)
            XCTAssertEqual(cartPage.promocode.error.label, errorMessage)
        }
    }
}
