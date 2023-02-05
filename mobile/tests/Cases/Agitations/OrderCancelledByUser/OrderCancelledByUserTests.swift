import XCTest

final class OrderCancelledByUserTests: LocalMockTestCase {

    typealias PopupPage = AgitationPopupPage

    var popupPage: PopupPage!

    func test_OrderCancelledByUser_AnswerYes() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4373")
        Allure.addEpic("Подтверждение отмены от продавца пользователем в DSBS")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь подтвердил отмену")

        openPopup()

        "Подтверждаем агитацию".ybm_run { _ in
            popupPage.firstButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
        }
    }

    func test_OrderCancelledByUser_AnswerNo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4463")
        Allure.addEpic("Подтверждение отмены от продавца пользователем в DSBS")
        Allure.addFeature("Попап подтверждения")
        Allure.addTitle("Пользователь отрицает отмену")

        openPopup()

        "Отклоняем агитацию".ybm_run { _ in
            popupPage.lastButton.tap()
        }

        "Ждем попап с извинениями".ybm_run { _ in
            ybm_wait {
                self.popupPage.descriptionLabel.label == "Извините, разберёмся, почему продавец отменил заказ"
            }
            XCTAssertEqual(popupPage.firstButton.label, "Закрыть")
        }

        "Закрываем попап".ybm_run { _ in
            popupPage.firstButton.tap()
            ybm_wait(forFulfillmentOf: { !self.popupPage.descriptionLabel.isVisible })
        }

    }

    // MARK: - Private Methods

    private func openPopup() {
        "Мокаем данные".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Agitations_OrderCancelledByUser")
        }

        "Открываем приложение".ybm_run { _ in
            appAfterOnboardingAndPopups()
        }

        "Ждем попап".ybm_run { _ in
            popupPage = PopupPage.currentPopup
            ybm_wait {
                self.popupPage.descriptionLabel.label == "Продавец сообщил, что вы передумали его получать"
            }
        }

        "Мокаем пустой ответ резолвера агитаций".run {
            let agitationsRule = MockMatchRule(
                id: "EMPTY_AGITATIONS_RULE",
                matchFunction:
                isPOSTRequest &&
                    isFAPIRequest &&
                    hasExactFAPIResolvers(["resolveOrderAgitations"]),
                mockName: "resolveOrderAgitations_empty"
            )
            mockServer?.addRule(agitationsRule)
        }
    }
}
