//
//  FullFormSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.12.2020.
//

import XCTest
import Snapshots

class SharkFullFormSteps: BaseSteps {
    let form = SharkFullFormScreen.Form()

    var screen: SharkFullFormScreen {
        return baseScreen.on(screen: SharkFullFormScreen.self)
    }

    func selectOffer() -> SharkSelectOfferSteps {
        onMainScreen().find(by: "Выбрать").firstMatch.tap()
        return SharkSelectOfferSteps(context: context)
    }

    func fillOptionsField<Option: RawRepresentable>(_ keyPath: KeyPath<SharkFullFormScreen.Form, FieldSelector>, option: Option) -> Self where Option.RawValue == String {
        let element = screen.element(form[keyPath: keyPath])
        screen.scrollTo(element: element, windowInsets: .init(top: 0, left: 0, bottom: 32, right: 0))
        element.tap()
        screen.option(option).tap()
        return self
    }

    func fillField(_ keyPath: KeyPath<SharkFullFormScreen.Form, FieldSelector>, value: String) -> Self {
        let element = screen.element(form[keyPath: keyPath])
        element.tap()
        element.clearText()
        element.typeText("\(value)")
        return self
    }

    func tapField(_ keyPath: KeyPath<SharkFullFormScreen.Form, FieldSelector>) -> Self {
        let element = screen.element(form[keyPath: keyPath])
        screen.scrollTo(element: element, windowInsets: .init(top: 0, left: 0, bottom: 32, right: 0))
        element.tap()
        return self
    }

    @discardableResult
    func notExist(_ keyPath: KeyPath<SharkFullFormScreen.Form, FieldSelector>) -> Self {
        notExist(selector: form[keyPath: keyPath].path)
        return self
    }

    func fillHomeRentAmount(_ value: Int) -> Self {
        screen.find(by: "homeRentAmount").firstMatch.typeText("\(value)")
        return self
    }

    func pressNext() -> Self {
        screen.find(by: "Далее").firstMatch.tap()
        return self
    }

    @discardableResult
    func validate(_ path: KeyPath<SharkFullFormScreen.Form, FieldSelector>, snapshotId: String) -> Self {
        let element = screen.find(by: form[keyPath: path].path).firstMatch
        screen.scrollTo(element: element, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 20, right: 0  ))
        validateSnapshot(of: element, snapshotId: snapshotId)
        return self
    }

    func validateAdditionalRequiredFields() -> Self {
        validateSnapshot(of: screen.additionalRequiredFields, snapshotId: "additionalRequiredFields_disExpanded")
        return self
    }

    func openAdditionalRequiredFields() -> Self {
        screen.additionalRequiredFields.tap()
        return self
    }

    func scrollTo(_ selector: String) -> Self {
        screen.scrollTo(element: screen.find(by: selector).firstMatch, windowInsets: .zero)
        return self
    }

    func swipe() -> Self {
        screen.swipe(.up)
        return self
    }

    func dragHorizontal(_ element: String, to xOffset: CGFloat) -> Self {
        let element = screen.find(by: element).firstMatch

        let elementCoordinat = element.coordinate(withNormalizedOffset: .init(dx: 0.5, dy: 0.5))
        let targetCoordinat = elementCoordinat.withOffset(.init(dx: xOffset, dy: 0))

        elementCoordinat.press(forDuration: 0, thenDragTo: targetCoordinat)
        return self
    }
}
