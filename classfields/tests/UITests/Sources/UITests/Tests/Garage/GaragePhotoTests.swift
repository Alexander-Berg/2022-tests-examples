import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

// дефолтные фото в симуляторе
private enum GalleryImages {
    struct GalleryImage {
        let systemId: String
        let id: String
    }

    static let image0 = GalleryImage(systemId: "image_cell_0", id: "CC95F08C-88C3-4012-9D6D-64A413D254B3/L0/001")
    static let image1 = GalleryImage(systemId: "image_cell_1", id: "ED7AC36B-A150-4C38-BB8C-B6D696F4F2ED/L0/001")
    static let image2 = GalleryImage(systemId: "image_cell_2", id: "99D53A1F-FEEF-40E1-8BB3-7DD55A43C8B7/L0/001")
}

final class GaragePhotoTests: BaseTest, KeyboardManaging {
    private lazy var mainSteps = MainSteps(context: self)
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    // BMW, добавляем вручную
    static let garageCard1: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")

    static let garageCard: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")

    // ГРЗ для ручного добавления
    static let govNumberManually = "Y111KA164"

    override func setUp() {
        super.setUp()
        self.setupServer()
    }

    func test_uploadingForManuallyAddedCar() {
        server.addHandler("GET /garage/user/vehicle_info/\(Self.garageCard1.vin)") { (_, _) -> Response? in
            Response.responseWith(code: "404", fileName: "garage_search_not_found", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_form_suggest_mark", userAuthorized: true)
        }

        let formScreen = openGarage()
            .should(provider: .garageCardScreen, .exist)
            .tap(.moreButton)
            .tap(.addCarButton)
            .should(provider: .garageAddCarScreen, .exist)
            .focus({
                $0.tap(.addByVin)
            })
            .should(provider: .garageSearchScreen, .exist)
            .focus {
                $0
                    .tap(.vinInputField)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.garageCard1.vin.lowercased()) }
                    .should(.bottomButton(.addManually), .be(.hidden))
                    .tap(.bottomButton(.search))
                    .wait(for: 1)
                    .should(.bottomButton(.addManually), .exist)
                    .tap(.bottomButton(.addManually))
            }
            .should(provider: .garageFormScreen, .exist)

        server.addMessageHandler("GET /garage/user/media/upload_url?upload_data_type=CAR_PHOTO") { _, index in
            Auto_Api_Vin_Garage_GarageUploadResponse.with { msg in
                msg.status = .success
                msg.uploadURL = self.makeUploadURL(withIndex: index)
            }
        }
        let cardIsSavedCondition = Condition(false)

