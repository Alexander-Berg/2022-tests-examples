//
//  SharkWizardScreen.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 10.02.2021.
//

import XCTest
import Snapshots

class SharkWizardScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var nextButton = find(by: "Далее").firstMatch
    lazy var saveButton = find(by: "Сохранить").firstMatch

    enum FieldID: String {
        case passportNumber
        case birthDay
        case birthPlace
        case passportCode
        case dateOfCreate
        case placeOfCreate
        case changeFIO
        case oldSurname
        case oldName
        case livingPlaceChoose
        case legalAddress
        case livingAddress
        case jobType
        case companyName
        case jobPhone
        case reasonOfNoneJob
        case otherNoneJobReason
        case avgMonthlyIncome
        case jobPosition
        case jobAge
        case jobAddress
        case approveIncomeMethod
        case additionalPhoneNumberType
        case additionalPhoneNumber
        case additionalPhoneNumberOwnerName
        case drivingLicenceType
        case drivingLicenceNumber
        case drivingLicenceIssueDate
        case drivingLicenceOwnerName
        case drivingLicenceOwnerPhone
        case childCount
        case keyWord
        case offer
        case fio
        case email
        case phone
        case homeRentAmount
    }

    enum SelectableOption: String {
        case changeName = "Менялась"
        case notChangeName = "Не менялась"
        case equelLegalAddress = "По адресу регистрации"
        case notEquelLegalAddress = "Другой адрес"
        case company = "В организации"
        case selfEmployed = "На себя"
        case notWork = "Не работаю"
        case economist = "Экономист"
        case ageOfWork57 = "5-7 лет"
        case other = "Другое"
        case lookingForJob = "Ищу работу"
        case ndfl2 = "Справка 2-НДФЛ"
        case myPhone = "Мой номер"
        case relativesPhone = "Номер родственника"
        case friendPhone = "Номер друга"
        case have = "Есть"
        case no = "Нет"
        case one = "1"
        case findAway = "Выберу позже"
        case offerFavorite = "offer_1102472565-ce08993f"
        case offerLast = "offer_1102416009-bec2f0c2"
        case last = "segmentControlSegmentLabel_0"
        case favorite = "segmentControlSegmentLabel_1"
        case higher = "Высшее"
        case civilMarriage = "Гражданский брак"
        case rent = "Плачу аренду"
        case own = "У меня своя квартира"
    }

    func field(_ id: FieldID) -> XCUIElement {
        return find(by: id.rawValue).firstMatch
    }

    func fieldTextView(_ id: FieldID) -> XCUIElement {
        return find(by: "\(id.rawValue)_textView").firstMatch
    }
}
