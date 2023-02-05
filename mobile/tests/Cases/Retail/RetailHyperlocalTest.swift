import MarketUITestMocks
import UIUtils
import XCTest

class RetailHyperlocalTest: RetailTestCase {

    typealias PopupPage = RetailInformerPopupPage

    func testAvailableAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5925")
        Allure.addEpic("Выдача")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Доступный адрес")

        var feedPage: FeedPage!
        var cartButtonPage: CartButtonPage!
        var popupPage: PopupPage!

        "Мокаем состояние".run {
            setupToggles()
            setupFeed()
            setupSku()
            setupCart()
            setupAvailableDelivery()
            setupUserAddress()
        }

        "Открываем выдачу".run {
            feedPage = goToFeed(with: "iphone")
        }

        "Добавляем в корзину".run {
            let firstSnippet = feedPage.collectionView.cellPage(at: 0)
            cartButtonPage = firstSnippet.addToCartButton
            cartButtonPage.element.tap()
        }

        "Ждем появления попапа ШиШа и закрываем его".run {
            popupPage = PopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.informerShopTitle])

            popupPage.element.ybm_checkingSwipe(
                to: .up,
                until: !popupPage.element.isVisible,
                checkConditionPerCycle: { popupPage.element.isVisible }
            )
        }

        "Проверяем кнопку добавления в корзину".run {
            XCTAssertTrue(cartButtonPage.minusButton.isVisible)
            XCTAssertTrue(cartButtonPage.plusButton.isVisible)
        }

    }

    func testUnavailableAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5924")
        Allure.addEpic("Выдача")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Недоступный адрес")

        var feedPage: FeedPage!
        var cartButtonPage: CartButtonPage!

        "Мокаем состояние".run {
            setupToggles()
            setupFeed()
            setupCart()
            setupUnavailableDelivery()
            setupUserAddress()
        }

        "Открываем выдачу".run {
            feedPage = goToFeed(with: "iphone")
        }

        "Добавляем в корзину".run {
            let firstSnippet = feedPage.collectionView.cellPage(at: 0)
            cartButtonPage = firstSnippet.addToCartButton
            cartButtonPage.element.tap()
        }

        "Проверяем попап".run {
            let barrierViewPage = BarrierViewPage.current
            ybm_wait(forVisibilityOf: [barrierViewPage.element])
            XCTAssertEqual(
                barrierViewPage.title.label,
                "Товар не получится доставить по этому адресу"
            )
            XCTAssertEqual(
                barrierViewPage.subtitle.label,
                "улица Шаболовка, д. 23к1"
            )

            XCTAssertEqual(
                barrierViewPage.actionButton.label,
                "Изменить адрес"
            )
        }

    }

    func testEmptyAddress() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5924")
        Allure.addEpic("Выдача")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Нет адреса")

        var feedPage: FeedPage!
        var cartButtonPage: CartButtonPage!

        "Мокаем состояние".run {
            setupToggles()
            setupFeed()
        }

        "Открываем выдачу".run {
            feedPage = goToFeed(with: "iphone")
        }

        "Добавляем в корзину".run {
            let firstSnippet = feedPage.collectionView.cellPage(at: 0)
            cartButtonPage = firstSnippet.addToCartButton
            cartButtonPage.element.tap()
        }

        "Проверяем попап".run {
            let barrierViewPage = BarrierViewPage.current
            ybm_wait(forVisibilityOf: [barrierViewPage.element])
            XCTAssertEqual(
                barrierViewPage.title.label,
                "Укажите адрес доставки, чтобы добавить товар"
            )
            XCTAssertEqual(
                barrierViewPage.subtitle.label,
                "И мы покажем товары, которые можем на него доставить"
            )
            XCTAssertEqual(
                barrierViewPage.actionButton.label,
                "Указать адрес"
            )
        }

    }

    // MARK: - Private

    func setupUnavailabilityDelivery() {
        var defaultState = DefaultState()
        defaultState.setAvailabilityDelivery(regionAvailability: .unavailable)
        stateManager?.setState(newState: defaultState)
    }

    func setupAvailableDelivery() {
        var defaultState = DefaultState()
        defaultState.setAvailabilityDelivery(regionAvailability: .moscow)
        stateManager?.setState(newState: defaultState)
    }

    func setupUnavailableDelivery() {
        var defaultState = DefaultState()
        defaultState.setAvailabilityDelivery(regionAvailability: .unavailable)
        stateManager?.setState(newState: defaultState)
    }

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

    func setupCart() {
        var cartState = CartState()
        cartState.addItemsToCartState(with: .init(offers: [.retail]))
        cartState.setCartStrategy(with: [.retail])
        stateManager?.setState(newState: cartState)
    }

}
