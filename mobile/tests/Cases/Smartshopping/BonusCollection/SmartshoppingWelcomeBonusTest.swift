import XCTest

final class SmartshoppingWelcomeBonusTest: SmartbonusDetailsTestCase {

    func testCanRegisterForUnauthorized() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3244")
        Allure.addEpic("Купоны")
        Allure.addFeature("Неавторизованный пользователь")
        Allure.addTitle("Выключенный тогл бесплатной доставки")

        var smartshoppingPage: SmartshoppingPage!

        "Переходим в таб \"Купоны\"".ybm_run { _ in
            smartshoppingPage = goToMyBonuses()
        }

        "Проверяем топ контент страницы".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.howToGetButton.isVisible
                    && smartshoppingPage.howToUseButton.isVisible
            })
            XCTAssertEqual(smartshoppingPage.howToGetButton.label, "Как получить")
            XCTAssertEqual(smartshoppingPage.howToUseButton.label, "Как использовать")
        }

        "Проверяем тайтл кнопки \"Войти и копить\"".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(
                to: .down,
                untilVisible: smartshoppingPage.smartBonusBannerView.loginAndStashButton
            )
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.smartBonusBannerView.loginAndStashButton.isVisible
            })
            XCTAssertEqual(smartshoppingPage.smartBonusBannerView.loginAndStashButton.label, "Войти и копить")
        }

        "Проверяем тайтл секции \"Что нужно знать о купонах\"".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(to: .down, untilVisible: smartshoppingPage.infoSectionTitle)
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.infoSectionTitle.isVisible
            })
            XCTAssertEqual(smartshoppingPage.infoSectionTitle.text, "Что нужно знать о купонах")
        }

        "Проверяем первое описание".ybm_run { _ in
            checkInfoSectionDetailText(
                with: smartshoppingPage,
                atIndex: 0,
                withDetailText: "Чтобы копить купоны, нужно зарегистрироваться или войти в личный кабинет."
            )
        }

        "Проверяем второе описание".ybm_run { _ in
            checkInfoSectionDetailText(
                with: smartshoppingPage,
                atIndex: 1,
                withDetailText: "Если при заказе вам выпал купон, он станет активным, когда вы получите товар."
            )
        }

        "Проверяем третье описание".ybm_run { _ in
            checkInfoSectionDetailText(
                with: smartshoppingPage,
                atIndex: 2,
                withDetailText: "Купоны можно потратить по одному или сложить и получить заказ почти бесплатно."
            )
        }

        "Проверяем четвертое описание".ybm_run { _ in
            checkInfoSectionDetailText(
                with: smartshoppingPage,
                atIndex: 3,
                withDetailText: "Жаль, но купоны не вечны. Их срок действия указан на карточке. Не дайте им сгореть!"
            )
        }
    }

    func testCanOpenHowToUseAndGetBonusWebView() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2797")
        Allure.addEpic("Купоны")
        Allure.addFeature("Неавторизованный пользователь")
        Allure.addTitle("Может перейти на лендинг \"Как получить купоны?\" и \"Как потратить купоны?\"")

        var smartshoppingPage: SmartshoppingPage!

        "Переходим в таб \"Купоны\"".ybm_run { _ in
            smartshoppingPage = goToMyBonuses()
        }

        "Проверяем топ контент страницы".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.howToGetButton.isVisible
                    && smartshoppingPage.howToUseButton.isVisible
            })
            XCTAssertEqual(smartshoppingPage.howToGetButton.label, "Как получить")
            XCTAssertEqual(smartshoppingPage.howToUseButton.label, "Как использовать")
        }

        "Проверяем тайтл кнопки \"Войти и копить\"".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(
                to: .down,
                untilVisible: smartshoppingPage.smartBonusBannerView.loginAndStashButton
            )
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.smartBonusBannerView.loginAndStashButton.isVisible
            })
            XCTAssertEqual(smartshoppingPage.smartBonusBannerView.loginAndStashButton.label, "Войти и копить")
        }

        "Проверяем тайтл секции \"Что нужно знать о купонах\"".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(to: .down, untilVisible: smartshoppingPage.infoSectionTitle)
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.infoSectionTitle.isVisible
            })
            XCTAssertEqual(smartshoppingPage.infoSectionTitle.text, "Что нужно знать о купонах")
        }

        "Проверяем первое описание".ybm_run { _ in
            checkInfoSectionDetailText(
                with: smartshoppingPage,
                atIndex: 0,
                withDetailText: "Чтобы копить купоны, нужно зарегистрироваться или войти в личный кабинет."
            )
        }

        "Нажимаем на кнопку \"Как получить\"".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(to: .up, untilVisible: smartshoppingPage.howToGetButton)
            smartshoppingPage.howToGetButton.tap()
            wait(forExistanceOf: WebViewPage.current.element)
        }

        "Переходим назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: smartshoppingPage.element)
        }

        "Нажимаем на кнопку \"Как использовать\"".ybm_run { _ in
            smartshoppingPage.howToUseButton.tap()
            wait(forExistanceOf: WebViewPage.current.element)
        }

        "Переходим назад".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: smartshoppingPage.element)
        }
    }

    func testCanAuthorizeFromSmartBonusBanner() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2798")
        Allure.addEpic("Купоны")
        Allure.addFeature("Неавторизованный пользователь")
        Allure.addTitle("Авторизация с экрана ББ")

        var smartshoppingPage: SmartshoppingPage!

        "Переходим на страницу ББ и проверяем кнопку \"Войти и копить\"".ybm_run { _ in
            smartshoppingPage = goToMyBonuses()

            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.element.isVisible
                    && smartshoppingPage.smartBonusBannerView.loginAndStashButton.isVisible
            })

            XCTAssertEqual(smartshoppingPage.smartBonusBannerView.loginAndStashButton.label, "Войти и копить")
        }

        "Нажимаем на кнопку \"Войти и копить\"".ybm_run { _ in
            smartshoppingPage.smartBonusBannerView.loginAndStashButton.tap()
            completeAuthFlow()

            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.element.isVisible
            })
        }
    }

    // MARK: - Private

    private func checkInfoSectionDetailText(
        with smartshoppingPage: SmartshoppingPage,
        atIndex index: Int,
        withDetailText text: String
    ) {
        let sectionDetailItem = smartshoppingPage.infoSectionDetailText(at: index)
        smartshoppingPage.collectionView.swipe(to: .down, untilVisible: sectionDetailItem)
        ybm_wait(forFulfillmentOf: {
            sectionDetailItem.isVisible
        })
        XCTAssertEqual(sectionDetailItem.text, text)
    }
}
