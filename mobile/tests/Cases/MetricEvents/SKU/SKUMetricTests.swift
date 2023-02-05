import MarketUITestMocks
import Metrics
import XCTest

final class SkuMetricTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testShouldSendSKUMetricsWhenOpenSKU() {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Просмотр КМ")
        Allure.addTitle("Отправка продуктовых метрик при просмотре КМ")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            _ = goToDefaultSKUPage()
        }

        "Проверяем отправку событий просмотра км в AppMetrica".ybm_run { _ in
            let openEvents = MetricRecorder.events(from: .appmetrica)
                .with(name: "PRODUCT_PAGE_OPEN")
                .with(params: ["skuId": "100902560734"])

            let visibleEvents = MetricRecorder.events(from: .appmetrica)
                .with(name: "PRODUCT_PAGE_OFFER_SHOW_VISIBLE")
                .with(params: [
                    "skuId": "101077347763",
                    "rate": 4.670_000_076_293_945,
                    "price": 81_990,
                    "hid": 91_491,
                    "isGiftAvailable": "false",
                    "isExpired": "false",
                    "skuType": "market",
                    "priceDrop": "false",
                    "responses": 89,
                    "productId": 722_979_017
                ])

            XCTAssertEqual(openEvents.count, 1)
            XCTAssertEqual(visibleEvents.count, 1)
        }

        "Проверяем отправку событий просмотра км в AdWords".ybm_run { _ in
            let events = MetricRecorder.events(from: .appmetrica).with(name: "view_item")
                .with(params: ["item_id": "100902560734"])
            XCTAssertEqual(events.count, 1)
        }

        "Проверяем отправку событий просмотра км в Firebase".ybm_run { _ in
            let events = MetricRecorder.events(from: .firebase)
                .with(name: "view_item")
                .with(params: [
                    "value": 81_990,
                    "currency": "RUB"
                ])
                .filter { event in
                    guard
                        let items = event.parameters["items"] as? [[String: Any]],
                        let itemId = items[0]["item_id"] as? String,
                        let itemCategory = items[0]["item_category"] as? String
                    else { return false }

                    return itemId == "101077347763"
                        && itemCategory == "91491"
                }

            XCTAssertEqual(events.count, 1)
        }

        "Проверяем отправку событий просмотра км в Adjust".ybm_run { _ in
            XCTAssertEqual(
                MetricRecorder.events(from: .adjust).with(name: "coh39b").with(params: [
                    "criteo_p": "101077347763",
                    "sku_id": "['101077347763']",
                    "content_type": "product",
                    "currency_code": "RUB",
                    "fb_content_type": "product",
                    "_valueToSum": "81990"
                ]).count, 1
            )
        }
    }

    func testShouldSendSkuMetricsWhenAddToCartFromSkuPage() {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Добавление в корзину")
        Allure.addTitle("Отправка продуктовых метрик при добавлении товара в корзину")

        var skuPage: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)

            var cartState = CartState()
            cartState.addItemsToCartState(with: .init(offers: [CAPIOffer.protein]))
            stateManager?.setState(newState: cartState)
        }

        "Открываем SKU".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Добавляем товар в корзину".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            skuPage.addToCartButton.element.tap()
        }

        "Проверяем отправку события добавления в корзину в AppMetrica".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "ADD-TO-CART")
                    .with(params: [
                        "price": 81_990,
                        "skuType": "market",
                        "skuId": "101077347763",
                        "isFirstOrder": "true"
                    ])
                    .isNotEmpty
            }
        }

        "Проверяем отправку события добавления в корзину в AdWords".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica).with(name: "add_to_cart")
                    .with(params: ["item_id": "101077347763"]).isNotEmpty
            })
        }

        "Проверяем отправку события добавления в корзину в Firebase".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                let event = MetricRecorder.events(from: .firebase)
                    .with(name: "add_to_cart")
                    .with(params: [
                        "value": 81_990,
                        "currency": "RUB"
                    ])
                    .first { event in
                        guard
                            let items = event.parameters["items"] as? [[String: Any]],
                            let itemId = items[0]["item_id"] as? String,
                            let itemCategory = items[0]["item_category"] as? String,
                            let itemName = items[0]["item_name"] as? String
                        else { return false }

                        return itemId == "101077347763"
                            && itemName == "Смартфон Apple iPhone 12 256GB, синий"
                            && itemCategory == "91491"
                    }
                return event != nil
            })
        }

        "Проверяем отправку события добавления в корзину в Adjust".ybm_run { _ in
            XCTAssertTrue(
                MetricRecorder.events(from: .adjust)
                    .with(name: "2yabky")
                    .with(params: [
                        "content_type": "product",
                        "currency_code": "RUB",
                        "fb_content_type": "product",
                        "_valueToSum": "81990"
                    ])
                    .contains { event in
                        guard let items = event.parameters["items"] as? String
                        else { return false }

                        return items.contains("'id':'101077347763'") && items.contains("'quantity':1")
                    }
            )
        }
    }

    // Разделил на 2 части, так как тест падал по таймауту
    func testShouldSendPurchaseMetricsWhenPurchaseSuccess_part1() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3277")
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Чекаут")
        Allure.addTitle("Отправка продуктовых метрик при чекауте")

        var checkoutPage: CheckoutPage!
        var cart: CartPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Моки".ybm_run { _ in
            let order = Order.Mapper(
                id: "57",
                status: .delivery,
                delivery: .init(
                    deliveryPartnerType: .yandex,
                    fromDate: "05-08-2020",
                    toDate: "05-08-2020",
                    fromTime: "12:00",
                    toTime: "18:00",
                    type: .service
                ),
                msku: ["1"]
            )
            var ordersState = OrdersState()
            let orderMapper = OrdersState.UserOrdersHandlerMapper(orders: [order])
            ordersState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byIds])
            stateManager?.setState(newState: ordersState)

            var cartState = CartState()
            cartState.setCartStrategy(with: [.protein])
            stateManager?.setState(newState: cartState)

            var userState = UserAuthState()
            userState.setContactsState(contacts: [.basic])
            userState.setAddressesState(addresses: [.default])
            stateManager?.setState(newState: userState)

            mockStateManager?.pushState(bundleName: "Checkout_Metrics")
        }

        "Переходим в корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Очищаем хранилище событий".ybm_run { _ in
            MetricRecorder.clear()
        }

        "Переходим на экран информации о доставке в чекауте".ybm_run { _ in
            wait(forVisibilityOf: cart.compactSummary.orderButton.element)
            checkoutPage = cart.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Проверяем отправку событий перехода в чекаут в Firebase".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .firebase).with(name: "begin_checkout").isNotEmpty
            }
        }

        "Проверяем отправку событий перехода в чекаут в Adjust".ybm_run { _ in
            ybm_wait {
                self.checkAdjustGoToCheckoutEvents()
            }
        }
    }

    func testShouldSendPurchaseMetricsWhenPurchaseSuccess_part2() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3277")
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Оформления заказа")
        Allure.addTitle("Отправка продуктовых метрик при оформлении заказа")

        var checkoutPage: CheckoutPage!
        var finishMultiorderPage: FinishMultiorderPage!
        var cart: CartPage!

        disable(toggles: FeatureNames.cartRedesign)

        "Моки".ybm_run { _ in
            let order = Order.Mapper(
                id: "57",
                status: .delivery,
                delivery: .init(
                    deliveryPartnerType: .yandex,
                    fromDate: "05-08-2020",
                    toDate: "05-08-2020",
                    fromTime: "12:00",
                    toTime: "18:00",
                    type: .service
                ),
                msku: ["1"]
            )
            var ordersState = OrdersState()
            let orderMapper = OrdersState.UserOrdersHandlerMapper(orders: [order])
            ordersState.setOrdersResolvers(mapper: orderMapper, for: [.all, .byIds])
            stateManager?.setState(newState: ordersState)

            var cartState = CartState()
            cartState.setCartStrategy(with: [.protein])
            stateManager?.setState(newState: cartState)

            var userState = UserAuthState()
            userState.setContactsState(contacts: [.basic])
            userState.setAddressesState(addresses: [.default])
            stateManager?.setState(newState: userState)

            mockStateManager?.pushState(bundleName: "Checkout_Metrics")
        }

        "Переходим в корзину".ybm_run { _ in
            cart = goToCart()
        }

        "Переходим на экран информации о доставке в чекауте".ybm_run { _ in
            wait(forVisibilityOf: cart.compactSummary.orderButton.element)
            checkoutPage = cart.compactSummary.orderButton.tap()
            wait(forVisibilityOf: checkoutPage.element)
        }

        "Очищаем хранилище событий".ybm_run { _ in
            MetricRecorder.clear()
        }

        "Подтверждаем заказ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)

            finishMultiorderPage = checkoutPage.paymentButton.tap()
            wait(forVisibilityOf: finishMultiorderPage.element)
        }

        "Проверяем отправку событий оформления заказа в Adjust".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                self.checkAdjustPurchaseSuccessEvents()
            })
        }

        "Проверяем отправку событий оформления заказа в AdWords".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .appmetrica).with(name: "ecommerce_purchase")
                    .with(params: ["item_id": ["101077347763"]]).isNotEmpty
            })
        }

        "Проверяем отправку событий оформления заказа в Firebase".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                self.checkFirebasePurchaseSuccessEvents()
            })
        }

        "Проверяем отправку событий оформления заказа в AppMetrica".ybm_run { _ in
            ybm_wait {
                self.checkAppMetricaPurchaseSuccessEvents()
            }
        }
    }

    private func checkAdjustGoToCheckoutEvents() -> Bool {
        let itemParts = [
            "\'price\':\'1413\'",
            "\'count\':\'1\'",
            "\'currency\':\'RUB\'",
            "\'sku_id\':\'100963252802\'"
        ]

        let events = MetricRecorder.events(from: .adjust)
            .with(name: "wh8f03")
            .with(params: [
                "sku_id": "[\'100963252802\']",
                "content_type": "product",
                "currency_code": "RUB",
                "fb_content_type": "product",
                "price": "1413",
                "info_available": "1"
            ])
            .filter { event in
                guard let items = event.parameters["items"] as? String else { return false }

                return itemParts.allSatisfy { items.contains($0) }
            }

        return events.count == 1
    }

    private func checkAppMetricaPurchaseSuccessEvents() -> Bool {
        let orderCreateEvents = MetricRecorder.events(from: .appmetrica).with(name: "CHECKOUT_SUMMARY_ORDER_CREATE")
            .filter { event in
                guard
                    let orderId = event.parameters["orderId"] as? String,
                    let shopId = event.parameters["shopId"] as? String,
                    let multiOrderId = event.parameters["multiOrderId"] as? String,
                    let paymentType = event.parameters["paymentType"] as? String,
                    let option = event.parameters["option"] as? [String: Any],
                    let optionType = option["type"] as? String,
                    let optionPrice = option["price"] as? String,
                    let optionDates = option["dates"] as? String,
                    let items = (event.parameters["items"] as? [[String: Any]])?.first,
                    let skuId = items["skuId"] as? String
                else { return false }

                return orderId == "57"
                    && shopId == "10268608"
                    && multiOrderId == "unknown"
                    && paymentType == "Наличными при получении"
                    && optionType == "Доставка"
                    && optionPrice == "0"
                    && optionDates == "2020-08-05 00:00:00 +0000 - 2020-08-05 00:00:00 +0000"
                    && skuId == "101077347763"
            }

        let checkoutSuccessEvents = MetricRecorder.events(from: .appmetrica).with(name: "CHECKOUT-SUCCESS_VISIBLE")
            .with(params: [
                "orderIds": "57",
                "paymentType": "CASH_ON_DELIVERY",
                "isPaymentSuccess": "false",
                "isPrepaid": "false"
            ])

        let grossOrderEvents = MetricRecorder.events(from: .appmetrica)
            .with(name: "CHECKOUT-SUCCESS_GROSS-ORDER-CONFIRMATION_VISIBLE")
            .with(params: [
                "orderId": "57",
                "multiOrderId": "unknown",
                "price": "102",
                "deliveryDate": "2020-08-05",
                "skuIds": ["101077347763"]
            ])

        let multiGrossOrderEvents = MetricRecorder.events(from: .appmetrica)
            .with(name: "CHECKOUT-SUCCESS_GROSS-MULTIORDER-CONFIRMATION_VISIBLE").filter { event in
                guard
                    let buckets = (event.parameters["buckets"] as? [[String: Any]])?.first,
                    let orderId = buckets["orderId"] as? String,
                    let multiOrderId = buckets["multiOrderId"] as? String,
                    let price = buckets["price"] as? String,
                    let deliveryDate = buckets["deliveryDate"] as? String,
                    let skuIds = buckets["skuIds"] as? [String]
                else { return false }

                return orderId == "57"
                    && multiOrderId == "unknown"
                    && price == "102"
                    && deliveryDate == "2020-08-05"
                    && skuIds == ["101077347763"]
            }

        let anyPaymentMethodOrderEvents = MetricRecorder.events(from: .appmetrica)
            .with(name: "CHECKOUT-SUCCESS_ANY-PAYMENT-METHOD-CONFIRMATION_VISIBLE").filter { event in
                guard
                    let buckets = (event.parameters["buckets"] as? [[String: Any]])?.first,
                    let orderId = buckets["orderId"] as? String,
                    let multiOrderId = buckets["multiOrderId"] as? String,
                    let price = buckets["price"] as? String,
                    let deliveryDate = buckets["deliveryDate"] as? String,
                    let skuIds = buckets["skuIds"] as? [String]
                else { return false }

                return orderId == "57"
                    && multiOrderId == "unknown"
                    && price == "102"
                    && deliveryDate == "2020-08-05"
                    && skuIds == ["101077347763"]
            }

        return orderCreateEvents.count == 1
            && checkoutSuccessEvents.count == 1
            && grossOrderEvents.count == 1
            && multiGrossOrderEvents.count == 1
            && anyPaymentMethodOrderEvents.count == 1
    }

    private func checkFirebasePurchaseSuccessEvents() -> Bool {
        let check = { (event: MetricRecorderEvent?) -> Bool in
            guard
                let event = event,
                let value = event.parameters["value"] as? Int,
                let currency = event.parameters["currency"] as? String,
                let item = (event.parameters["items"] as? [[String: Any]])?.first
            else {
                return false
            }
            let newCustomer = event.parameters["new_customer"] as? Int
            return value == 102
                && currency == "RUB"
                && item["item_id"] as? String == "101077347763"
                && item["item_category"] as? String == "91491"
                && item["item_name"] as? String == "Смартфон Apple iPhone 12 256GB, синий"
                && (event.name == "purchase_any_payment_method_multiorder" || newCustomer == 0)
        }

        let ecommercePurchase = MetricRecorder.events(from: .firebase).with(name: "ecommerce_purchase").first
        let purchase = MetricRecorder.events(from: .firebase).with(name: "purchase").first
        let multiOrderWithoutCredit = MetricRecorder.events(from: .firebase)
            .with(name: "purchase_without_credit_multiorder").first
        let anyPaymentMethodOrder = MetricRecorder.events(from: .firebase)
            .with(name: "purchase_any_payment_method_multiorder").first
        return [ecommercePurchase, purchase, multiOrderWithoutCredit, anyPaymentMethodOrder]
            .allSatisfy(check)
    }

    private func checkAdjustPurchaseSuccessEvents() -> Bool {
        let commonCheck = { (event: MetricRecorderEvent) -> Bool in
            let items = event.parameters["items"] as? String
            return event.parameters["sku_id"] as? String == "['101077347763']"
                && items?.contains("'id':'101077347763'") == true
                && items?.contains("'quantity':1") == true
                && event.parameters["items_count"] as? String == "1"
                && event.parameters["content_type"] as? String == "product_group"
                &&
                (
                    event.parameters["currency"] as? String == "RUB" || event
                        .parameters["fb_currency"] as? String == "RUB"
                )
                && event.parameters["fb_content_type"] as? String == "product"
                && event.parameters["price"] as? String == "102"
                && event.parameters["criteo_p"] as? String ==
                "%5B%7B%22i%22:%22101077347763%22,%22pr%22:81990.000000,%22q%22:1%7D%5D"
        }
        let purchaseEvents = MetricRecorder.events(from: .adjust).with(name: "yfw49c").filter { event in
            guard
                commonCheck(event),
                let orderId = event.parameters["order_id"] as? String,
                let transactionId = event.parameters["transaction_id"] as? String,
                let multiOrderId = event.parameters["multi_order_id"] as? String
            else { return false }

            return orderId == "['unknown']"
                && transactionId == "unknown"
                && multiOrderId == "['unknown']"
        }

        let purchaseWithoutCreditEvents = MetricRecorder.events(from: .adjust).with(name: "rxg80i").filter { event in
            guard
                commonCheck(event),
                let orderId = event.parameters["order_id"] as? String,
                let transactionId = event.parameters["transaction_id"] as? String,
                let multiOrderId = event.parameters["multi_order_id"] as? String
            else {
                return false
            }

            return orderId == "['57']"
                && transactionId == "57"
                && multiOrderId == "['unknown']"
        }
        let multiPurchaseWithoutCreditEvents = MetricRecorder.events(from: .adjust).with(name: "jk30lx")
            .filter { event in
                guard
                    commonCheck(event),
                    let orderId = event.parameters["order_id"] as? String,
                    let orderIds = event.parameters["order_ids"] as? String,
                    let transactionId = event.parameters["transaction_id"] as? String,
                    let multiOrderId = event.parameters["multi_order_id"] as? String
                else { return false }

                return orderId == "['unknown']"
                    && orderIds == "['57']"
                    && transactionId == "unknown"
                    && multiOrderId == "['unknown']"
            }

        let anyPaymentMethodOrderEvents = MetricRecorder.events(from: .adjust).with(name: "s3nzwp").filter { event in
            guard commonCheck(event),
                  let orderId = event.parameters["order_id"] as? String,
                  let transactionId = event.parameters["transaction_id"] as? String,
                  let multiOrderId = event.parameters["multi_order_id"] as? String
            else { return false }

            return orderId == "['unknown']"
                && transactionId == "unknown"
                && multiOrderId == "['unknown']"
        }

        return purchaseEvents.count == 1
            && purchaseWithoutCreditEvents.count == 1
            && multiPurchaseWithoutCreditEvents.count == 1
            && anyPaymentMethodOrderEvents.count == 1
    }
}
