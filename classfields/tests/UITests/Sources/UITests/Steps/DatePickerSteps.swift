//
//  DatePickerSteps.swift
//  UITests
//
//  Created by Aleksey Gotyanov on 12/22/20.
//

import XCTest
import Snapshots

final class DatePickerSteps: BaseSteps {
    @discardableResult
    func select(components: String...) -> Self {
        step("Выбираем в пикере даты значения'\(components.joined(separator: " "))'") {
            let wheels = app.pickerWheels

            XCTAssert(
                wheels.count == components.count,
                "Количество слайдеров \(wheels.count) отличается от количества переданных компонентов \(components.count)"
            )

            for (offset, component) in components.enumerated() {
                wheels.element(boundBy: offset).adjust(toPickerWheelValue: component)
            }
        }
    }

    @discardableResult
    func tapDone() -> Self {
        step("Нажимаем готово в пикере даты") {
            self.app.staticTexts["Готово"].firstMatch.tap()
        }
    }
}
