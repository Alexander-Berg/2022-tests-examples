//
//  ReviewSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 24.04.2020.
//

import XCTest
import Snapshots

class ReviewSteps: BaseSteps {
    let screen: ReviewScreen = ReviewScreen()

    func openMarkSelector() -> ReviewSteps {
        screen.markSelector.tap()
        return self
    }

    func selectBMW() -> ReviewSteps {
        screen.find(by: "BMW").firstMatch.tap()
        screen.find(by: "X5").firstMatch.tap()
        return self
    }

    func scrollTo(_ value: String) -> Bool {
        return screen.scrollTo(element: screen.find(by: value).firstMatch, maxSwipes: 5)
    }

    @discardableResult
    func tap(selector: String) -> ReviewSteps {
        screen.find(by: selector).firstMatch.tap()
        return self
    }

    func cleanModel() -> ReviewSteps {
        exist(selector: "clear_button")
        screen.find(by: "clear_button").element(boundBy: 1).tap()
        return self
    }

    func cleanMark() -> ReviewSteps {
        exist(selector: "clear_button")
        screen.find(by: "clear_button").element(boundBy: 0).tap()
        return self
    }

    func selectGeneration() -> ReviewSteps {
        screen.generationField.shouldExist(timeout: 10)
        screen.generationField.tap()
        tap(selector: "с 2018 по 2020 IV (G05)")
        return self
    }
}
