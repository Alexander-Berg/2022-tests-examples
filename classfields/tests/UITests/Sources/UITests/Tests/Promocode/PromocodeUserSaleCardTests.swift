//
//  PromocodeUserSaleCardTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/19/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuUserProfile AutoRuPayments
final class PromocodeUserSaleCardTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
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

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_premium_offer_inactive")
        }

        server.addHandler("GET /user/offers/CARS/1097093888-84b3a4c4") { (_, _) -> Response? in
            Response.okResponse(fileName: "user_offers_CARS_1103241307-876733ad")
        }

        server.addHandler("GET /user/offers/CARS/1103241307-876733ad") { _, _ -> Response? in
            Response.okResponse(fileName: "user_offers_CARS_1103241307-876733ad")
        }

        server.addHandler("POST /user/offers/CARS/1103241307-876733ad/activate", { _, _ -> Response? in
            Response.okResponse(fileName: "user_offers_CARS_1103241307-876733ad_activate")
        })

        server.addMessageHandler("POST /billing/autoru/payment/init") {
            Auto_Api_Billing_InitPaymentResponse.with { (response: inout Auto_Api_Billing_InitPaymentResponse) in
                    response.baseCost = 4199
                    response.cost = 4199
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
                }
        }

        server.addHandler("POST /promocode/promocode", { _, _ in
            return Response.okResponse(message: Auto_Api_SuccessResponse.with({ $0.status = .success }))
        })

        try! server.start()
    }

    func test_saleCardPricesUpdateOnPromocodeActivation() {
        let steps = self.mainSteps
            .openTab(.offers)
            .as(OffersSteps.self)
            .openOffer(offerId: "1097093888-84b3a4c4")
            .checkScreenLoaded()
            .tapActivate()
            .as(VASTrapSteps.self)
            .wait(for: 1)
            .tapPurchaseVASButton()

        let reloadPricesExpectation = expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri.lowercased() == "/user/offers/cars/1103241307-876733ad"
        }
        let promocodeActivationExpectation = expectationForRequest { req -> Bool in
            if(req.method == "POST" && req.uri.lowercased() == "/promocode/promocode") {
                if let data = req.messageBody,
                   let request = try? Auto_Api_PromocodeActivationRequest(jsonUTF8Data: data) {
                    return request.validation.products == ["turbo-package"]
                }
            }
            return false
        }

        steps
            .tapPromocodeOption()
            .enterPromocodeText("promocode")
            .activatePromocode()

        self.wait(for: [
            reloadPricesExpectation,
            promocodeActivationExpectation
        ], timeout: 3)
    }
}
