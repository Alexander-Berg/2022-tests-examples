import Metrics
import XCTest

final class MordaNotLoadedTest: LocalMockTestCase {

    func testMordaNotLoaded() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3806")
        Allure.addEpic("Морда")
        Allure.addFeature("Состояние ошибки")
        Allure.addTitle("Отправлять ошибку при не отображении морды")

        var morda: MordaPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_Empty")
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем что отображается состояние ошибки".ybm_run { _ in
            let errorPage = morda.errorPage

            wait(forVisibilityOf: errorPage.element)

            XCTAssertEqual(
                errorPage.title.label,
                "Что-то пошло не так"
            )

            XCTAssertEqual(
                errorPage.refreshButton.element.label,
                "Повторить"
            )

            XCTAssertFalse(errorPage.goOnMainButton.isVisible)
        }

        "Проверяем что отправлено Health событие".ybm_run { _ in
            let requestMetrics = MetricRecorder.events(from: .health).with(name: "CMS_NOT_LOADED")
            XCTAssertEqual(requestMetrics.count, 1)
        }
    }
}
