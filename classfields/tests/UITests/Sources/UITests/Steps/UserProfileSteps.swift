//
//  UserProfileSteps.swift
//  AutoRu
//
//  Created by Vitalii Stikhurov on 02.03.2020.
//

import XCTest
import Snapshots

class UserProfileSteps: BaseSteps {

    @discardableResult
    func checkUserProfileScreenVisible() -> UserProfileSteps {
        Step("Should see User Profile screen") {
            onUserProfileScreen().nameTitle.shouldExist(timeout: Const.timeout)
        }
        return self
    }

    @discardableResult
    func openPicker(type: UserProfileScreen.Picker) -> UserProfileSteps {
        onUserProfileScreen().pickerElement(type).shouldExist(timeout: Const.timeout)
        if !onUserProfileScreen().pickerElement(type).isHittable {
            wait(for: 5)
        }
        onUserProfileScreen().pickerElement(type).tap()

        if !onUserProfileScreen().find(by: type.validationLabel).firstMatch.waitForExistence(timeout: 3) {
            let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

            if springboard.buttons.element(boundBy: 1).waitForExistence(timeout: Const.timeout) {
                springboard.buttons.element(boundBy: 1).tap()
                if springboard.buttons.element(boundBy: 1).waitForExistence(timeout: Const.timeout) {
                    springboard.buttons.element(boundBy: 1).tap()
                }
            }
        }

        onUserProfileScreen().find(by: type.validationLabel).firstMatch.shouldExist(timeout: Const.timeout)
        return self
    }

    func setValueToPicker(type: UserProfileScreen.Picker, value: String, updateProfile: (() -> Void)? = nil) -> UserProfileSteps {
        updateProfile?()
        switch type {
        case .experience:
            onUserProfileScreen().find(by: value).firstMatch.tap()

        case .birthday:
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd"
            dateFormatter.locale = Locale(identifier: "ru_RU")
            let date = dateFormatter.date(from: value)!
            dateFormatter.dateFormat = "d MMMM"
            let monthValue = dateFormatter.string(from: date).split(separator: " ").last ?? ""
            let comp = Calendar.current.dateComponents([.year, .day], from: date)

            let datePicker = onUserProfileScreen().app.datePickers.firstMatch
            // Была интересная проблема - если выставить день, а потом месяц, то при прокрутке месяца через февраль, день сбросится на 28, если был больше, что уронит тест.
            datePicker.pickerWheels.element(boundBy: 2).adjust(toPickerWheelValue: "\(comp.year!)")
            datePicker.pickerWheels.element(boundBy: 1).adjust(toPickerWheelValue:  String(monthValue).capitalized)
            datePicker.pickerWheels.element(boundBy: 0).adjust(toPickerWheelValue: "\(comp.day!)")

        case .city:
            onUserProfileScreen().find(by: value).firstMatch.tap()

        case .email:
            onUserProfileScreen().find(by: "Введите почту").firstMatch.tap()
            enterText(value)

        case .email_phoneConfirmation:
            enterText(value)

        case .password:
            let values = value.components(separatedBy: " ")
            onUserProfileScreen().find(by: "Старый пароль").firstMatch.tap()
            enterText(values[0])
            onUserProfileScreen().find(by: "Новый пароль").firstMatch.tap()
            enterText(values[1])
            onUserProfileScreen().find(by: "Новый пароль ещё раз").firstMatch.tap()
            enterText(values[2])

        case .social:
            onUserProfileScreen().find(by: value).firstMatch.tap()

        case .currentSocial:
             onUserProfileScreen().find(by: "delete_soc_acc_\(value)").firstMatch.tap()

        case .avatar:
            onUserProfileScreen().app.cells.element(boundBy: Int(value)!).tap()
            onUserProfileScreen().find(by: "send_photos").firstMatch.tap()
        }
        return self
    }

    func nameTitleExist(value: String) -> UserProfileSteps {
        onUserProfileScreen().nameTitle.shouldExist(timeout: Const.timeout)
        XCTAssert(onUserProfileScreen().nameTitle.label == value)
        return self
    }

    func nameSubtitleExist(value: String) -> UserProfileSteps {
        onUserProfileScreen().nameSubtitle.shouldExist(timeout: Const.timeout)
        XCTAssert(onUserProfileScreen().nameSubtitle.label == value)
        return self
    }

    func nameSubtitleNotExist(value: String) -> UserProfileSteps {
        onUserProfileScreen().nameSubtitle.shouldNotExist(timeout: Const.timeout)
        return self
    }

    func nameShouldBe(value: String) {
        XCTAssert(onUserProfileScreen().name.textFields.firstMatch.value as! String == value)
    }

    @discardableResult
    func enterText(_ text: String) -> UserProfileSteps {
        app.typeText(text)
        return self
    }

    func selectAbout() -> UserProfileSteps {
        onUserProfileScreen().about.tap()
        return self
    }

    func selectName() -> UserProfileSteps {
        onUserProfileScreen().name.tap()
        return self
    }

    func pressAddName() -> UserProfileSteps {
        onUserProfileScreen().addNameButton.tap()
        return self
    }

    func pressDone() -> UserProfileSteps {
        onUserProfileScreen().find(by: "Готово").firstMatch.tap()
        return self
    }

    func pressClearText() -> UserProfileSteps {
        app.descendants(matching: .button).matching(
            NSPredicate(format: "label IN %@", ["Очистить текст", "Clear text"])
        ).firstMatch.tap()

        return self
    }

    @discardableResult
    func makeAction(_ action: () -> Void) -> UserProfileSteps {
        action()
        return self
    }

    func pressLogout() -> UserProfileSteps {
        if onUserProfileScreen().app.scrollTo(element: onUserProfileScreen().logout, swipeDirection: .up) {
            self.onUserProfileScreen().logout.tap()
            self.onUserProfileScreen().find(by: "Да").firstMatch.tap()
        }
        return self
    }

    func pressDelete() -> UserProfileSteps {
        if onUserProfileScreen().app.scrollTo(element: onUserProfileScreen().delete, swipeDirection: .up) {
            self.onUserProfileScreen().delete.tap()
            self.onUserProfileScreen().find(by: "Удалить").firstMatch.tap()
        }
        return self
    }

    func pressVKIcon() -> UserProfileSteps {
        onUserProfileScreen().vkicon.tap()
        return self
    }
}
