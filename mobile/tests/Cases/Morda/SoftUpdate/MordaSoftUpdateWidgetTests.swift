final class MordaSoftUpdateWidgetTests: LocalMockTestCase {

    func testSoftUpdateVisibleForGuestWithOutdatedVersion() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3294")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет SoftUpdate")
        Allure.addTitle("Виджет отображается для гостя с устаревшей версией")

        var morda: MordaPage!

        "Включаем feature toggle softUpdate".ybm_run { _ in
            enable(toggles: FeatureNames.softUpdateWidget)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_SoftUpdateOutdatedVersion")
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda()
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests.contains { $0.contains("lookup") } == true
            })

            wait(forVisibilityOf: morda.singleActionContainerWidget.container.softUpdateWidget.element)
        }
    }

}
