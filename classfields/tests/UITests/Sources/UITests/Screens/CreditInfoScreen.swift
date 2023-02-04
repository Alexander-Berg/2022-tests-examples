//
//  CreditInfoScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 05.06.2020.
//

import XCTest
import Snapshots

class CreditInfoScreen: BaseScreen {
    lazy var offerButton = find(by: "Отправить заявку").firstMatch
}
