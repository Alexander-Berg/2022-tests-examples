//
//  SharkFullFormScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.02.2021.
//

import XCTest
import Snapshots

struct FieldSelector {
    var path: String
    fileprivate init(path: String) {
        self.path = path
    }
}

class SharkFullFormScreen: BaseScreen, Scrollable {
    struct Form {
        struct AdditionalRequiredFields {
            var root = FieldSelector(path: ".credit_root.additionalRequiredFields")

            private static var rootPath = ".additionalRequiredFields."

            var education = FieldSelector(path: "\(rootPath)education")
            var familyStatus = FieldSelector(path: "\(rootPath)familyStatus")
            var homeRentStatus = FieldSelector(path: "\(rootPath)homeRentStatus")
            var homeRentAmount = FieldSelector(path: "\(rootPath)homeRentAmount")
        }

        struct PersonInfoFields {
            var root = FieldSelector(path: ".credit_root.personInfoFields")

            private static var rootPath = ".personInfoFields."

            var email = FieldSelector(path: "\(rootPath)email")
            var fio = FieldSelector(path: "\(rootPath)fio")
            var phone = FieldSelector(path: "\(rootPath)phone")
            var childCount = FieldSelector(path: "\(rootPath)childCount")
            var keyWord = FieldSelector(path: "\(rootPath)keyWord")
        }

        struct Passport {
            var root = FieldSelector(path: ".credit_root.passport")

            private static var rootPath = ".passport."

            var passportNumber = FieldSelector(path: "\(rootPath)passportNumber")
            var birthDay = FieldSelector(path: "\(rootPath)birthDay")
            var birthPlace = FieldSelector(path: "\(rootPath)birthPlace")
            var dateOfCreate = FieldSelector(path: "\(rootPath)dateOfCreate")
            var passportCode = FieldSelector(path: "\(rootPath)passportCode")
            var placeOfCreate = FieldSelector(path: "\(rootPath)placeOfCreate")
            var changeFIO = FieldSelector(path: "\(rootPath)changeFIO")
            var oldSurname = FieldSelector(path: "\(rootPath)oldSurname")
        }

        struct Address {
            var root = FieldSelector(path: ".credit_root.address")

            private static var rootPath = ".address."

            var legalAddress = FieldSelector(path: "\(rootPath)legalAddress")
            var livingAddress = FieldSelector(path: "\(rootPath)livingAddress")
            var livingPlaceChoose = FieldSelector(path: "\(rootPath)livingPlaceChoose")
        }

        struct Job {
            var root = FieldSelector(path: ".credit_root.job")

            private static var rootPath = ".job."

            var jobType = FieldSelector(path: "\(rootPath)jobType")
            var companyName = FieldSelector(path: "\(rootPath)companyName")
            var jobPhone = FieldSelector(path: "\(rootPath)jobPhone")
            var jobPosition = FieldSelector(path: "\(rootPath)jobPosition")
            var jobAge = FieldSelector(path: "\(rootPath)jobAge")
            var jobAddress = FieldSelector(path: "\(rootPath)jobAddress")
            var reasonOfNoneJob = FieldSelector(path: "\(rootPath)reasonOfNoneJob")
            var otherNoneJobReason = FieldSelector(path: "\(rootPath)otherNoneJobReason")
            var avgMonthlyIncome = FieldSelector(path: "\(rootPath)avgMonthlyIncome")
            var approveIncomeMethod = FieldSelector(path: "\(rootPath)approveIncomeMethod")
        }

        struct Offer {
            var root = FieldSelector(path: ".credit_root.offer")
        }

        struct AdditionalNumber {
            private static var rootPath = ".additionalNumberFields."
            var root = FieldSelector(path: ".credit_root.additionalNumberFields")
            var additionalPhoneNumberType = FieldSelector(path: "\(rootPath)additionalPhoneNumberType")
            var additionalPhoneNumber = FieldSelector(path: "\(rootPath)additionalPhoneNumber")
            var additionalPhoneNumberOwnerName = FieldSelector(path: "\(rootPath)additionalPhoneNumberOwnerName")
        }

        struct CreditInfo {
            private static var rootPath = ".creditInfoFields."
            var root = FieldSelector(path: ".credit_root.creditInfoFields")
            var calculator = FieldSelector(path: "\(rootPath)CreditInfo")
            var sum = FieldSelector(path: "creditSumSlider_field")
            var sumThumb = FieldSelector(path: "creditSumSlider_thumb")
            var termThumb = FieldSelector(path: "creditTermSlider_thumb")
        }

        var additionalRequiredFields = AdditionalRequiredFields()
        var personInfoFields = PersonInfoFields()
        var offer = Offer()
        var passport = Passport()
        var address = Address()
        var job = Job()
        var creditInfo = CreditInfo()
        var additionalNumber = AdditionalNumber()
        var submitButton = FieldSelector(path: ".credit_root.submit")
    }

    func element(_ fieldSelector: FieldSelector) -> XCUIElement {
        find(by: fieldSelector.path).firstMatch
    }

    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var additionalRequiredFields = find(by: ".credit_root.additionalRequiredFields").firstMatch

    func option<Option: RawRepresentable>(_ option: Option) -> XCUIElement where Option.RawValue == String {
        return find(by: option.rawValue).firstMatch
    }

    enum EducationOption: String {
        case primary = "Начальное / Ниже среднего"
        case secondary = "Среднее"
        case secondarySpecial = "Средне-специальное"
        case incompleteHigher = "Неполное высшее"
        case higher = "Высшее"
        case twoOrMoreHigher = "Второе высшее / Два и более высших"
        case academicDegree = "Учёная степень"
    }

    enum FamilyStatusOption: String {
        case single = "Холост / не замужем"
        case married = "Женат / замужем"
        case divorced = "В разводе"
        case widower = "Вдовец / вдова"
        case civilMarriage = "Гражданский брак"
    }

    enum RentOption: String {
        case rent = "Плачу аренду"
        case own = "У меня своя квартира"
    }

    enum ChildCount: String {
        case no = "Нет"
        case one = "1"
    }

    enum ChangeFIO: String {
        case no = "Не менялась"
        case yes = "Менялась"
    }

    enum LivingPlaceChoose: String {
        case notEquelLegalAddress = "Другой адрес"
        case equelLegalAddress = "По адресу регистрации"
    }

    enum JobType: String {
        case company = "В организации"
        case selfEmployed = "На себя"
        case notWork = "Не работаю"
    }

    enum JobPosition: String {
        case economist = "Экономист"
    }

    enum JobAge: String {
        case ageOfWork57 = "5-7 лет"
    }

    enum ReasonOfNoJob: String {
        case lookingForJob = "Ищу работу"
    }

    enum IncomeApproveMethod: String {
        case ndfl2 = "Справка 2-НДФЛ"
    }

    enum AdditionalNumberType: String {
        case related = "Номер родственника"
        case friend = "Номер друга"
        case myNumber = "Мой номер"
    }
}
