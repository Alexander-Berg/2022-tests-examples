import XCTest

class SKUCardModelBaseTestCase: LocalMockTestCase {

    struct SKU {
        var title: String
        var price: String
        var oldPrice: String?
    }

    /// Метод для проверки соответствия элемента корзины элементу с SKU
    func checkCart(cart: CartPage, sku: SKU) {
        ybm_wait(forFulfillmentOf: {
            cart.element.isVisible
        })

        let addedGood = cart.cartItem(at: 0)
        ybm_wait(forFulfillmentOf: { addedGood.element.exists })
        XCTAssertEqual(addedGood.price.label, sku.price)
        XCTAssertEqual(addedGood.title.label, sku.title)
    }

}
