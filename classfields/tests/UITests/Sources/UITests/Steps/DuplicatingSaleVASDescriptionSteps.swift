//
//  DuplicatingSaleVASDescriptionSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 4/6/21.
//

import XCTest
import Snapshots

final class DuplicatingSaleVASDescriptionSteps: BaseSteps {
    func onDuplicatingSaleVASDescriptionScreen() -> DuplicatingSaleVASDescriptionScreen {
        return self.baseScreen.on(screen: DuplicatingSaleVASDescriptionScreen.self)
    }

    @discardableResult
    func tapActivate(_ identifier: String = "Активировать") -> Self {
        step("Нажимаем кнопку \(identifier)") {
            onDuplicatingSaleVASDescriptionScreen().find(by: identifier).firstMatch.tap()
        }
    }

    @discardableResult
    func tapRecover() -> Self {
        step("Нажимаем кнопку \"Восстановить\"") {
            onDuplicatingSaleVASDescriptionScreen().recoverButton.tap()
        }
    }
}
