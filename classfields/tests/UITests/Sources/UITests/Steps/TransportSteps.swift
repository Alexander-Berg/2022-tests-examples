//
//  TransportSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.07.2020.
//

import XCTest
import Snapshots

class TransportSteps: MainSteps {
       @discardableResult
       override func exist(selector: String) -> Self {
           let element = baseScreen.find(by: selector).firstMatch
           element.shouldExist(timeout: Const.timeout)
           return self
       }
}
