import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

class BackendStatefulTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    private var _server: StubServer!
    private var baseAppSettings: [String: Any]?

    var state: BackendState {
        get { .global }
        set { BackendState.global = newValue }
    }

    var expectedFailures: ExpectedFailures {
        get { .global }
        set { ExpectedFailures.global = newValue }
    }

    override var appSettings: [String: Any] {
        get {
            if let setting = baseAppSettings {
                return setting
            } else {
                baseAppSettings = super.appSettings
                return baseAppSettings!
            }
        }
        set { baseAppSettings = newValue }
    }

    override func setUp() {
        super.setUp()

        state = BackendState()
        expectedFailures = ExpectedFailures()

        let server = StubServer(port: self.port)
        server.forceLoginMode = .preservingResponseState

        setUpServer(server)

        self._server = server
        try! server.start()
    }

    func setUpServer(_ server: StubServer) {
        addDeviceHandlers(server)
        addAuthHandlers(server)
        addUserHandlers(server)
        addOfferHandlers(server)
        addSearchHandlers(server)
        addChatHandlers(server)
        addCarfaxHandlers(server)

        server.addHandler("GET /session") {
            Responses.Session.success(for: .global)
        }
    }

    private func addAuthHandlers(_ server: StubServer) {
        server.addHandler("POST /auth/login-or-register") { (_: Vertis_Passport_LoginOrRegisterParameters) in
            Responses.Auth.LoginOrRegister.success(for: .global)
        }
    }

    private func addDeviceHandlers(_ server: StubServer) {
        server.addHandler("POST /device/hello") {
            Responses.Device.hello(for: .global)
        }
    }

    private func addUserHandlers(_ server: StubServer) {
        server.addHandler("POST /user/confirm") { (_: Vertis_Passport_ConfirmIdentityParameters) in
            if ExpectedFailures.global.wrongCode {
                MessageResponse(code: 404, message: Responses.User.Confirm.failure(for: .global))
            } else {
                Responses.User.Confirm.success(for: .global)
                    .modifyState { $0.user.authorized = true }
            }
        }

        server.addHandler("GET /user *") {
            Responses.User.get(for: .global)
        }

        server.addHandler("GET /user/favorites/all *") {
            Responses.User.Favorites.success(for: .global)
        }
    }

    private func addOfferHandlers(_ server: StubServer) {
        for offer in BackendState.Offer.allCases {
            for category in ["cars", "moto", "trucks"] {
                server.addHandler("GET /offer/\(category)/\(offer.rawValue)/phones") {
                    Responses.Offer.Phones.success(for: .global, offer: offer)
                }
            }
        }
    }

    private func addSearchHandlers(_ server: StubServer) {
        server.addHandler("POST /search/cars *") {
            Responses.Search.Cars.success(for: .global)
        }
    }

    private func addChatHandlers(_ server: StubServer) {
        server.addHandler("GET /chat/room") {
            Responses.Chat.Room.get(for: .global)
        }

        for id in BackendState.Chats.Room.ID.allCases {
            server.addHandler("GET /chat/room/\(id.rawValue)") {
                Responses.Chat.Room.get(id: id, for: .global)
            }
        }
    }

    private func addCarfaxHandlers(_ server: StubServer) {
        for offer in BackendState.Offer.allCases {
            server.addHandler("GET /carfax/offer/cars/\(offer.rawValue)/raw *") {
                Responses.Carfax.Offer.Cars.get(offer: offer, state: .global)
            }

            server.addHandler("GET /ios/makeXmlForOffer *") {
                Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForOffer-bought")
            }
            server.addHandler("GET /ios/makeXmlForReport *") {
                Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForReport-bought")
            }
        }
    }
}
