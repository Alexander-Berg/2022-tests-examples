import MarketUITestMocks
import XCTest

final class PhramaPriceFromTest: LocalMockTestCase {

    func testFeedSnippetWithPharmaOfferHasPriceFrom() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5435")
        Allure.addEpic("Выдача")
        Allure.addFeature("Покупка списком медицинских товаров")
        Allure
            .addTitle(
                "Проверяем что на сниппете выдачи на медицинском товаре проставляется ценник в формате 'от'"
            )

        var feedPage: FeedPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.purchaseByListMedicineFeature,
                FeatureNames.purchaseByListMedicineForceOnFeature
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupFeedState()
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "Парацетамол")
        }

        "Проверяем отображение ценников от".ybm_run { _ in
            let firstSnippetPage = feedPage.collectionView.cellPage(at: 0)
            let priceLabel = firstSnippetPage.currentPrice
            wait(forVisibilityOf: priceLabel)
            XCTAssertTrue(
                priceLabel.label.contains("от"),
                "На сниппете выдачи на медицинском товаре не проставляется ценник в формате 'от'"
            )
        }
    }

    func testPharmaSKUCardHasPriceFrom() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5435")
        Allure.addEpic("КМ")
        Allure.addFeature("Покупка списком медицинских товаров")
        Allure
            .addTitle(
                "Проверяем что на КТ медицины проставляется ценник в формате 'от' на цене и в саммари"
            )

        var skuPage: SKUPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.purchaseByListMedicineFeature,
                FeatureNames.purchaseByListMedicineForceOnFeature
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupSkuInfoState()
        }

        "Открываем SKU".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Проверяем отображение ценника от в цене в байбоксе".ybm_run { _ in
            skuPage.element.ybm_swipe(toFullyReveal: skuPage.price.price)
            let price = skuPage.price.price
            wait(forVisibilityOf: price)
            XCTAssertTrue(
                price.label.contains("от"),
                "на КТ медицины не проставляется ценник в формате 'от' в цене"
            )
        }

        "Проверяем отображения ценника от в цене в compactOfferView".ybm_run { _ in
            let price = CompactOfferViewPage.current.priceLabel
            wait(forExistanceOf: price)
            XCTAssertTrue(
                price.label.contains("от"),
                "на КТ медицины не проставляется ценник в формате 'от' в саммари"
            )
        }
    }

    func testCartRedesignedSnippetWithPharmaOfferHasPriceFrom() {
        testCartSnippetWithPharmaOfferHasPriceFrom(redesigned: true)
    }

    func testCartSnippetWithPharmaOfferHasPriceFrom() {
        testCartSnippetWithPharmaOfferHasPriceFrom(redesigned: false)
    }

    func testWishListSnippetWithPharmaOfferHasPriceFrom() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5435")
        Allure.addEpic("Избранное")
        Allure.addFeature("Покупка списком медицинских товаров")
        Allure
            .addTitle(
                "Проверяем что на сниппете экрана избранного на медицинском товаре проставляется ценник в формате 'от'"
            )

        var profilePage: ProfilePage!
        var wishListPage: WishlistPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.purchaseByListMedicineFeature,
                FeatureNames.purchaseByListMedicineForceOnFeature
            )
        }

        "Мокаем состояние".ybm_run { _ in
            setupSkuInfoState()
            setupWishListState()
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profilePage = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profilePage.wishlist.element)

            wishListPage = profilePage.wishlist.tap()
            wait(forExistanceOf: wishListPage.collectionView)
        }

        "Проверяем отображение ценников от".ybm_run { _ in
            let item = wishListPage.wishlistItem(at: 0)
            wait(forVisibilityOf: item.element)
            XCTAssertTrue(
                item.currentPrice.label.contains("от"),
                "На сниппете экрана избранного на медицинском товаре не проставляется ценник в формате 'от'"
            )
        }
    }
}

// MARK: - Private

private extension PhramaPriceFromTest {

    private func testCartSnippetWithPharmaOfferHasPriceFrom(redesigned: Bool) {
        let messages: (title: String, errorMessage: String) = redesigned
            ? (
                "Проверяем что на сниппете корзины с редизайном на медицинском товаре проставляется ценник в формате 'от' на цене",
                "На сниппете корзины с редизайном на медицинском товаре не проставляется ценник в формате 'от' на цене"
            )
            : (
                "Проверяем что на сниппете корзины без редизайна на медицинском товаре проставляется ценник в формате 'от' на цене",
                "На сниппете корзины без редизайна на медицинском товаре не проставляется ценник в формате 'от' на цене"
            )

        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5435")
        Allure.addEpic("Корзина")
        Allure.addFeature("Покупка списком медицинских товаров")
        Allure.addTitle(messages.title)

        var cartPage: CartPage!

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.purchaseByListMedicineFeature,
                FeatureNames.purchaseByListMedicineForceOnFeature
            )
            if redesigned {
                enable(toggles: FeatureNames.cartRedesign)
            }
        }

        "Мокаем состояние".ybm_run { _ in
            setupCartState()
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
        }

        "Проверяем отображение ценников от".ybm_run { _ in
            let item = cartPage.cartItem(at: 0)
            wait(forVisibilityOf: item.element)
            XCTAssertTrue(
                item.price.label.contains("от"),
                messages.errorMessage
            )
        }
    }
}

// MARK: - Test state setup methods

private extension PhramaPriceFromTest {
    func setupFeedState() {
        var feedState = FeedState()
        feedState.setSearchOrUrlTransformState(
            mapper: .init(offers: [.pharma])
        )
        feedState.setSearchStateFAPI(
            mapper: .init(offers: [.pharma])
        )
        stateManager?.setState(newState: feedState)
    }

    func setupSkuInfoState() {
        var skuState = SKUInfoState()
        let offer = modify(FAPIOffer.default) {
            $0.specs = .medicine
        }

        let model = modify(FAPIModel.default) {
            $0.specs = .medicine
        }

        let mapper = ResolveSKUInfoResolveProductOffersWithHyperId.Mapper(
            results: .default, collections: modify(.default) {
                $0.offer = [offer]
                $0.product = [model]
            }
        )

        skuState.setSkuInfoState(offer: offer, model: model)
        skuState.setSkuInfoProductOffersWithHyperIdState(with: mapper)

        stateManager?.setState(newState: skuState)
    }

    func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: [.pharma])
        stateManager?.setState(newState: cartState)
    }

    func setupWishListState() {
        var wishListState = WishlistState()
        wishListState.setWishlistItems(items: [.default])
        stateManager?.setState(newState: wishListState)
    }
}
