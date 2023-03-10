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
    let model = "6 ??????????"
    let year = "2020"
    let generation = "?? 2020 ???? 2020, IV (G32) ????????????????????"
    let body = "??????????????"
    let engine = "????????????"
    let drive = "????????????"
    let transmission = "????????????????????????????"
    let modification = "640i xDrive / 3.0 / AT / ???????????? / 333 ??.??."
    let color = "??????????"
    let pts = "????????????????/??????????????????????"
    let owners = "????????"
    let mileage = "20000"

    // ?????????????????? ???????????? ?? ?????????????? ?????????? ????????????????????????
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
            .step("???????????????????? ??????????, ???????? ??????????") { $0
                .handleSystemAlertIfNeeded(allowButtons: ["OK"])
            }

        do {
            wizardScreen
                .step("???????????????? ?????????? ????????") { s in s
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
                .step("???????????????? ?????????? ??????") { s in s
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
                .step("???????????????? ?????????? ??????????") { s in s
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
                .step("???????????????? ?????????? ????????????") { s in s
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
                .step("???????????????? ?????????? ????????") { s in s
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
                .step("???????????????? ?????????? ??????????????????") { s in s
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
                .step("???????????????? ?????????? ????????????") { s in s
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
                .step("???????????????? ?????????? ??????????????????") { s in s
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
                .step("???????????????? ?????????? ??????????????") { s in s
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
                .step("???????????????? ?????????? ??????????????????????") { s in s
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
                .step("???????????????? ?????????? ??????????????????????") { s in s
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
                .step("???????????????? ?????????? ????????") { s in s
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
                .step("???????????????? ?????????? ??????") { s in s
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
                .step("???????????????? ?????????? ????????????????????") { s in s
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
                .step("???????????????? ?????????? ?? ??????????????????") { s in s
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
                .step("???????????????? ?????????? ????????") { s in s
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
                                    .step("???????????????????? ?????????? ?? ??????????????????????") { pickerScreen in
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
                .step("???????????????? ?????????? ?? ??????????????????") { s in s
                    .should(provider: .wizardDescriptionPicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        } else {
                            screen
                                .should(provider: .wizardScreen, .exist)
                                .wait(for: 2) // ?????????? ?????????????????????? ?? ?????????????? ???? ???????? ??????????, ?????????? ?????????????????? ????????????????????
                                .focus { $0.tap(.skipButton) }
                        }
                    }
                }

            if matched { return }
        }

        do {
            wizardScreen
                .step("???????????????? ?????????? ?? ????????????????") { s in s
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
                .step("???????????????? ?????????? ?? ????????????????????") { s in s
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
                .step("???????????????? ?????????? ??????????????????") { s in s
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
                .step("???????????????? ?????????? ???????????????????????? ????????????????????????") { s in s
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
                .step("???????????????? ?????????? ????????") { s in s
                    .should(provider: .wizardPricePicker, .exist)
                    .focus { screen in
                        if type(of: screen) == T.self {
                            matched = true
                        }
                    }
                }
        }

        if !matched { XCTFail("???? ?????????????? ?????????? ???? ?????????????? ???????????? ??????????????") }
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
