import AutoMate
import MarketUITestMocks
import XCTest

final class SKUCardModelUnitInfoTest: LocalMockTestCase {

    func testShowUnitInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5574")
        Allure.addEpic("КМ")
        Allure.addFeature("Единицы измерений")
        Allure.addTitle("Проверяем отображение информации о единицах измрения")

        var skuState = SKUInfoState()

        "Настраиваем стейт".run {
            skuState.setSkuInfoState(with: .defaultWithUnit)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".run {
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение основной единицы в цене и цены за второстепенную единицу".run {
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.price.element)
            ybm_wait(forFulfillmentOf: { sku.price.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.price.unitPrice.isVisible })

            ybm_wait(forFulfillmentOf: { sku.price.price.label == "81 990 ₽/уп" })
            ybm_wait(forFulfillmentOf: { sku.price.unitPrice.label == "1 100 ₽ / шт" })
        }

        "Проверяем отображение основной единицы в цене и цены за второстепенную единицу у экспресс оффера".run {
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.expressPrice.element)
            ybm_wait(forFulfillmentOf: { sku.expressPrice.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.expressPrice.unitPrice.isVisible })

            ybm_wait(forFulfillmentOf: { sku.expressPrice.price.label == "92 105 ₽/уп" })
            ybm_wait(forFulfillmentOf: { sku.expressPrice.unitPrice.label == "1 150 ₽ / шт" })
        }
    }

    func testUnitCalculator() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5869")
        Allure.addEpic("КМ")
        Allure.addFeature("Калькулятор единиц")
        Allure.addTitle("Проверяем отображение калькулятора единиц измерения")

        var sku: SKUPage!
        var skuState = SKUInfoState()

        "Настраиваем стейт".run {
            var config = CustomSKUConfig(
                productId: Constants.productId,
                offerId: Constants.offerId
            )
            config.unitInfo = .default
            config.navnode = [
                modify(
                    NavNode(nid: Constants.nid, hid: Constants.productId, name: "Молочные смеси")
                ) {
                    $0.tags = ["unit_calc"]
                }
            ]
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".run {
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".run {
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "Добавить в корзину" })
        }

        "Проверяем текст калькулятора".run {
            ybm_wait(forFulfillmentOf: { sku.unitCalc.label == "1 уп = 0,50 шт" })
        }

        var cartState = CartState()
        "Настраиваем стейт с товаром в корзине".run {
            cartState.addItemsToCartState(with: .init(offers: [Constants.capiOffer]))
            cartState.setCartStrategy(with: [Constants.fapiOffer])
            stateManager?.setState(newState: cartState)

        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".run {
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "Добавить в корзину" })

            sku.addToCartButton.element.tap()
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "1 уп в корзине" })
        }

        "Проверяем текст калькулятора".run {
            ybm_wait(forFulfillmentOf: { sku.unitCalc.label == "1 уп = 0,50 шт" })
        }

        "Мокаем увеличение товара в корзине".run {
            let cartItem = CartItemInCart.makeFromOffer(Constants.fapiOffer, count: 2)
            let strategy = modify(
                ResolveUserCartWithStrategiesAndBusinessGroups
                    .VisibleStrategiesFromUserCart(offers: [Constants.fapiOffer])
            ) {
                $0.cartItem = [cartItem]
            }

            cartState.changeCartItemsState(with: .init(
                cartItems: [cartItem],
                threshold: []
            ))

            cartState.setCartStrategy(with: strategy)
            stateManager?.setState(newState: cartState)
        }

        "Нажимаем на '+' в кнопке добавления в корзину и проверяем данные".run {
            sku.addToCartButton.plusButton.tap()
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "2 уп в корзине" })
        }

        "Проверяем текст калькулятора".run {
            ybm_wait(forFulfillmentOf: { sku.unitCalc.label == "2 уп = 1,00 шт" })
        }
    }
}

// MARK: - Nested Types

private extension SKUCardModelUnitInfoTest {

    enum Constants {

        static let nid = 78_085
        static let productId = 13_621_355
        static let offerId = "J6zCIjgXkqgppGtMDBpsrQ"

        static let capiOffer = modify(CAPIOffer.protein) {
            $0.wareId = offerId
        }

        static let fapiOffer = modify(FAPIOffer.default) {
            $0.wareId = offerId
            $0.unitInfo = .default
        }
    }
}
