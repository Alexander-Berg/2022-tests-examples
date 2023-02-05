import MarketUITestMocks
import XCTest

final class FeedSuperHypeGoodsTests: LocalMockTestCase {
    func testListSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5928")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5933")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5934")
        Allure.addEpic("Выдача")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Листовой сниппет. Добавление в избранное. Добавление в Сравнение.")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupSuperHypeGoods(count: 1, isGrid: false)
            setupWishlistState()
            setupAddToComparisonState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем наличие товара \"Громкие новинки\" и элементы на этом снипете. Проверяем дробавление в Избранное"
            .ybm_run { _ in
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                XCTAssertTrue(snippetPage.wishListButton.isVisible)
                XCTAssertTrue(snippetPage.comparsionButton.isVisible)
                XCTAssertTrue(snippetPage.titleLabel.isVisible)
                XCTAssertTrue(snippetPage.imageView.isVisible)
                XCTAssertEqual(snippetPage.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage.addToCartButton.element.isVisible)

                snippetPage.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPage.wishListButton.isSelected })

                snippetPage.comparsionButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPage.comparsionButton.isSelected })
            }

        "Проверяем сниппеты товаров в продаже, идущих после сниппета новинки".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 2, section: 7))
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            let snipetWidth = snippetPage.element.frame.width
            let collectionViewWidth = feedPage.collectionView.element.frame.width
            XCTAssertEqual(snipetWidth, collectionViewWidth)
        }
    }

    func testGridSnippetAddToWishlist() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5929")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5932")
        Allure.addEpic("Выдача")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Гридовый сниппет. Добавление в избранное")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupSuperHypeGoods(count: 1, isGrid: true)
            setupWishlistState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем наличие товара \"Громкие новинки\" и элементы на этом снипете. Проверяем дробавление в Избранное"
            .ybm_run { _ in
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                XCTAssertTrue(snippetPage.wishListButton.isVisible)
                XCTAssertTrue(snippetPage.titleLabel.isVisible)
                XCTAssertTrue(snippetPage.imageView.isVisible)
                XCTAssertEqual(snippetPage.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage.addToCartButton.element.isVisible)

                snippetPage.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPage.wishListButton.isSelected })
            }

        "Проверяем сниппеты товаров в продаже, идущих после сниппета новинки".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 1, section: 7))
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            let snipetWidth = snippetPage.element.frame.width
            let collectionViewWidth = feedPage.collectionView.element.frame.width
            XCTAssertEqual(snipetWidth, collectionViewWidth / 2)
        }
    }

    func testManyGridSnippets() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5937")
        Allure.addEpic("Выдача")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Гридовый сниппет. Три товара \"Громкие новинки\".")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupSuperHypeGoods(count: 3, isGrid: true)
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем наличие товара \"Громкие новинки\" и элементы на этом снипете. Проверяем дробавление в Избранное"
            .ybm_run { _ in
                let snippetPage1 = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                XCTAssertEqual(snippetPage1.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage1.addToCartButton.element.isVisible)

                let snippetPage2 = feedPage.collectionView.cellPage(at: IndexPath(item: 1, section: 7))
                XCTAssertEqual(snippetPage2.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage2.addToCartButton.element.isVisible)

                let snippetPage3 = feedPage.collectionView.cellPage(at: IndexPath(item: 3, section: 7))
                XCTAssertEqual(snippetPage3.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage3.addToCartButton.element.isVisible)
            }

        "Проверяем сниппет товара в продаже, идущего после сниппетов новинки".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 4, section: 7))
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            XCTAssertTrue(snippetPage.currentPrice.isVisible)
            XCTAssertEqual(snippetPage.element.frame.minX, feedPage.collectionView.element.frame.midX)
            XCTAssertEqual(snippetPage.element.frame.width, feedPage.collectionView.element.frame.width / 2)
        }
    }

    func testManyListSnippets() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5936")
        Allure.addEpic("Выдача")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Листовой сниппет. Три товара \"Громкие новинки\".")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupSuperHypeGoods(count: 3, isGrid: false)
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем наличие товара \"Громкие новинки\" и элементы на этом снипете. Проверяем дробавление в Избранное"
            .ybm_run { _ in
                let snippetPage1 = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                XCTAssertEqual(snippetPage1.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage1.addToCartButton.element.isVisible)

                let snippetPage2 = feedPage.collectionView.cellPage(at: IndexPath(item: 2, section: 7))
                XCTAssertEqual(snippetPage2.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage2.addToCartButton.element.isVisible)

                let snippetPage3 = feedPage.collectionView.cellPage(at: IndexPath(item: 4, section: 7))
                XCTAssertEqual(snippetPage3.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage3.addToCartButton.element.isVisible)
            }

        "Проверяем сниппет товара в продаже, идущего после сниппетов новинки".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 6, section: 7))
            feedPage.collectionView.element.swipeUp()
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            XCTAssertTrue(snippetPage.addToCartButton.element.isVisible)
            XCTAssertTrue(snippetPage.currentPrice.isVisible)
            XCTAssertEqual(snippetPage.element.frame.width, feedPage.collectionView.element.frame.width)
        }
    }

    func testFashionSnippets() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6024")
        Allure.addEpic("Выдача")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Cниппет фейшена")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "платье"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupSuperHypeGoods(count: 1, isGrid: true, isVisual: true)
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Проверяем наличие товара \"Громкие новинки\" и элементы на этом снипете"
            .ybm_run { _ in
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                XCTAssertTrue(snippetPage.wishListButton.isVisible)
                XCTAssertTrue(snippetPage.titleLabel.isVisible)
                XCTAssertTrue(snippetPage.imageView.isVisible)
                XCTAssertEqual(snippetPage.superHypeView.label, "Скоро в продаже")
                XCTAssertFalse(snippetPage.addToCartButton.element.isVisible)
            }

        "Проверяем сниппеты товаров в продаже, идущих после сниппета новинки".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 1, section: 7))
            XCTAssertFalse(snippetPage.addToCartButton.element.isVisible)
            XCTAssertEqual(snippetPage.element.frame.width, feedPage.collectionView.element.frame.width / 2)
            XCTAssertEqual(snippetPage.element.frame.minX, feedPage.collectionView.element.frame.midX)
        }
    }

    func testSKUPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5935")
        Allure.addEpic("Избранное")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("КМ")

        enable(toggles: FeatureNames.superHypeGoods)

        let searchText = "iphone"

        var feedPage: FeedPage!
        var skuPage: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            setupSuperHypeGoods(count: 1, isGrid: false)

            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .superHypeGood)
            stateManager?.setState(newState: skuState)

            setupWishlistState()
            setupAddToComparisonState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Открываем КМ товара \"Громкие новинки\"".ybm_run { _ in
            let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }

        "Проверяем содержимое КМ".ybm_run { _ in
            XCTAssertFalse(skuPage.addToCartButton.element.isVisible)
            XCTAssertTrue(skuPage.title.isVisible)
            XCTAssertTrue(skuPage.gallery.element.isVisible)
            XCTAssertTrue(skuPage.navigationBar.wishlistButton.isVisible)
            XCTAssertTrue(skuPage.navigationBar.comparisonButton.isVisible)
            XCTAssertTrue(skuPage.navigationBar.shareButton.isVisible)
            XCTAssertEqual(skuPage.stock.label, "Скоро в продаже")
        }

        "Проверяем дробавление в Избранное. Добавление в Сравнение. Шеринг ссылки.".ybm_run { _ in
            skuPage.navigationBar.wishlistButton.tap()
            ybm_wait(forFulfillmentOf: { skuPage.navigationBar.wishlistButton.isSelected })

            skuPage.navigationBar.comparisonButton.tap()
            ybm_wait(forFulfillmentOf: { skuPage.navigationBar.comparisonButton.isSelected })

            skuPage.navigationBar.shareButton.tap()
            let activityListView = ActivityListViewPage.current
            wait(forExistanceOf: activityListView.collectionView)
        }
    }

    func testWishlistPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5932")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5933")
        Allure.addEpic("Избранное")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Отображение Громких новинок в Избранном")

        enable(toggles: FeatureNames.superHypeGoods)

        var profile: ProfilePage!
        var wishlist: WishlistPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .superHypeGoodDefault,
                    collections: .superHypeGoodDefault
                )
            )
            stateManager?.setState(newState: skuState)

            setupWishlistState()
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист и проверяем товар \"Громкие новинки\"".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)

            wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)

            let cell = wishlist.wishlistItem(at: 0)
            wait(forExistanceOf: cell.element)
            XCTAssertTrue(cell.wishListButton.isSelected)
            XCTAssertEqual(cell.soldOutView.label, "Нет в продаже")
        }
    }

    func testComparisonPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5934")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Громкие новинки")
        Allure.addTitle("Отображение Громких новинок в Сравнении")

        enable(toggles: FeatureNames.superHypeGoods)

        var comparisonList: ComparisonListPage!
        var comparison: ComparisonPage!

        "Настраиваем стейт".ybm_run { _ in
            setupComparisonState()
        }

        "Перейти на экран сравнений".ybm_run { _ in
            let profile = goToProfile()

            wait(forVisibilityOf: profile.comparison.element)

            comparisonList = profile.comparison.tap()
            wait(forExistanceOf: comparisonList.fistComparisonCell.element)
            XCTAssertEqual(comparisonList.fistComparisonCell.count.label, "1 товар")
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        "Проверяем отображение товара \"Громкие новинки\" в разделе Сравнения".ybm_run { _ in
            let modelCell = comparison.collectionView.modelCell(with: 0)
            XCTAssertFalse(modelCell.price.element.isVisible)
            XCTAssertTrue(modelCell.outOfStockImage.isVisible)
        }
    }
}

