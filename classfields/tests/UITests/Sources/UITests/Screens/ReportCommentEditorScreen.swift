//
//  ReportCommentEditorScreen.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 29.05.2020.
//

import XCTest
import Snapshots

class ReportCommentEditorScreen: BaseScreen {

    var saveCommentButton: XCUIElement {
        return findAll(.staticText)["Отправить"]
    }

    var addPhotosButton: XCUIElement {
        return findAll(.button)["add photo bar icon"]
    }

    var closeButton: XCUIElement {
        return app.otherElements["NavBarView"].children(matching: .other).element(boundBy: 0).children(matching: .button).element
    }

    var textView: XCUIElement {
        return findAll(.textView).firstMatch
    }
}
