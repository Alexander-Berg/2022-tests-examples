//
//  PromocodeGarageTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/16/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarageCard AutoRuPayments
final class PromocodeGarageTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)
    static let garageCard: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override var appSettings: [String: Any] {
        var appSettings = super.appSettings
        appSettings["showVASPaymentListExpandedByDefault"] = "true"
        return appSettings
    }

    override func setUp() {
        super.setUp()
        self.setupServer()

        launch()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user *") { [weak self] _, _ in
            guard let strongSelf = self else {
                return Response.badResponse(code: Auto_Api_ErrorCode.badRequest)
            }
            return Response.responseWithStatus(body: try! strongSelf.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        server.addHandler("GET /session") { (_, _) -> Response? in
            Response.okResponse(fileName: "session", userAuthorized: true)
        }

        server.addHandler("POST /garage/user/cards") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_cards_\(Self.garageCard.id)", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/rating *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        server.addHandler("GET /garage/user/card/\(Self.garageCard.id)") { (_, _) -> Response? in
            Response.okResponse(fileName: "garage_card_\(Self.garageCard.id)_with_report", userAuthorized: true)
        }

        server.addHandler("GET /reference/catalog/CARS/suggest?body_type=COUPE&engine_type=GASOLINE&gear_type=REAR_DRIVE&mark=BMW&model=2ER&super_gen=21006297&tech_param_id=21006573&transmission=AUTOMATIC&year=2018") { _, _ in
            Response.okResponse(fileName: "garage_form_suggest_BMW2", userAuthorized: true)
        }

        server.addMessageHandler("POST /billing/autoru/payment/init") {
            Auto_Api_Billing_InitPaymentResponse.with { (response: inout Auto_Api_Billing_InitPaymentResponse) in
                    response.cost = 13900
                    response.baseCost = 13900
                    response.ticketID = "xxx"
                    response.salesmanDomain = "autoru"
                    response.accountBalance = 0
                    response.paymentMethods = [
                        Vertis_Banker_PaymentMethod.with({ pm in
                            pm.id = "bank-card"
                            pm.psID = .yandexkassaV3
                            pm.preferred = true
                            pm.name = "Банковская карта"
                            pm.properties = Vertis_Banker_PaymentMethod.Properties.with({ props in
                                props.card = Vertis_Banker_PaymentMethod.CardProperties.with({ card in
                                    card.expireMonth = "12"
                                    card.expireYear = "2099"
                                    card.cddPanMask = "555555|4444"
                                    card.brand = .mastercard
                                    card.invoiceID = "261010e7-000f-5000-8000-1dcef5d45d47"
                                })
                            })
                        })
                    ]
                    response.detailedProductInfos.append(
                        Auto_Api_Billing_InitPaymentResponse.DetailedProductInfo.with { (response: inout Auto_Api_Billing_InitPaymentResponse.DetailedProductInfo) in
                            response.days = 365
                            response.effectivePrice = 13900
                            response.basePrice = 13900
                            response.duration = .init(seconds: 31_536_000, nanos: 0)
                            response.name = "Отчет о проверке по VIN"
                        }
                    )
                }
        }

        server.addHandler("POST /promocode/promocode", { _, _ in
            return Response.okResponse(message: Auto_Api_SuccessResponse.with({ $0.status = .success }))
        })

        try! server.start()
    }

    func test_garageCardPromocodeEntryUpdatesCard() {
        server.addHandler("GET /ios/makeXmlForGarage?garage_id=\(Self.garageCard.id)") {
            Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForOffer")
        }

        let packagePricesExpectation = expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri.lowercased() == "/garage/user/card/\(Self.garageCard.id)"
        }
        let paymentPricesExpectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/billing/autoru/payment/init"
        }

        let steps = mainSteps
            .openTab(.garage)
            .as(GarageCardSteps.self)
            .shouldSeeCard()
            .tapBuyReport()

        steps
            .tapPromocodeOption()
            .wait(for: 1)
            .enterPromocodeText("promocode")
            .activatePromocode()

        wait(for: [
            packagePricesExpectation,
            paymentPricesExpectation
        ], timeout: 3)
    }
}
