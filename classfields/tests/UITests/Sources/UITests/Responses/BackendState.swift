import Foundation
import AutoRuProtoModels

struct BackendState {
    static var global = BackendState()

    var now = Date()
    var user = UserInfo()
    var device = DeviceInfo()
    var session = SessionInfo()
    var experiments = Experiments()
    var search = SearchInfo()
    var modifiers = Modifiers()
    var chats = Chats()
}

extension BackendState {
    struct UserInfo {
        struct PhoneInfo {
            var added: Date
            var phone: String
        }

        struct EmailInfo {
            var email: String
            var confirmed = true
        }

        struct FavoritesInfo {
            var offers: [BackendState.Offer] = [.bmw3g20]
        }

        var id = "19201127"
        var alias = "test user"
        var fullName = "Test User"
        var geoID: UInt64 = 213
        var cityID: UInt64 = 1123
        var regionID: UInt64 = 87
        var authorized = true
        var isDealer = false

        var emails = [
            EmailInfo(email: "test@example.com")
        ]
        var phones = [
            PhoneInfo(added: Date().adding(-1, component: .year), phone: "71234567890")
        ]
        var favorites = FavoritesInfo()
    }

    struct DeviceInfo {
        var uid = "g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb"
    }

    struct SessionInfo {
        var id = "a:g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb|1573116174599.604800.f8QbOBNLQ34w6k86sTWvlw.NSNfTZZlBLrKgNyPU76OSYpg8Jmyj6bQgJmOhh1IIlU"
    }

    struct SearchInfo {
        var listing: [Offer] = [.bmw3g20]
    }

    struct Modifiers {
        var offers: [Offer: [(inout Auto_Api_Offer, BackendState) -> Void]] = [:]

        mutating func addOfferModifier(for offer: Offer, modifier: @escaping (inout Auto_Api_Offer, BackendState) -> Void) {
            offers[offer, default: []].append(modifier)
        }
    }

    struct Chats {
        struct Room {
            enum ID: String, CaseIterable {
                case chatWithBmw3g20
            }

            struct User {
                var id: String
                var alias: String
            }

            struct Message {
                var id: String = UUID().uuidString
                var text: String = "Lorem ipsum dolor sit amet consectetur adipisicing elit."
                var author: String?
            }

            var id: ID
            var offer: Offer?
            var users: [User] = [User(id: "3200d72d65d84c31", alias: "Smith")]
            var messages: [Message] = [
                Message()
            ]
        }

        var rooms: [Room] = [
            chatWithBmw3g20
        ]

        static let chatWithBmw3g20 = Room(id: .chatWithBmw3g20, offer: .bmw3g20)
    }
}

extension BackendState {
    enum Offer: String, CaseIterable {
        case bmw3g20 = "1103768888-7759ab74"
    }
}
