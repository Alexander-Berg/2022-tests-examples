import MarketUITestMocks
import XCTest

class ChangeServiceInCartTest: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testChangeServiceInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4743")
        Allure.addEpic("Корзина")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Редактирование доп. услуги")

        var rootPage: RootPage!
        var skuPage: SKUPage!
        var cartPage: CartPage!
        var servicesPopup: ServicesPopupPage!
        var cartItem: CartPage.CartItem!
        let specialSpace = String.ble_nonBreakingSpace

        "Мокаем ручки".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем карточку товара".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: rootPage)
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

        "Выбираем услугу и проверяем что она отображается".ybm_run { _ in
            servicesPopup.selectService(at: 1)

            let selectedService = servicesPopup.selectedService
            XCTAssertEqual(selectedService.title.label, "Установка")
            XCTAssertEqual(selectedService.subtitle.label, "Базовая установка для любых типов кондиционеров")
            XCTAssertEqual(selectedService.price.label, "5\(specialSpace)000 ₽")
        }

        "Нажимаем сохранить".ybm_run { _ in
            servicesPopup.saveButton.tap()
            wait(forInvisibilityOf: servicesPopup.element)
            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })
            wait(forVisibilityOf: skuPage.element)
        }

        "Открываем корзину".ybm_run { _ in
            setupCartServicesFirst()
            cartPage = goToCart(root: rootPage)
        }

        "Проверяем что в карточке товара показывается выбранная услуга".ybm_run { _ in
            cartItem = cartPage.cartItem(at: 0)
            XCTAssertEqual(cartItem.serviceButton.element.label, "Установка 5\(specialSpace)000 ₽")
        }

        "Проверяем самари".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalServices.element)
            XCTAssertEqual(cartPage.summary.totalServices.title.label, "Установки (1)")
            XCTAssertEqual(cartPage.summary.totalServices.details.label, "5\(specialSpace)000 ₽")

            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)
            XCTAssertEqual(
                cartPage.summary.totalPrice.details.label,
                "40\(specialSpace)898 ₽"
            )
            XCTAssertEqual(cartPage.compactSummary.totalPrice.label, "40\(specialSpace)898 ₽")
        }

        "Выбираем другую услугу".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartItem.element)
            servicesPopup = cartItem.serviceButton.tap()
            wait(forVisibilityOf: servicesPopup.element)

            setupCartServicesSecond()

            let selectedService = servicesPopup.selectedService
            XCTAssertEqual(selectedService.title.label, "Установка")
            XCTAssertEqual(selectedService.subtitle.label, "Базовая установка для любых типов кондиционеров")
            XCTAssertEqual(selectedService.price.label, "5\(specialSpace)000 ₽")

            servicesPopup.selectService(at: 2)

            XCTAssertEqual(selectedService.title.label, "Установка + демонтаж")
            XCTAssertEqual(
                selectedService.subtitle.label,
                "Базовая установка для любых типов кондиционеров + демонтаж старого"
            )
            XCTAssertEqual(selectedService.price.label, "6\(specialSpace)000 ₽")

            servicesPopup.saveButton.tap()

            wait(forInvisibilityOf: servicesPopup.element)
            XCTAssertEqual(cartItem.serviceButton.element.label, "Установка + демонтаж 6 000 ₽")

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина1" })

        }

        "Проверяем самари".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalServices.element)
            XCTAssertEqual(cartPage.summary.totalServices.title.label, "Установки (1)")
            XCTAssertEqual(cartPage.summary.totalServices.details.label, "6\(specialSpace)000 ₽")

            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalPrice.element)
            XCTAssertEqual(cartPage.summary.totalPrice.details.label, "41\(specialSpace)898 ₽")
            XCTAssertEqual(cartPage.compactSummary.totalPrice.label, "41\(specialSpace)898 ₽")
        }
    }

    func testDeleteItemWithServiceFromCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4746")
        Allure.addEpic("Корзина")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Удаление товара и доп.услуги")

        var rootPage: RootPage!
        var skuPage: SKUPage!
        var cartPage: CartPage!
        var servicesPopup: ServicesPopupPage!
        var cartItem: CartPage.CartItem!
        let specialSpace = String.ble_nonBreakingSpace

        "Мокаем ручки".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем карточку товара".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: rootPage)
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

        "Добавляем установку".ybm_run { _ in
            XCTAssertEqual(skuPage.addServiceButton.element.label, "Доступна услуга установки")
            servicesPopup = skuPage.addServiceButton.tap()
            wait(forVisibilityOf: servicesPopup.element)
            servicesPopup.selectService(at: 1)
        }

        "Нажимаем сохранить".ybm_run { _ in
            servicesPopup.saveButton.tap()
            wait(forInvisibilityOf: servicesPopup.element)
            wait(forVisibilityOf: skuPage.element)
        }

        "Открываем корзину".ybm_run { _ in
            setupCartServicesFirst()
            cartPage = goToCart(root: rootPage)
        }

        "Проверяем что в карточке товара показывается выбранная услуга".ybm_run { _ in
            cartItem = cartPage.cartItem(at: 0)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartItem.serviceButton.element)
            XCTAssertEqual(cartItem.serviceButton.element.label, "Установка 5\(specialSpace)000 ₽")
        }

        "Проверяем самари".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartPage.summary.totalServices.element)
            XCTAssertEqual(cartPage.summary.totalServices.title.label, "Установки (1)")
            XCTAssertEqual(cartPage.summary.totalServices.details.label, "5\(specialSpace)000 ₽")
        }

        "Мокаем пустую корзину".ybm_run { _ in
            setupEmptyCartState()
        }

        "Удаляем товар".ybm_run { _ in
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: cartItem.removeButton)
            cartItem.removeButton.tap()
            wait(forInvisibilityOf: cartItem.serviceButton.element)
            wait(forInvisibilityOf: cartItem.element)

            ybm_wait(forFulfillmentOf: { TabBarPage.current.cartTabItem.element.label == "Корзина" })
        }
    }
}

extension ChangeServiceInCartTest {
    func setupCartServicesFirst() {
        var cartState = CartState()

        cartState
            .setCartStrategy(with: modify(
                ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart(offers: [.withServices])
            ) {
                $0.cartItem[0].selectedServiceId = FAPIOffer.withServices.wareId
                $0.offerService = OfferService.default
                $0.offerSelectedService = [
                    .init(
                        id: FAPIOffer.withServices.wareId,
                        selectedServices: [.init(serviceId: OfferService.default[0].id)]
                    )
                ]
            })

        cartState.setUserOrdersState(with: modify(CartState.UserOrderOptions.basic) {
            $0.summary.serviceInfo = .init(totalCount: 1, totalPrice: 5_000)
        })

        stateManager?.setState(newState: cartState)
    }

    func setupCartServicesSecond() {
        var cartState = CartState()

        cartState
            .setCartStrategy(with: modify(
                ResolveUserCartWithStrategiesAndBusinessGroups.VisibleStrategiesFromUserCart(offers: [.withServices])
            ) {
                $0.cartItem[0].selectedServiceId = FAPIOffer.withServices.wareId
                $0.offerService = OfferService.default
                $0.offerSelectedService = [
                    .init(
                        id: FAPIOffer.withServices.wareId,
                        selectedServices: [.init(serviceId: OfferService.default[1].id)]
                    )
                ]
            })

        cartState.setUserOrdersState(with: modify(CartState.UserOrderOptions.basic) {
            $0.summary.serviceInfo = .init(totalCount: 1, totalPrice: 6_000)
        })
        stateManager?.setState(newState: cartState)
    }
}
