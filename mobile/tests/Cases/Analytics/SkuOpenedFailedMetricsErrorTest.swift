import Metrics
import XCTest

final class SkuOpenedFailedMetricsErrorTest: LocalMockTestCase {
    func testThatSmthGoesWrongMetricsSent() {
        Allure.addEpic("Метрики здоровья")
        Allure.addTitle("Проверяем отправку события \"SKU_OPEN_FAILED\" при ошибке открытия карточки модели")

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MetricsSet_SkuOpenedFailed")
        }

        "Открываем SKU".ybm_run { _ in
            goToDefaultSKUPage()
        }

        "Проверяем, что метрика отправилась".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                MetricRecorder.events(from: .health).with(name: "SKU_OPEN_FAILED").isNotEmpty
            })
        }
    }
}
