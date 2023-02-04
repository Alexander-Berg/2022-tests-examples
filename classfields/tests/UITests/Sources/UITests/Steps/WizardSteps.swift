//
//  WizardSteps.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/25/20.
//

import XCTest
import Snapshots

class WizardSteps: BaseSteps {
    func onWizardScreen() -> WizardScreen {
        return baseScreen.on(screen: WizardScreen.self)
    }

    func tapEnterManually() -> Self {
        Step("Нажимаем Заполнить вручную", step: {
            onWizardScreen().enterManually.tap()
        })
        return self
    }
    func tapEnterVin() -> Self {
        Step("Нажимаем на поле VIN", step: {
            onWizardScreen().enterVINField.tap()
        })
        return self
    }

    func tapSkip() -> Self {
        Step("Нажимаем Пропустить", step: {
            onWizardScreen().skipButton.shouldExist()
            onWizardScreen().skipButton.tap()
        })
        return self
    }

    func tapBMW() -> Self {
        Step("Нажимаем ячейку BMW", step: {
            onWizardScreen().bmwCell.shouldExist()
            onWizardScreen().bmwCell.tap()
        })
        return self
    }

    func tapBMW6() -> Self {
        Step("Нажимаем ячейку BMW 6 серии", step: {
            onWizardScreen().bmw6Cell.shouldExist()
            onWizardScreen().bmw6Cell.tap()
        })
        return self
    }

    func tap2020() -> Self {
        Step("Нажимаем ячейку 2020 года выпуска", step: {
            onWizardScreen().bmw6_2020Cell.shouldExist()
            onWizardScreen().bmw6_2020Cell.tap()
        })
        return self
    }

    func tapRestyle() -> Self {
        Step("Нажимаем ячейку поколения Рестайлинг", step: {
            onWizardScreen().restyleCell.shouldExist()
            onWizardScreen().restyleCell.tap()
        })
        return self
    }

    func tapBody() -> Self {
        Step("Нажимаем ячейку кузова Лифтбек", step: {
            onWizardScreen().bodyCell.shouldExist()
            onWizardScreen().bodyCell.tap()
        })
        return self
    }

    func tapEngine() -> Self {
        Step("Нажимаем ячейку бензинового двигателя", step: {
            onWizardScreen().engineCell.shouldExist()
            onWizardScreen().engineCell.tap()
        })
        return self
    }

    func tapWheels() -> Self {
        Step("Нажимаем ячейку полного привода", step: {
            onWizardScreen().wheelsCell.shouldExist()
            onWizardScreen().wheelsCell.tap()
        })
        return self
    }

    func tapTransmission() -> Self {
        Step("Нажимаем ячейку автоматической коробки передач", step: {
            onWizardScreen().transmissionCell.shouldExist()
            onWizardScreen().transmissionCell.tap()
        })
        return self
    }

    func tapModification() -> Self {
        Step("Нажимаем ячейку модификации 333 лс", step: {
            onWizardScreen().modificationCell.shouldExist()
            onWizardScreen().modificationCell.tap()
        })
        return self
    }

    func tapColor() -> Self {
        Step("Нажимаем ячейку белого цвета", step: {
            onWizardScreen().colorCell.shouldExist()
            onWizardScreen().colorCell.tap()
        })
        return self
    }

    func tapPTS() -> Self {
        Step("Нажимаем ячейку оригинального птс", step: {
            onWizardScreen().ptsCell.shouldExist()
            onWizardScreen().ptsCell.tap()
        })
        return self
    }

    func tapOwner() -> Self {
        Step("Нажимаем ячейку одного владельца", step: {
            onWizardScreen().ownerCell.shouldExist()
            onWizardScreen().ownerCell.tap()
        })
        return self
    }

    func tapAddPhoto() -> Self {
        Step("Нажимаем кнопку-ячейку добавления фото", step: {
            onWizardScreen().addPhotoCell.shouldExist()
            onWizardScreen().addPhotoCell.tap()
        })
        return self
    }

    func tapAddPhotoInList() -> Self {
        Step("Нажимаем кнопку-ячейку добавления фото в списке", step: {
            onWizardScreen().addCell.shouldExist()
            onWizardScreen().addCell.tap()
        })
        return self
    }

    func tapRemoveFirstPhotoInList() -> Self {
        Step("Нажимаем кнопку удаления первого фото в списке", step: {
            onWizardScreen().removeFirstPhotoButton.shouldExist()
            onWizardScreen().removeFirstPhotoButton.tap()
        })
        return self
    }

    func tapManualPhotoOrder() -> Self {
        Step("Нажимаем кнопку ручного порядка фотографий", step: {
            onWizardScreen().manualOrderButton.shouldExist()
            onWizardScreen().manualOrderButton.tap()
        })
        return self
    }

    func tapBackToPhoto() -> Self {
        Step("Нажимаем кнопку возвращения на экран фотографий", step: {
            onWizardScreen().photoButton.shouldExist()
            onWizardScreen().photoButton.tap()
        })
        return self
    }

    func tapBack() -> Self {
        Step("Нажимаем кнопку назад в навбаре", step: {
            onWizardScreen().backButton.shouldExist()
            onWizardScreen().backButton.tap()
        })
        return self
    }

    func tapVinCameraButton() -> Self {
        Step("Нажимаем кнопку камеры на экране распознавания VIN", step: {
            onWizardScreen().vinCameraButton.shouldExist()
            onWizardScreen().vinCameraButton.tap()
        })
        return self
    }

