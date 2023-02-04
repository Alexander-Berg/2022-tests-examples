//
//  PromocodeWizardTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/19/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuWizard AutoRuPayments
final class PromocodeWizardTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

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

        advancedMockReproducer.setup(server: server, mockFolderName: "PromocodeWizardTests")

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

    func test_wizardPromocodeEntry() {
        let steps = mainSteps
            .openOffersTab()
            .tapSkipPanoramaPromo()
            .tapAddOffer()
            .tapToCarsCategoryForWizard()
            .handleSystemAlertIfNeeded()
            .tapNext()
            .tapNext()
            .wait(for: 1)
            .tapSkip()
            .tapPTS()
            .tapOwner()
            .tapSkip()
            .tapAddPhoto()
            .handleSystemAlertIfNeeded()
            .addPhoto()
            .tapAddPhotoInList()
            .addPhoto(index: 1)
            .tapNext()
            .tapSkip()
            .typeInActiveField("10000")
            .tapNext()
            .tapContinue()
            .tapNext()
            .wait(for: 1)
            .typeInActiveField("1800000")
            .tapNext()

        let reloadPricesExpectation = expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri.lowercased() == "/user/offers/cars/1103241307-876733ad"
        }
        let promocodeActivationExpectation = expectationForRequest { req -> Bool in
            if(req.method == "POST" && req.uri.lowercased() == "/promocode/promocode") {
                if let data = req.messageBody,
                   let request = try? Auto_Api_PromocodeActivationRequest(jsonUTF8Data: data) {
                    return request.validation.products == ["placement"]
                }
            }
            return false
        }

        steps.wait(for: 3)
            .as(AdViewPickerSteps.self)
            .tapActivateStandard()
            .wait(for: 3)
            .tapPromocodeOption()
            .enterPromocodeText("promocode")
            .activatePromocode()

        self.wait(for: [
            reloadPricesExpectation,
            promocodeActivationExpectation
        ], timeout: 3)
    }
}
