import XCTest

final class SKUCardModelAlternativeOffersTest: SKUCardModelBaseTestCase {

    func testReasonCheaper() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3395")
        Allure.addEpic("КМ")
        Allure.addFeature("Мультиоффер")
        Allure.addTitle("Причина - Дешевле")

        var root: RootPage!
        var sku: SKUPage!
        var alternativeOffer: SKUPage.AlternativeOffers.AlternativeOffer!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Cheaper")
        }

        "Открываем SKU".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Проверяем ячейку альтернативного оффера".ybm_run { _ in
            alternativeOffer = sku.alternativeOffers.cheaper
            sku.element.ybm_swipeCollectionView(toFullyReveal: alternativeOffer.element)
            compareReasonDetails(
                lhs: alternativeOffer.title.label,
                rhs: "Самая выгодная цена — забрать в торговом зале сегодня, 0 ₽"
            )
            XCTAssertEqual(alternativeOffer.subtitle.label, "bluepharma.one")
        }

        "Мокаем состояние корзины".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Cheaper_InCart")
        }

        "Добавляем оффер в корзину".ybm_run { _ in
            alternativeOffer.cartButton.tap()
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)

            let skuData = SKU(
                title: "Автомобильная шина APLUS A501 195/70 R15 104/102R зимняя",
                price: "3 650 ₽",
                oldPrice: nil
            )
            checkCart(cart: cart, sku: skuData)
        }
    }

    func testReasonDeliveryType() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3398")
        Allure.addEpic("КМ")
        Allure.addFeature("Мультиоффер")
        Allure.addTitle("Причина - Другой способ доставки")

        var root: RootPage!
        var sku: SKUPage!
        var alternativeOffer: SKUPage.AlternativeOffers.AlternativeOffer!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_DeliveryType")
        }

        "Открываем SKU".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Проверяем ячейку альтернативного оффера".ybm_run { _ in
            alternativeOffer = sku.alternativeOffers.deliveryType
            sku.element.ybm_swipeCollectionView(toFullyReveal: alternativeOffer.element)
            compareReasonDetails(
                lhs: alternativeOffer.title.label,
                rhs: "Есть доставка в пункт выдачи — завтра, 0 ₽"
            )
            XCTAssertEqual(alternativeOffer.subtitle.label, "bugs-princess")
        }

        "Мокаем состояние корзины".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_DeliveryType_InCart")
        }

        "Добавляем оффер в корзину".ybm_run { _ in
            alternativeOffer.cartButton.tap()
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)

            let skuData = SKU(
                title: "Автомобильная шина APLUS A501 195/70 R15 104/102R зимняя",
                price: "3 700 ₽",
                oldPrice: nil
            )
            checkCart(cart: cart, sku: skuData)
        }
    }

    func testReasonFaster() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3391")
        Allure.addEpic("КМ")
        Allure.addFeature("Мультиоффер")
        Allure.addTitle("Причина - Быстрее")

        var root: RootPage!
        var sku: SKUPage!
        var alternativeOffer: SKUPage.AlternativeOffers.AlternativeOffer!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Faster")
        }

        "Открываем SKU".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Проверяем ячейку альтернативного оффера".ybm_run { _ in
            alternativeOffer = sku.alternativeOffers.faster
            sku.element.ybm_swipeCollectionView(toFullyReveal: alternativeOffer.element)
            compareReasonDetails(
                lhs: alternativeOffer.title.label,
                rhs: "Доставка быстрее — завтра, 0 ₽"
            )
            XCTAssertEqual(alternativeOffer.subtitle.label, "bugs-princess")
        }

        "Мокаем состояние корзины".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Faster_InCart")
        }

        "Добавляем оффер в корзину".ybm_run { _ in
            alternativeOffer.cartButton.tap()
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)

            let skuData = SKU(
                title: "Автомобильная шина APLUS A501 195/70 R15 104/102R зимняя",
                price: "3 700 ₽",
                oldPrice: nil
            )
            checkCart(cart: cart, sku: skuData)
        }
    }

    func testReasonGift() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3401")
        Allure.addEpic("КМ")
        Allure.addFeature("Мультиоффер")
        Allure.addTitle("Причина - С подарком")

        var root: RootPage!
        var sku: SKUPage!
        var alternativeOffer: SKUPage.AlternativeOffers.AlternativeOffer!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Gift")
        }

        "Открываем SKU".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            sku = goToDefaultSKUPage(root: root)
        }

        "Проверяем ячейку альтернативного оффера".ybm_run { _ in
            alternativeOffer = sku.alternativeOffers.gift
            sku.element.ybm_swipeCollectionView(toFullyReveal: alternativeOffer.element)
            compareReasonDetails(
                lhs: alternativeOffer.title.label,
                rhs: "С подарком — Каталка-толокар Chi lok BO Mega (381А) со звуковыми эффектами красный"
            )
            XCTAssertEqual(alternativeOffer.subtitle.label, "Яндекс.Маркет")
        }

        "Мокаем состояние корзины".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_AlternativeOffers_Gift_InCart")
        }

        "Добавляем оффер в корзину".ybm_run { _ in
            alternativeOffer.cartButton.tap()
        }

        "Переходим в корзину и проверяем данные".ybm_run { _ in
            let cart = goToCart(root: root)

            let skuData = SKU(
                title: "Ковшик Бытпласт Phibo 1.3 л с декором голубой",
                price: "850 ₽",
                oldPrice: nil
            )
            checkCart(cart: cart, sku: skuData)
        }
    }

    private func compareReasonDetails(lhs: String, rhs: String) {
        func normalizeSpaces(_ string: String) -> String {
            string.replacingOccurrences(of: "\u{00a0}", with: " ")
        }
        XCTAssertEqual(normalizeSpaces(lhs), normalizeSpaces(rhs))
    }

}
