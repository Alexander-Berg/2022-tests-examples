import AutoRuProtoModels

extension BackendState {
    func makeSession() -> Vertis_Passport_Session {
        return .with { msg in
            msg.id = session.id
            msg.creationTimestamp = .init(date: now.adding(-1, component: .day))
            msg.expireTimestamp = .init(date: now.adding(10, component: .day))
            msg.userID = user.id
            msg.deviceUid = device.uid
            msg.ttlSec = 7776000
        }
    }

    func makeUser() -> Vertis_Passport_User {
        return .with { msg in
            msg.emails = user.emails.map { emailInfo in
                Vertis_Passport_UserEmail.with { msg in
                    msg.confirmed = emailInfo.confirmed
                    msg.email = emailInfo.email
                }
            }
            msg.active = true
            msg.registrationDate = "2016-10-25"
            msg.id = user.id
            msg.phones = user.phones.map { phoneInfo in
                Vertis_Passport_UserPhone.with { msg in
                    msg.added = .init(date: phoneInfo.added)
                    msg.phone = phoneInfo.phone
                }
            }
            msg.profile = makeUserProfile()
        }
    }

    func makeUserProfile() -> Vertis_Passport_UserProfile {
        return .with { msg in
            msg.autoru = .with { msg in
                msg.showCard = true
                msg.drivingYear = 1990
                msg.fullName = user.fullName
                msg.countryID = 1
                msg.showMail = true
                msg.geoID = user.geoID
                msg.cityID = user.cityID
                msg.regionID = user.regionID
                msg.allowMessages = true
                msg.clientID = user.isDealer ? "notEmpty" : ""
            }
        }
    }
}
