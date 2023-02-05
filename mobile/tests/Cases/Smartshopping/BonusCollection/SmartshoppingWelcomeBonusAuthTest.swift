import MarketUITestMocks
import XCTest

final class SmartshoppingWelcomeBonusAuthTest: SmartbonusDetailsTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testRegisteredUserWithoutBonuses() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2799")
        Allure.addEpic("Купоны")
        Allure.addFeature("Нет купонов")
        Allure.addTitle("Пользователь потратил все свои купоны")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!

        "Открываем приложение, авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на страницу ББ".ybm_run { _ in
            smartshoppingPage = goToMyBonuses(root: root)

            ybm_wait(forFulfillmentOf: { smartshoppingPage.element.isVisible })
        }

        "Проверяем отсутствие купонов".ybm_run { _ in
            XCTAssertFalse(smartshoppingPage.carouselView.isBonusExists)
        }

        "Проверяем тайтл заглушки".ybm_run { _ in
            XCTAssertEqual(
                smartshoppingPage.tokenCard.title.label,
                "Хочу купон!"
            )
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
                withDetailText: "Только постоянные покупатели могут копить купоны — не выходите из своего аккаунта."
            )
        }
    }

    func testActiveBeruBonusContent() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3034")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1057")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3261")
        Allure.addEpic("Купоны")
        Allure.addFeature("Активный ББ")
        Allure.addTitle("У пользователя есть активный ББ. Подробная информация. Закрытие Купона")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        let endDate = getNewDate(byAddingDays: 14)

        changeCoinForMock(.testCoin, endDate: endDate)

        "Открываем приложение, авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на страницу ББ".ybm_run { _ in
            smartshoppingPage = goToMyBonuses(root: root)

            ybm_wait(forFulfillmentOf: { smartshoppingPage.element.isVisible })
        }

        "Нажимаем на купон".ybm_run { _ in
            let smartBonusSnippet = smartshoppingPage.singleCouponCard
            smartBonusSnippet.tap()
            bonusPopup = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: bonusPopup.element)
        }

        "Проверяем контент карточки купона".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                bonusPopup.bonusImage.exists
                    && bonusPopup.bonusTitle.isVisible
                    && bonusPopup.bonusDescription.isVisible
                    && bonusPopup.expirationDate.isVisible
                    && bonusPopup.bottomButton.isVisible
            })

            XCTAssertEqual(bonusPopup.bottomButton.label, "Выбрать товары")
            let expirationDateTitle = String(
                format: "до %@",
                getDateString(from: endDate)
            )
            XCTAssertEqual(
                bonusPopup.bonusDescription.label,
                "Тестовый промо бонус"
            )
            XCTAssertEqual(bonusPopup.expirationDate.label, expirationDateTitle)
        }

        "Нажимаем на кнопку закрыть".ybm_run { _ in
            bonusPopup.closeButton.tap()
            ybm_wait(forVisibilityOf: [smartshoppingPage.element])
        }
    }

    func testNotActiveBeruBonusContent() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3032")
        Allure.addEpic("Купоны")
        Allure.addFeature("Неактивный ББ")
        Allure.addTitle("У пользователя есть неактивный ББ")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!
        var bonusPopup: SmartbonusDetailsPopupPage!

        let endDate = getNewDate(byAddingDays: 14)
        let orderId = 2_994_556

        changeCoinForMock(.testCoin, endDate: endDate, withStatus: "INACTIVE", forOrders: [orderId])

        "Открываем приложение, авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на страницу ББ".ybm_run { _ in
            smartshoppingPage = goToMyBonuses(root: root)

            ybm_wait(forFulfillmentOf: { smartshoppingPage.element.isVisible })
        }

        "Нажимаем на купон".ybm_run { _ in
            let smartBonusSnippet = smartshoppingPage.singleCouponCard
            smartBonusSnippet.tap()
            bonusPopup = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: bonusPopup.element)
        }

        "Проверяем контент карточки купона".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                bonusPopup.bonusImage.exists
                    && bonusPopup.bonusTitle.isVisible
                    && bonusPopup.bonusDescription.isVisible
                    && bonusPopup.topButton.isVisible
                    && bonusPopup.bonusConditionTitle.isVisible
                    && bonusPopup.lockImage.exists
            })

            XCTAssertEqual(bonusPopup.topButton.label, "Отследить заказ")
            XCTAssertEqual(
                bonusPopup.bonusConditionTitle.label,
                "Начнет действовать после доставки заказа \(orderId)"
            )
        }
    }

    // MARK: - Nested types

    typealias Coin = ResolveBonusesForPerson.Bonus

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

    private func changeCoinForMock(
        _ coin: Coin,
        endDate: Date,
        withStatus status: String = "ACTIVE",
        forOrders reasonOrderIds: [Int] = []
    ) {
        var bonusState = UserAuthState()

        let formattedEndDate = getEnFormattedDateString(from: endDate)

        "Мокаем бонусы".ybm_run { _ in
            bonusState.setBonusesForPerson(bonuses: [
                .changeCoin(
                    coin,
                    status: status,
                    endDate: formattedEndDate,
                    reasonOrderIds: reasonOrderIds
                )
            ])

            stateManager?.setState(newState: bonusState)
        }
    }
}
