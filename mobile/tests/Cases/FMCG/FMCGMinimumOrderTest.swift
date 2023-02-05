import MarketUITestMocks
import XCTest

/// FMCG: Тестирование минимального заказа (В корзину от n)
final class FMCGMinimumOrderTest: ServicesTestCase {

    override func setUp() {
        super.setUp()

        enable(toggles: FeatureNames.minimumOrder, FeatureNames.cartRedesign)
    }

    func testMinimumOrderInSearch() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5910")
        Allure.addEpic("Минимальный заказ")
        Allure.addEpic("Выдача")
        Allure.addTitle("Добавление/удаление")

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupFMCGSKUInfoState()
            setupFMCGFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "iphone")
        }

        let snippetPage = feedPage.collectionView.cellPage(at: 0)
        "Проверяем кнопку 'В корзину от'".ybm_run { _ in
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            ybm_wait(forVisibilityOf: [snippetPage.addToCartButton.element])
            XCTAssertEqual(snippetPage.addToCartButton.element.label, "\(Constants.outOfCompactOfferLabelText)")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            setupFMCGAddToCartState()
            snippetPage.addToCartButton.element.tap()
            ybm_wait(forVisibilityOf: [snippetPage.addToCartButton.minusButton])
            XCTAssertEqual(snippetPage.addToCartButton.element.label, "\(Constants.bundleSettingsMinimum)")
        }

        "Удаляем товар из корзины".ybm_run { _ in
            setupEmptyCartState()
            snippetPage.addToCartButton.minusButton.tap()
            ybm_wait(forFulfillmentOf: {
                snippetPage.addToCartButton.element.label == Constants.outOfCompactOfferLabelText
            })
        }
    }

    func testMinimumOrderInCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5908")
        Allure.addEpic("Минимальный заказ")
        Allure.addEpic("Корзина")
        Allure.addTitle("Удаление товара")

        var cartPage: CartPage!
        var cartItem: CartPage.CartItem!

        "Мокаем состояние".ybm_run { _ in
            setupFMCGSKUInfoState()
            setupFMCGCartState()
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Ищем товар в корзине".ybm_run { _ in
            cartItem = cartPage.cartItem(at: 0)
            cartPage.element.ybm_swipe(toFullyReveal: cartItem.element)
            ybm_wait(forVisibilityOf: [cartItem.cartButtonRedesign.plusButton])
        }

        "+1 товар и проверяем количество".ybm_run { _ in
            cartItem.cartButtonRedesign.plusButton.tap()
            let countInfo = "\(Constants.bundleSettingsMinimum + Constants.bundleSettingsStep)"
            ybm_wait(forFulfillmentOf: { cartItem.countInfo.label == countInfo })
        }

        "-1 товар и проверяем количество".ybm_run { _ in
            cartItem.cartButtonRedesign.minusButton.tap()
            let countInfo = "\(Constants.bundleSettingsMinimum)"
            ybm_wait(forFulfillmentOf: { cartItem.countInfo.label == countInfo })
        }

        "-1 товар и проверяем, что корзина пуста".ybm_run { _ in
            setupEmptyCartState()
            cartItem.cartButtonRedesign.minusButton.tap()
            checkCartIsEmpty()
        }
    }

    func testMinimumOrderInWishlist() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5911")
        Allure.addEpic("Минимальный заказ")
        Allure.addEpic("Избранное")
        Allure.addTitle("Добавление/удаление")

        var profile: ProfilePage!
        var wishlist: WishlistPage!

        "Настраиваем стейт".ybm_run { _ in
            setupFMCGSKUInfoState()
            setupFMCGWishlistState()
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)
            wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)
        }

        let snippetPage = wishlist.wishlistItem(at: 0)
        "Проверяем кнопку в 'Корзину от'".ybm_run { _ in
            XCTAssertEqual(snippetPage.addToCartButton.element.label, "\(Constants.outOfCompactOfferLabelText)")
        }

        "Добавляем товар в корзину".ybm_run { _ in
            setupFMCGAddToCartState()
            snippetPage.addToCartButton.element.tap()
            ybm_wait(forVisibilityOf: [snippetPage.addToCartButton.minusButton])
            XCTAssertEqual(snippetPage.addToCartButton.element.label, "\(Constants.bundleSettingsMinimum)")
        }

        "Удаляем товар из корзины".ybm_run { _ in
            setupEmptyCartState()
            snippetPage.addToCartButton.minusButton.tap()
            ybm_wait(forFulfillmentOf: {
                snippetPage.addToCartButton.element.label == Constants.outOfCompactOfferLabelText
            })
        }
    }
}

// MARK: - Private

private extension FMCGMinimumOrderTest {

    func setupFMCGAddToCartState() {
        var cartState = CartState()
        cartState.addItemsToCartState(with: .init(offers: [Constants.modifiedOffer]))
        cartState.setCartStrategy(with: .init(offers: [Constants.modifiedOffer], useAvailableCount: true))
        stateManager?.setState(newState: cartState)
    }

    func setupFMCGCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: .init(offers: [Constants.modifiedOffer], useAvailableCount: true))
        stateManager?.setState(newState: cartState)
    }

    func setupFMCGSKUInfoState() {
        var skuState = SKUInfoState()
        let mapper = ResolveSKUInfoResolveProductOffersWithHyperId.Mapper(
            results: .default, collections: modify(.default) {
                $0.offer = [Constants.modifiedOffer]
            }
        )

        skuState.setSkuInfoState(offer: Constants.modifiedOffer)
        skuState.setSkuInfoProductOffersWithHyperIdState(with: mapper)

        stateManager?.setState(newState: skuState)
    }

    func setupFMCGWishlistState() {
        var wishlistState = WishlistState()
        wishlistState.setWishlistItems(items: [.default])
        stateManager?.setState(newState: wishlistState)
    }

    func checkCartIsEmpty() {
        let navigationBar = NavigationBarPage.current
        let orderButton = navigationBar.orderBarButton

        ybm_wait(forFulfillmentOf: { !orderButton.element.isEnabled })

        XCTAssertEqual(navigationBar.orderBarButton.element.label, "Оформить")
    }

    func setupFMCGFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(
            mapper: FeedState.SearchResultFAPI(fapiOffers: [Constants.modifiedOffer])
        )
        feedState.setSearchStateFAPI(
            mapper: FeedState.SearchResultFAPI(fapiOffers: [Constants.modifiedOffer])
        )
        stateManager?.setState(newState: feedState)
    }
}

// MARK: - Nested Types

private extension FMCGMinimumOrderTest {

    enum Constants {
        static let bundleSettingsMinimum = 3
        static let bundleSettingsStep = 1
        static let availableCount = 3
        static let outOfCompactOfferLabelText = "В корзину от 3"

        static var modifiedOffer: FAPIOffer {
            modify(FAPIOffer.default) {
                $0.bundleSettings?.quantityLimit?.minimum = Constants.bundleSettingsMinimum
                $0.bundleSettings?.quantityLimit?.step = Constants.bundleSettingsStep
                $0.availableCount = Constants.availableCount

                $0.wareId = "7arCgmF2hF0ih5GripG8ww"
                $0.serviceIds = OfferService.defaultIds
            }
        }
    }
}
