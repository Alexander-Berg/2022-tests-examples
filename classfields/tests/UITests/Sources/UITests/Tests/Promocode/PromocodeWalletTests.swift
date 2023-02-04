//
//  PromocodeWalletTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/9/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuWallet AutoRuPayments
final class PromocodeWalletTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        advancedMockReproducer.setup(server: server, mockFolderName: "PromocodeWalletTests")

        server.addMessageHandler("POST /billing/autoru/payment/init") {
            Auto_Api_Billing_InitPaymentResponse.with { (response: inout Auto_Api_Billing_InitPaymentResponse) in
                    response.baseCost = 0
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

        try! server.start()
    }

    func test_walletPrmocodeEntry() {
        let reloadPricesExpectation = expectationForRequest { req -> Bool in
            req.method == "GET" && req.uri.lowercased().hasPrefix("/user/offers/all")
        }
        let promocodeActivationExpectation = expectationForRequest { req -> Bool in
            if(req.method == "POST" && req.uri.lowercased() == "/promocode/saletop") {
                if let data = req.messageBody,
                   let request = try? Auto_Api_PromocodeActivationRequest(jsonUTF8Data: data) {
                    return request.validation.products == []
                }
            }
            return false
        }

        let steps = openWallet()
            .tapSegment(.promocodes)
            .tapPromocodeEntry()
            .enterPromocodeText("ifiwerearichman")
            .validatePromocodeInputIs("ifiwerearichman")
            .tapActivatePromocode()
            .validateHasPromocodeSnackbar(false)
            .validatePromocodeActivationError("Такого промокода не существует")
            .tapPromocodeEntry()
            .tapClearPromocode()
            .validatePromocodeInputIs("Введите промокод")

        steps.tapPromocodeEntry()
            .enterPromocodeText("saletop")
            .tapActivatePromocode()
            .validateHasPromocodeSnackbar()
            .validatePromocodeCellScreenshot(idPrefix: "promocode_percent_20_5")

        self.wait(for: [
            reloadPricesExpectation,
            promocodeActivationExpectation
        ], timeout: 3)
    }

    func test_walletPaymentHasNoPromocodeOption() {
        openWallet()
            .tapSegment(.balance)
            .tapRefundWalletButton()
            .validatePromocodeOptionAvailable(false)
    }

    private func openWallet() -> WalletSteps {
        launchMain()
            .container
            .focus(on: .tabBar, ofType: .tabBar) {
                $0.tap(.tab(.favorites))
            }
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.superMenuButton) }
            .should(provider: .superMenuScreen, .exist)
            .focus {
                $0.tap(.wallet)
            }
        return WalletSteps(context: self)
    }
}
