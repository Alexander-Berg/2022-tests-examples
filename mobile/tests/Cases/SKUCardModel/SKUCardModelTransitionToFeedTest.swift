import MarketUITestMocks
import XCTest

final class SKUCardModelTransitionToFeedTest: LocalMockTestCase {

    func testVendor() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-993")
        Allure.addEpic("КМ")
        Allure.addFeature("Переход на выдачу")
        Allure.addTitle("Проверяем переход на выдачу при тапе на производителя под фотографией")

        var sku: SKUPage!
        var feed: FeedPage!

        let vendor = "Apple"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_MoscowYPlusDelivery")
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Мокаем переход на выдачу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Vendor")
        }

        "Под фото товара проверить наличие ссылки Вендор".ybm_run { _ in
            wait(forVisibilityOf: sku.vendorLinkButton.element)
            XCTAssertEqual(sku.vendorLinkButton.element.label, vendor)
        }

        "Нажать на ссылку “вендор”: переход на выдачу".ybm_run { _ in
            feed = sku.vendorLinkButton.tap()
            ybm_wait(forFulfillmentOf: { feed.element.isVisible
                    && feed.navigationBar.searchedTextButton.label == vendor
            })
        }

        "Нажать стрелку назад: вернулись на КМ".ybm_run { _ in
            feed.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }
    }

    func testCategoryCell() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-995")
        Allure.addEpic("КМ")
        Allure.addFeature("Переход на выдачу")
        Allure.addTitle("Проверяем переход на выдачу при тапе \"Все товары из категории ...\"")

        var sku: SKUPage!
        var feed: FeedPage!
        let categoryCellTitle = "Все товары категории «Коляски для кукол»"
        let navBarTitle = "Коляски для кукол"

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .strollers)
            stateManager?.setState(newState: skuState)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Долистываем до блока категорий, проверяем его содержание".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.didFinishLoadingInfo
            })

            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.categoryLink.element,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.categoryLink.element.text, categoryCellTitle)
            feed = sku.categoryLink.tap()
        }

        "Открываем и закрываем выдачу по тапу на ячейку категорий".ybm_run { _ in
            checkTransition(fromFeed: feed, navBarTitle: navBarTitle, toSKU: sku)
        }
    }

    func testVendorCell() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-994")
        Allure.addEpic("КМ")
        Allure.addFeature("Переход на выдачу")
        Allure.addTitle("Проверяем переход на выдачу при тапе на \"Другие товары от производителя\"")

        var sku: SKUPage!
        var feed: FeedPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем для перехода на выдачу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_VendorCellToFeed")
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проскролить страницу до кнопки \"Другие товары ”Вендор\"\"".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.didFinishLoadingInfo
            })

            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.vendorLink.element,
                inset: sku.stickyViewInset
            )

            XCTAssertEqual(sku.vendorLink.element.text, "Все товары Apple")
            feed = sku.vendorLink.tap()
        }

        "Нажать на кнопку \"Все товары \"Вендор\"\": переход на выдачу, фильтр".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { feed.element.isVisible })
        }

        "Возврат на КМ с выдачи".ybm_run { _ in
            feed.navigationBar.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }
    }

    func testLego() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-996")
        Allure.addEpic("КМ")
        Allure.addFeature("Переход на выдачу")
        Allure.addTitle("Проверяем переход на webview при тапе на производителя LEGO")

        var sku: SKUPage!
        var webview: WebViewPage!

        let vendor = "LEGO"

        "Мокаем LEGO".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_LEGO")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .lego)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Под фото товара проверить наличие ссылки Вендор".ybm_run { _ in
            wait(forExistanceOf: sku.vendorLinkButton.element)
            XCTAssertEqual(sku.vendorLinkButton.element.label, vendor)
        }

        "Нажать на ссылку “LEGO”: переход на webview".ybm_run { _ in
            webview = sku.vendorLinkButton.tapLego()
            // проверка на existance из-за долгой загрузки страницы
            wait(forExistanceOf: webview.element)
        }

        "Нажать стрелку назад: вернулись на КМ".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: sku.element)
        }
    }

    private func checkTransition(fromFeed feed: FeedPage, navBarTitle: String, toSKU sku: SKUPage) {
        ybm_wait(forFulfillmentOf: { feed.element.isVisible
                && NavigationBarPage.current.title.label == navBarTitle
        })

        feed.navigationBar.backButton.tap()
        wait(forVisibilityOf: sku.element)
    }
}
