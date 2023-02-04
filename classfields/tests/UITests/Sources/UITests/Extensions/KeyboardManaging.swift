import XCTest

protocol KeyboardManaging { }

extension KeyboardManaging {
    func typeFromKeyboard(_ input: String) {
        Step("Набираем текст с клавиатуры") {
            let app = XCUIApplication.make()
            let keyboard = app.keyboards.firstMatch
            input.forEach {
                let key = keyboard.keys[String($0)]
                if !key.exists {
                    keyboard.keys["more"].tap()
                }
                key.tap()
            }
        }
    }

    func deleteStringFromKeyboard(_ input: String) {
        let app = XCUIApplication.make()
        let keyboard = app.keyboards.firstMatch
        input.forEach { _ in
            keyboard.keys["delete"].tap()
        }
    }
}