    func addPhoto(index: Int = 0) -> Self {
        self.should(provider: .attachmentPicker, .exist).focus { picker in
            if index > 1 {
                for i in 1...index {
                    picker.should(.systemImage(i - 1), .exist).focus { $0.swipe(.left) }
                }
            }
            picker
                .tap(.systemImage(index))
                .tap(.send)
        }
        return self
    }

    func typeInActiveField(_ text: String) -> Self {
        // Нормальными способами достучаться до наших очень кастомных текстовых полей не получилось.
        let app = XCUIApplication.make()
        let keyboard = app.keyboards.firstMatch
        for letter in text {
            keyboard.keys[String(letter)].tap()
        }
        return self
    }

    func typeSpecialsInActiveField(_ text: String) -> Self {
        let app = XCUIApplication.make()
        let keyboard = app.keyboards.firstMatch
        keyboard.keys[text].tap()
        return self
    }

    func tapNext() -> Self {
        Step("Нажимаем Далее", step: {
            onWizardScreen().nextButton.shouldExist()
            onWizardScreen().nextButton.tap()
        })
        return self
    }

    func tapContinue() -> Self {
        Step("Нажимаем Продолжить", step: {
            onWizardScreen().continueButton.shouldExist()
            onWizardScreen().continueButton.tap()
        })
        return self
    }

    func tapClose() -> Self {
        Step("Нажимаем закрывающий крестик", step: {
            onWizardScreen().closeButton.shouldExist()
            onWizardScreen().closeButton.tap()
        })
        return self
    }

    func checkElementExistWithText(_ text: String) -> Self {
        onWizardScreen().find(by: text).firstMatch.shouldExist()
        return self
    }

    func checkPureTextExist(_ text: String) -> Self {
        onWizardScreen().findStaticText(by: text).firstMatch.shouldExist()
        return self
    }

    func checkElementExistWithPartText(_ text: String, not: Bool = false) -> Self {
        if not {
            onWizardScreen().findContainedText(by: text).firstMatch.shouldNotExist()
        } else {
            onWizardScreen().findContainedText(by: text).firstMatch.shouldExist()
        }
        return self
    }
    
    func checkDescriptionScreenshot(name: String) -> Self {
        // Да, тестировщики прямо хотели проверять экран целиком, поэтому скрин всего.
        let screen = onWizardScreen().descriptionScreen.waitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screen.image, identifier: name)
        return self
    }

    func checkDescriptionScreenContent() -> Self {
        onWizardScreen().find(by: "Описание").firstMatch.shouldExist()
        onWizardScreen().find(by: "Честно опишите достоинства и недостатки своего автомобиля").firstMatch.shouldExist()
        onWizardScreen().findContainedText(by: "Не указывайте почту, телефон, цену, адрес места осмотра и не предлагайте услуги — такое объявление не пройдёт модерацию").firstMatch.shouldExist()
        return self
    }

    func checkDistanceScreenContent(distance: String, tapSwitch: Bool) -> Self {
        onWizardScreen().find(by: "Пробег").firstMatch.shouldExist()
        onWizardScreen().find(by: "Пробег, км").firstMatch.shouldExist()
        onWizardScreen().find(by: "Битый или не на ходу").firstMatch.shouldExist()
        onWizardScreen().find(by: "textAndSwitcherInputView.inputLabel").firstMatch.containsText("1 000 000")
        // Да, тестировщики прямо хотели проверять экран целиком, поэтому скрин всего.
        let screen = onWizardScreen().distanceScreen.waitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screen.image, identifier: "wizard_distance_full_state")
        if tapSwitch {
            onWizardScreen().find(by: "textAndSwitcherInputView.switcher").firstMatch.tap()
        }
        return self
    }

    func chackNextButtonDontExist() -> Self {
        onWizardScreen().nextButton.shouldNotExist()
        return self
    }

    func checkPriceScreenContent() -> Self {
        onWizardScreen().ndsLabel.tap()
        let screenNDS = onWizardScreen().ndsDesc.waitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenNDS.image, identifier: "wizard_price_nds_desc")
        app.tap()
        onWizardScreen().exchangeLabel.tap()
        let screenExchange = onWizardScreen().popUpScreen.waitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenExchange.image, identifier: "wizard_price_exchange_desc")
        app.tap()
        onWizardScreen().exchangeSwitcher.firstMatch.tap()
        onWizardScreen().ndsSwitcher.tap()
        onWizardScreen().estimatedPriceLabel.tap()
        onWizardScreen().find(by: "Средняя цена  861 000 ₽").firstMatch.shouldExist()
        return self
    }

    @discardableResult
    func tapFinish() -> Self {
        step("Завершаем размещение") {
            onWizardScreen()
                .find(by: "Завершить")
                .firstMatch
                .tap()
        }
    }

    func publishNow() -> Self {
        XCUIApplication.make().swipeUp()
        Step("Нажимаем на кнопку \"Разместить бесплатно\"") {
            onWizardScreen()
                .find(by: "Разместить бесплатно")
                .firstMatch
                .tap()
        }
        return self
    }

    func publishLater() -> Self {
        let app = XCUIApplication.make()
        app.swipeUp()
        onWizardScreen().find(by: "Не публиковать сразу").firstMatch.tap()
        return self
    }
}
