//
//  ReportScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 15.06.2020.
//

import XCTest
import Snapshots

class ReportScreen: BaseScreen, Scrollable, NavigationControllerContent {
    public lazy var creditButton = find(by: "Оформить заявку").firstMatch
    public lazy var draftCreditButton = find(by: "Дополнить заявку").firstMatch
    public lazy var draftActiveButton = find(by: "Проверить статус заявки").firstMatch

    public lazy var userRating = find(by: "userRating").firstMatch
    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }
}
