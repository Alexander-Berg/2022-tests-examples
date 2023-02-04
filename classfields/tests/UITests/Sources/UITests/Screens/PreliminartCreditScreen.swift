//
//  PreliminaryCreditScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.06.2020.
//

import XCTest
import Snapshots

class PreliminaryCreditScreen: BaseScreen, Scrollable, NavigationControllerContent {

    public lazy var fio = find(by: "field_name").firstMatch
    public lazy var email = find(by: "field_email").firstMatch
    public lazy var phone = find(by: "field_phone").firstMatch
    public lazy var login = find(by: "field_login").firstMatch
    public lazy var done = find(by: "Готово").firstMatch
    public lazy var submitButton = find(by: "button").firstMatch
    public lazy var agreement = find(by: "agreement").firstMatch
    public var closeButton: XCUIElement {
        let q = self.find(by: "dismiss_modal_button")
        return q.element(boundBy: q.count - 1)
    }

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    func cellFor(_ identifier: String) -> XCUIElement {
       return XCUIApplication.make().collectionViews.cells.containing(.any, identifier: identifier).firstMatch
    }
}
