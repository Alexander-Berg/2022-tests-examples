import MarketUITestMocks
import XCTest

class FeedServicesTest: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testSearchWithServiceFilter() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4743")
        Allure.addEpic("Выдача")
        Allure.addFeature("Доп. услуги")
        Allure.addTitle("Фильтр и выдача")

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            setupFeedState(isServiceFilterEnabled: false)
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: "iphone")
        }

        "Проверяем выдачу без фильтра".ybm_run { _ in
            let hasNoServicesSnippet = feedPage.collectionView.cellPage(at: 0)
            let hasServicesSnippet = feedPage.collectionView.cellPage(at: 2)

            XCTAssertFalse(hasNoServicesSnippet.triggersLabel.exists)
            XCTAssertEqual(hasServicesSnippet.triggersLabel.label, "Есть установка")
        }

        "Мокаем поиск после применения фильтра".ybm_run { _ in
            setupFeedState(isServiceFilterEnabled: true)
        }

        "Тапаем фильтр \"Есть установка\"".ybm_run { _ in
            let quickFilters = feedPage.collectionView.headerFeedFilter.quickFilterView
            wait(forVisibilityOf: quickFilters.element)
            quickFilters.cellElement(at: .init(item: 0, section: .zero)).tap()
        }

        "Проверяем выдачу c фильтром".ybm_run { _ in
            let firstSnippet = feedPage.collectionView.cellPage(at: 0)
            XCTAssertEqual(firstSnippet.triggersLabel.label, "Есть установка")
        }

    }

}
