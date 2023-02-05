import XCTest

final class FeedFlashTest: LocalMockTestCase {
    func testFlashTimerInGridView() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3742")
        Allure.addEpic("Выдача")
        Allure.addFeature("Флеш")
        Allure.addTitle("Таймер на grid снипете")

        performTest(isGridView: true)
    }

    func testFlashTimerInListView() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3742")
        Allure.addEpic("Выдача")
        Allure.addFeature("Флеш")
        Allure.addTitle("Таймер на list снипете")

        performTest(isGridView: false)
    }

    private func performTest(isGridView: Bool) {
        let search = "тарелка десертная"

        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_Flash_List")
            if isGridView { mockStateManager?.pushState(bundleName: "FeedSet_Flash_Grid") }
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Проверяем наличие таймера на снипете первого товара".ybm_run { _ in
            let snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)
            wait(forVisibilityOf: snippetPage.flashTimer)
        }
    }
}
