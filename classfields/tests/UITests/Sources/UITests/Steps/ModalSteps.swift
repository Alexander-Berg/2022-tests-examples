//
//  ModalSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 5/6/20.
//

import XCTest
import Snapshots

class ModalSteps<SourceSteps, Screen: ModalScreen>: BaseSteps {
    private var source: SourceSteps?

    init(context: StepsContext, source: SourceSteps) {
        self.source = source
        super.init(context: context)
    }

    required init(context: StepsContext, root: XCUIElement? = nil) {
        super.init(context: context, root: root)
    }

    func onModalScreen() -> Screen {
        return baseScreen.on(screen: Screen.self)
    }

    func snapshot() -> UIImage {
        return onModalScreen().snapshot()
    }

    @discardableResult
    func dismissModal() -> SourceSteps? {
        Step("Тапаем и закрываем попап") {
            self.onModalScreen().dismissButton().tap()
        }

        return source
    }
}
