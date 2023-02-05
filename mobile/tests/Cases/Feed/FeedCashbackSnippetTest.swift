import MarketUITestMocks
import XCTest

final class FeedCashbackSnippetTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCashbackSnippetWithoutYandexPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3879")
        Allure.addEpic("Выдача")
        Allure.addFeature("Кешбэк")
        Allure.addTitle("Бейдж на сниппетах для неплюсовика")

        let search = "теннисный стол"
        var feedPage: FeedPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "FeedSet_CashbackWithoutYandexPlus")
        }

        "Открываем выдачу".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            feedPage = open(search: search)
        }

        "Скроллим к товару и проверяем сниппет".ybm_run { _ in
            let snippetPage = feedPage.collectionView.snippetFirstMatchingCell()
            feedPage.collectionView.element.swipe(to: .down, until: snippetPage.element.isVisible)
            feedPage.collectionView.element.ybm_swipe(toFullyReveal: snippetPage.element)

            XCTAssertTrue(snippetPage.cashback.isVisible)
            XCTAssertEqual(" 600", snippetPage.cashback.label)
        }
    }
}
