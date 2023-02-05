import MarketUITestMocks

final class AdvertisingCampaignWidgetTests: LocalMockTestCase {

    func testWidgetGuest() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4319")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет \"Дарим 500 баллов Плюса\". Незалогин")
        Allure.addTitle("Проверка содержимого виджета")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_AdvertisingCampaignWidget")

            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
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
            ybm_wait(forFulfillmentOf: {
                snippet.titleLabel.label == "Дарим 500 баллов Плюса"
                    && snippet.subtitleLabel.label == "За первый заказ от 3 500 ₽"
                    && snippet.actionButton.element.label == "Войти"
                    && !snippet.iconImageView.exists
            })
        }
    }

    func testGuestLoginButtonClick() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4321")
        Allure.addEpic("Морда")
        Allure.addFeature("Виджет \"Дарим 500 баллов Плюса\". Незалогин")
        Allure.addTitle("Нажатие на кнопку \"Войти\"")

        var morda: MordaPage!
        var snippet: HoveringSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "MordaSet_AdvertisingCampaignWidget")

            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Авторизуемся, открываем морду".ybm_run { _ in
            morda = goToMorda()
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

        "Ожидаем открытия АМ".ybm_run { _ in
            let yandexLogin = YandexLoginPage(element: app.webViews.firstMatch)
            wait(forVisibilityOf: yandexLogin.element)
        }
    }
}