// MARK: - Private

private extension FeedSuperHypeGoodsTests {

    func setupSuperHypeGoods(count: Int, isGrid: Bool, isVisual: Bool = false) {
        var skus: [Sku] = []
        for _ in 0 ..< count {
            let sku = isVisual ? Sku.superHypeGoodFashion : Sku.superHypeGoodDefault
            skus.append(sku)
        }

        var searchConfigurations: [ResolveSearch.SearchConfiguration] = [.default]
        if isVisual {
            searchConfigurations = [.visualGrid]
        } else if isGrid {
            searchConfigurations = [.detailedGrid]
        }

        var feedState = FeedState()
        feedState.setSearchOrRedirectState(mapper: .init(
            offers: isVisual ? [.dress] : [.default],
            skus: skus,
            searchConfigurations: searchConfigurations,
            feedType: isGrid ? .grid : .list
        ))
        stateManager?.setState(newState: feedState)
    }

    func setupWishlistState() {
        var wishlistState = WishlistState()
        wishlistState.setAddWishlistItem(with: .superHypeGoodDefault)
        wishlistState.setWishlistItems(items: [.superHypeGoodDefault])
        stateManager?.setState(newState: wishlistState)
    }

    func setupAddToComparisonState() {
        var comparisonState = ComparisonState()
        comparisonState.addItemToComparison()
        stateManager?.setState(newState: comparisonState)
    }

    func setupComparisonState() {
        var comparisonState = ComparisonState()
        comparisonState.setComparisonItems(items: [.init(items: [.superHypeGood], category: .default)])
        comparisonState
            .setComparisonEntities(.init(
                sku: [.superHypeGoodDefault],
                category: .default,
                vendor: [.default],
                product: [.default]
            ))
        stateManager?.setState(newState: comparisonState)
    }
}
