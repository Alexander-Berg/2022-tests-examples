import XCTest

class DSBSQuestionnaireTestCase: LocalMockTestCase {

    // MARK: - Public

    var questionnairePage: QuestionnairePage!

    let checkIn24HoursSub = "В ближайшие 24 часа уточним у продавца и вернемся с новой датой доставки"
    let checkIn24HoursTitle = "Простите, что подвели"

    let sorryForWaitingTitle = "Извините за ожидание"
    let sorryForWaitingSub = "Мы поговорим с продавцом, чтобы такое не повторялось."

    var checkIn24HoursAppeared: Bool {
        questionnairePage.subtitle.text == checkIn24HoursSub
            && questionnairePage.title.text == checkIn24HoursTitle
    }

    var sorryForWaitingAppeared: Bool {
        questionnairePage.subtitle.text == sorryForWaitingSub
            && questionnairePage.title.text == sorryForWaitingTitle
    }

    func notDeliveredNoNewDateFlow_tapContinue() {
        notDeliveredNoNewDateFlow()

        "Нажимаем на 'Продолжить'".ybm_run { _ in
            questionnairePage.mainActionButton.button.tap()
        }

        wait(forInvisibilityOf: questionnairePage.element)
    }

    func notDeliveredNoNewDateFlow_consultationChat() {
        notDeliveredNoNewDateFlow()

        "Нажимаем на 'Позвонить в поддержку'".ybm_run { _ in
            questionnairePage.extraActionButton.button.tap()
        }

        wait(forInvisibilityOf: questionnairePage.element)
    }

    func alreadyGotItScenario_feedbackPopupOpens() {
        "Ждем появления попапа с оценкой".ybm_run { _ in
            let page = FeedbackDrawerPage.current
            wait(forExistanceOf: page.element)
        }

    }

    func notDelivered_NewDateFoundOut() {
        "Ждем появления попапа 'Вам назвали новую дату доставки?'".ybm_run { _ in
            questionnairePage = QuestionnairePage.current
            wait(forVisibilityOf: questionnairePage.title)
            XCTAssertEqual(questionnairePage.title.text, "Вам назвали новую дату доставки?")
            XCTAssertEqual(questionnairePage.mainActionButton.button.label, "Да")
            XCTAssertEqual(questionnairePage.extraActionButton.button.label, "Нет")
        }

        "Нажимаем кнопку 'Да' и ждем следующего попапа".ybm_run { _ in
            questionnairePage.mainActionButton.button.tap()
            ybm_wait {
                self.questionnairePage = QuestionnairePage.current
                return self.sorryForWaitingAppeared
            }
        }

        "Нажимаем на 'Понятно'".ybm_run { _ in
            XCTAssertEqual(questionnairePage.mainActionButton.button.label, "Понятно")
            questionnairePage.mainActionButton.button.tap()
        }

        wait(forInvisibilityOf: questionnairePage.element)
    }

    private func notDeliveredNoNewDateFlow() {
        "Ждем появления попапа 'Вам назвали новую дату доставки?'".ybm_run { _ in
            questionnairePage = QuestionnairePage.current
            wait(forVisibilityOf: questionnairePage.title)
            XCTAssertEqual(questionnairePage.title.text, "Вам назвали новую дату доставки?")
            XCTAssertEqual(questionnairePage.mainActionButton.button.label, "Да")
            XCTAssertEqual(questionnairePage.extraActionButton.button.label, "Нет")
        }

        "Нажимаем кнопку 'Нет' и ждем следующего попапа".ybm_run { _ in
            questionnairePage.extraActionButton.button.tap()
            ybm_wait {
                self.questionnairePage = QuestionnairePage.current
                return self.checkIn24HoursAppeared
            }
        }

        "Проверяем надписи на кнопках".ybm_run { _ in
            self.questionnairePage = QuestionnairePage.current
            XCTAssertEqual(questionnairePage.mainActionButton.button.label, "Продолжить")
            XCTAssertEqual(questionnairePage.extraActionButton.button.label, "Позвонить в поддержку")
        }
    }

    func getDateString(withDateFormat dateFormat: String, date: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.locale = Locale(identifier: "ru")
        return dateFormatter.string(from: date)
    }
}
