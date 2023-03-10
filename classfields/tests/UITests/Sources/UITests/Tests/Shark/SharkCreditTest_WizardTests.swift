//
//  SharkCreditTest_Wizard.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 10.02.2021.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_WizardTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    var sharkMocker: SharkMocker!
    
    override var appSettings: [String: Any] {
        return settings
    }
    
    private var offerId = "1098252972-99d8c274"
    
    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["dadataServiceAPIURL"] = "http://127.0.0.1:\(port)"
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        sharkMocker = SharkMocker(server: server)
    }
    
    // MARK: - Tests
    
    func test_wizard_step1() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        
        let createWithDadataName = expectationForRequest(requestChecker: ({
            if $0.uri == "/shark/credit-application/create" {
                if let req = try? Vertis_Shark_CreditApplicationSource(jsonUTF8Data: $0.messageBody!),
                   req.borrowerPersonProfile.name.nameEntity.name == "ัะตัั" {
                    return true
                }
            }
            return false
        }))

        launchAndOpenWizard()
            .fillField(.passportNumber, value: "")
            .fillField(.passportNumber, value: "123")
            .fillField(.passportNumber, value: "123123")
            .fillField(.passportNumber, value: "asdadasdsa")
            .fillField(.passportNumber, value: "1234675521")
            .fillField(.birthDay, value: "")
            .fillField(.birthDay, value: "12")
            .fillField(.birthDay, value: "1211")
            .fillField(.birthDay, value: "12112020")
            .fillField(.birthDay, value: "12111900")
            .fillField(.birthDay, value: "12112000")
            .fillField(.birthPlace, value: "ะะพัะบะฒะฐ", suggestAction: .select(index: 0))
            .fillField(.passportCode, value: "")
            .fillField(.passportCode, value: "123")
            .fillField(.passportCode, value: "123123")
            .fillField(.dateOfCreate, value: "")
            .fillField(.dateOfCreate, value: "12")
            .fillField(.dateOfCreate, value: "1211")
            .fillField(.dateOfCreate, value: "12111980")
            .fillField(.dateOfCreate, value: "12112000", skipScreenValidation: true)
            .fillField(.placeOfCreate, value: "")
            .fillField(.placeOfCreate, value: "ะฃะคะะก")
            .fillField(.placeOfCreate, value: "ะฃะคะะก1", suggestAction: .select(index: 0))
            .pressNext()
        wait(for: [createWithDadataName], timeout: 10)
    }
    
    func test_wizard_changeName() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.changeName)
            .fillField(.oldSurname, value: "")
            .fillField(.oldSurname, value: "ะััะธะฝ", suggestAction: .select(index: 0))
            .pressNext()
            .exist(selector: "ะะดัะตั ัะตะณะธัััะฐัะธะธ")
    }
    
    func test_wizard_resedinceAddressEquelLegalAddress() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .wait(for: 1)
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "")
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ", suggestAction: .selectAndNext(index: 0))
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .select(index: 0))
            .selectSuggest(1)
            .pressNext()
            .select(.equelLegalAddress)
            .exist(selector: "ะขะธะฟ ะทะฐะฝััะพััะธ")
    }
    
    func test_wizard_resedinceAddressNotEquelLegalAddress() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "")
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ", suggestAction: .selectAndNext(index: 0))
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .select(index: 0))
            .selectSuggest(1)
            .pressNext()
            .select(.notEquelLegalAddress)
            .fillField(.livingAddress, value: "")
            .fillField(.livingAddress, value: "ะะพัะบะฒะฐ", suggestAction: .selectAndNext(index: 0))
            .fillField(.livingAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .select(index: 0))
            .selectSuggest(1)
            .pressNext()
            .exist(selector: "ะขะธะฟ ะทะฐะฝััะพััะธ")
    }
    
    func test_wizard_selfEmployed() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.selfEmployed)
            .fillField(.companyName, value: "")
            .fillField(.companyName, value: "ะ?ะพะณ", suggestAction: .select(index: 0))
            .pressNext()
            .exist(selector: "ะะถะตะผะตัััะฝัะน ะดะพัะพะด")
    }
    
    func test_wizard_company() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .checkScreen("NameChange")
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .checkScreen("EquelLegalAddress")
            .select(.equelLegalAddress)
            .checkScreen("Company")
            .select(.company)
            .fillField(.companyName, value: "")
            .fillField(.companyName, value: "sada")
            .fillField(.companyName, value: "ะ?ะพะณ", suggestAction: .selectAndNext(index: 0), skipScreenValidation: true)
            .checkScreen("Workscreen")
            .select(.economist)
            .checkScreen("AgeOfWork")
            .select(.ageOfWork57)
            .fillField(.jobPhone, value: "")
            .fillField(.jobPhone, value: "91232")
            .fillField(.jobPhone, value: "9123223435", skipScreenValidation: true)
            .exist(selector: "ะะถะตะผะตัััะฝัะน ะดะพัะพะด")
    }
    
    func test_wizard_notWork() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.other)
            .fillField(.otherNoneJobReason, value: "")
            .fillField(.otherNoneJobReason, value: "sasd", skipScreenValidation: true)
            .exist(selector: "ะะถะตะผะตัััะฝัะน ะดะพัะพะด")
    }
    
    func test_wizard_myPhone() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "")
            .fillField(.additionalPhoneNumber, value: "9123213")
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .exist(selector: "ะะพะปะธัะตััะฒะพ ะดะตัะตะน ะดะพ 21 ะณะพะดะฐ")
    }
    
    func test_wizard_friendPhone() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "21312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.friendPhone)
            .fillField(.additionalPhoneNumber, value: "", prefix: "friend")
            .fillField(.additionalPhoneNumber, value: "9123213", prefix: "friend")
            .fillField(.additionalPhoneNumber, value: "9123213213", prefix: "friend")
            .fillField(.additionalPhoneNumberOwnerName, value: "", prefix: "friend")
            .fillField(.additionalPhoneNumberOwnerName, value: "ะะฐัั", prefix: "friend")
            .fillField(.additionalPhoneNumberOwnerName, value: "ะะฐัั ะัะฟะบะธะฝ", skipScreenValidation: true, prefix: "friend")
            .exist(selector: "ะะพะปะธัะตััะฒะพ ะดะตัะตะน ะดะพ 21 ะณะพะดะฐ")
    }
    
    func test_wizard_relativesPhone() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "0")
            .fillField(.avgMonthlyIncome, value: "1000000000000", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.relativesPhone)
            .fillField(.additionalPhoneNumber, value: "", prefix: "relatives")
            .fillField(.additionalPhoneNumber, value: "9123213", prefix: "relatives")
            .fillField(.additionalPhoneNumber, value: "9123213213", prefix: "relatives")
            .fillField(.additionalPhoneNumberOwnerName, value: "", prefix: "relatives")
            .fillField(.additionalPhoneNumberOwnerName, value: "ะะฐัั", prefix: "relatives")
            .fillField(.additionalPhoneNumberOwnerName, value: "ะะฐัั ะัะฟะบะธะฝ", skipScreenValidation: true, prefix: "relatives")
            .exist(selector: "ะะพะปะธัะตััะฒะพ ะดะตัะตะน ะดะพ 21 ะณะพะดะฐ")
    }
    
    func test_wizard_lastStepFindAway() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
            .start()

        let step = launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .select(.one)
            .fillField(.keyWord, value: "")
            .fillField(.keyWord, value: "123")
            .fillField(.keyWord, value: "asds")
            .fillField(.keyWord, value: "123ะ", skipScreenValidation: true)
            .select(.higher)
            .select(.civilMarriage)
            .select(.own)

        sharkMocker
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .select(.findAway)
            .exist(selector: "CreditFormViewController")
    }
    
    func test_wizard_lastStepSelectFromLast() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
            .start()

        let step = launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .checkScreen("Income docs")
            .select(.ndfl2)
            .checkScreen("MyPhone")
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .checkScreen("Children")
            .select(.one)
            .fillField(.keyWord, value: "123ะ", skipScreenValidation: true)
            .select(.higher)
            .select(.civilMarriage)
            .select(.own)
            .select(.last, forceTap: true)
            .select(.offerLast)

        sharkMocker
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .exist(selector: "CreditFormViewController")
    }
    
    func test_wizard_lastStepSelectFromFavorite() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
            .start()

        let step = launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .select(.one)
            .fillField(.keyWord, value: "123ะ", skipScreenValidation: true)
            .select(.higher)
            .select(.civilMarriage)
            .select(.own)
            .select(.favorite, forceTap: true)

        sharkMocker
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .select(.offerFavorite)
            .exist(selector: "CreditFormViewController")
    }
    
    func test_wizard_phoneValidation() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()

        let step = launchAndOpenWizard()
        sharkMocker
            .mockApplication(status: .draft, filled: true, claims: [], offerType: .none)
        
        step
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.company)
            .fillField(.companyName, value: "ะ?ะพะณ", suggestAction: .selectAndNext(index: 0), skipScreenValidation: true)
            .select(.economist)
            .select(.ageOfWork57)
            .fillField(.jobPhone, value: "9000000000")
            .fillField(.jobPhone, value: "9000000001", skipScreenValidation: true)
            .fillField(.avgMonthlyIncome, value: "100000", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.relativesPhone)
            .fillField(.additionalPhoneNumber, value: "9000000000")
            .fillField(.additionalPhoneNumber, value: "9000000001")
            .fillField(.additionalPhoneNumber, value: "9000000002")
            .fillField(.additionalPhoneNumberOwnerName, value: "ะะฐัั ะัะฟะบะธะฝ", skipScreenValidation: true, prefix: "relatives")
            .exist(selector: "ะะพะปะธัะตััะฒะพ ะดะตัะตะน ะดะพ 21 ะณะพะดะฐ")
    }
    
    func test_wizard_rent() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
            .start()

        let step = launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .select(.one)
            .fillField(.keyWord, value: "")
            .fillField(.keyWord, value: "123")
            .fillField(.keyWord, value: "asds")
            .fillField(.keyWord, value: "123ะ", skipScreenValidation: true)
            .select(.higher)
            .select(.civilMarriage)
            .select(.rent)
            .fillField(.homeRentAmount, value: "0")
            .fillField(.homeRentAmount, value: "132131", skipScreenValidation: true)
            .select(.last, forceTap: true)

        sharkMocker
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .select(.offerLast)
            .exist(selector: "CreditFormViewController")
    }
    
    func test_wizard_noRent() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .favoriteLast()
            .start()

        let step = launchAndOpenWizard()
            .pressSkip()
            .select(.notChangeName)
            .fillField(.legalAddress, value: "ะะพัะบะฒะฐ1", suggestAction: .selectAndNext(index: 1), skipScreenValidation: true)
            .select(.equelLegalAddress)
            .select(.notWork)
            .select(.lookingForJob)
            .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true)
            .select(.ndfl2)
            .select(.myPhone)
            .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
            .select(.one)
            .fillField(.keyWord, value: "")
            .fillField(.keyWord, value: "123")
            .fillField(.keyWord, value: "asds")
            .fillField(.keyWord, value: "123ะ", skipScreenValidation: true)
            .select(.higher)
            .select(.civilMarriage)
            .select(.own)
            .select(.last, forceTap: true)

        sharkMocker
            .mockApplication(status: .draft, claims: [], offerType: .none)

        step
            .select(.offerLast)
            .exist(selector: "CreditFormViewController")
    }
    
    // MARK: - ShortCuts
    private func launchAndOpenWizard() -> SharkWizardSteps {
        launch(options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                                     overrideAppSettings: ["skipPreliminaryClaimDraft" : true]))
        return PreliminaryCreditSteps(context: self)
            .exist(selector: "field_name")
            .enterFio("ะะฐัั ะัะฟะบะธะฝ")
            .tapSubmit().as(SharkWizardSteps.self)
    }
}
