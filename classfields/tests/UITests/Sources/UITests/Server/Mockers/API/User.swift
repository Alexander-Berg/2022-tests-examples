//
//  User.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_user(id: String = "1",
                   userPhones: [String] = ["+7 987 564-32-12"],
                   userEmail: String = "pet@sa.ru",
                   userEmailConfirmed: Bool = false,
                   about: String = "",
                   fullName: String = "Вася ПЕТРОВ АЛЕК") -> Self {
        let userProfile = Auto_Api_UserResponse.with { profile in
            profile.user.id = id
            profile.user.phones = userPhones.map { phone in
                Vertis_Passport_UserPhone.with { userPhone in
                    userPhone.phone = phone
                }
            }
            profile.user.emails = [{
                var email = Vertis_Passport_UserEmail()
                email.email = userEmail
                email.confirmed = userEmailConfirmed
                return email
            }()]
            profile.user.profile.autoru.about = about
            profile.user.profile.autoru.fullName = fullName
        }

        server.addHandler("GET /user *") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_dealer() -> Self {
        server.addHandler("GET /user *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants_including_panoramas", userAuthorized: true)
        }
        return self
    }

    func mock_postAuthLoginOrRegister() -> Self  {
        server.addHandler("POST /auth/login-or-register") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_auth_login-or-register")
        }
        return self
    }

    @discardableResult
    func mock_postUserConfirm() -> Self  {
        server.addHandler("POST /user/confirm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_user_confirm", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_getSession() -> Self  {
        server.addHandler("GET /session *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "session", userAuthorized: true)
        }
        return self
    }

    // Ручка отправки жалобы юзера на приложение
    @discardableResult
    func mock_feedbackSend() -> Self {
        server.api.feedback.send.post.ok(mock: .file("success"))
        return self
    }

    @discardableResult
    func mock_publicProfileUser(encryptedUserID id: String) -> Self {
        let userInfoResponse = Auto_Api_UserInfoResponse.with {
            $0.alias = "CartelTurbo"
            $0.registrationDate = "2020-03-06"
            $0.offersStatsByCategory = [
                "CARS": Auto_Api_UserInfoResponse.OffersStats.with {
                    $0.activeOffersCount = 22
                    $0.inactiveOffersCount = 354
                }
            ]
            $0.shareURL = "https://auto.ru/reseller/\(id)/all"
        }

        server.addHandler("GET /user/\(id)/info?count_inactive_offers=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userInfoResponse.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_publicProfileList(encryptedUserID id: String) -> Self  {
        server.addHandler("GET /user/\(id)/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "public_profile_list", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_reseller(
        encryptedUserID: String,
        id: String = "1",
        allowOffersShow: Bool = false
    ) -> Self {
        let userProfile = Auto_Api_UserResponse.with { profile in
            profile.user.id = id
            profile.user.moderationStatus.reseller = true
            profile.user.profile.autoru.allowOffersShow.value = allowOffersShow
            profile.encryptedUserID = encryptedUserID
        }

        server.addHandler("GET /user *") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userUpdate(customResponseHander: ((inout Vertis_Passport_AutoruUserProfilePatch) -> Void)? = nil) -> Self {
        var response = Vertis_Passport_AutoruUserProfilePatch()
        customResponseHander?(&response)

        server.addHandler("POST /user/profile *") {
            return Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }
        return self
    }
}
