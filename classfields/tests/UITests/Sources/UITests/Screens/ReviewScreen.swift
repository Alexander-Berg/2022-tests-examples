//
//  ReviewScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 24.04.2020.
//

import XCTest
import Snapshots

class ReviewScreen: BaseScreen, Scrollable, NavigationControllerContent {
    public lazy var markSelector = find(by: "Марка, модель").firstMatch
    public lazy var generationField = find(by: "Поколение").firstMatch

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }
}
