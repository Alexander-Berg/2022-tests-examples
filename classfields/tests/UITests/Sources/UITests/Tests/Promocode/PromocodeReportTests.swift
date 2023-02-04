//
//  PromocodeReportTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/16/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuUserProfile AutoRuPayments
final class PromocodeReportTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    
    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addMessageHandler("GET /billing/subscriptions/offers-history-reports/prices?domain=autoru") {
            Auto_Salesman_User_ProductPrices.with { (response: inout Auto_Salesman_User_ProductPrices) in
                    response.productPrices.append(
                        Auto_Salesman_User_ProductPrice.with { (response: inout Auto_Salesman_User_ProductPrice) in
                            response.product = "offers-history-reports"
                            response.counter = 10
                            response.days = 365
                            response.price.basePrice = 99000
                            response.price.effectivePrice = 99000
                            response.duration = .init(seconds: 15_552_000, nanos: 0)
                            response.productPriceInfo = Auto_Salesman_User_ProductPriceInfo.with({ (info: inout Auto_Salesman_User_ProductPriceInfo) in
                                info.aliases = ["offers-history-reports-10"]
                            })
                        }
                    )
                }
        }

        server.addMessageHandler("POST /billing/autoru/payment/init") {
            Auto_Api_Billing_InitPaymentResponse.with { (response: inout Auto_Api_Billing_InitPaymentResponse) in
                    response.cost = 99000
                    response.baseCost = 99000
                    response.ticketID = "xxx"
                    response.salesmanDomain = "autoru"
                    response.accountBalance = 15000
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
                            response.effectivePrice = 99000
                            response.basePrice = 99000
                            response.duration = .init(seconds: 31_536_000, nanos: 0)
                            response.name = "Отчет о проверке по VIN"
                        }
                    )
                }
        }

        server.addHandler("POST /promocode/ifiwerearichman", { _, _ in
            return Response.badResponse(code: .notFound)
        })

        server.addHandler("POST /promocode/promocode", { _, _ in
            return Response.okResponse(message: Auto_Api_SuccessResponse.with({ $0.status = .success }))
        })

        try! server.start()
    }

    func test_promocodeOptionAvailableInPayments() {
        server.addHandler("GET /offer/CARS/1101613244-b69e1290") { (_, _) -> Response? in
            return Response.okResponse(fileName: "PaymentHistoryAll_discount-precedence_offer")
        }
        server.addHandler("GET /carfax/offer/cars/1101613244-b69e1290/raw?version=2") { (_, _) -> Response? in
            return Response.okResponse(fileName: "PaymentHistoryAll_discount-precedence_carfax")
        }
        mocker.mock_reportLayoutForOffer(bought: false)

        let steps = launch(on: .saleCardScreen,
                           options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290"),
                                          overrideAppSettings: ["showVASPaymentListExpandedByDefault": "true"]))
            .scrollToReportBuySingleButton(windowInsets: .init(top: 0, left: 0, bottom: 80, right: 0))
            .tapReportBuySingleButton()

        steps.tapPromocodeOption()
            .wait(for: 1)
            .enterPromocodeText("ifiwerearichman")
            .activatePromocode()
            .validatePromocodeActivationError("Такого промокода не существует")
            .clearPromocodeText()
            .validatePromocodeInputIs("")

        let packagePricesExpectation = expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri.lowercased().hasPrefix("/billing/subscriptions/offers-history-reports/prices")
        }
        let paymentPricesExpectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/billing/autoru/payment/init"
        }
        let promocodeActivationExpectation = expectationForRequest { req -> Bool in
            if(req.method == "POST" && req.uri.lowercased() == "/promocode/promocode") {
                if let data = req.messageBody,
                   let request = try? Auto_Api_PromocodeActivationRequest(jsonUTF8Data: data) {
                    return request.validation.products.contains("offers-history-reports-10")
                }
            }
            return false
        }

        steps
            .as(PromocodeBottomsheetSteps.self)
            .enterPromocodeText("promocode")
            .activatePromocode()

        wait(for: [
            packagePricesExpectation,
            paymentPricesExpectation,
            promocodeActivationExpectation
        ], timeout: 3)

        steps
            .validatePaymentScreenExists()
    }
}
