//
//  SharkCreditTest_Form.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.02.2021.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_FormTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var sharkMocker: SharkMocker!

    typealias Form = SharkFullFormScreen.Form
    typealias Screen = SharkFullFormScreen

    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        sharkMocker = SharkMocker(server: server)
    }

    func test_fill_additionalReqFields() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .fillOptionsField(\.additionalRequiredFields.education,
                           option: Screen.EducationOption.higher)
        .fillOptionsField(\.additionalRequiredFields.familyStatus, option: Screen.FamilyStatusOption.divorced)
        .fillOptionsField(\.additionalRequiredFields.homeRentStatus, option: Screen.RentOption.rent)
        .tapField(\.additionalRequiredFields.homeRentAmount)
        .pressNext()
        .validateSnapshot(of: "homeRentAmount", snapshotId: "homeRentAmount_error_empty")
        .fillHomeRentAmount(0)
        .pressNext()
        .validateSnapshot(of: "homeRentAmount", snapshotId: "homeRentAmount_error_0")
        .fillHomeRentAmount(9999999)
        .validateSnapshot(of: "homeRentAmount", snapshotId: "homeRentAmount_max")
        .pressNext()
        .validate(\.additionalRequiredFields.root, snapshotId: "additionalRequiredFields_disExpanded")
        .validateAdditionalRequiredFields()
        .openAdditionalRequiredFields()
        .fillOptionsField(\.additionalRequiredFields.homeRentStatus, option: Screen.RentOption.own)
        .notExist(\.additionalRequiredFields.homeRentAmount)
    }

    func test_fill_personalData() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none) {
                var resp = $0
                resp.creditApplication.borrowerPersonProfile.clearPhones()
                resp.creditApplication.borrowerPersonProfile.clearName()
                resp.creditApplication.borrowerPersonProfile.clearEmails()
                return resp
            }
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)
        .validate(\.personInfoFields.childCount, snapshotId: "childCount_emptyError")
        .validate(\.personInfoFields.keyWord, snapshotId: "keyWord_emptyError")
        .tapField(\.personInfoFields.email).as(SharkWizardSteps.self)
        .fillField(.fio, value: "")
        .fillField(.fio, value: "Василий")
        .fillField(.fio, value: "Василий Арко")
        .fillField(.email, value: "")
        .fillField(.email, value: "asdas")
        .fillField(.email, value: "asdas@")
        .fillField(.email, value: "asdas@asdsa")
        .fillField(.email, value: "asdas@asdsa.ru", skipScreenValidation: true).as(SharkFullFormSteps.self)
        .tapField(\.personInfoFields.phone).as(SharkWizardSteps.self)
        .tap("79875643212").as(SharkFullFormSteps.self)
        .fillOptionsField(\.personInfoFields.childCount, option: Screen.ChildCount.one)
        .tapField(\.personInfoFields.keyWord).as(SharkWizardSteps.self)
        .fillField(.keyWord, value: "")
        .fillField(.keyWord, value: "123")
        .fillField(.keyWord, value: "asds")
        .fillField(.keyWord, value: "123А", skipScreenValidation: true).as(SharkFullFormSteps.self)
        .notExist(\.personInfoFields.email)
    }
    
    func test_fill_phone() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, filled: true, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.personInfoFields.root)
        .tapField(\.personInfoFields.phone).as(SharkWizardSteps.self)
        .exist(selector: "phone_list_popup")
        .tap("79875643212").as(SharkFullFormSteps.self)
        .tapField(\.personInfoFields.root)
        .validate(\.personInfoFields.phone, snapshotId: "phone_+7 987 564-32-12")
    }

    func test_fill_passportData() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)
        .wait(for: 1)
        .tapStatusBar()
        .validate(\.offer.root, snapshotId: "offer_none")
        .swipe()
        .validate(\.passport.passportNumber, snapshotId: "passportNumber_emptyError")
        .validate(\.passport.birthDay, snapshotId: "birthDay_emptyError")
        .validate(\.passport.birthPlace, snapshotId: "birthPlace_emptyError")
        .validate(\.passport.dateOfCreate, snapshotId: "dateOfCreate_emptyError")
        .validate(\.passport.passportCode, snapshotId: "passportCode_emptyError")
        .validate(\.passport.placeOfCreate, snapshotId: "placeOfCreate_emptyError")
        .validate(\.passport.changeFIO, snapshotId: "changeFIO_emptyError")
        .tapField(\.passport.passportNumber).as(SharkWizardSteps.self)
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
        .fillField(.birthPlace, value: "Москва", suggestAction: .select(index: 0))
        .fillField(.passportCode, value: "")
        .fillField(.passportCode, value: "123")
        .fillField(.passportCode, value: "123123")
        .fillField(.dateOfCreate, value: "")
        .fillField(.dateOfCreate, value: "12")
        .fillField(.dateOfCreate, value: "1211")
        .fillField(.dateOfCreate, value: "12111980")
        .fillField(.dateOfCreate, value: "12112000", skipScreenValidation: true)
        .fillField(.placeOfCreate, value: "")
        .fillField(.placeOfCreate, value: "УФМС")
        .fillField(.placeOfCreate, value: "УФМС1", suggestAction: .select(index: 0))
        .pressSave().as(SharkFullFormSteps.self)
        .fillOptionsField(\.passport.changeFIO, option: Screen.ChangeFIO.yes)
        .tapField(\.passport.oldSurname).as(SharkWizardSteps.self)
        .fillField(.oldSurname, value: "")
        .fillField(.oldSurname, value: "Путин", suggestAction: .select(index: 0))
        .pressSave().as(SharkFullFormSteps.self)
        .tapField(\.passport.root)
        .fillOptionsField(\.passport.changeFIO, option: Screen.ChangeFIO.no)
        .tapField(\.passport.root)
        .notExist(\.passport.oldSurname)
    }

    func test_fill_address() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)
        .wait(for: 1)
        .swipe()
        .validate(\.address.legalAddress, snapshotId: "legalAddress_emptyError")
        .tapField(\.address.legalAddress).as(SharkWizardSteps.self)
        .fillField(.legalAddress, value: "")
        .fillField(.legalAddress, value: "Москва", suggestAction: .selectAndNext(index: 0))
        .fillField(.legalAddress, value: "Москва1", suggestAction: .select(index: 0))
        .selectSuggest(1)
        .pressSave().as(SharkFullFormSteps.self)
        .fillOptionsField(\.address.livingPlaceChoose, option: Screen.LivingPlaceChoose.notEquelLegalAddress)
        .tapField(\.address.livingAddress).as(SharkWizardSteps.self)
        .fillField(.livingAddress, value: "")
        .fillField(.livingAddress, value: "Москва", suggestAction: .selectAndNext(index: 0))
        .fillField(.livingAddress, value: "Москва1", suggestAction: .select(index: 0))
        .selectSuggest(1)
        .pressSave().as(SharkFullFormSteps.self)
        .tapField(\.address.root)
        .fillOptionsField(\.address.livingPlaceChoose, option: Screen.LivingPlaceChoose.equelLegalAddress)
        .notExist(\.address.livingAddress)
    }

    func test_fill_job() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)
        .wait(for: 1)
        .swipe()
        .validate(\.job.jobType, snapshotId: "jobType_emptyError")
        .fillOptionsField(\.job.jobType, option: Screen.JobType.selfEmployed)
        .tapField(\.job.companyName).as(SharkWizardSteps.self)
        .fillField(.companyName, value: "Рог", suggestAction: .select(index: 0))
        .pressSave().as(SharkFullFormSteps.self)
        .tapField(\.job.avgMonthlyIncome).as(SharkWizardSteps.self)
        .fillField(.avgMonthlyIncome, value: "12312", skipScreenValidation: true).as(SharkFullFormSteps.self)
        .fillOptionsField(\.job.approveIncomeMethod, option: Screen.IncomeApproveMethod.ndfl2)
        .tapField(\.job.root)
        .fillOptionsField(\.job.jobType, option: Screen.JobType.company)
        .tapField(\.job.companyName).as(SharkWizardSteps.self)
        .fillField(.companyName, value: "")
        .fillField(.companyName, value: "sada")
        .fillField(.companyName, value: "Рог", suggestAction: .selectAndNext(index: 0), skipScreenValidation: true).as(SharkFullFormSteps.self)
        .tapField(\.job.jobPhone).as(SharkWizardSteps.self)
        .fillField(.jobPhone, value: "")
        .fillField(.jobPhone, value: "91232")
        .fillField(.jobPhone, value: "9123223435", skipScreenValidation: true).as(SharkFullFormSteps.self)
        .fillOptionsField(\.job.jobPosition, option: Screen.JobPosition.economist)
        .fillOptionsField(\.job.jobAge, option: Screen.JobAge.ageOfWork57)
        .tapField(\.job.root)
        .fillOptionsField(\.job.jobType, option: Screen.JobType.notWork)
        .fillOptionsField(\.job.reasonOfNoneJob, option: Screen.ReasonOfNoJob.lookingForJob)
    }

    func test_fill_additionalNumberFields() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)
        .wait(for: 1)
        .swipe()
        .validate(\.additionalNumber.additionalPhoneNumberType, snapshotId: "additionalPhoneNumberType_emptyError")
        .fillOptionsField(\.additionalNumber.additionalPhoneNumberType, option: Screen.AdditionalNumberType.myNumber)
        .tapField(\.additionalNumber.additionalPhoneNumber).as(SharkWizardSteps.self)
        .fillField(.additionalPhoneNumber, value: "9123213213", skipScreenValidation: true)
        .as(SharkFullFormSteps.self)
        .tapField(\.additionalNumber.root)
        .fillOptionsField(\.additionalNumber.additionalPhoneNumberType, option: Screen.AdditionalNumberType.friend)
        .tapField(\.additionalNumber.additionalPhoneNumber).as(SharkWizardSteps.self)
        .fillField(.additionalPhoneNumber, value: "", prefix: "friend")
        .fillField(.additionalPhoneNumber, value: "9123213", prefix: "friend")
        .fillField(.additionalPhoneNumber, value: "9123213213", prefix: "friend")
        .fillField(.additionalPhoneNumberOwnerName, value: "", prefix: "friend")
        .fillField(.additionalPhoneNumberOwnerName, value: "Вася", prefix: "friend")
        .fillField(.additionalPhoneNumberOwnerName, value: "Вася Пупкин", skipScreenValidation: true, prefix: "friend")
        .as(SharkFullFormSteps.self)
        .tapField(\.additionalNumber.root)
        .fillOptionsField(\.additionalNumber.additionalPhoneNumberType, option: Screen.AdditionalNumberType.related)
        .tapField(\.additionalNumber.additionalPhoneNumber)
        .as(SharkWizardSteps.self)
        .fillField(.additionalPhoneNumber, value: "", prefix: "relatives")
        .fillField(.additionalPhoneNumber, value: "9123213", prefix: "relatives")
        .fillField(.additionalPhoneNumber, value: "9123213213", prefix: "relatives")
        .fillField(.additionalPhoneNumberOwnerName, value: "", prefix: "relatives")
        .fillField(.additionalPhoneNumberOwnerName, value: "Вася", prefix: "relatives")
        .fillField(.additionalPhoneNumberOwnerName, value: "Вася Пупкин", skipScreenValidation: true, prefix: "relatives")
        .as(SharkFullFormSteps.self)
        .validate(\.additionalNumber.root, snapshotId: "additionalPhoneNumberType_ok_filled")
    }

    func test_autoSendProduct() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .raif])
            .mockApplication(status: .draft, filled: true, claims: [], offerType: .none)
            .start()

        let addProductExpect = expectationForRequest(requestChecker: ({
            if $0.uri == "/shark/credit-application/add-products/\(self.sharkMocker.applicationId)?credit_product_ids=tinkoff-1,raif&with_send_delay_secs=600" {
                return true
            }
            return false
        }))

        _ = launch(on: .creditFormScreen,
                   options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                                  overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                        "skipCreditAlert": true,
                                                        "creditSendOnlyTinkoff": false,
                                                        "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)

        wait(for: [addProductExpect], timeout: 10)
    }

    func test_autoSendProductToTinkof() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .raif])
            .mockApplication(status: .draft, filled: true, claims: [], offerType: .none)
            .start()

        let addProductExpect = expectationForRequest(requestChecker: ({
            if $0.uri == "/shark/credit-application/add-products/\(self.sharkMocker.applicationId)?credit_product_ids=tinkoff-1&with_send_delay_secs=600" {
                return true
            }
            return false
        }))

        _ = launch(on: .creditFormScreen,
                   options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                                  overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                        "skipCreditAlert": true,
                                                        "creditSendOnlyTinkoff": true]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.submitButton)

        wait(for: [addProductExpect], timeout: 10)
    }

    func test_phonesValidation() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .raif])
            .mockApplication(status: .draft, filled: true, claims: [], offerType: .none)
            .start()

        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .swipe()
        .tapField(\.personInfoFields.root)
        .tapField(\.personInfoFields.phone)
        .as(SharkWizardSteps.self)
        .tap("79875643212")
        .as(SharkFullFormSteps.self)
        .tapField(\.job.root)
        .tapField(\.job.jobPhone)
        .as(SharkWizardSteps.self)
        .fillField(.jobPhone, value: "9875643212")
        .fillField(.jobPhone, value: "0000000002")
        .fillField(.jobPhone, value: "9000000001", skipScreenValidation: true)
        .as(SharkFullFormSteps.self)
        .tapField(\.additionalNumber.root)
        .tapField(\.additionalNumber.additionalPhoneNumber)
        .as(SharkWizardSteps.self)
        .fillField(.additionalPhoneNumber, value: "9875643212")
        .fillField(.additionalPhoneNumber, value: "9000000001")
        .fillField(.additionalPhoneNumber, value: "0000000002", skipScreenValidation: true)
        .exist(selector: "CreditFormViewController")
    }

    func test_calculator() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()


        launch(on: .creditFormScreen,
               options: .init(launchType: .deeplink("https://auto.ru/my/credits"),
                              overrideAppSettings: ["skipPreliminaryClaimDraft" : true,
                                                    "skipCreditAlert": true,
                                                    "dadataServiceAPIURL": "http://127.0.0.1:\(port)"]))
        .as(SharkFullFormSteps.self)
        .dragHorizontal(SharkFullFormScreen.Form().creditInfo.sumThumb.path, to: -100)
        .validate(\.creditInfo.calculator, snapshotId: "creditInfoFields_lessHalf")
        .dragHorizontal(SharkFullFormScreen.Form().creditInfo.sumThumb.path, to: 100)
        .validate(\.creditInfo.calculator, snapshotId: "creditInfoFields_half")
        .dragHorizontal(SharkFullFormScreen.Form().creditInfo.termThumb.path, to: -90)
        .validate(\.creditInfo.calculator, snapshotId: "creditInfoFields_termLessMaх")
        .fillField(\.creditInfo.sum, value: "0")
        .validate(\.creditInfo.calculator, snapshotId: "creditInfoFields_max")
    }
}
