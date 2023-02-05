import MarketUITestMocks
import XCTest

final class FeedExpressTest: LocalMockTestCase {

    func testExpressDeliveryFilterPresentedInFeed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4257")
        Allure.addEpic("Выдача")
        Allure.addFeature("Фильтр экспресс-доставки")
        Allure.addTitle("Проверяем работу фильтра экспресс-доставки в выдаче")

        let searchText = "Химические средства"
        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var quickFilterView: FeedPage.CollectionView.HeaderFeed.QuickFiltersCollectionView!
        var saleCell: XCUIElement!

        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTime] = "17:00:00"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_FilterExpress")
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: searchText)
            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Отображается товар".ybm_run { _ in
            snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            feedPage.collectionView.element.swipe(to: .down, until: snippetPage.element.isVisible)
        }

        "Отображаются быстрые фильтры".ybm_run { _ in
            quickFilterView = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilterView.element)
        }

        "Скроллим `Быстрые фильтры` до позиции `Доставка за 2 часа`".ybm_run { _ in
            let indexPath = IndexPath(item: 0, section: 0)
            saleCell = quickFilterView.cellElement(at: indexPath)
            quickFilterView.collectionView.ybm_swipeCollectionView(to: .left, toFullyReveal: saleCell)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_FilterExpress1")
        }

        "Тапаем на фильтр `Доставка за 2 часа`".ybm_run { _ in
            wait(forVisibilityOf: saleCell)
            XCTAssertEqual(saleCell.staticTexts.firstMatch.label, "Доставка за 2 часа")
            saleCell.tap()
        }

        "Фильтр `Доставка за 2 часа` первый в быстрых фильтрах".ybm_run { _ in
            let indexPath = IndexPath(item: 0, section: 0)
            saleCell = quickFilterView.cellElement(at: indexPath)
            wait(forVisibilityOf: saleCell)
            XCTAssertEqual(saleCell.staticTexts.firstMatch.label, "Доставка за 2 часа")
        }

        "Проверяем выдачу с зажатым фильтром `Доставка за 2 часа`".ybm_run { _ in
            snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            feedPage.collectionView.element.swipe(to: .down, until: snippetPage.element.isVisible)

            let deliveryLabel = snippetPage.deliveryLabel
            ybm_wait(forFulfillmentOf: {
                deliveryLabel.isVisible
            })
            let trimmedTargetText = deliveryLabel.label.trimmingCharacters(in: .whitespaces)
            XCTAssert(
                trimmedTargetText.contains("от Яндекса"),
                "В лейбле экспресса \"\(trimmedTargetText)\" нет фразы от Яндекса"
            )
        }
    }

    func testDeliveryTimeOnExpressOffersInFeed() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-6071")
        Allure.addEpic("Выдача")
        Allure.addFeature("Экспресс-товары")
        Allure.addTitle("Проверяем, что у экспресс-офферов на выдаче отображается время экспресс-доставки")

        let searchText = "айфон"
        var feedPage: FeedPage!
        var snippetPage: FeedSnippetPage!
        var feedState = FeedState()

        app.launchEnvironment[TestLaunchEnvironmentKeys.currentTime] = "13:00:00"
        enable(toggles: FeatureNames.expressDeliveryIntervals)

        "Мокаем состояние".ybm_run { _ in
            feedState.setSearchOrUrlTransformState(mapper: .init(offers: [.express], deliveryOverride: nil))
            feedState.setSearchStateFAPI(mapper: .init(offers: [.express], deliveryOverride: nil))
            stateManager?.setState(newState: feedState)
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: searchText)

            ybm_wait(forFulfillmentOf: {
                feedPage.element.isVisible
            })
        }

        "Отображается товар".ybm_run { _ in
            snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            feedPage.collectionView.element.swipe(to: .down, until: snippetPage.element.isVisible)
        }

        "На сниппете есть время экспресс-доставки".ybm_run { _ in
            let deliveryLabel = snippetPage.deliveryLabel
            ybm_wait(forFulfillmentOf: {
                deliveryLabel.isVisible
            })
            let trimmedTargetText = deliveryLabel.label.trimmingCharacters(in: .whitespaces)
            XCTAssertEqual(
                trimmedTargetText,
                "Express, сегодня с 17:15",
                "Лейбл экспресса в сниппете не содержит правильного времени экспресс-доставки"
            )
        }
    }
}
