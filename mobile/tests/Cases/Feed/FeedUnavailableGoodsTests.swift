import MarketUITestMocks
import XCTest

final class FeedUnavailableGoodsTests: LocalMockTestCase {

    func testSearchFeedListSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5637")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5654")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Поисковая выдача. Листовой сниппет")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!
        var skuPage: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleDefaultState()
            setupSKUInfoState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар. Проверяем элементы на сниппете"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.wishListButton.isVisible)
                XCTAssertTrue(snippetPage.titleLabel.isVisible)
                XCTAssertTrue(snippetPage.ratingLabel.isVisible)
                XCTAssertTrue(snippetPage.reasonsToBuyRecomendations.element.isVisible)
                XCTAssertFalse(snippetPage.currentPrice.isVisible)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }

        "Открываем КМ".ybm_run { _ in
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }
    }

    func testNoDoublePagination() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5644")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Нет двойной пагинации")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleGridFeedOneProductState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feedPage.collectionView.element
                    .ybm_swipeCollectionView(toFullyReveal: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipeCollectionView(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }

        "Пролистываем раздел \"Товары не в продаже\" до конца и проверяем, сколько товаров отображается"
            .ybm_run { _ in
                let nextSnippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 1, section: 9))
                XCTAssertFalse(nextSnippetPage.element.isVisible)
            }
    }

    func testCategoryCarousel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5650")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Переход в категорию с карусели категорий")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "шоколад"

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!
        var categoriesCollection: FeedPage.CollectionView.CategoriesCollectionView!

        // поиск с редиректом одним запросом
        enable(toggles: FeatureNames.searchPerOneRequest)

        stateManager?.mockingStrategy = .dtoMock

        "Мокаем состояние".run {
            setupGoodsNotForSaleCategoryCarouselState()
        }

        "Открываем выдачу".run {
            feedPage = open(search: searchText)
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }

        "Пролистываем до карусели категорий".ybm_run { _ in
            feedPage.element.swipe(to: .up, untilVisible: feedPage.collectionView.categoriesCollectionView.element)
        }

        "Проверяем, что состав карусели категорий не изменился".ybm_run { _ in
            categoriesCollection = feedPage.collectionView.categoriesCollectionView
            for (index, element) in ResolveSearch.Intent.chocolates.enumerated() {
                let categoryPage = categoriesCollection.cellPage(at: IndexPath(item: index, section: 0))
                categoriesCollection.collectionView.swipe(
                    to: .right,
                    until: categoryPage.element.isVisible
                )
                XCTAssertEqual(categoryPage.titleLabel.label, element.name)
            }
        }

        "Переходим на уточненную категорию".run {
            let categoryPage = categoriesCollection.cellPage(at: IndexPath(item: 0, section: 0))
            categoriesCollection.collectionView.swipe(
                to: .left,
                until: categoryPage.element.isVisible
            )
            categoryPage.element.tap()
            ybm_wait(forVisibilityOf: [feedPage.element])
        }

        "Снова пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }
    }

    func testWishlistGridSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5648")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Листовой сниппет. Добавление в вишлист товара не в продаже")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleGridFeedOneProductState()
            setupWishlistState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPageNotForSale = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                let snippetPageForSale = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPageNotForSale.element)
                XCTAssertTrue(snippetPageNotForSale.wishListButton.isVisible)
                snippetPageNotForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPageNotForSale.wishListButton.isSelected })
                feedPage.collectionView.element.ybm_swipeCollectionView(
                    to: .up,
                    toFullyReveal: snippetPageForSale.element
                )
                snippetPageForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPageForSale.wishListButton.isSelected })
                snippetPageForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { !snippetPageForSale.wishListButton.isSelected })
                feedPage.collectionView.element.swipe(to: .down, untilVisible: snippetPageNotForSale.element)
                XCTAssertTrue(snippetPageNotForSale.wishListButton.isSelected)
            }
    }

    func testWishlistListSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5648")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Листовой сниппет. Добавление в вишлист товара не в продаже")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleDefaultState()
            setupWishlistState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPageNotForSale = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                let snippetPageForSale = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 7))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPageNotForSale.element)
                XCTAssertTrue(snippetPageNotForSale.wishListButton.isVisible)
                snippetPageNotForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPageNotForSale.wishListButton.isSelected })
                snippetPageForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { snippetPageForSale.wishListButton.isSelected })
                snippetPageForSale.wishListButton.tap()
                ybm_wait(forFulfillmentOf: { !snippetPageForSale.wishListButton.isSelected })
                XCTAssertTrue(snippetPageNotForSale.wishListButton.isSelected)
            }
    }

    func testCategoryFeed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5638")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Категорийная выдача")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        var catalog: CatalogPage!
        var subcategory: SubcategoryPage!
        var feed: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupCatalogCMSState()
        }

        "Открываем каталог".ybm_run { _ in
            catalog = goToCatalog()
        }

        "Переход в подкатегорию".ybm_run { _ in
            let departmentPage = goToDepartament(fromCatalog: catalog)
            subcategory = goToSubcategory(fromDepartment: departmentPage)
        }

        "Мокаем состояние выдачи".ybm_run { _ in
            var navigationTreeState = NavigationTreeState()
            navigationTreeState.setNavigationTreeState(.laptops)
            stateManager?.setState(newState: navigationTreeState)

            setupGoodsNotForSaleDefaultState()
        }

        "Переход в фид".ybm_run { _ in
            let cell = subcategory.subcategoryTreeCell(index: 2)
            feed = cell.goToFeed()
            ybm_wait(forFulfillmentOf: { feed.element.isVisible })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар"
            .ybm_run { _ in
                feed.element.swipe(to: .down, untilVisible: feed.collectionView.categoryInfo.element)
                XCTAssertEqual(feed.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPage = feed.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feed.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }
    }

    func testFashionGridSnippet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5640")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5655")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5639")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Гридовая выдача фешн с подкатегорими")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)
        enable(toggles: FeatureNames.searchSnippetCMSConfig)

        let searchText = "платье"

        var feedPage: FeedPage!
        var snippetPage: FeedPage.CollectionView.CellPage!
        var skuPage: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleFashionState()
            setupSKUInfoState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что отображается хотя бы один товар. Проверяем элементы на сниппете"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertTrue(feedPage.collectionView.categorySeparator.isVisible)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.wishListButton.isVisible)
                XCTAssertTrue(snippetPage.titleLabel.isVisible)
                XCTAssertTrue(snippetPage.ratingLabel.isVisible)
                XCTAssertFalse(snippetPage.currentPrice.isVisible)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }

        "Открываем КМ".ybm_run { _ in
            skuPage = snippetPage.tap()
            wait(forExistanceOf: skuPage.element)
        }
    }

    func testGridFeedOneProduct() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5641")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Гридовая выдача. Один товар не в продаже")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)
        enable(toggles: FeatureNames.searchSnippetCMSConfig)

        let searchText = "платье"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleGridFeedOneProductState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что товар отображается слева и занимает только половину экрана"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertTrue(feedPage.collectionView.categorySeparator.isVisible)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipeCollectionView(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
                let snipetWidth = snippetPage.element.frame.width
                let collectionViewWidth = feedPage.collectionView.element.frame.width
                XCTAssertLessThanOrEqual(snipetWidth, collectionViewWidth / 2)
                XCTAssertEqual(snippetPage.element.frame.minX, 0)
            }
    }

    func testFilter() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5642")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Выставление фильтра")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var expressCell: FeedPage.CollectionView.HeaderFeed.QuickFilterButton!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleDefaultState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Скроллим `Быстрые фильтры` до позиции и выбираем фильтр `Доставка за 2 часа`".run {
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
            expressCell = quickFilterView.filter(at: 1)
            quickFilterView.collectionView.swipe(to: .left, until: expressCell.element.isVisible)
            XCTAssertEqual(expressCell.element.staticTexts.firstMatch.label, "Доставка за 2 часа")
        }

        "Нажимаем на фильтр `Доставка за 2 часа`".run {
            expressCell.element.tap()
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что товар отображается слева"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }
    }

    func testSorting() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5641")
        Allure.addEpic("Выдача")
        Allure.addFeature("Товары не в продаже")
        Allure.addTitle("Выставление сортировки")

        enable(toggles: FeatureNames.outOfStockSearch)
        enable(toggles: FeatureNames.outStockReturnDate)

        let searchText = "iphone"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupGoodsNotForSaleDefaultState()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Выбираем сортировку".run {
            feedPage.collectionView.headerFeedFilter.sortButton.tap()
            let dicountsFirst = app.buttons.element(withLabelMatching: "по рейтингу")
            dicountsFirst.tap()
            XCTAssertEqual(feedPage.collectionView.headerFeedFilter.sortButton.label, "По рейтингу")
        }

        "Пролистываем до раздела \"Товары не в продаже\" и проверяем, что товар отображается слева"
            .ybm_run { _ in
                feedPage.element.swipe(to: .down, untilVisible: feedPage.collectionView.categoryInfo.element)
                XCTAssertEqual(feedPage.collectionView.categoryInfo.name.label, "Товары не в продаже")
                let snippetPage = feedPage.collectionView.cellPage(at: IndexPath(item: 0, section: 9))
                feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
                XCTAssertTrue(snippetPage.soldOutView.isVisible)
            }
    }
}

