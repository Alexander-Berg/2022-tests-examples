//
//  AboutScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 2/3/22.
//

import XCTest

final class AboutScreen: BaseSteps, UIRootedElementProvider {
    typealias Element = Void
    static let rootElementID = "AboutViewController"
    static let rootElementName = "О приложении"
}
