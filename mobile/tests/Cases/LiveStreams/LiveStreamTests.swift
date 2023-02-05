import MarketUITestMocks
import XCTest

final class LiveStreamTest: LocalMockTestCase {

    func testLiveStreamsAnounceLessThanDay() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4107")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4113")
        Allure.addEpic("Морда")
        Allure.addFeature("Трансляции")
        Allure.addTitle("Точка входа. Анонс трансляции менее суток")

        let newStartDate = Date().addingTimeInterval(.hour * 5)
        changeAndPushLiveStreamMock(newStartDate: newStartDate)

        enable(toggles: FeatureNames.liveStreamsRelease)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        checkAnounce(
            widgetTitle: "Скидки в прямом эфире",
            title: "Создаем яркое настроение с умным освещением Philips Hue",
            subtitle: makeFullDateStringValue(withDate: newStartDate)
        )
    }

    func testLiveStreamsAnounceMoreThanDay() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4107")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4113")
        Allure.addEpic("Морда")
        Allure.addFeature("Трансляции")
        Allure.addTitle("Точка входа. Анонс трансляции более суток")

        let newStartDate = Date().addingTimeInterval(.day * 2)
        changeAndPushLiveStreamMock(newStartDate: newStartDate)

        enable(toggles: FeatureNames.liveStreamsRelease)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        checkAnounce(
            widgetTitle: "Скидки в прямом эфире",
            title: "Создаем яркое настроение с умным освещением Philips Hue",
            subtitle: makeFullDateStringValue(withDate: newStartDate)
        )
    }

    // MARK: - Private

    private func checkAnounce(widgetTitle: String, title: String, subtitle: String) {
        var morda: MordaPage!
        var widget: MordaPage.LiveStreamsWidget!

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Скроллим до виджета \"Трансляции\"".ybm_run { _ in
            widget = morda.liveStreamsWidget
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: widget.title)
            XCTAssertEqual(widget.title.label, widgetTitle)
        }

        "Проверяем тайтл в сниппете".ybm_run { _ in
            let snippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
            widget.collectionView.ybm_swipeCollectionView(toFullyReveal: snippet.subtitleLabel)
            XCTAssertTrue(snippet.titleLabel.label.contains(title))
            XCTAssertTrue(snippet.subtitleLabel.label.contains(subtitle))
        }

        var announcementPage: LiveAnnouncementPage!

        "Нажимаем на анонс трансляции".ybm_run { _ in
            let snippet = widget.container.cellPage(at: IndexPath(item: 0, section: 0))
            snippet.element.tap()
            announcementPage = LiveAnnouncementPage.current
            wait(forVisibilityOf: announcementPage.element)
            wait(forVisibilityOf: announcementPage.skuScrollView)
        }

        "Нажимаем на товар в карусели".ybm_run { _ in
            announcementPage.skuScrollView.cells.firstMatch.tap()
            let skuPage = SKUPage.current
            wait(forVisibilityOf: skuPage.element)
            XCTAssertTrue(skuPage.element.isVisible)
            skuPage.navigationBar.backButton.tap()
            wait(forVisibilityOf: announcementPage.element)
            XCTAssertTrue(announcementPage.element.isVisible)
            announcementPage.navigationBar.closeButton.tap()
            wait(forVisibilityOf: morda.element)
            XCTAssertTrue(morda.element.isVisible)
        }
    }

    private func changeAndPushLiveStreamMock(newStartDate date: Date) {
        let oldBundleName = "LiveStream"
        let newBundleName = "LiveStream1"

        let dateFormat = "yyyy-MM-dd'T'HH:mm:ss.sss"
        let startDate = getDateString(withDateFormat: dateFormat, date: date)

        mockStateManager?.changeMock(
            bundleName: oldBundleName,
            newBundleName: newBundleName,
            filename: "POST_api_v1_resolveLiveStreamEntrypointsByStatuses",
            changes: [
                (
                    #""startTime" : "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z""#,
                    "\"startTime\" : \"\(startDate)\""
                )
            ]
        )

        mockStateManager?.changeMock(
            bundleName: oldBundleName,
            newBundleName: newBundleName,
            filename: "POST_api_v1_resolveLiveStreamContent",
            changes: [
                (
                    #""startTime" : "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z""#,
                    "\"startTime\" : \"\(startDate)\""
                )
            ]
        )

        "Мокаем морду с виджетом трансляций".ybm_run { _ in
            mockStateManager?.pushState(bundleName: oldBundleName)
            mockStateManager?.pushState(bundleName: newBundleName)
        }
    }

    private func getDateString(
        withDateFormat dateFormat: String,
        date: Date,
        timeZone: TimeZone? = TimeZone(secondsFromGMT: 0)
    ) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = timeZone
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = dateFormat
        return formatter.string(from: date)
    }

    private func makeFullDateStringValue(withDate startDate: Date) -> String {
        let diffTimeInHours = (startDate.timeIntervalSince1970 - Date().timeIntervalSince1970) / 3_600
        let dayMonth = getDateString(
            withDateFormat: "d MMMM",
            date: startDate,
            timeZone: TimeZone(secondsFromGMT: 3_600 * 3)
        )
        let hourMinutes = getDateString(
            withDateFormat: "HH:mm",
            date: startDate,
            timeZone: TimeZone(secondsFromGMT: 3_600 * 3)
        )
        guard diffTimeInHours < 24 && startDate.ybm_isToday else {
            return "\(dayMonth) в \(hourMinutes)"
        }

        return "Сегодня в \(hourMinutes)"
    }
}