// MARK: - Private

private extension FeedUnavailableGoodsTests {

    enum Constants {
        static let tinkoffInstallments = "TINKOFF_INSTALLMENTS"
        static let fapiOffer = modify(FAPIOffer.default) {
            $0.installmentsInfo = .default
            $0.financialProductPriority = [tinkoffInstallments]
        }
    }

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(offer: Constants.fapiOffer)
        stateManager?.setState(newState: skuState)
    }

    func setupGoodsNotForSaleCategoryCarouselState() {
        var feedState = FeedState()
        feedState.setOutOfStockReturnDate()
        // мок ответа на первый запрос без редиректа в категорию
        feedState.setSearchOrUrlTransformState(
            mapper: .init(
                offers: FAPIOffer.all,
                intent: ResolveSearch.Intent.chocolates
            ),
            redirect: nil
        )
        // мок ответа на запрос поиска после уточнения категории
        feedState.setSearchStateFAPI(mapper: .init(offers: FAPIOffer.all))
        stateManager?.setState(newState: feedState)

        var goodsNotForSaleState = FeedState()
        goodsNotForSaleState.setSearchStateFAPI(mapper: .init(
            products: [
                FAPIModel.notForSaleHighRating,
                FAPIModel.notForSaleDefault
            ]
        ))
        stateManager?.setState(
            newState: goodsNotForSaleState,
            matchedBy: hasStringInBody(#""cpa":"any""#) && hasStringInBody(#""onstock":0"#)
                && hasStringInBody(#""use_default_offers":0"#) && hasStringInBody(#""cpa-out-of-stock-models":1"#)
        )
    }

    func setupWishlistState() {
        var wishlistForSaleState = WishlistState()
        wishlistForSaleState.setAddWishlistItem(with: .default)
        wishlistForSaleState.setDeleteWishlistItem(.default, with: 1)
        wishlistForSaleState.setWishlistItems(items: [.goodNotForSale])
        stateManager?.setState(newState: wishlistForSaleState)

        var wishlistNotForSaleState = WishlistState()
        wishlistNotForSaleState.setAddWishlistItem(with: .goodNotForSale)
        stateManager?.setState(
            newState: wishlistNotForSaleState,
            matchedBy: hasStringInBody(#""referenceId":"995338""#)
        )
    }

    func setupGoodsNotForSaleGridFeedOneProductState() {
        var feedState = FeedState()
        feedState.setOutOfStockReturnDate()
        feedState.setSearchOrRedirectState(mapper: .init(
            offers: FAPIOffer.all,
            intent: ResolveSearch.Intent.chocolates,
            searchConfigurations: [.detailedGrid],
            feedType: .grid
        ))
        stateManager?.setState(newState: feedState)

        var goodsNotForSaleState = FeedState()
        goodsNotForSaleState.setSearchStateFAPI(mapper: .init(
            products: [
                FAPIModel.notForSaleDefault
            ],
            searchConfigurations: [.detailedGrid],
            feedType: .grid
        ))
        stateManager?.setState(
            newState: goodsNotForSaleState,
            matchedBy: hasStringInBody(#""cpa":"any""#) && hasStringInBody(#""onstock":0"#)
                && hasStringInBody(#""use_default_offers":0"#) && hasStringInBody(#""cpa-out-of-stock-models":1"#)
        )
    }

    func setupGoodsNotForSaleFashionState() {
        var feedState = FeedState()
        feedState.setOutOfStockReturnDate()
        feedState.setSearchOrRedirectState(mapper: .init(
            offers: [
                FAPIOffer.dress, FAPIOffer.dress,
                FAPIOffer.dress, FAPIOffer.dress
            ],
            intent: ResolveSearch.Intent.chocolates,
            searchConfigurations: [.visualGrid]
        ))
        stateManager?.setState(newState: feedState)

        var goodsNotForSaleState = FeedState()
        goodsNotForSaleState.setSearchStateFAPI(mapper: .init(
            products: [
                FAPIModel.notForSaleFashion,
                FAPIModel.notForSaleFashion,
                FAPIModel.notForSaleFashion,
                FAPIModel.notForSaleFashion
            ],
            searchConfigurations: [.visualGrid]
        ))
        stateManager?.setState(
            newState: goodsNotForSaleState,
            matchedBy: hasStringInBody(#""cpa":"any""#) && hasStringInBody(#""onstock":0"#)
                && hasStringInBody(#""use_default_offers":0"#) && hasStringInBody(#""cpa-out-of-stock-models":1"#)
        )
    }

    func setupGoodsNotForSaleDefaultState() {
        var feedState = FeedState()
        feedState.setOutOfStockReturnDate()
        feedState.setSearchOrRedirectState(mapper: .init(
            offers: [
                FAPIOffer.defaultTemplate
            ],
            intent: ResolveSearch.Intent.chocolates
        ))
        feedState.setOutOfStockReturnDate()
        stateManager?.setState(newState: feedState)

        var goodsNotForSaleState = FeedState()
        goodsNotForSaleState.setSearchStateFAPI(mapper: .init(
            products: [
                FAPIModel.notForSaleHighRating,
                FAPIModel.notForSaleDefault,
                FAPIModel.notForSaleDefault,
                FAPIModel.notForSaleDefault
            ]
        ))

        stateManager?.setState(
            newState: goodsNotForSaleState,
            matchedBy: hasStringInBody(#""cpa":"any""#) && hasStringInBody(#""onstock":0"#)
                && hasStringInBody(#""use_default_offers":0"#) && hasStringInBody(#""cpa-out-of-stock-models":1"#)
        )
    }

    func setupCatalogCMSState() {
        stateManager?.mockingStrategy = .dtoMock

        var cmsState = CMSState()
        cmsState.setCMSState(with: CMSState.CMSCollections.rootCatalogCollections)
        stateManager?.setState(newState: cmsState)

        var electronicsDepartmentState = CMSState()
        electronicsDepartmentState.setCMSState(with: CMSState.CMSCollections.electronicsDepartamentCollections)
        stateManager?.setState(
            newState: electronicsDepartmentState,
            matchedBy: hasStringInBody(#""type":"mp_department_app""#)
        )

        var laptopAndAccessoriesCategoryState = CMSState()
        laptopAndAccessoriesCategoryState
            .setCMSState(with: CMSState.CMSCollections.laptopAndAccessoriesCategoryCollections)
        stateManager?.setState(
            newState: laptopAndAccessoriesCategoryState,
            matchedBy: hasStringInBody(#""type":"mp_navigation_node_app""#)
        )

        var navigationImagesState = CMSState()
        navigationImagesState.setCMSState(with: CMSState.CMSCollections.navigationImagesCollections)
        stateManager?.setState(
            newState: navigationImagesState,
            matchedBy: hasStringInBody(#""type":"mp_navigation_node_images""#)
        )
    }
}
