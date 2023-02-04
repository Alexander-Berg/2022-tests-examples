import XCTest
import AutoRuProtoModels
import Snapshots
import SwiftProtobuf

final class InsuranceFormTests: BaseTest {
    private let garageCardId = "1955418404"
    private var insurances: [Auto_Api_Vin_Garage_Insurance] = [
        .with { insurance in
            let year: TimeInterval = 60 * 60 * 24 * 365
            insurance.insuranceType = .osago
            insurance.isActual = true
            insurance.from = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(year))
            insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(2 * year))
            insurance.company = .with { company in
                company.name = "ПАО СК «Росгосстрах»"
                company.phoneNumber = "+7 (911) 123-45-67"
            }
            insurance.serial = "XXX"
            insurance.number = "1234567890"
        }
    ]
    private let policySerial = "AAB"
    private let policyNumber = "0123456789"
    private let companyName = "Рога и копыта"
    private let companyPhone = "+7 999 123-45-67"
    private lazy var currentYear: Int = {
        let components = Calendar.current.dateComponents([.year], from: Date())
        return components.year ?? -1
    }()

    private var updateGarageCardExpectation: XCTestExpectation {
        func checkInsurance(_ insurance: Auto_Api_Vin_Garage_Insurance) -> Bool {
            let fromComponents = Calendar.current.dateComponents([.day, .month, .year], from: insurance.from.date)
            let toComponents = Calendar.current.dateComponents([.day, .month, .year], from: insurance.to.date)

            return
                insurance.insuranceType == .osago &&
                insurance.company.name == companyName &&
                insurance.company.phoneNumber == companyPhone &&
                insurance.serial == policySerial &&
                insurance.number == policyNumber &&
                fromComponents.day == 19 &&
                fromComponents.month == 1 &&
                fromComponents.year == currentYear &&
                toComponents.day == 9 &&
                toComponents.month == 1 &&
                toComponents.year == currentYear + 1
        }

        return
            api.garage.user.card.cardId(garageCardId).put.expect() { request, _ in
                self.insurances = request.card.insuranceInfo.insurances

                for i in self.insurances.indices {
                    self.insurances[i].isActual = true
                }

                return request.card.insuranceInfo.insurances
                    .contains(where: { checkInsurance($0) })
                ? .ok
                : .fail(reason: "Saved insurance fields invalid")
            }
    }

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func setupServer() {
        api.garage.user.cards
            .post
            .ok(mock: .file("garage_cards_\(garageCardId)") { response in
                response.listing[0].insuranceInfo.insurances = self.insurances
            })

        api.garage.user.card.cardId(garageCardId)
            .get(parameters: .parameters([]))
            .ok(mock: .file("garage_card_\(garageCardId)_manually_mocked_filled") { response in
                response.card.insuranceInfo.insurances = self.insurances
            })

        api.user
            .get(parameters: .wildcard)
            .ok(mock: .model( .with { _ in }))

        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func openInsuranceForm() -> InsuranceFormScreen {
        launchMain(
            options: .init(overrideAppSettings: ["insuranceStubPhoto" : true])
        ) { screen in
            screen
                .toggle(to: \.garage)
                .wait(for: 1)
                .should(provider: .garageCardScreen, .exist)
                .focus { garageCard in
                    garageCard
                        .scroll(to: .insuranceHeader)
                        .tap(.insuranceHeader)
                        .should(.insurances, .exist)
                        .focus(on: .insuranceRow(0), ofType: .insuranceRowCell) {
                            $0.tap()
                        }
                }
                .should(provider: .insuranceCardScreen, .exist).focus { card in
                    card.tap(.edit)
                }
                .should(provider: .insuranceFormScreen, .exist)
        }
    }

    func test_addNewInsurance() {
        Step("Проверяем добавление новой страховки")

        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))

        launchMain { screen in
            screen
                .toggle(to: \.garage)
                .should(provider: .garageCardScreen, .exist)
                .focus { garageCard in
                    garageCard
                        .scroll(to: .insuranceHeader)
                        .tap(.insuranceHeader)
                        .should(.insurances, .exist)
                        .tap(.insuranceAdd)
                }
                .should(provider: .actionsMenuPopup, .exist).focus { menu in
                    menu.tap(.osago)
                }
                .should(provider: .insuranceFormScreen, .exist)
        }
        .should(.saveButton, .exist)
        .tap(.policyEdit)
        .type(policySerial + policyNumber, in: .policyEdit)
        .should(.policyEdit, .match("\(policySerial) \(policyNumber)"))
        .tap(.validFrom)
        .should(provider: .datePicker, .exist).focus { picker in
            picker.select(components: "19", "Января", "\(currentYear)")
            picker.tap(.doneButton)
        }
        .should(.validTill, .match("18.01.\(currentYear + 1)"))
        .tap(.validTill)
        .should(provider: .datePicker, .exist).focus { picker in
            picker.select(components: "9", "Января", "\(currentYear + 1)")
            picker.tap(.doneButton)
        }
        .tap(.companyPhone)
        .type(companyPhone.filter("0123456789".contains), in: .companyPhone)
        .should(.companyPhone, .match(companyPhone))
        .tap(.companyName)
        .type(companyName, in: .companyName)
        .tap(.saveButton)
        .wait(for: [updateGarageCardExpectation], timeout: 5)
        .wait(for: 1)
        .log("Проверяем обновленное содержимое на карточке гаража в блоке страховок")
        .should(provider: .garageCardScreen, .exist).focus { garageCard in
            garageCard
                .scroll(to: .insuranceRow(1))
                .focus(on: .insuranceRow(1), ofType: .insuranceRowCell) { row in
                    row.should(.title, .match("ОСАГО до 09.01.\((currentYear + 1) % 100)"))
                    row.should(.subtitle, .match("\(policySerial) \(policyNumber)"))
                }
        }
    }

    func test_editInsurance() {
        Step("Проверяем редактирование существующей страховки")

        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))

        openInsuranceForm()
            .log("Меняем поля")
            .should(.saveButton, .exist)
            .tap(.policyEdit)
            .clearText(in: .policyEdit)
            .type(policySerial + policyNumber, in: .policyEdit)
            .tap(.validFrom)
            .should(provider: .datePicker, .exist).focus { picker in
                picker.tap(.resetButton)
            }
            .tap(.validTill)
            .should(provider: .datePicker, .exist).focus { picker in
                picker.tap(.resetButton)
            }
            .tap(.validFrom)
            .should(provider: .datePicker, .exist).focus { picker in
                picker.select(components: "19", "Января", "\(currentYear)")
                picker.tap(.doneButton)
            }
            .should(.validTill, .match("18.01.\(currentYear + 1)"))
            .tap(.validTill)
            .should(provider: .datePicker, .exist).focus { picker in
                picker.select(components: "9", "Января", "\(currentYear + 1)")
                picker.tap(.doneButton)
            }
            .tap(.companyPhone)
            .clearText(in: .companyPhone)
            .type(companyPhone.filter("0123456789".contains), in: .companyPhone)
            .tap(.companyName)
            .clearText(in: .companyName)
            .type(companyName, in: .companyName)
            .tap(.saveButton)
            .wait(for: [updateGarageCardExpectation], timeout: 5)
            .log("Проверяем обновленное содержимое на карточке страховки")
            .should(provider: .insuranceCardScreen, .exist).focus { [self] insuranceCard in
                insuranceCard
                    .should(.field(companyName), .exist)
                    .should(.field(companyPhone), .exist)
                    .should(.field("\(policySerial) \(policyNumber)"), .exist)
                    .should(.field("19.01.\(currentYear)"), .exist)
                    .should(.field("09.01.\(currentYear + 1)"), .exist)
                    .tap(.close)
            }
            .log("Проверяем обновленное содержимое на карточке гаража в блоке страховок")
            .should(provider: .garageCardScreen, .exist).focus { garageCard in
                garageCard
                    .scroll(to: .insuranceRow(0))
                    .focus(on: .insuranceRow(0), ofType: .insuranceRowCell) { row in
                        row.should(.title, .match("ОСАГО до 09.01.\((currentYear + 1) % 100)"))
                        row.should(.subtitle, .match("\(policySerial) \(policyNumber)"))
                    }
            }
    }

    func test_removeInsurance() {
        Step("Проверяем удаление существующей страховки")

        let removeExpectation = api.garage.user.card.cardId(garageCardId).put.expect() { request, _ in
            self.insurances = []
            return request.card.insuranceInfo.insurances.isEmpty ? .ok : .fail(reason: "Invalid insurance list")
        }

        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))

        openInsuranceForm()
            .tap(.removeButton)
            .tap(.removeAlertButton)
            .wait(for: [removeExpectation], timeout: 5)
            .should(provider: .garageCardScreen, .exist).focus { garageCard in
                garageCard
                    .scroll(to: .addInsuranceHeader, direction: .down)
                    .should(.addInsuranceHeader, .exist)
            }
    }

    func test_removeInsuranceError() {
        Step("Проверяем удаление существующей страховки с ошибкой")

        let errorText = "It's a very bad request"

        let removeExpectation = api.garage.user.card.cardId(garageCardId).put.expect()

        api.garage.user.card.cardId(garageCardId).put
            .error(status: ._400, mock: .model(Auto_Api_ErrorResponse.with { response in
                response.status = .error
                response.error = .badRequest
                response.detailedError = errorText
            }))

        openInsuranceForm()
            .tap(.removeButton)
            .tap(.removeAlertButton)
            .wait(for: [removeExpectation], timeout: 5)
            .do {
                app.staticTexts[errorText].shouldExist()
            }
    }

    func test_insuranceFormUploadPhoto() {
        let uploadPhotoExpectation = XCTestExpectation(description: "Загрузили фото на сервачок")
        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))
        let updateGarageCardExpectation = api.garage.user.card.cardId(garageCardId).put.expect()

        openInsuranceForm()
            .log("Проверяем неудачную попытку загрузки фото")
            .tap(.uploadButton)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.chooseFromGallery)
            }
            .wait(for: 5)
            .focus(on: .uploadButton) { button in
                button.validateSnapshot(snapshotId: "insurance_card_upload_failed")
            }
            .should(.photoPreview, .be(.hidden))
            .log("Проверяем повторную загрузку, должна тоже зафейлиться")
            .tap(.uploadButton)
            .wait(for: 5)
            .focus(on: .uploadButton) { button in
                button.validateSnapshot(snapshotId: "insurance_card_upload_failed")
            }
            .log("Мокаем ручки для успешных ответов загрузки фото")
            .do {
                let uploadUrlResponse: Auto_Api_Vin_Garage_GarageUploadResponse = .with { response in
                    response.status = .success
                    response.uploadURL = "http://localhost:\(self.port)/insurancePhotoUpload"
                }

                api.garage.user.media.uploadUrl
                    .get(parameters: .parameters([.uploadDataType("INSURANCE_PHOTO")]))
                    .ok(mock: .model(uploadUrlResponse))

                server.addMessageHandler("POST /insurancePhotoUpload") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
                    uploadPhotoExpectation.fulfill()

                    return .with { msg in
                        msg.responseStatus = .success
                        msg.photo.sizes = ["100x100": "http://yachan.ru/bbs"]
                    }
                }
            }
            .log("Повторная загрузка с успешным результатом")
            .tap(.uploadButton)
            .wait(for: [uploadPhotoExpectation, updateGarageCardExpectation], timeout: 5)
            .should(.uploadButton, .be(.hidden))
            .should(.photoPreview, .exist)
    }

    func test_chooseDocumentForUpload() {
        openInsuranceForm()
            .log("Проверяем выбор файла для загрузки")
            .tap(.uploadButton)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.chooseFile)
            }
            .wait(for: 2)
            .should(.documentPicker, .exist)
    }

    func test_insuranceFormWithPhotoAttachment() {
        insurances[0].attachment = .with { attachment in
            attachment.content = .image(.with { photo in
                photo.sizes = ["100x100": "http://ya.ru/"]
            })
        }

        let updateGarageCardExpectation = api.garage.user.card.cardId(garageCardId).put.expect()
        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))

        openInsuranceForm()
            .log("Проверяем карточку страховки с фотоаттачментом")
            .should(.uploadButton, .be(.hidden))
            .tap(.photoPreview)
            .should(provider: .galleryScreen, .exist).focus { gallery in
                gallery.tap(.closeButton)
            }
            .log("Проверяем возможность переснять фото")
            .tap(.retakePhoto)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.dismissButton)
            }
            .log("Проверяем возможность удалить фото")
            .tap(.removePhoto)
            .tap(.removeAlertButton)
            .wait(for: [updateGarageCardExpectation], timeout: 5)
            .should(.photoPreview, .be(.hidden))
            .should(.uploadButton, .exist)
    }

    func test_insuranceFormWithPdfAttachment() {
        insurances[0].attachment = .with { attachment in
            attachment.content = .file(.with { file in
                file.url = "https://ya.ru"
            })
        }

        openInsuranceForm()
            .log("Проверяем карточку страховки с pdf-аттачментом")
            .focus(on: .uploadButton) { button in
                button.validateSnapshot(snapshotId: "insurance_card_has_document")
            }
            .tap(.uploadButton)
            .should(provider: .webViewPicker, .exist).focus { picker in
                picker.tap(.closeButton)
            }
    }
}
