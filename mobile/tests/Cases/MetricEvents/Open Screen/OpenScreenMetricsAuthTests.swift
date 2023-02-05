import MarketUITestMocks
import Metrics
import UIUtils
import XCTest

class OpenScreenMetricsAuthTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testShouldSendMetrics_Profile() throws {
        Allure.addEpic("Продуктовая метрика")
        Allure.addFeature("Открытие экрана")
        Allure.addTitle("Профиль")

        var profile: ProfilePage!

        func goBack() {
            "Идем обратно".run {
                NavigationBarPage.current.backButton.tap()
                wait(forVisibilityOf: profile.element)
            }
        }

        "Открываем профиль".run {
            profile = goToProfile()
            wait(forVisibilityOf: profile.myOrders.element)
        }

        "Открываем заказы".run {
            let page = profile.myOrders.tap()
            wait(forExistanceOf: page.element)
        }

        goBack()

        "Открываем бонусы".run {
            let page = profile.myBonuses.tap()
            wait(forVisibilityOf: page.element)
        }

        goBack()

        "Открывайем избранное".run {
            let page = profile.wishlist.tap()
            wait(forVisibilityOf: page.element)
        }

        goBack()

        "Открывайем справку".run {
            profile.collectionView.swipe(to: .down, untilVisible: profile.help.element)
            let page = profile.help.tap()
            wait(forVisibilityOf: page.element)
        }

        goBack()

        "Открывайем настройки".run {
            profile.collectionView.swipe(to: .down, untilVisible: profile.settings.element)
            let settingsPage = profile.settings.tap()
            wait(forVisibilityOf: settingsPage.element)
            let regionSelectPage = settingsPage.regionValue.tap()
            wait(forVisibilityOf: regionSelectPage.element)
        }

        try "Чекаем метрики".run {
            try checkOpenScreenEvent(name: "HOME")
            try checkOpenScreenEvent(name: "PROFILE")
            try checkOpenScreenEvent(name: "ALL_ORDERS")
            try checkOpenScreenEvent(name: "SMART_COINS")
            try checkOpenScreenEvent(name: "WISHLIST")
            try checkOpenScreenEvent(name: "SETTINGS")
            try checkOpenScreenEvent(name: "REGION_CHOOSE")
            let feedbackEvent = try checkOpenScreenEvent(name: "WEB_VIEW")
            XCTAssertEqual(
                try XCTUnwrap(feedbackEvent.parameters["url"] as? String),
                "https://yandex.ru/support/market/index.html"
            )
        }
    }

    // MARK: - Private methods

    @discardableResult
    private func checkOpenScreenEvent(name: String) throws -> MetricRecorderEvent {
        try XCTUnwrap(
            MetricRecorder.events(from: .appmetrica).with(name: "OPEN-PAGE_VISIBLE")
                .with(params: ["pageName": name]).first
        )
    }
}
