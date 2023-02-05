import MarketUITestMocks
import XCTest

class RetailCartTest: RetailTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCheckout() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5922")
        Allure.addEpic("Корзина")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Чекаут")

        var cartPage: CartPage!

        "Мокаем состояние".run {
            setupToggles()
            setupUserAddress()
            setupRetail()
            mockStateManager?.pushState(bundleName: "Retail_Cart")
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            ybm_wait(forVisibilityOf: [cartPage.element])
        }

        "Переходим к оформлению".run {
            let orderButton = cartPage.retailOrderButton(at: .zero)
            ybm_wait(forFulfillmentOf: { orderButton.isHittable })
            orderButton.tap()
        }

        "Проверяем ритейловый чекаут".run {
            let lavkaHostingPage = LavkaHostingPage.current
            ybm_wait(forVisibilityOf: [lavkaHostingPage.element])
        }

    }

    func testScrollbox() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5921")
        Allure.addEpic("Корзина")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Скроллбокс")

        var cartPage: CartPage!

        "Мокаем состояние".run {
            setupToggles()
            setupUserAddress()
            setupRetail()
            mockStateManager?.pushState(bundleName: "Retail_Cart")
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            ybm_wait(forVisibilityOf: [cartPage.element])
        }

        "Переходим к маркетовой корзине по скроллеру".run {
            let scrollableHeader = cartPage.scrollableHeader
            ybm_wait(forVisibilityOf: [scrollableHeader.element])
            let button = scrollableHeader.button(at: 1)
            XCTAssertEqual(button.label, "Маркет")
            button.tap()
            let marketItem = cartPage.cartItem(with: 1)
            ybm_wait(forVisibilityOf: [marketItem.element])
        }
    }

    // MARK: - Private

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

}
