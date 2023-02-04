import XCTest
import AutoRuProtoModels

final class PaymentWithPromocodeTests: BaseTest {
    override func setUp() {
        super.setUp()
        setupServer()
    }

    func testPaymentWhenUserHasPromocode() {
        let offer = makeOffer()

        server.addHandler("GET /ios/makeXmlForOffer?offer_id=\(offer.id)") {
            self.makeCarReportResponseWithFreeSingleReportOption(isPaid: false)
        }

        server.addHandler("GET /ios/makeXmlForReport?offer_id=\(offer.id)") {
            Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForReport-bought")
        }

        server.addHandler("GET /ios/makeXmlForOffer?decrement_quota=true&offer_id=\(offer.id)") {
            self.makeCarReportResponseWithFreeSingleReportOption(isPaid: true)
        }

        api.carfax.offer.category(.cars).offerID(offer.id).raw.get(parameters: .wildcard).ok(
            mock: .file("carfax_offer_cars_1090794514-915f196d_raw_GET_ok")
        )

        api.offer.category(.cars).offerID(offer.id).get.ok(mock: .model { response in
            response.offer = offer
            response.status = .success
        })

        api.billing.salesmanDomain(.autoru).payment.`init`.post.ok(
            mock: .model(makeInitPaymentResponse())
        )

        api.billing.salesmanDomain(.autoru).payment.process.post.ok(
            mock: .dynamic { _, _  in
                Thread.sleep(forTimeInterval: 1)
                return self.makeProcessPaymentResponse()
            }
        )

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offer.id)")))
            .scroll(to: .purchaseReportButton)
            .tap(.purchaseReportButton)
            .should(provider: .paymentOptionsScreen, .exist)
            .should(provider: .carReportScreen, .exist)
    }

    private func makeCarReportResponseWithFreeSingleReportOption(isPaid: Bool) -> Auto_Api_ReportLayoutResponse {
        var response = Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForOffer")
        let singleReportPriceIndex = response.billing.servicePrices.firstIndex(where: { $0.counter == 1 })!
        response.billing.servicePrices[singleReportPriceIndex].price = 0
        response.billing.servicePrices[singleReportPriceIndex].originalPrice = 397

        response.report.additionalData.priceData[0].servicePrices = response.billing.servicePrices
        response.report.reportType = isPaid ? .paidReport : .freeReport
        return response
    }

    private func makeOffer() -> Auto_Api_Offer {
        let response = Auto_Api_OfferResponse(mockFile: "user_offer_102125077-f94ee448")
        return response.offer
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()

        let promocode = Auto_Api_Promocode.with { promocode in
            promocode.product = .with { product in
                product.humanName = "Отчёт ПроАвто"
            }

            promocode.deadline = .init(date: Date().adding(1, component: .year))
            promocode.percent = .with { percent in
                percent.percent = 100
                percent.count = 1
            }
        }

        let promocodeListing = Auto_Api_PromocodeListing.with { listing in
            listing.promocodes = [promocode]
        }

        api.promocode.listing.get.ok(
            mock: .model(promocodeListing)
        )

        mocker.startMock()
    }

    private func makeInitPaymentResponse() -> Auto_Api_Billing_InitPaymentResponse {
        var response = Auto_Api_Billing_InitPaymentResponse()
        response.ticketID = "user:63016812-926c492c13ba2124f24f875fa5fc6ec0-1656414122136"
        response.paymentMethods = [
            .with { method in
                method.id = "bank_card"
                method.psID = .yandexkassaV3
                method.name = "Банковская карта"
            },
            .with { method in
                method.id = "sberbank"
                method.psID = .yandexkassaV3
                method.name = "SberPay"
                method.restriction.upperBound = 1500000
            },
            .with { method in
                method.id = "yandex_money"
                method.psID = .yandexkassaV3
                method.name = "ЮMoney"
            },
            .with { method in
                method.id = "yoo_money"
                method.psID = .yandexkassaV3
                method.name = "ЮMoney"
            },
            .with { method in
                method.id = "qiwi"
                method.psID = .yandexkassaV3
                method.name = "QIWI Кошелек"
            }
        ]

        response.baseCost = 39700
        response.cost = 0
        response.accountBalance = 0
        response.salesmanDomain = "autoru"
        response.offerURL = "https://yandex.ru/legal/autoru_licenseagreement"
        response.detailedProductInfos = [
            .with { info in
                info.prolongationAllowed = false
                info.duration.seconds = 31536000
                info.basePrice = 39700
                info.service = "offers-history-reports"
                info.name = "Отчёт о проверке по VIN"
                info.days = 365
                info.prolongationForced = false
                info.prolongationForcedNotTogglable = false
            }
        ]

        return response
    }

    private func makeProcessPaymentResponse() -> Auto_Api_Billing_ProcessPaymentResponse {
        .with { response in
            response.ticketID = "user:63016812-926c492c13ba2124f24f875fa5fc6ec0-1656414122136"
        }
    }
}
