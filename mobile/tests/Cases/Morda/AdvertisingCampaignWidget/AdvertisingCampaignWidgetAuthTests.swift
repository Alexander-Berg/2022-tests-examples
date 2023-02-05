import MarketUITestMocks

final class AdvertisingCampaignWidgetAuthTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testWidgetAuthorizedUser() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4310")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет \"Дарим 500 баллов Плюса\". Залогин")
        Allure.addTitle("Проверка содержимого виджета")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_AdvertisingCampaignWidget")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests
                    .contains { $0.contains("checkUserWelcomeCashbackOrderEmitAvailable") } == true
            })

            snippet = morda.singleActionContainerWidget.container.advertisingCampaignWidget.snippet
            wait(forVisibilityOf: snippet.element)
        }

        "Проверяем содержимое виджета".ybm_run { _ in
            let snippet = morda.singleActionContainerWidget.container.advertisingCampaignWidget.snippet
            ybm_wait(forFulfillmentOf: {
                snippet.titleLabel.label == "Дарим 500 баллов Плюса"
                    && snippet.subtitleLabel.label == "За первый заказ от 3 500 ₽"
                    && snippet.actionButton.element.label == "Здорово"
                    && !snippet.iconImageView.exists
            })
        }
    }

    func testAuthorizedUserDoneButtonClick() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4312")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет \"Дарим 500 баллов Плюса\". Залогин")
        Allure.addTitle("Нажатие на кнопку \"Здорово\"")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_AdvertisingCampaignWidget")
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            let root = appAfterOnboardingAndPopups()
            morda = goToMorda(root: root)
        }

        "Проверяем наличие виджета".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { [weak self] in
                self?.mockServer?.handledRequests
                    .contains { $0.contains("checkUserWelcomeCashbackOrderEmitAvailable") } == true
            })

            snippet = morda.singleActionContainerWidget.container.advertisingCampaignWidget.snippet
            wait(forVisibilityOf: snippet.element)
        }

        "Нажимаем на кнопку".ybm_run { _ in
            snippet.actionButton.element.tap()
        }

        "Ожидаем скрытия виджета".ybm_run { _ in
            wait(forInvisibilityOf: snippet.element)
        }
    }
}
