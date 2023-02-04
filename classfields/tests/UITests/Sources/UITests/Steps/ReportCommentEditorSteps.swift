//
//  ReportCommentEditorSteps.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 29.05.2020.
//

import XCTest
import Snapshots

class ReportCommentEditorSteps: BaseSteps {

    let screen = ReportCommentEditorScreen()

    @discardableResult
    func save() -> SaleCardSteps {
        screen.saveCommentButton.tap()
        return SaleCardSteps(context: context)
    }

    func close() -> SaleCardSteps {
        screen.closeButton.tap()
        return SaleCardSteps(context: context)
    }

    func writeComment(text: String = "Тестовый комментарий длиннее 10 символов") -> ReportCommentEditorSteps {
        screen.textView.typeText(text)
        return self
    }
}
