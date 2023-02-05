import Metrics
import XCTest

final class SomethingGoesWrongMetricsErrorTest: LocalMockTestCase {

    func testThatSomethingGoesWrongMetricsSent() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку события \"SOMETHING_GOES_WRONG\" при показе заглушки")

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MetricsSet_SmthGoesWrongMorda")
        }

        var errorPage: ErrorPage!

        "Открываем морду".ybm_run { _ in
            let morda = goToMorda()
            errorPage = morda.errorPage
        }

        "Проверяем, что отобразилось \"Что-то пошло не так\"".ybm_run { _ in
            wait(forVisibilityOf: errorPage.element)
            XCTAssertEqual(errorPage.title.label, "Что-то пошло не так")
        }

        "Проверяем, что метрика отправилась".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .health).with(name: "SOMETHING_GOES_WRONG").isNotEmpty
            })
        }
    }
}
