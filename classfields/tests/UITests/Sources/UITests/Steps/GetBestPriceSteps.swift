//
//  GetBestPriceSteps.swift
//  UITests
//
//  Created by Roman Bevza on 10/16/20.
//

import XCTest
import Snapshots

class GetBestPriceSteps: BaseSteps {
    func onGetBestPriceScreen() -> GetBestPriceScreen {
        return self.baseScreen.on(screen: GetBestPriceScreen.self)
    }

    @discardableResult
    func tapSendButton() -> Self {
        Step("Тапаем \"Жду предложений\"") {
            self.onGetBestPriceScreen().sendButton.tap()
        }
        return self
    }

    func checkErrorAlert(message: String) -> Self {
        Step("Ищем сообщение \"\(message)\"") {
            let element = self.onGetBestPriceScreen().find(by: message).firstMatch
            element.shouldExist()
        }
        return self
    }

    func pickMark(_ mark: String) -> Self {
        Step("Выбираем марку") {
            self.onGetBestPriceScreen().pickMarkButton.tap()
            self.onGetBestPriceScreen().find(by: mark).firstMatch.tap()
            self.onGetBestPriceScreen().find(by: "Готово").firstMatch.tap()
        }
        return self
    }

    func validatePicked(mark: String, model: String) -> Self {
        Step("Проверяем что выбрана \(mark) \(model)") {
            self.onGetBestPriceScreen().find(by: "\(mark) \(model)").firstMatch.shouldExist()
        }
        return self
    }

    @discardableResult
    func validateSuccessHUD() -> Self {
        Step("Проверяем что заявка отправилась") {
            self.onGetBestPriceScreen().succesHUD.shouldExist()
        }
        return self
    }
}
