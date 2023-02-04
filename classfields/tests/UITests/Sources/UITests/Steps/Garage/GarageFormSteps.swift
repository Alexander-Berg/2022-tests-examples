import Foundation
import XCTest
import Snapshots

final class GarageFormSteps: BaseSteps {
    @discardableResult
    func shouldSeeForm() -> Self {
        step("Проверяем, что показали форму") {
            let fields: [GarageFormScreen.Field] = [.mark, .model, .year]

            for field in fields {
                self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
                self.onGarageFormScreen().fieldAndPlaceholder(field).shouldExist()
            }
        }
    }

    @discardableResult
    func tapAndTypeText(field: GarageFormScreen.Field, text: String) -> Self {
        step("Вводим в поле '\(field.title)' текст '\(text)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let element = self.onGarageFormScreen().textField(field)
            element.tap()
            element.typeText(text)
        }
    }

    @discardableResult
    func hideKeyboard(field: GarageFormScreen.Field = .vin) -> Self {
        step("Скрываем клавиатуру по тапу на return") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let element = self.onGarageFormScreen().textField(field)
            element.tap()
            element.typeText("\n")
        }
    }

    @discardableResult
    func clearText(field: GarageFormScreen.Field) -> Self {
        step("Очищаем поле '\(field.title)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let element = self.onGarageFormScreen().textField(field)
            element.tap()
            element.clearText()
        }
    }

    @discardableResult
    func openSinglePicker(field: GarageFormScreen.Field) -> SingleValuePickerSteps {
        step("Открываем пикер по тапу на '\(field.title)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            self.onGarageFormScreen().fieldAndPlaceholder(field).tap()
        }
        .as(SingleValuePickerSteps.self)
    }

    @discardableResult
    func openDatePicker(field: GarageFormScreen.Field) -> DatePickerSteps {
        step("Открываем пикер по тапу на '\(field.title)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            self.onGarageFormScreen().fieldAndPlaceholder(field).tap()
        }
        .as(DatePickerSteps.self)
    }

    @discardableResult
    func tapOnSave() -> Self {
        step("Тапаем на сохранение") {
            self.onGarageFormScreen().scrollToSaveButton()
            self.onGarageFormScreen().saveButton.tap()
        }
    }

    @discardableResult
    func tapOnDelete() -> Self {
        step("Тапаем на удаление") {
            self.onGarageFormScreen().scrollToDeleteButton()
            self.onGarageFormScreen().deleteButton.tap()
        }
    }

    @discardableResult
    func checkText(field: GarageFormScreen.Field, text: String) -> Self {
        step("Проверяем, что в поле '\(field.title)' введено '\(text)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let textField = self.onGarageFormScreen().textField(field)
            var textFieldValue = textField.value as? String

            if text.isEmpty, textFieldValue == textField.placeholderValue {
                textFieldValue = ""
            }

            XCTAssert(
                textFieldValue == text,
                "Неверное значение в текстфилде: '\(textFieldValue ?? "<nil>")' вместо '\(text)'"
            )
        }
    }

    @discardableResult
    func checkValue(field: GarageFormScreen.Field, text: String) -> Self {
        step("Проверяем, что в поле '\(field.title)' выбрано '\(text)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let cell = self.onGarageFormScreen().fieldAndPlaceholder(field)
            let label = cell.staticTexts.matching(identifier: "garage_form_value_label").firstMatch

            if label.waitForExistence(timeout: 1) {
                return
            }

            XCTAssert(
                label.value as? String == text,
                "Неверное значение: <nil> вместо '\(text)'"
            )
        }
    }

    @discardableResult
    func checkSaveButtonState(isEnabled: Bool) -> Self {
        step("Скриншотим и проверяем состояние кнопки: \(isEnabled ? "активна" : "неактивна")") {
            self.onGarageFormScreen().scrollToSaveButton()
            self.onGarageFormScreen().swipe(.up)
            let screenshot = self.onGarageFormScreen().saveButton.waitAndScreenshot()
            Snapshot.compareWithSnapshot(
                image: screenshot.image,
                identifier: isEnabled ? "save_button_enabled" : "save_button_disabled"
            )
        }
    }

    @discardableResult
    func checkFieldSnapshot(_ field: GarageFormScreen.Field, snapshot: String) -> Self {
        step("Скриншотим поле '\(field.title)' и проверяем снепшот '\(snapshot)'") {
            self.onGarageFormScreen().scrollToFieldAndPlaceholder(field)
            let screenshot = self.onGarageFormScreen().fieldAndPlaceholder(field).waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: snapshot)
        }
    }

    @discardableResult
    func scrollTo(field: GarageFormScreen.Field) -> Self {
        step("Скроллим до поля '\(field.title)'") {
            let element = self.onGarageFormScreen().fieldAndPlaceholder(field)
            self.onGarageFormScreen().scrollTo(element: element, maxSwipes: 5)
        }
    }

    @discardableResult
    func checkFormErrorSnapshot(_ id: String) -> Self {
        step("Скриншотим ошибку на форме и проверяем снепшот '\(id)'") {
            let element = self.onGarageFormScreen().errorBlock
            self.onGarageFormScreen()
                .scrollTo(element: element, maxSwipes: 4, windowInsets: .init(top: 56, left: 0, bottom: 120, right: 0), swipeDirection: .down)
            let screenshot = element.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: id)
        }
    }

    @discardableResult
    func shouldNotSeeAddExtraFieldsBanner() -> Self {
        step("Проверяем отсутствие блока 'Укажите дополнительные параметры'") {
            onGarageFormScreen().scrollToExtraCarInfoBanner()
            onGarageFormScreen().addExtraCarInfoBanner.shouldNotExist()
        }
    }

    @discardableResult
    func shouldSeeAddExtraFieldsBanner() -> Self {
        step("Проверяем наличие блока 'Укажите дополнительные параметры'") {
            onGarageFormScreen().scrollToExtraCarInfoBanner()
            onGarageFormScreen().addExtraCarInfoBanner.shouldExist()
        }
    }

    @discardableResult
    func tapOnAddPhotoButton() -> Self {
        step("Тапаем на кнопку добавления фото") {
            onGarageFormScreen().addPhotoButton.tap()
        }
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        step("Тапаем на кнопку Назад") {
            self.onGarageFormScreen().backButton.tap()
        }
    }

    @discardableResult
    func checkPhoto(withID id: String) -> Self {
        step("Проверка наличия фото \(id) на форме") {
            onGarageFormScreen().photo(id).shouldExist()
        }
    }

    @discardableResult
    func checkAddPhotoButton() -> Self {
        step("Проверка наличия кнопки добавления фото") {
            onGarageFormScreen().addPhotoButton.shouldExist()
        }
    }

    @discardableResult
    func tapOnPhoto(withID id: String) -> GaragePhotoGridSteps {
        step("Тапаем на фото \(id) на форме") {
            onGarageFormScreen().photo(id).tap()
        }
        .as(GaragePhotoGridSteps.self)
    }

    // MARK: - Private

    private func onGarageFormScreen() -> GarageFormScreen {
        self.baseScreen.on(screen: GarageFormScreen.self)
    }
}

