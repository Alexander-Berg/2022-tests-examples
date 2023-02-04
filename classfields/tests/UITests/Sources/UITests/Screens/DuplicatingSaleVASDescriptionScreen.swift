//
//  DuplicatingSaleVASDescriptionScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 4/7/21.
//

import XCTest
import Snapshots

final class DuplicatingSaleVASDescriptionScreen: BaseScreen {
    lazy var recoverButton = find(by: "Восстановить старое объявление").firstMatch
}
