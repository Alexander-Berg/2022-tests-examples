import AutoRuProtoModels
import XCTest

protocol WizardTraversable: BaseTest, KeyboardManaging { }

struct WizardTraverseFinish<T: UIRootedElementProvider> {
    let provider: T.Type
}

extension WizardTraverseFinish where T == WizardVINPicker {
    static let vin = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardMarkPicker {
    static let mark = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardModelPicker {
    static let model = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardGovNumberPicker {
    static let govNumber = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardYearPicker {
    static let year = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardGenerationPicker {
    static let generation = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardBodyTypePicker {
    static let body = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardEngineTypePicker {
    static let engine = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardDriveTypePicker {
    static let drive = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardTransmissionTypePicker {
    static let transmission = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardModificationPicker {
    static let modification = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardOwnersCountPicker {
    static let owners = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardPTSPicker {
    static let pts = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardDescriptionPanoramaPicker {
    static let panoramaDescription = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardPhotoPicker {
    static let photos = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardDescriptionPicker {
    static let description = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardMileagePicker {
    static let mileage = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardPhonesPicker {
    static let phones = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardContactsPicker {
    static let contacts = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardProvenOwnerPicker {
    static let provenOwner = Self(provider: T.self)
}

extension WizardTraverseFinish where T == WizardPricePicker {
    static let price = Self(provider: T.self)
}

struct WizardTraverseAction {
    let mark = "BMW"
    let model = "6 серии"
    let year = "2020"
    let generation = "с 2020 по 2020, IV (G32) Рестайлинг"
    let body = "Лифтбек"
    let engine = "Бензин"
    let drive = "Полный"
    let transmission = "Автоматическая"
    let modification = "640i xDrive / 3.0 / AT / Бензин / 333 л.с."
    let color = "Белый"
    let pts = "Оригинал/Электронный"
    let owners = "Один"
    let mileage = "20000"

    // Некоторые пикеры в визарде можно проскакивать
    let hasMark = true
    let hasModel = true
    let hasYear = true
    let hasGeneration = true
    let hasBody = true
    let hasModification = true
}

extension WizardTraversable {
    func traverseWizard<T: UIRootedElementProvider>(
        for draftID: String,
        from wizardScreen: WizardScreen_,
        to: WizardTraverseFinish<T>,
        action: WizardTraverseAction = .init()
    ) {
        var matched = false

        wizardScreen
            .step("Пропускаем алерт, если нужно") { $0
                .handleSystemAlertIfNeeded(allowButtons: ["OK"])
            }

        do {
            wizardScreen
                .step("Проходим пикер ВИНа") { s in s
                    .should(provider: .wizardVINPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер ГРЗ") { s in s
                    .should(provider: .wizardGovNumberPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        if action.hasMark {
            wizardScreen
                .step("Проходим пикер марки") { s in s
                    .should(provider: .wizardMarkPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.mark))
                        }
                    }
                }

            if matched { return }
        }

        if action.hasModel {
            wizardScreen
                .step("Проходим пикер модели") { s in s
                    .should(provider: .wizardModelPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.model))
                        }
                    }
                }

            if matched { return }
        }

        if action.hasYear {
            wizardScreen
                .step("Проходим пикер года") { s in s
                    .should(provider: .wizardYearPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.year))
                        }
                    }
                }

            if matched { return }
        }

        if action.hasGeneration {
            wizardScreen
                .step("Проходим пикер поколения") { s in s
                    .should(provider: .wizardGenerationPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.generation))
                        }
                    }
                }

            if matched { return }
        }

        if action.hasBody {
            wizardScreen
                .step("Проходим пикер кузова") { s in s
                    .should(provider: .wizardBodyTypePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.body))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер двигателя") { s in s
                    .should(provider: .wizardEngineTypePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.engine))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер привода") { s in s
                    .should(provider: .wizardDriveTypePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.drive))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер трансмиссии") { s in s
                    .should(provider: .wizardTransmissionTypePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.transmission))
                        }
                    }
                }

            if matched { return }
        }

        if action.hasModification {
            wizardScreen
                .step("Проходим пикер модификации") { s in s
                    .should(provider: .wizardModificationPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.modification))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер цвет") { s in s
                    .should(provider: .wizardColorPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.color))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер ПТС") { s in s
                    .should(provider: .wizardPTSPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.pts))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер владельцев") { s in s
                    .should(provider: .wizardOwnersCountPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen.tap(.wizardItem(action.owners))
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер с панорамой") { s in s
                    .should(provider: .wizardDescriptionPanoramaPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер фото") { s in s
                    .should(provider: .wizardPhotoPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.addPhotoButton) }
                                .should(provider: .attachmentPicker, .exist)
                                .focus { $0
                                    .step("Пропускаем алерт с пермишенами") { pickerScreen in
                                        pickerScreen.handleSystemAlertIfNeeded()
                                    }
                                    .tap(.systemImage(0))
                                    .tap(.send)
                                }
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.nextButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер с описанием") { s in s
                    .should(provider: .wizardDescriptionPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .wait(for: 2) // экран открывается с фокусом на поле ввода, нужно подождать клавиатуру
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер с пробегом") { s in s
                    .should(provider: .wizardMileagePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            self.typeFromKeyboard(action.mileage)
                            
                            screen.should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.nextButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер с телефонами") { s in s
                    .should(provider: .linkPhoneScreen, .exist)
                    .focus { $0.tap(.closeButton) }
                    .should(provider: .wizardPhonesPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.continueButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер контактов") { s in s
                    .should(provider: .wizardContactsPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.nextButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер проверенного собственника") { s in s
                    .should(provider: .wizardProvenOwnerPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("Проходим пикер цены") { s in s
                    .should(provider: .wizardPricePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        }
                    }
                }
        }

        if !matched { XCTFail("Не удалось дойти до нужного экрана визарда") }
    }

    func setupMocksForWizard(draftID: String) {
        mocker
            .mock_base()
            .mock_user()
            .mock_wizardReferenceCatalogCars()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_wizardPhotoUpload()
            .mock_wizardUserAuthData()

        api.user.offers.category(._unknown("all"))
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()))

        api.user.draft.category(.cars).get
            .ok(
                mock: .file(
                    "GET user_draft_empty_wizard",
                    mutation: { model in
                        model.offerID = draftID
                        model.offer.id = draftID
                        model.offer.seller.phones = [.with { $0.phone = "79001112233" }]
                    }
                )
            )

        api.user.draft.category(.cars).offerId(draftID).get
            .ok(
                mock: .file(
                    "GET user_draft_empty_wizard",
                    mutation: { model in
                        model.offerID = draftID
                        model.offer.id = draftID
                        model.offer.seller.phones = [.with { $0.phone = "79001112233" }]
                    }
                )
            )

        api.user.draft.category(.cars).offerId(draftID).put
            .ok(
                mock: .dynamic { req, offer in
                    Auto_Api_DraftResponse.with {
                        $0.offer = offer
                    }
                }
            )
    }
}
