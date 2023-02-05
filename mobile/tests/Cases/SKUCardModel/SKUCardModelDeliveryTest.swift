import MarketUITestMocks
import XCTest

final class SKUCardModelDeliveryTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    // MARK: - Nested Types

    struct DeliveryOptionData {
        let title: String?
        let contains: [String]
        let excepts: [String]

        init(title: String? = nil, contains: [String] = [], excepts: [String] = []) {
            self.title = title
            self.contains = contains
            self.excepts = excepts
        }
    }

    // MARK: - Tests

    func testDeliveryCard() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-929")
        Allure.addEpic("КМ")
        Allure.addFeature("Блок доставки")
        Allure.addTitle("Проверить корректность отображения информации на карточке с доставкой (без цены доставки)")

        var sku: SKUPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            let pickupDelivery = FAPIOffer.Delivery.PickupOptions(dayFrom: 2, dayTo: 5)
            let courierDelivery = FAPIOffer.Delivery.PickupOptions(dayFrom: 2, dayTo: 2)
            let offer = modify(FAPIOffer.default) {
                $0.delivery.pickupOptions = [pickupDelivery]
                $0.delivery.courierOptions = [courierDelivery]
            }

            skuState.setSkuInfoState(offer: offer, model: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем ячейки с информацией о доставке, их видимость, наличие, содержание".ybm_run { _ in
            // Скроллимся до кнопки "в корзину", чтобы скролл точно работал
            // при этом все опции доставки будут видны
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
            let dateString = string(from: dateInFuture(days: 2))
            check(
                deliveryOption: sku.deliveryOptions.service,
                data: DeliveryOptionData(contains: ["Курьером", dateString], excepts: ["249"])
            )
            let fromDate = dateInFuture(days: 2)
            let toDate = dateInFuture(days: 5)
            let dateIntervalString = string(from: fromDate, to: toDate)
            check(
                deliveryOption: sku.deliveryOptions.pickup,
                data: DeliveryOptionData(title: "Самовывоз \(dateIntervalString) — 99 ₽")
            )
            XCTAssertEqual(sku.supplierInfo.title.label, "Яндекс.Маркет")
        }
    }

    func testDeliveryOptionsLargeSum() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-946")
        Allure.addEpic("КМ")
        Allure.addFeature("Блок доставки")
        Allure.addTitle(
            "Проверить корректность отображения информации на карточке с доставкой," +
                " когда цена > трешхолда (без цены доставки)"
        )

        var sku: SKUPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            let pickupDelivery = FAPIOffer.Delivery.PickupOptions(price: "0", dayFrom: 1, dayTo: 1)
            let courierDelivery = FAPIOffer.Delivery.PickupOptions(price: "0", dayFrom: 1, dayTo: 1)
            let offer = modify(FAPIOffer.default) {
                $0.delivery.pickupOptions = [pickupDelivery]
                $0.delivery.courierOptions = [courierDelivery]
            }

            skuState.setSkuInfoState(offer: offer, model: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем, что в блоке для самовывоза и курьерки отображаются нужные данные и сами блоки видны и имеют нужное содержание"
            .ybm_run { _ in
                sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)
                let dateString = string(from: dateInFuture(days: 1))
                check(
                    deliveryOption: sku.deliveryOptions.service,
                    data: DeliveryOptionData(title: "Курьером завтра, \(dateString) — 0 ₽")
                )

                check(
                    deliveryOption: sku.deliveryOptions.pickup,
                    data: DeliveryOptionData(title: "Самовывоз завтра, \(dateString) — бесплатно")
                )
            }
    }

    func testClickAndCollect() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-942")
        Allure.addEpic("КМ")
        Allure.addFeature("Блок доставки")
        Allure.addTitle("Проверить карточку c&c товара")

        var cartPage: CartPage!
        var purchasedCell: CartPage.CartItem!
        var sku: SKUPage!

        var skuState = SKUInfoState()

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .alco)
            stateManager?.setState(newState: skuState)
        }

        "Переход в корзину с товарами".ybm_run { _ in
            cartPage = goToCart()
            purchasedCell = cartPage.cartItem(at: 0)
        }

        "Переход на товар в корзине".ybm_run { _ in
            wait(forExistanceOf: purchasedCell.element)
            sku = purchasedCell.tap()
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        "Отображается надпись \"Выкупить в торговом зале\" и продавец товара + иконки".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.deliveryOptions.clickAndCollect.element)
            check(
                deliveryOption: sku.deliveryOptions.clickAndCollect,
                data: DeliveryOptionData(title: "Самовывоз из торгового зала  — 0 ₽")
            )

            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.supplierInfo.element)
            XCTAssertEqual(sku.supplierInfo.title.label, "DropShopDM")
        }
    }

    func testThatOptionsHasDeliveryTimeAndShopWorkingTime() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4223")
        Allure.addEpic("КМ")
        Allure.addFeature("Опции")
        Allure.addTitle("Проверяем отображение в опциях SKU информации об экспресс доставке и времени работы магазина")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            let courierDelivery = FAPIOffer.Delivery.PickupOptions(price: "0", dayFrom: 1, dayTo: 1)
            let offer = modify(FAPIOffer.express) {
                $0.delivery.courierOptions = [courierDelivery]
            }

            skuState.setSkuInfoState(offer: offer, shop: [FAPIOffer.ShopInfo.Preset.express.shop])
            stateManager?.setState(newState: skuState)
        }

        checkExpressDeliveryOptionShopWorkingTime(
            time: "22:13:20",
            expressLabelText: "Express-доставка Завтра с 10:00"
        )

        "Переоткрываем приложение".ybm_run { _ in
            relaunchApp(clearData: false)
        }

        checkExpressDeliveryOptionShopWorkingTime(
            time: "01:13:20",
            expressLabelText: "Express-доставка Сегодня с 10:00"
        )

        "Переоткрываем приложение".ybm_run { _ in
            relaunchApp(clearData: false)
        }

        checkExpressDeliveryOptionShopWorkingTime(
            time: "13:13:20",
            expressLabelText: "Express-доставка За 1-2 часа"
        )
    }

    func testFreeDeliveryByPlusWithoutPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4179")
        Allure.addEpic("КМ")
        Allure.addFeature("Опции")
        Allure.addTitle("Проверяем надпись 'С Яндекс Плюс доставка бесплатная' у неплюсовика")

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = [
            FeatureNames.showPlus,
            FeatureNames.plusBenefits
        ].joined(separator: ",")

        var root: RootPage!
        var profile: ProfilePage!

        "Мокаем состояние, регион - Москва, цена товара 1250".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDelivery_Tier1_1250")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            var authState = UserAuthState()
            skuState.setSkuInfoState(with: .custom(.freeDelivery))
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: skuState)
            stateManager?.setState(newState: authState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        checkFreeDeliveryLabel(root: root)

        "Открываем профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Меняем моки, регион - Псков, цена товара 1250".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDelivery_Tier2_1250")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.noFreeDelivery))
            stateManager?.setState(newState: skuState)
        }

        checkNoFreeDeliveryLabel(root: root)

        "Открываем профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Меняем моки, регион - Псков, цена товара 1650".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDelivery_Tier2_1650")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.freeDelivery))
            stateManager?.setState(newState: skuState)
        }

        checkFreeDeliveryLabel(root: root)

        "Открываем профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Меняем моки, регион - Чита, цена товара 1650".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDelivery_Tier3_1650")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.noFreeDelivery))
            stateManager?.setState(newState: skuState)
        }

        checkNoFreeDeliveryLabel(root: root)

        "Открываем профиль".ybm_run { _ in
            profile = goToProfile(root: root)
            wait(forVisibilityOf: profile.element)
        }

        "Меняем моки, регион - Чита, цена товара 10.000 +".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FreeDelivery_Tier3_10k")
        }
        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            var authState = UserAuthState()
            skuState.setSkuInfoState(with: .custom(.freeDelivery))
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: skuState)
        }

        checkFreeDeliveryLabel(root: root)
    }

    // MARK: - Private

    private func checkFreeDeliveryLabel(root: RootPage) {
        "Проверка блока с надписью 'С Яндекс Плюс доставка бесплатная'".ybm_run { _ in
            var skuPage: SKUPage!

            skuPage = goToDefaultSKUPage(root: root)
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.freeDeliveryByYandexPlus)

            XCTAssertEqual(
                skuPage.freeDeliveryByYandexPlus.label,
                "С Яндекс Плюс доставка бесплатная"
            )

            goToMorda(root: root)
        }
    }

    private func checkNoFreeDeliveryLabel(root: RootPage) {
        "Проверка отсутствия блока с надписью 'С Яндекс Плюс доставка бесплатная'".ybm_run { _ in
            var skuPage: SKUPage!

            skuPage = goToDefaultSKUPage(root: root)
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)

            XCTAssertFalse(skuPage.freeDeliveryByYandexPlus.exists)

            goToMorda(root: root)
        }
    }

    private func checkExpressDeliveryOptionShopWorkingTime(time: String, expressLabelText: String) {
        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTime] = time

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Долистываем до секции с опциями и проверяем его содержание".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.deliveryOptions.service.element)
            check(
                deliveryOption: sku.deliveryOptions.service,
                data: DeliveryOptionData(title: expressLabelText)
            )
        }
    }

    private func check(deliveryOption: SKUPage.DeliveryOptions.DeliveryOption, data: DeliveryOptionData) {
        data.title.ble_apply { title in
            XCTAssertTrue(deliveryOption.title.isVisible)
            func normalizeSpaces(_ string: String) -> String {
                string.replacingOccurrences(of: "\u{00a0}", with: " ")
            }
            XCTAssertEqual(normalizeSpaces(deliveryOption.title.label), normalizeSpaces(title))
        }

        data.contains.forEach { substring in
            XCTAssertTrue(deliveryOption.title.isVisible)
            XCTAssertTrue(deliveryOption.title.label.contains(substring))
        }

        data.excepts.forEach { exception in
            XCTAssertTrue(deliveryOption.title.isVisible)
            XCTAssertFalse(deliveryOption.title.label.contains(exception))
        }
    }

    private func dateInFuture(days: Int) -> Date {
        Date().addingTimeInterval(60 * 60 * 24 * TimeInterval(days))
    }

    private func string(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMMM"
        formatter.locale = Locale(identifier: "ru_RU")
        return formatter.string(from: date).lowercased()
    }

    private func string(from fromDate: Date, to toDate: Date) -> String {
        guard
            let fromMonth = Calendar.current.dateComponents([.month], from: fromDate).month,
            let toMonth = Calendar.current.dateComponents([.month], from: toDate).month,
            fromMonth == toMonth
        else { return separateMonthsString(from: fromDate, to: toDate) }

        return oneMonthString(from: fromDate, to: toDate)
    }

    private func oneMonthString(from fromDate: Date, to toDate: Date) -> String {
        let locale = Locale(identifier: "ru_RU")

        let fromFormatter = DateFormatter()
        fromFormatter.dateFormat = "d"
        fromFormatter.locale = locale

        let toFormatter = DateFormatter()
        toFormatter.dateFormat = "d MMMM"
        toFormatter.locale = locale

        return fromFormatter.string(from: fromDate) + "–" + toFormatter.string(from: toDate).lowercased()
    }

    private func separateMonthsString(from fromDate: Date, to toDate: Date) -> String {
        let locale = Locale(identifier: "ru_RU")

        let fromFormatter = DateFormatter()
        fromFormatter.dateFormat = "d MMMM"
        fromFormatter.locale = locale

        let toFormatter = DateFormatter()
        toFormatter.dateFormat = "d MMMM"
        toFormatter.locale = locale

        return fromFormatter.string(from: fromDate) + "–" + toFormatter.string(from: toDate).lowercased()
    }
}

private extension CustomSKUConfig {
    static var noFreeDelivery: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 13_621_355,
            offerId: "J6zCIjgXkqgppGtMDBpsrQ"
        )
        config.delivery.isBetterWithPlus = false
        return config
    }

    static var freeDelivery: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 13_621_355,
            offerId: "J6zCIjgXkqgppGtMDBpsrQ"
        )
        config.delivery.isBetterWithPlus = true
        return config
    }
}
