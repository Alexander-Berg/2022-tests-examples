import AutoMate
import XCTest

final class SmartshoppingChooseBonusAfterOrderTest: SmartshoppingBonusAfterOrderTestCase {

    func testPopupAfterOrderWithOneBonus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3404")
        Allure.addEpic("Купоны")
        Allure.addFeature("Выбор купона")
        Allure.addTitle("Позитивный сценарий выбора")

        "Мокаем состояние выбора купона".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SmartshoppingSet_OrderChooseBonus")
        }

        completeCheckoutFlow()

        var unlockCoinPopup: UnlockCoinPopupPage!
        var selectedBonusDetails: SmartbonusDetailsPopupPage!
        var selectedBonusPopup: UnlockCoinPopupPage!

        "Проверяем попап получения монетки".ybm_run { _ in
            unlockCoinPopup = UnlockCoinPopupPage.currentPopup
            ybm_wait(forVisibilityOf: [unlockCoinPopup.element])

            XCTAssertTrue(unlockCoinPopup.closeButton.isVisible)
            XCTAssertFalse(unlockCoinPopup.continueButton.isVisible)
            XCTAssertTrue(unlockCoinPopup.title.isVisible)
            XCTAssertEqual(unlockCoinPopup.title.text, "Выберите свой купон за заказ")
            XCTAssertTrue(unlockCoinPopup.pageController.isVisible)
            XCTAssertEqual(unlockCoinPopup.pageController.value as? String, "1 страница из 3")
        }

        let steps = [
            (
                stepInfo: "Проверяем средний купон",
                position: 1,
                swipeDirection: SwipeDirection.right,
                bonusTitle: "Скидка 1000 ₽",
                bonusSubtitle: "на товары для дома",
                pageControllerValue: "1 страница из 3"
            ),
            (
                stepInfo: "Проверяем третий купон",
                position: 2,
                swipeDirection: .left,
                bonusTitle: "Скидка 1000 ₽",
                bonusSubtitle: "на кухонные ножи",
                pageControllerValue: "2 страница из 3"
            ),
            (
                stepInfo: "Проверяем первый купон",
                position: 0,
                swipeDirection: .left,
                bonusTitle: "Скидка 1000 ₽",
                bonusSubtitle: "на посуду",
                pageControllerValue: "0 страница из 3"
            )
        ]

        for step in steps.enumerated() {
            step.element.stepInfo.ybm_run { _ in
                let bonusSnippet = unlockCoinPopup.scrollToBonus(
                    at: step.element.position,
                    direction: step.element.swipeDirection
                )

                bonusSnippet.verifyContent(
                    with: step.element.bonusTitle,
                    subtitle: step.element.bonusSubtitle
                )
                XCTAssertEqual(unlockCoinPopup.pageController.value as? String, step.element.pageControllerValue)
            }
        }

        "Нажимаем на купон и ждем открытие деталей".ybm_run { _ in
            let bonusSnippet = unlockCoinPopup.scrollToBonus(at: 0)
            bonusSnippet.tap()
            selectedBonusDetails = SmartbonusDetailsPopupPage.currentPopup
            wait(forExistanceOf: selectedBonusDetails.element)
        }

        "Проверяем детали купона".ybm_run { _ in
            XCTAssertTrue(selectedBonusDetails.closeButton.isVisible)
            XCTAssertTrue(selectedBonusDetails.bonusImage.exists)
            XCTAssertEqual(selectedBonusDetails.deliveryLabel.label, "Начнёт действовать, как только заказ будет у вас")
            XCTAssertEqual(selectedBonusDetails.bonusTitle.label, "Скидка 1000 ₽")
            XCTAssertEqual(selectedBonusDetails.bonusDescription.label, "через плечо")
            XCTAssertEqual(selectedBonusDetails.bottomButton.label, "Выбираю этот купон")
        }

        "Нажимаем на кнопку 'Выбираю этот купон'".ybm_run { _ in
            selectedBonusDetails.bottomButton.tap()
        }

        "Проверяем попап выбранного купона".ybm_run { _ in
            selectedBonusPopup = UnlockCoinPopupPage.currentPopup
            ybm_wait(forVisibilityOf: [selectedBonusPopup.element])

            XCTAssertTrue(selectedBonusPopup.closeButton.isVisible)
            XCTAssertTrue(selectedBonusPopup.continueButton.isVisible)
            XCTAssertTrue(selectedBonusPopup.title.isVisible)
            XCTAssertEqual(selectedBonusPopup.title.text, "Хороший выбор!")
            XCTAssertFalse(selectedBonusPopup.pageController.isVisible)

            let bonusSnippet = unlockCoinPopup.scrollToBonus(at: 0)

            ybm_wait(forVisibilityOf: [bonusSnippet.element])

            bonusSnippet.verifyContent(
                with: "Скидка 1000 ₽",
                subtitle: "на посуду"
            )
        }

        "Нажимаем на кнопку 'Отлично'".ybm_run { _ in
            selectedBonusPopup.continueButton.tap()
        }

        "Проверяем возвращение на экран спасибки".ybm_run { _ in
            let finishMultiorderPage = FinishMultiorderPage.current
            wait(forVisibilityOf: finishMultiorderPage.element)
        }
    }
}
