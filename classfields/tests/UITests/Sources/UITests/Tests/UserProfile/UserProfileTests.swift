//
//  UserProfileTests.swift
//  AutoRu
//
//  Created by Vitalii Stikhurov on 28.02.2020.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuUserProfile
class UserProfileTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=true") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=false") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("POST /user/profile") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_profile_update_ok", userAuthorized: false)
        }

        server.addHandler("GET /geo/suggest *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "geo_suggest_ok", userAuthorized: false)
        }

        server.addHandler("GET /geo/213") { (request, _) -> Response? in
            return Response.okResponse(fileName: "geo_suggest_ok", userAuthorized: false)
        }

        server.addHandler("GET /user/profile/userpic-upload-uri") {[unowned self] (request, _) -> Response? in
            return Response.responseWithStatus(
                body: "{\"uri\": \"http://127.0.0.1:\(self.port)/upload\",\"status\": \"SUCCESS\"}".data(using: .utf8),
                userAuthorized: false)
        }

        server.addHandler("POST /user/forget") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }


        server.addHandler("POST /auth/logout") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_profile_update_ok", userAuthorized: false)
        }

        server.addHandler("GET /story/search") { (request, _) -> Response? in
            return Response.okResponse(fileName: "story_search_ok", userAuthorized: false)
        }

        try! server.start()
    }

    func test_openProfileFrom_offers() {
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
    }

    func test_setExperience() {
        let value = "2014"
        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.drivingYear == Google_Protobuf_UInt32Value(UInt32(value)!) {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .experience)
            .setValueToPicker(type: .experience, value: value) { [weak self] in
                self!.userProfile.user.profile.autoru.drivingYear = UInt32(value)!
            }
            .exist(selector: "С \(value) года")
            .notExist(selector: UserProfileScreen.Picker.experience.validationLabel)

        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setBirthDay() {
        let date = Date()
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        dateFormatter.locale = Locale(identifier: "ru_RU")
        let birthdayStr = dateFormatter.string(from: date)

        dateFormatter.dateFormat = "dd MMMM yyyy"
        let checkStr = dateFormatter.string(from: date)

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.birthday == Google_Protobuf_StringValue(birthdayStr) {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .birthday)
            .setValueToPicker(type: .birthday, value: birthdayStr) { [weak self] in
                self!.userProfile.user.profile.autoru.birthday = birthdayStr
            }
            .pressDone()
            .exist(selector: checkStr)
            .notExist(selector: UserProfileScreen.Picker.birthday.validationLabel)

        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setCity() {
        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.geoID == 213 {
                        return true
                    }
                }
            }
            return false
        }

        let value = "Москва"
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .city)
            .setValueToPicker(type: .city, value: value) { [weak self] in
                self!.userProfile.user.profile.autoru.geoID = 213
            }
            .exist(selector: "Москва")
        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setAbout() {
        let value = "Test"

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.about == Google_Protobuf_StringValue(value) {
                        return true
                    }
                }
            }
            return false
        }
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .exist(selector: "Расскажите о себе")
            .selectAbout()
            .enterText(value)
            .makeAction { [weak self] in
                self!.userProfile.user.profile.autoru.about = value
            }
            .pressDone()
            .exist(selector: value)
            .notExist(selector: "Расскажите о себе")
        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setName() {
        let value = "Name Test"

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.fullName == Google_Protobuf_StringValue(value) {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
        .openOffersTab()
        .tapProfile()
        .checkUserProfileScreenVisible()
        .nameTitleExist(value: "ID 1")
        .nameSubtitleNotExist(value: "")
        .selectName()
        .enterText(value)
        .makeAction { [weak self] in
            self!.userProfile.user.profile.autoru.fullName = value
        }
        .pressDone()
        .wait(for: 2)
        .nameTitleExist(value: value)
        .nameSubtitleExist(value: "ID 1")
        .nameShouldBe(value: value)

        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setName_fromNameLabel() {
        let value = "Name Test"

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/profile" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_AutoruUserProfilePatch(jsonUTF8Data: data) {
                    if messageBody.fullName == Google_Protobuf_StringValue(value) {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()

            .nameTitleExist(value: "ID 1")
            .nameSubtitleNotExist(value: "")
            .pressAddName()
            .enterText(value)
            .makeAction { [weak self] in
                self!.userProfile.user.profile.autoru.fullName = value
        }
        .pressDone()
        .wait(for: 2)
        .nameTitleExist(value: value)
        .nameSubtitleExist(value: "ID 1")
        .nameShouldBe(value: value)

        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_setEmail_ok() {
        server.addHandler("POST /user/email/change") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_email_change_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/email/request-change-code") { (request, _) -> Response? in
            return Response.okResponse(fileName: "request_change_code_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/confirm") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_confirm_ok", userAuthorized: false)
        }

        let emailStr = "Hh@h.ru"
        var phone = Vertis_Passport_UserPhone()
        phone.phone = "79165395678"
        userProfile.user.phones = [phone]

        let requestExpectationChangeEmail: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/email/change" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_ChangeEmailParameters(jsonUTF8Data: data) {
                    if messageBody.confirmationCode.identity.phone == phone.phone, messageBody.email == emailStr, messageBody.confirmationCode.code == "1" {
                        return true
                    }
                }
            }
            return false
        }

        let requestExpectationRequestCode: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/email/request-change-code" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_RequestEmailChangeParameters(jsonUTF8Data: data) {
                    if messageBody.currentIdentity.phone == phone.phone {
                        return true
                    }
                }
            }
            return false
        }

        let requestExpectationUserConfirm: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/confirm" {
                if let data = request.messageBody, let messageBody = try? Vertis_Passport_ConfirmIdentityParameters(jsonUTF8Data: data) {
                    if messageBody.code == "1", messageBody.email == emailStr {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
        .openOffersTab()
        .tapProfile()
        .checkUserProfileScreenVisible()
        .notExist(selector: "Пароль")
        .openPicker(type: .email)
        .setValueToPicker(type: .email, value: emailStr)
        .openPicker(type: .email_phoneConfirmation)
        .setValueToPicker(type: .email_phoneConfirmation, value: "1") { [weak self] in
            var email = Vertis_Passport_UserEmail()
            email.email = emailStr
            email.confirmed = true
            self!.userProfile.user.emails = [email]
        }
        .enterText("1")
        .exist(selector: emailStr)
        .exist(selector: "Пароль")
        self.wait(for: [requestExpectationChangeEmail, requestExpectationRequestCode, requestExpectationUserConfirm], timeout: 5)
    }

    func test_setEmail_changeCodeRequestError() {
        let emailStr = "Hh@h.ru"
        var phone = Vertis_Passport_UserPhone()
        phone.phone = "79165395678"
        userProfile.user.phones = [phone]
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .email)
            .setValueToPicker(type: .email, value: emailStr)
            .pressDone()
            .exist(selector: "Что-то пошло не так (код 0)")
    }

    func test_setEmail_wrongCode() {
        server.addHandler("POST /user/email/request-change-code") { (request, _) -> Response? in
            return Response.okResponse(fileName: "request_change_code_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/email/change") { (request, _) -> Response? in
            return Response.badResponse(fileName: "user_email_change_wrong_code_error", userAuthorized: false)
        }

        let emailStr = "Hh@h.ru"
        var phone = Vertis_Passport_UserPhone()
        phone.phone = "79165395678"
        userProfile.user.phones = [phone]
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .email)
            .setValueToPicker(type: .email, value: emailStr)
            .pressDone()
            .wait(for: 1)
            .enterText("2")
            .exist(selector: "Неверный код")
            .pressClearText()
            .notExist(selector: "Неверный код")
    }

    func test_setEmail_disabled() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .makeAction {
                mainSteps.onUserProfileScreen().find(by: email.email).firstMatch.shouldExist(timeout: 5)
                mainSteps.onUserProfileScreen().find(by: email.email).firstMatch.tap()
            }
            .notExist(selector: UserProfileScreen.Picker.email.validationLabel)
    }

    func test_setEmail_enable() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]
        var phone = Vertis_Passport_UserPhone()
        phone.phone = "79165395678"
        userProfile.user.phones = [phone]

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .makeAction {
                mainSteps.onUserProfileScreen().find(by: email.email).firstMatch.shouldExist(timeout: 5)
                mainSteps.onUserProfileScreen().find(by: email.email).firstMatch.tap()
            }
            .exist(selector: UserProfileScreen.Picker.email.validationLabel)
    }

    func test_changePassword_ok() {
        server.addHandler("POST /user/password") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_password_ok", userAuthorized: false)
        }

        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]

        let currentPassword = "Ghsarsa"
        let newPassword = "Hspr1252s"

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
            .setValueToPicker(type: .password, value: "\(currentPassword) \(newPassword) \(newPassword)")
            .pressDone()
            .exist(selector: "Готово")
            .notExist(selector: UserProfileScreen.Picker.password.validationLabel)
    }

    func test_changePassword_equalPasswords() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]

        let currentPassword = "Ghsarsa1"

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
            .setValueToPicker(type: .password, value: "\(currentPassword) \(currentPassword) \(currentPassword)")
            .pressDone()
            .exist(selector: UserProfileScreen.Picker.password.validationLabel)
    }

    func test_changePassword_badNewPasswords() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]

        let currentPassword = "Ghsarsa1"
        let newPassword = "Gh"
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
            .setValueToPicker(type: .password, value: "\(currentPassword) \(newPassword) \(newPassword)")
            .pressDone()
            .exist(selector: UserProfileScreen.Picker.password.validationLabel)
    }

    func test_changePassword_notEqualNewPasswords() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]

        let currentPassword = "Ghsarsa1"
        let newPassword = "Ghasd13s"
        let newPasswordConfirm = "Ghasd13ss"

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
            .setValueToPicker(type: .password, value: "\(currentPassword) \(newPassword) \(newPasswordConfirm)")
            .pressDone()
            .exist(selector: UserProfileScreen.Picker.password.validationLabel)
    }

    func test_changePassword_intError() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]

        let currentPassword = "Ghsarsa"
        let newPassword = "Hspr1252s"

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
            .setValueToPicker(type: .password, value: "\(currentPassword) \(newPassword) \(newPassword)")
            .pressDone()
            .exist(selector: "Ошибка")
            .exist(selector: UserProfileScreen.Picker.password.validationLabel)
    }

    func test_passwordChangeEnable() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]
        self.userProfile.allowCodeLogin = false
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .password)
    }

    func test_passwordChangeDisable_noEmail() {
        self.userProfile.allowCodeLogin = false
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .notExist(selector: "Пароль")
    }

    func test_passwordChangeDisable_allowCodeLogin() {
        var email = Vertis_Passport_UserEmail()
        email.email = "hhs@ss.ru"
        email.confirmed = true
        self.userProfile.user.emails = [email]
        self.userProfile.allowCodeLogin = true
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .notExist(selector: "Пароль")
    }

    func test_addSocialAccount_vk() {
        _ = mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .social)
            .setValueToPicker(type: .social, value: "VK")
            .exist(selector: "Done")
    }

    func test_addSocialAccount_vk_fromIcon() {
        _ = mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()

            .pressVKIcon()
            .exist(selector: "Done")
    }

    func test_socialAccount_exist() {
        var soc = Vertis_Passport_UserSocialProfile()
        soc.provider = .vk
        soc.socialUserID = "1"
        userProfile.user.socialProfiles = [soc]
        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "DELETE", request.uri == "/user/social-profiles/VK/1" {
                return true
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .notExist(selector: "Привяжите аккаунт")
            .openPicker(type: .currentSocial)
            .exist(selector: "VK")
            .setValueToPicker(type: .currentSocial, value: "VK")
            .exist(selector: "Что-то пошло не так (код 0)")
        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_socialAccount_exist_addNew() {
        var soc = Vertis_Passport_UserSocialProfile()
        soc.provider = .vk
        soc.socialUserID = "1"
        userProfile.user.socialProfiles = [soc]

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .makeAction {
                let elem = mainSteps.onUserProfileScreen().find(by: "addSocialAccountLabel").firstMatch
                elem.shouldExist(timeout: 5)
                elem.firstMatch.tap()
            }
            .exist(selector: "Добавить аккаунт")
    }

    func test_openAvatarPicker() {
        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/upload" {
                return true
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .openPicker(type: .avatar)
            .handleSystemAlertIfNeeded()
            .setValueToPicker(type: .avatar, value: "1")
            .notExist(selector: UserProfileScreen.Picker.avatar.validationLabel)
        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_logout_ok() {
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .pressLogout()
            .makeAction { [weak self] in
                self!.server.forceLoginMode = .preservingResponseState
            }
            .as(OffersSteps.self)
            .onOffersScreen()
            .profileButton
            .shouldExist()
    }

    func test_delete_ok() {
        mainSteps
            .openOffersTab()
            .tapProfile()
            .checkUserProfileScreenVisible()
            .pressDelete()
            .makeAction { [weak self] in
                self!.server.forceLoginMode = .preservingResponseState
            }
            .as(OffersSteps.self)
            .onOffersScreen()
            .profileButton
            .shouldExist()
    }
}
