//
//  ReviewFeatureScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 05.07.2022.
//

import XCTest
import Snapshots

final class ReviewFeatureScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case snippet(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .snippet(let text):
            return "snippet_\(text)"
        }
    }

    static let rootElementID = "ReviewFeatureViewController"
    static let rootElementName = "Экран отзывов на фичу авто"
}