extension GarageFormSteps: UIElementProvider {
    enum Element {
        case provenOwnerCell
        case passVerificationButton
        case provenOwnerPhotoController
        case provenOwnerAddPhotoButton
        case attachmentPicker

        case createPanoramaButton
        case createNewPanoramaButton
        case createPanoramaView
        case panoramaError
        case panoramaProcessing
        case panoramaPopUp
        case delete
        case panoramaReshootMenu
        case field(String)
        case garageFormSuggest
        case saveButton
        case deleteButton
        case vin
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .provenOwnerCell:
            return "proven_owner_cell"
        case .passVerificationButton:
            return "pass_verification_button"
        case .provenOwnerPhotoController:
            return "proven_owner_photo_controller"
        case .provenOwnerAddPhotoButton:
            return "proven_owner_add_photo_button"
        case .attachmentPicker:
            return "AttachmentsPickerViewController"
        case.createPanoramaButton:
            return "Снять панораму"
        case .createNewPanoramaButton:
            return "Снять новую панораму"
        case .createPanoramaView:
            return "panorama_picker_view"
        case .panoramaError:
            return "Ошибка"
        case .panoramaProcessing:
            return "Обработка..."
        case .panoramaPopUp:
            return "Ошибка обработки"
        case .delete:
            return "Удалить"
        case .panoramaReshootMenu:
            return "Панорама обрабатывается"
        case .field(let title):
            return title
        case .garageFormSuggest:
            return "show_extra_car_info_banner"
        case .saveButton:
            return "garage_form_save_button"
        case .deleteButton:
            return "garage_form_delete_button"
        case .vin:
            return "vin_textfield"
        }
    }
}
