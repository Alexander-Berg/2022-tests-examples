//
//  OfferBookingTests.swift
//  UITests
//
//  Created by Alexander Ignatyev on 12/8/20.
//

import AutoRuProtoModels
import SwiftProtobuf
import Snapshots
import XCTest

final class OfferBookingTests: BaseTest {
    private let suiteName = SnapshotIdentifier.suiteName(from: #file)
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func testBookingOffer() {
        addBookingTermsHandler(price: 1_000)
        addProfileHandler(phone: "123456789", fullName: "Иванов Петр Иванович")

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .wait(for: 2)
            .tapBookButton()
            .validateSnapshot(of: "offer_booking_terms")
            .tapBookButton()
            .tapBookButton()
            .tapShowFavoritesButton()
            .as(FavoritesSteps.self)
            .tap(.offer(.custom("1098230510-dd311329")))
            .should(provider: .saleCardScreen, .exist)
            .focus({
                $0.booked(byMe: true)
            })
    }

    func testBookingOffer_termsFail() {
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .wait(for: 2)
            .tapBookButton()
            .wait(for: 2)
            .validateSnapshot(of: "offer_booking_terms")
    }

    func testBookingOffer_requiredNameAndPhone() {
        addBookingTermsHandler(price: 1_000)
        addProfileHandler(phone: nil, fullName: nil)
        
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .wait(for: 2)
            .tapBookButton()
            .tapBookButton()
            .wait(for: 2)
            .validateSnapshot(of: "offer_booking")
            .type(name: "Иванов Иван Иванович")
            .tapPhoneField()
            .tapAddPhoneButton()
            .type(phone: "9998884433")
            .type(code: "1234")
            .tapDoneButton()
            .bookButton(price: 10, enabled: true)
            .tapBookButton()
            .tapShowFavoritesButton()
    }

    // MARK: - Private

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }
        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "history_last_all_characteristic_ok", userAuthorized: true)
        }
        server.addHandler("GET /offer/CARS/1092222570-3203c7f6") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1092222570-3203c7f6_ok", userAuthorized: true)
        }
        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1098230510-dd311329_ok", userAuthorized: true)
        }
        server.addHandler("GET /offer/CARS/1098230510-dd311329") { (_, _) -> Response? in
            var model: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098230510-dd311329_ok")
            model.offer.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.with({ (booking: inout Auto_Api_AdditionalInfo.Booking) in
                booking.allowed = true
            })
            return Response.okResponse(message: model, userAuthorized: true)
        }
        // Skip payment
        server.addHandler("POST /billing/booking/payment/init") {_, _ in
            Response.badResponse(code: .productAlreadyActivated)
        }
        server.addHandler("POST /user/phones") { _, _ in
            Response.okResponse(fileName: "best_offers_user_phones")
        }
        server.addHandler("POST /user/phones/confirm") { _, _ in
            Response.okResponse(fileName: "best_offers_user_phones_confirm")
        }

        try! server.start()
    }

    private func addProfileHandler(phone: String?, fullName: String?) {
        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            var profile = Auto_Api_UserResponse()
            profile.user.id = "112231"

            if let number = phone {
                var phone = Vertis_Passport_UserPhone()
                phone.phone = number
                profile.user.phones = [phone]
            }
            if let fullName = fullName {
                profile.user.profile.autoru.fullName = fullName
            }
            return Response.okResponse(message: profile)
        }
    }

    private func addBookingTermsHandler(price: UInt64) {
        server.addHandler("GET /booking/terms/cars/1098230510-dd311329?category=cars&offerId=1098230510-dd311329") { _, _ -> Response? in
            var terms = Auto_Booking_Api_GetBookingTermsResponse()
            terms.price.kopecks = price
            terms.deadline = Google_Protobuf_Timestamp(date: Date().addingTimeInterval(60 * 60 * 24 * 365))
            return Response.okResponse(message: terms)
        }
    }

}
