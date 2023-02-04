import XCTest
import AutoRuProtoModels
import Snapshots
import SwiftProtobuf

final class InsuranceCardTests: BaseTest {
    private let garageCardId = "1955418404"
    private var insurances: [Auto_Api_Vin_Garage_Insurance] = [
        .with { insurance in
           insurance.insuranceType = .osago
           insurance.isActual = true
           insurance.from = Google_Protobuf_Timestamp(date: Date())
           insurance.to = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(60 * 60 * 24 * 365))
           insurance.company = .with { company in
               company.name = "ПАО СК «Росгосстрах»"
               company.phoneNumber = "+7 (911) 123-45-67"
           }
           insurance.serial = "XXX"
           insurance.number = "1234567890"
        }
    ]

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

    func openInsuranceCard() -> InsuranceCardScreen {
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
                .should(provider: .insuranceCardScreen, .exist)
        }
    }

    func test_insuranceCardUploadPhoto() {
        let uploadPhotoExpectation = XCTestExpectation(description: "Загрузили фото на сервачок")
        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))
        let updateGarageCardExpectation = api.garage.user.card.cardId(garageCardId).put.expect()
        
        openInsuranceCard()
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
        openInsuranceCard()
            .log("Проверяем выбор файла для загрузки")
            .tap(.uploadButton)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.tap(.chooseFile)
            }
            .wait(for: 2)
            .should(.documentPicker, .exist)
    }

    func test_insuranceCardWithPhotoAttachment() {
        insurances[0].attachment = .with { attachment in
            attachment.content = .image(.with { photo in
                photo.sizes = ["100x100": "http://ya.ru/"]
            })
        }

        let updateGarageCardExpectation = api.garage.user.card.cardId(garageCardId).put.expect()
        api.garage.user.card.cardId(garageCardId).put.ok(mock: .model( .with { _ in }))

        openInsuranceCard()
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
            .tap(.removePhotoAlertButton)
            .wait(for: [updateGarageCardExpectation], timeout: 5)
            .should(.photoPreview, .be(.hidden))
            .should(.uploadButton, .exist)
    }

    func test_insuranceCardWithPdfAttachment() {
        insurances[0].attachment = .with { attachment in
            attachment.content = .file(.with { file in
                file.url = "https://ya.ru"
            })
        }

        openInsuranceCard()
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
