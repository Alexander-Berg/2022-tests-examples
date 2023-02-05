import MarketUITestMocks
import XCTest

class RegionTestCase: LocalMockTestCase {

    override func setUp() {
        super.setUp()

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        addUIInterruptionMonitor(withDescription: "Notifications alert") { alert in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }

            return false
        }
    }

    // MARK: - Public

    func testRegionAndNotifications() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3000")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("Отлично - доставка есть")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var settingsPage: SettingsPage!
        var rootPage: RootPage!

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "PushNotifications_On")
        }

        enablePlusAvailability()

        "Открываем приложение".ybm_run { _ in
            rootPage = appWithOnboarding()
            welcomeOnboarding = rootPage.onboarding
        }

        "Подтверждаем гоорд".ybm_run { _ in
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
            welcomeOnboarding.geoActionButton.tap()
        }

        "Переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings(root: rootPage)
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем данные на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionValue.element.label, "Москва")
        }
    }

    func testRegionAndNotificationsWhenNoDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3001")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("Отлично - доставки нет")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var profilePage: ProfilePage!
        var settingsPage: SettingsPage!
        var rootPage: RootPage!

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "NoDeliveryRegion")
        }

        enablePlusAvailability()

        "Открываем приложение".ybm_run { _ in
            rootPage = appWithOnboarding()
            welcomeOnboarding = rootPage.onboarding
        }

        "Подтверждаем город".ybm_run { _ in
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
            welcomeOnboarding.geoActionButton.tap()
        }

        "Выбираем первый ближайший город".ybm_run { _ in
            let regionSelectPage = RegionSelectPage.current
            wait(forVisibilityOf: regionSelectPage.element, timeout: 20)

            mockStateManager?.pushState(bundleName: "RegionDelivery_True")
            regionSelectPage.nearestRegionSuggest.first?.tap() // Ольша
            profilePage = goToProfile(root: rootPage)
        }

        "Переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings(profile: profilePage)
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем данные на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionValue.element.label, "Ольша")
        }
    }

    func testRegionAndNotificationsWhenWrongRegionAnotherRight() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3003")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("А вот и нет - Разрешить - есть доставка- другой город - да, все так.")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var regionConfirmPage: RegionConfirmationPopupPage!
        var rootPage: RootPage!
        var settingsPage: SettingsPage!

        "Настраиваем локацию".ybm_run { _ in
            // широта и долгота Петербурга, но это не сильно важно, тк ответы запросов замоканы, данные берутся оттуда
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "59.95"
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "30.3"
        }

        enablePlusAvailability()

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "PushNotifications_On")
            mockStateManager?.pushState(bundleName: "RegionDelivery_StPetersburg")
        }

        "Открываем приложение".ybm_run { _ in
            rootPage = appWithOnboarding()
            welcomeOnboarding = rootPage.onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
        }

        "Закрываем по крестику".ybm_run { _ in
            welcomeOnboarding.crossButton.tap()

            wait(forVisibilityOf: RegionConfirmationPopupPage.current.element, timeout: 15)
            regionConfirmPage = RegionConfirmationPopupPage.current
        }

        "Проверяем данные на попапе потдтверджения автоопределения".ybm_run { _ in
            let elements = [regionConfirmPage.title, regionConfirmPage.confirmButton, regionConfirmPage.rejectButton]
            ybm_wait(forVisibilityOf: elements)
            XCTAssertEqual(regionConfirmPage.title.label, "Ваш населённый пункт — Санкт-Петербург?")
            XCTAssertEqual(regionConfirmPage.confirmButton.label, "Да, всё так")
            XCTAssertEqual(regionConfirmPage.rejectButton.label, "Нет, другой")
        }

        "Нажимаем `Да, все так`".ybm_run { _ in
            regionConfirmPage.confirmButton.tap()
            wait(forInvisibilityOf: regionConfirmPage.element)
            wait(forVisibilityOf: MordaPage.current.element)
        }

        "Переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings(root: rootPage)
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем данные на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionValue.element.label, "Санкт-Петербург")
        }
    }

    func testRegionAndNotificationsWhenWrongRegionGeoSuggest() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3004")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("А вот и нет - Разрешить - есть доставка - другой город - нет, другой")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var rootPage: RootPage!
        var regionConfirmPage: RegionConfirmationPopupPage!
        var regionSelectPage: RegionSelectPage!
        var settingsPage: SettingsPage!

        "Настраиваем локацию".ybm_run { _ in
            // широта и долгота Москвы, но это не сильно важно, тк ответы запросов замоканы, данные берутся оттуда
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.73"
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.58"
        }

        enablePlusAvailability()

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "PushNotifications_On")
            mockStateManager?.pushState(bundleName: "OnboardingRegion_Tyva")
        }

        "Открываем приложение".ybm_run { _ in
            rootPage = appWithOnboarding()
            welcomeOnboarding = rootPage.onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
        }

        "Закрываем по крестику".ybm_run { _ in
            welcomeOnboarding.crossButton.tap()

            wait(forVisibilityOf: RegionConfirmationPopupPage.current.element, timeout: 15)
            regionConfirmPage = RegionConfirmationPopupPage.current
        }

        "Проверяем данные на попапе подтверждения автоопределения".ybm_run { _ in
            let elements = [regionConfirmPage.title, regionConfirmPage.confirmButton, regionConfirmPage.rejectButton]
            ybm_wait(forVisibilityOf: elements)
            XCTAssertEqual(regionConfirmPage.title.label, "Ваш населённый пункт — Москва?")
            XCTAssertEqual(regionConfirmPage.confirmButton.label, "Да, всё так")
            XCTAssertEqual(regionConfirmPage.rejectButton.label, "Нет, другой")
        }

        "Нажимаем `Нет, другой`".ybm_run { _ in
            regionConfirmPage.rejectButton.tap()
            wait(forVisibilityOf: RegionSelectPage.current.element)
            regionSelectPage = RegionSelectPage.current
        }

        "Проверяем экран выбора региона".ybm_run { _ in
            // В случае если не отобразится клавиатура на экране - тест будет фейлится.
            // Иметь это в виду
            ybm_wait(forVisibilityOf: [regionSelectPage.regionInputClearButton, KeyboardPage.current.element])
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Москва")
            XCTAssertEqual(regionSelectPage.doneChoosingButton.label, "Готово")
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "RegionDelivery_False")
        }

        "Вводим город \"Тыва\"".ybm_run { _ in
            regionSelectPage.regionInput.ybm_clearAndEnterText("Тыва")
            regionSelectPage.doneChoosingButton.tap()
        }

        "Проверяем экран выбора региона с городом Тыва".ybm_run { _ in
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Тыва")
            XCTAssertEqual(regionSelectPage.noDeliveryTitle.text, "В Тыву мы пока не доставляем")
            XCTAssertEqual(
                regionSelectPage.noDeliverySubtitle.text,
                "Вот места поблизости, куда есть доставка"
            )
            XCTAssertFalse(regionSelectPage.nearestRegionSuggest.isEmpty)
            XCTAssertEqual(regionSelectPage.doneChoosingButton.label, "Готово")
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "RegionDelivery_StPetersburg")
        }

        "Вводим город \"Санкт-Петербург\"".ybm_run { _ in
            regionSelectPage.regionInput.ybm_clearAndEnterText("Санкт-Петербург")
            ybm_wait(forFulfillmentOf: { regionSelectPage.geoSuggest.first?.isVisible ?? false })
            regionSelectPage.geoSuggest.first?.tap()
            wait(forVisibilityOf: MordaPage.current.element)
        }

        "Переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings(root: rootPage)
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем данные на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionValue.element.label, "Санкт-Петербург")
        }
    }

    func testRegionSelectWhenNoDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3005")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("А вот и нет - Разрешить - нет доставки")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var regionSelectPage: RegionSelectPage!

        "Настраиваем локацию".ybm_run { _ in
            // широта и долгота Тывы, но это не сильно важно, тк ответы запросов замоканы, данные берутся оттуда
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "51.71"
            app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "94.43"

            // При выключенных анимациях, viewDidAppear срабатывает раньше чем showNoDelivery.
            // То есть, сперва срабатывает textField.becomeFirstResponder(), а затем textField.resignFirstResponder()
            app.launchEnvironment[TestLaunchEnvironmentKeys.animationsDisabled] = "false"
        }

        enablePlusAvailability()

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OnboardingRegion_Tyva")
            mockStateManager?.pushState(bundleName: "RegionDelivery_False")
        }

        "Открываем приложение".ybm_run { _ in
            welcomeOnboarding = appWithOnboarding().onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
        }

        "Нажимаем `В другой город`".ybm_run { _ in
            welcomeOnboarding.geoAdditionalActionButton.tap()

            wait(forVisibilityOf: RegionSelectPage.current.element, timeout: 15)
            regionSelectPage = RegionSelectPage.current
        }

        "Проверяем экран выбора региона".ybm_run { _ in
            ybm_wait(forVisibilityOf: [regionSelectPage.regionInputClearButton, KeyboardPage.current.element])
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Тыва")
            XCTAssertEqual(regionSelectPage.doneChoosingButton.label, "Готово")
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "GeoSuggest_FriendshipVillage")
        }

        "Вводим \"Деревня Дружба\"".ybm_run { _ in
            regionSelectPage.regionInput.ybm_clearAndEnterText("Деревня Дружба")
            ybm_wait(forFulfillmentOf: { regionSelectPage.geoSuggest.isNotEmpty })
            XCTAssertFalse(regionSelectPage.doneChoosingButton.isEnabled)
        }

        "Вводим \"Николаевка\"".ybm_run { _ in
            regionSelectPage.regionInput.ybm_clearAndEnterText("Николаевка")
            ybm_wait(forFulfillmentOf: { !regionSelectPage.doneChoosingButton.isEnabled })
        }
    }

    func testRegionSelectFromSettings() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3065")
        Allure.addEpic("Профиль")
        Allure.addFeature("Регион")
        Allure.addTitle("Из настроек")

        var settingsPage: SettingsPage!
        var regionSelectPage: RegionSelectPage!

        "Открываем приложение и переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings()
            wait(forVisibilityOf: settingsPage.element)
        }

        "Тапаем по полю \"Город\"".ybm_run { _ in
            regionSelectPage = settingsPage.regionValue.tap()
        }

        "Проверяем данные на экране выбора региона".ybm_run { _ in
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Москва")
            XCTAssertEqual(regionSelectPage.doneChoosingButton.label, "Готово")
            XCTAssertTrue(regionSelectPage.regionInputClearButton.isVisible)
        }

        "Нажимаем на крестик в поле ввода".ybm_run { _ in
            regionSelectPage.regionInputClearButton.tap()
            ybm_wait(forFulfillmentOf: { regionSelectPage.regionInput.text.isEmpty })
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "OnboardingRegion_Tyva")
            mockStateManager?.pushState(bundleName: "RegionDelivery_False")
        }

        "Вводим город \"Тыва\"".ybm_run { _ in
            regionSelectPage.regionInput.ybm_clearAndEnterText("Тыва")
            regionSelectPage.doneChoosingButton.tap()
        }

        "Проверяем экран выбора региона с городом Тыва".ybm_run { _ in
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Тыва")
            XCTAssertEqual(regionSelectPage.noDeliveryTitle.text, "В Тыву мы пока не доставляем")
            XCTAssertEqual(regionSelectPage.noDeliverySubtitle.text, "Вот места поблизости, куда есть доставка")
            XCTAssertFalse(regionSelectPage.nearestRegionSuggest.isEmpty)
        }

        "Мокаем состояния".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "RegionDelivery_True")
        }

        "Выбираем первый ближайший город".ybm_run { _ in
            regionSelectPage.nearestRegionSuggest.first?.tap() // Ошлапье
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем город на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionTitle.label, "Город")
            XCTAssertEqual(settingsPage.regionValue.element.label, "Ольша")
        }

        "Тапаем по полю \"Город\"".ybm_run { _ in
            regionSelectPage = settingsPage.regionValue.tap()
        }

        "Закрываем экран выбора региона".ybm_run { _ in
            NavigationBarPage.current.closeButton.tap()
            wait(forVisibilityOf: settingsPage.element)
        }
    }

    func testRegionAndNotificationsWhenWrongRegionSameRight() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3112")
        Allure.addEpic("Онбординг")
        Allure.addFeature("Регион")
        Allure.addTitle("А вот и нет - Разрешить - есть доставка - тот же город - IOS")

        var welcomeOnboarding: WelcomeOnboardingPage!
        var profilePage: ProfilePage!
        var settingsPage: SettingsPage!
        var regionSelectPage: RegionSelectPage!

        let regionDeliverySuspendFile = "POST_api_v1_resolveAvailableDelivery"

        enablePlusAvailability()

        "Нажимаем `А вот и нет`".ybm_run { _ in
            welcomeOnboarding = appWithOnboarding().onboarding
            wait(forVisibilityOf: welcomeOnboarding.geoCell, timeout: 20)
            welcomeOnboarding.geoAdditionalActionButton.tap()

            wait(forVisibilityOf: RegionSelectPage.current.element)
            regionSelectPage = RegionSelectPage.current
        }

        "Проверяем экран выбора региона".ybm_run { _ in
            XCTAssertEqual(regionSelectPage.regionInputPlaceholder.label, "Название населенного пункта")
            XCTAssertEqual(regionSelectPage.regionInput.text, "Москва")
            XCTAssertEqual(regionSelectPage.doneChoosingButton.label, "Готово")
            XCTAssertTrue(regionSelectPage.regionInputClearButton.isVisible)
        }

        "Нажимаем на крестик в поле ввода".ybm_run { _ in
            regionSelectPage.regionInputClearButton.tap()

            ybm_wait(forFulfillmentOf: {
                regionSelectPage.regionInput.text.isEmpty
                    && regionSelectPage.autodetectSuggest.label == "Москва"
            })
        }

        "Выбираем автоопределение".ybm_run { _ in
            mockServer?.addSuspended(filename: regionDeliverySuspendFile)
            regionSelectPage.autodetectSuggest.element.tap()
            ybm_wait(forFulfillmentOf: { regionSelectPage.regionInput.text == "Москва" })

            mockServer?.deleteSuspended(filename: regionDeliverySuspendFile)
            profilePage = goToProfile()
        }

        "Переходим в Настройки".ybm_run { _ in
            settingsPage = goToSettings(profile: profilePage)
            wait(forVisibilityOf: settingsPage.element)
        }

        "Проверяем данные на экране настроек".ybm_run { _ in
            XCTAssertEqual(settingsPage.regionValue.element.label, "Москва")
        }
    }

    // MARK: - Private

    private func enablePlusAvailability() {
        "Настраиваем стейт кешбэка".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }
    }
}
