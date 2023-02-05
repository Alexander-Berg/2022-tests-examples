import MarketUITestMocks
import XCTest

/// тестирование кнопки "В корзину" для товаров, с остатками на стоках меньшими, чем минимальное кол-во для заказа.
final class SKUCardModelVirtualPackTest: SKUCardModelBaseTestCase {

    // проверка дизейбла кнопки + на картбатоне ДО на КМ, когда кол-во на стоках меньше чем кол-во минимального заказа
    func testPlusButtonIsBlocked() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5945")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5944")
        Allure.addEpic("КМ")
        Allure.addEpic("Добавление в корзину спайки")
        Allure.addTitle("Кол-во на стоках меньше, чем в минимальное кол-во товара в спайке")

        var skuPage: SKUPage!

        "Настраиваем FT".ybm_run { _ in
            enable(toggles: FeatureNames.minimumOrder)
        }

        "Мокаем состояние".ybm_run { _ in
            setupSkuInfoState()
        }

        "Открываем SKU".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        let cartButton = skuPage.addToCartButton

        "Проверяем отображение тайтла 'В корзину от 3 товаров' на картбатоне байбокса".ybm_run { _ in
            skuPage.element.ybm_swipe(toFullyReveal: cartButton.element)

            let label = cartButton.element.label
            XCTAssertEqual(label, Constants.outOfCartLabelText)
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Нажимаем на кнопку в корзину".ybm_run { _ in
            skuPage.element.ybm_swipe(toFullyReveal: cartButton.element)
            ybm_wait(forVisibilityOf: [cartButton.element])
            cartButton.element.tap()
            XCTAssertFalse(cartButton.plusButton.isEnabled)
        }

    }

    func testAddToCartFromVirtualBlock() {
        addToCartFromVirtualBlock(redesigned: false)
    }

    func testAddToCartFromVirtualBlockRedesigned() {
        addToCartFromVirtualBlock(redesigned: true)
    }

    // проверяем добавление в корзину, когда кол-во товара на стоке меньше, чем минимальное для заказа.
    private func addToCartFromVirtualBlock(redesigned: Bool) {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5943")
        Allure.addEpic("Корзина")
        Allure.addEpic("Добавление в корзину спайки")
        Allure.addTitle("Кол-во на стоках меньше, чем в минимальное кол-во товара в спайке")

        var rootPage: RootPage!
        var skuPage: SKUPage!
        var cartPage: CartPage!

        "Настраиваем FT".ybm_run { _ in
            enable(toggles: FeatureNames.minimumOrder)
            if redesigned {
                enable(toggles: FeatureNames.cartRedesign)
            }
        }

        "Мокаем состояние".ybm_run { _ in
            setupSkuInfoState()
        }

        "Открываем SKU".ybm_run { _ in
            rootPage = appAfterOnboardingAndPopups()
            skuPage = goToDefaultSKUPage(root: rootPage)
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Нажимаем на кнопку в корзину".ybm_run { _ in
            let cartButton = skuPage.addToCartButton
            skuPage.element.ybm_swipe(toFullyReveal: cartButton.element)
            ybm_wait(forVisibilityOf: [cartButton.element])
            cartButton.element.tap()
        }

        "Переходим в корзину".ybm_run { _ in
            cartPage = goToCart(root: rootPage)
        }

        "Проверяем, что в корзине лежит availableCount товаров из спайки".ybm_run { _ in
            let cartItem = cartPage.cartItem(at: 0)
            cartPage.element.ybm_swipe(toFullyReveal: cartItem.element)
            ybm_wait(forVisibilityOf: [cartItem.element])

            if redesigned {
                XCTAssertEqual(
                    cartItem.cartButton.label.trimmingCharacters(in: .whitespaces),
                    Constants.availableCount.string
                )
            } else {
                XCTAssertEqual(cartItem.countInfo.label, Constants.availableCount.string)
            }
        }

    }

    private func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(
            with: .init(
                offers: [Constants.modifiedOffer],
                useAvailableCount: true
            )
        )
        stateManager?.setState(newState: cartState)
    }

    private func setupSkuInfoState() {
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
}

// MARK: - Nested Types

private extension SKUCardModelVirtualPackTest {

    enum Constants {
        static let bundleSettingsMinimum = 4
        static let bundleSettingsStep = bundleSettingsMinimum
        static let availableCount = 3

        static let outOfCartLabelText = "В корзину от 3 товаров"
        static let inCartLabelText = "3 товара в корзине"

        static var modifiedOffer: FAPIOffer {
            modify(FAPIOffer.default) {
                $0.bundleSettings?.quantityLimit?.minimum = Constants.bundleSettingsMinimum
                $0.bundleSettings?.quantityLimit?.step = Constants.bundleSettingsStep
                $0.availableCount = Constants.availableCount
            }
        }
    }
}
