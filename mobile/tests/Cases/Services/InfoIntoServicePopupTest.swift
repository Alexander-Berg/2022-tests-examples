import MarketUITestMocks
import XCTest

class InfoIntoServicePopupTest: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testInfoIntoServicePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4742")
        Allure.addEpic("Попап")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Переход на страницу Услуг")

        var skuPage: SKUPage!
        var servicesPopup: ServicesPopupPage!

        "Мокаем ручки".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем карточку товара".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Добавляем товар в корзину".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            setupAddToCartState()
            skuPage.addToCartButton.element.tap()
        }

        "Проверяем отображение кнопки и бейдж в таб баре".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { skuPage.addToCartButton.element.label == "1 товар в корзине" })
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })

            wait(forVisibilityOf: skuPage.addToCartButton.plusButton)
            wait(forVisibilityOf: skuPage.addToCartButton.minusButton)
        }

        "Нажимаем Добавить установку".ybm_run { _ in
            XCTAssertEqual(skuPage.addServiceButton.element.label, "Доступна услуга установки")
            servicesPopup = skuPage.addServiceButton.tap()
            wait(forVisibilityOf: servicesPopup.element)
        }

        "Проверяем что услуга в попапе не выбрана".ybm_run { _ in
            let selectedService = servicesPopup.selectedService
            XCTAssertEqual(selectedService.title.label, "Не нужна")
            XCTAssertFalse(selectedService.subtitle.isVisible)
            XCTAssertFalse(selectedService.price.isVisible)
        }

        "Открываем страницу услуг и закрываем".ybm_run { _ in
            servicesPopup.moreInfoLink.tap()

            let webview = WebViewPage.current
            wait(forVisibilityOf: webview.element)

            webview.navigationBar.closeButton.tap()
            wait(forInvisibilityOf: webview.element)
        }
    }
}