        server.addMessageHandler("POST /mockUploader/0") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            print("Фото 0 загрузилось")

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image0.id
            }
        }

        // фото загружается после сохранения формы
        server.addMessageHandler("POST /mockUploader/1") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            cardIsSavedCondition.wait({ $0 })

            Thread.sleep(forTimeInterval: 1)

            print("Фото 1 загрузилось")

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image1.id
            }
        }

        server.addMessageHandler("POST /garage/user/card") { (request: Auto_Api_Vin_Garage_CreateCardRequest) -> Auto_Api_Vin_Garage_CreateCardResponse in
            cardIsSavedCondition.updateValueAndSignal(true)

            return .with { msg in
                msg.status = .success
                msg.card = request.card
                msg.card.id = Self.garageCard1.id
            }
        }

        let firstPhotoAdded = XCTestExpectation(description: "\(GalleryImages.image0) добавлено в карточку")
        let secondPhotoAdded = XCTestExpectation(description: "\(GalleryImages.image1) добавлено в карточку")

        server.addMessageHandler("PUT /garage/user/card/\(GaragePhotoTests.garageCard1.id)") { (request: Auto_Api_Vin_Garage_UpdateCardRequest) -> Auto_Api_Vin_Garage_UpdateCardResponse in

            let addedImages = request.card.vehicleInfo.vehicleImages.map(\.mdsPhotoInfo.name)
            if addedImages.contains(GalleryImages.image0.id) {
                firstPhotoAdded.fulfill()
            }

            if addedImages.contains(GalleryImages.image1.id) {
                secondPhotoAdded.fulfill()
            }

            if addedImages.count == 2 {
                XCTAssertEqual(addedImages, [GalleryImages.image0.id, GalleryImages.image1.id])
            }

            return .with { msg in
                msg.status = .success
                msg.card = request.card
            }
        }

        addFormSuggestionHandlers()

        formScreen.focus { screen in
            screen
                .tapOnAddPhotoButton()
        }
        .should(provider: .photoGrid, .exist).focus { grid in
            grid.tap(.addPhotos)
        }

        mainSteps.handleSystemAlertIfNeeded()
        mainSteps.handleSystemAlertIfNeeded()

        formScreen
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .scroll(to: .systemImage(1))
                    .tap(.systemImage(1))
                    .tap(.send)
            }
            .should(provider: .photoGrid, .exist).focus { grid in
                grid.tapOnBackButton()
            }
            .should(provider: .garageFormScreen, .exist).focus { formScreen in
                formScreen.openSinglePicker(field: .mark).selectValue(title: "BMW")
                formScreen.openSinglePicker(field: .model).selectValue(title: "5 серии")
                formScreen.openSinglePicker(field: .year).selectValue(title: "2018")
            }

        server.addHandler("GET /garage/user/card/\(Self.garageCard1.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard1.id)_manually_mocked")
        }

        formScreen.focus { $0.tapOnSave() }

        wait(for: [firstPhotoAdded, secondPhotoAdded], timeout: 10)
    }

    func test_showAddedPhotoInsteadOfCatalogImage() {
        var mutableCard = self.getGarageCard()

        let photoURLString = "https://localhost:\(self.port)/nonExistingPhoto"

        let mainPhotoName = "test"

        mutableCard.vehicleInfo.carInfo.configuration.mainPhoto.name = mainPhotoName
        mutableCard.vehicleInfo.carInfo.configuration.mainPhoto.sizes["mobile"] = String(photoURLString.dropFirst(6))

        server.addMessageHandler("POST /garage/user/cards") {
            Auto_Api_Vin_Garage_GetListingResponse.with { msg in
                msg.status = .success
                msg.listing = [mutableCard]
            }
        }

        server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { msg in
                msg.status = .success
                msg.card = mutableCard
            }
        }

        addFormSuggestionHandlers()

        self.launch()
        let formSteps = openGarage()
            .shouldSeeCard()
            .checkPhoto(withID: mainPhotoName)
            .tapOnEditButton()

        formSteps.tapOnAddPhotoButton()
            .should(provider: .photoGrid, .exist).focus { grid in
                grid.tap(.addPhotos)
            }

        mainSteps.handleSystemAlertIfNeeded()
        mainSteps.handleSystemAlertIfNeeded()

        formSteps
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .tap(.send)
            }
            .should(provider: .photoGrid, .exist).focus { grid in
                grid.tapOnBackButton()
            }
    }

    func test_showCatalogImageInsteadOfRemovedPhoto() {
        var mutableCard = self.getGarageCard()

        let photoURLString = "https://localhost:\(self.port)/nonExistingPhoto"

        let mainPhotoName = "test"

        mutableCard.vehicleInfo.carInfo.configuration.mainPhoto.name = mainPhotoName
        mutableCard.vehicleInfo.carInfo.configuration.mainPhoto.sizes["mobile"] = String(photoURLString.dropFirst(6))

        var photo = Auto_Api_Vin_Photo()
        photo.mdsPhotoInfo.name = GalleryImages.image0.id
        let addedPhotoURL = "https://localhost:\(self.port)/nonExisting0"
        photo.sizes["832x624"] = String(addedPhotoURL.dropFirst(6))

        mutableCard.vehicleInfo.vehicleImages.append(photo)

        server.addMessageHandler("POST /garage/user/cards") {
            Auto_Api_Vin_Garage_GetListingResponse.with { msg in
                msg.status = .success
                msg.listing = [mutableCard]
            }
        }

        server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { msg in
                msg.status = .success
                msg.card = mutableCard
            }
        }

        addFormSuggestionHandlers()

        self.launch()
        let formSteps = openGarage()
            .shouldSeeCard()
            .checkPhoto(withID: GalleryImages.image0.id)
            .tapOnEditButton()

        server.addMessageHandler("PUT /garage/user/card/\(GaragePhotoTests.garageCard.id)") { (request: Auto_Api_Vin_Garage_UpdateCardRequest) -> Auto_Api_Vin_Garage_UpdateCardResponse in

            mutableCard = request.card

            return .with { msg in
                msg.status = .success
                msg.card = request.card
            }
        }

        formSteps
            .tapOnPhoto(withID: GalleryImages.image0.id)
            .removePhoto(GalleryImages.image0.id)
            .tapOnBackButton()

        formSteps.checkAddPhotoButton()

        let cardSteps = formSteps.tapOnBackButton().as(GarageCardSteps.self)

        cardSteps.checkPhoto(withID: mainPhotoName)
    }

    func test_moveSavedPhoto() {
        var mutableCard = self.getGarageCard()

        var photo0 = Auto_Api_Vin_Photo()
        photo0.mdsPhotoInfo.name = GalleryImages.image0.id
        let addedPhoto0URL = "https://localhost:\(self.port)/nonExisting0"
        photo0.sizes["832x624"] = String(addedPhoto0URL.dropFirst(6))

        var photo1 = Auto_Api_Vin_Photo()
        photo1.mdsPhotoInfo.name = GalleryImages.image1.id
        let addedPhoto1URL = "https://localhost:\(self.port)/nonExisting1"
        photo1.sizes["832x624"] = String(addedPhoto1URL.dropFirst(6))

        mutableCard.vehicleInfo.vehicleImages = [photo0, photo1]

        server.addMessageHandler("POST /garage/user/cards") {
            Auto_Api_Vin_Garage_GetListingResponse.with { msg in
                msg.status = .success
                msg.listing = [mutableCard]
            }
        }

        server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { msg in
                msg.status = .success
                msg.card = mutableCard
            }
        }

        addFormSuggestionHandlers()

        self.launch()
        let formSteps = openGarage()
            .shouldSeeCard()
            .tapOnEditButton()

        let photosReorderedExpectation = XCTestExpectation()

        server.addMessageHandler("PUT /garage/user/card/\(GaragePhotoTests.garageCard.id)") { (request: Auto_Api_Vin_Garage_UpdateCardRequest) -> Auto_Api_Vin_Garage_UpdateCardResponse in

            mutableCard = request.card

            if request.card.vehicleInfo.vehicleImages.map(\.mdsPhotoInfo.name) == [GalleryImages.image1.id, GalleryImages.image0.id] {
                photosReorderedExpectation.fulfill()
            } else {
                XCTFail("wrong order of photos")
            }

            return .with { msg in
                msg.status = .success
                msg.card = request.card
            }
        }

        formSteps
            .tapOnPhoto(withID: GalleryImages.image0.id)
            .movePhoto(GalleryImages.image1.id, to: 0)
            .tapOnBackButton()

        formSteps.checkPhoto(withID: GalleryImages.image1.id)

        let cardSteps = formSteps.tapOnBackButton().as(GarageCardSteps.self)

        cardSteps.checkPhoto(withID: GalleryImages.image1.id)

        wait(for: [photosReorderedExpectation], timeout: 10)
    }

    /// кейс, когда перемещаемое фото ещё не добавлено в карточку
    func test_movePendingPhoto() {
        var mutableCard = self.getGarageCard()

        server.addMessageHandler("POST /garage/user/cards") {
            Auto_Api_Vin_Garage_GetListingResponse.with { msg in
                msg.status = .success
                msg.listing = [mutableCard]
            }
        }

        server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { msg in
                msg.status = .success
                msg.card = mutableCard
            }
        }

        addFormSuggestionHandlers()

        self.launch()
        let formSteps = openGarage()
            .shouldSeeCard()
            .tapOnEditButton()

        let photosReorderedExpectation = XCTestExpectation()

        server.addMessageHandler("PUT /garage/user/card/\(GaragePhotoTests.garageCard.id)") { (request: Auto_Api_Vin_Garage_UpdateCardRequest) -> Auto_Api_Vin_Garage_UpdateCardResponse in

            mutableCard = request.card

            let ids = request.card.vehicleInfo.vehicleImages.map(\.mdsPhotoInfo.name)
            if ids.count == 3 {
                if ids == [GalleryImages.image2.id, GalleryImages.image0.id, GalleryImages.image1.id] {
                    photosReorderedExpectation.fulfill()
                } else {
                    XCTFail("wrong order of photos")
                }
            }

            return .with { msg in
                msg.status = .success
                msg.card = request.card
            }
        }

        server.addMessageHandler("GET /garage/user/media/upload_url?upload_data_type=CAR_PHOTO") { _, index in
            Auto_Api_Vin_Garage_GarageUploadResponse.with { msg in
                msg.status = .success
                msg.uploadURL = self.makeUploadURL(withIndex: index)
            }
        }

        // откладываем сохранение первого фото, чтобы перемещаемое фото не попало в карточку.
        let photosReorderedCondition = Condition(false)
        let photo0UploadedExpectation = XCTestExpectation()
        let photo1UploadedExpectation = XCTestExpectation()
        let photo2UploadedExpectation = XCTestExpectation()

        server.addMessageHandler("POST /mockUploader/0") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            photosReorderedCondition.wait({ $0 })

            print("Фото 0 загрузилось")
            photo0UploadedExpectation.fulfill()

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image0.id
            }
        }

        server.addMessageHandler("POST /mockUploader/1") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            print("Фото 1 загрузилось")
            photo1UploadedExpectation.fulfill()

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image1.id
            }
        }

        server.addMessageHandler("POST /mockUploader/2") { () -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            print("Фото 2 загрузилось")
            photo2UploadedExpectation.fulfill()

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image2.id
            }
        }

        formSteps
            .tapOnAddPhotoButton()
            .should(provider: .photoGrid, .exist).focus { grid in
                grid.tap(.addPhotos)
            }

        mainSteps.handleSystemAlertIfNeeded()

        formSteps
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .tap(.systemImage(0))
                    .scroll(to: .systemImage(1))
                    .tap(.systemImage(1))
                    .scroll(to: .systemImage(2))
                    .tap(.systemImage(2))
                    .tap(.send)
            }
            .wait(for: 2)
            .should(provider: .photoGrid, .exist).focus { grid in
                grid
                    .movePhoto(GalleryImages.image1.id, to: 2)
                    .movePhoto(GalleryImages.image2.id, to: 0)
                    .tapOnBackButton()
            }

        photosReorderedCondition.updateValueAndSignal(true)

        formSteps.checkPhoto(withID: GalleryImages.image2.id)

        wait(
            for: [
                photosReorderedExpectation,
                photo0UploadedExpectation,
                photo1UploadedExpectation,
                photo2UploadedExpectation
            ],
            timeout: 10
        )
    }

    func test_retryUploadingForFailedPhoto() {
        var mutableCard = self.getGarageCard()

        server.addMessageHandler("POST /garage/user/cards") {
            Auto_Api_Vin_Garage_GetListingResponse.with { msg in
                msg.status = .success
                msg.listing = [mutableCard]
            }
        }

        server.addMessageHandler("GET /garage/user/card/\(Self.garageCard.id)") {
            Auto_Api_Vin_Garage_GetCardResponse.with { msg in
                msg.status = .success
                msg.card = mutableCard
            }
        }

        addFormSuggestionHandlers()

        self.launch()

        let formSteps = openGarage()
            .shouldSeeCard()
            .tapOnEditButton()

        let photoAddedToCardExpectation = XCTestExpectation()

        server.addMessageHandler("PUT /garage/user/card/\(GaragePhotoTests.garageCard.id)") { (request: Auto_Api_Vin_Garage_UpdateCardRequest) -> Auto_Api_Vin_Garage_UpdateCardResponse in

            mutableCard = request.card

            if request.card.vehicleInfo.vehicleImages.map(\.mdsPhotoInfo.name) == [GalleryImages.image0.id] {
                photoAddedToCardExpectation.fulfill()
            }

            return .with { msg in
                msg.status = .success
                msg.card = request.card
            }
        }

        server.addMessageHandler("GET /garage/user/media/upload_url?upload_data_type=CAR_PHOTO") {
            Auto_Api_Vin_Garage_GarageUploadResponse.with { msg in
                msg.status = .success
                msg.uploadURL = self.makeUploadURL(withIndex: 0)
            }
        }

        server.addMessageHandler("POST /mockUploader/0") { (_, index) -> Auto_Api_Vin_Garage_GarageUploadedPhotoResponse in
            if index == 0 {
                print("Загрузка фото зафэйлилась")

                return .with { msg in
                    msg.responseStatus = .error
                }
            }

            print("Фото загрузилось")

            return .with { msg in
                msg.responseStatus = .success
                msg.photo.mdsPhotoInfo.name = GalleryImages.image0.id
            }
        }

        formSteps
            .tapOnAddPhotoButton()
            .should(provider: .photoGrid, .exist).focus { grid in
                grid.tap(.addPhotos)
            }

        mainSteps.handleSystemAlertIfNeeded()

        formSteps
            .should(provider: .attachmentPicker, .exist).focus { picker in
                picker
                    .should(.systemImage(0), .exist)
                    .tap(.systemImage(0))
                    .tap(.send)
            }
            .should(provider: .photoGrid, .exist).focus { grid in
                grid
                    .checkFailedPhoto(GalleryImages.image0.id)
                    .tapOnPhoto(GalleryImages.image0.id)
                    .tapOnBackButton()
            }

        formSteps.checkPhoto(withID: GalleryImages.image0.id)

        wait(for: [photoAddedToCardExpectation], timeout: 10)
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user *") { [weak self] _, _ in
            guard let strongSelf = self else {
                return Response.badResponse(code: Auto_Api_ErrorCode.badRequest)
            }
            return Response.responseWithStatus(body: try! strongSelf.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        server.addHandler("GET /session") { (_, _) -> Response? in
            Response.okResponse(fileName: "session", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard.id)", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/rating *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        try! server.start()
    }

    private func openGarage() -> GarageCardScreen_ {
        return launch(on: .mainScreen) { screen in
            screen.toggle(to: .garage)
            return screen.should(provider: .garageCardScreen, .exist)
        }
    }

    private func makeUploadURL(withIndex index: Int) -> String {
        "http://localhost:\(self.port)/mockUploader/\(index)"
    }

    private func addFormSuggestionHandlers() {
        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW") { _, _ in
            .okResponse(fileName: "garage_form_suggest_model")
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER") { _, _ in
            .okResponse(fileName: "garage_form_suggest_year")
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER&year=2018") { _, _ in
            .okResponse(fileName: "garage_form_suggest_generation")
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?mark=BMW&model=5ER&super_gen=20856169&year=2018") { _, _ in
            .okResponse(fileName: "garage_form_suggest_body")
        }
    }

    private func getGarageCard() -> Auto_Api_Vin_Garage_Card {
        let fileName = "garage_card_\(Self.garageCard.id)_manually_mocked"
        let url = Bundle.resources.url(forResource: fileName, withExtension: "json", subdirectory: nil)
        var card = try! Auto_Api_Vin_Garage_GetCardResponse(jsonUTF8Data: Data(contentsOf: url!)).card
        card.vehicleInfo.carInfo.superGenID = 20856169
        card.vehicleInfo.carInfo.superGen.id = 20856169
        card.vehicleInfo.carInfo.superGen.name = "VII (G30/G31)"
        return card
    }
}
