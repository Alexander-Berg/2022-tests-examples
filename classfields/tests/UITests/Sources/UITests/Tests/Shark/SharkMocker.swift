//
//  SharkMocker.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.02.2021.
//

import Foundation
import AutoRuProtoModels

class SharkMocker {
    let applicationId = "test_\(UInt16.random(in: 1024 ..< UInt16.max))"
    struct Claim {
        var status: Vertis_Shark_CreditApplication.Claim.ClaimState
        var amount: Int
        var rate: Float
        var term: Int
        var product: Product

        init(_ status: Vertis_Shark_CreditApplication.Claim.ClaimState, _ product: Product, term: Int = 48) {
            self.status = status
            self.amount = 3_000_000
            self.term = 48
            self.term = term
            self.rate = 20
            self.product = product
        }
    }

    enum ApplicationOfferType {
        case none
        case offer(id: String?, isActive: Bool, allowedProducts: [Product])
    }

    enum Product {
        case tinkoff_1
        case tinkoff_2
        case tinkoff_creditCard
        case tinkoff_refin
        case tinkoff_cash
        case tinkoff_auto
        case alfa499
        case raif
        case sberbank
        case sravniru(String)

        var rawValue: String {
            switch self {
            case .tinkoff_1:
                return "tinkoff-1"
            case .tinkoff_2:
                return "tinkoff-2"
            case .tinkoff_creditCard:
                return "tinkoff_creditCard"
            case .tinkoff_refin:
                return "tinkoff_refin"
            case .tinkoff_cash:
                return "tinkoff_cash"
            case .tinkoff_auto:
                return "tinkoff_auto"
            case .alfa499:
                return "alfa499"
            case .raif:
                return "raif"
            case .sberbank:
                return "sberbank"
            case .sravniru(_):
                return "sravniru"
            }
        }
    }

    var server: StubServer

    init(server: StubServer) {
        self.server = server
        server.forceLoginMode = .forceLoggedIn
    }

    func forceLogin(_ value: Bool) -> Self {
        server.forceLoginMode = value ? .forceLoggedIn : .preservingResponseState
        return self
    }

    func favoriteLast() -> Self {
        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "shark_user_favorites_all")
        }

        server.addHandler("GET /history/last/CARS *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "shark_history_last_CARS")
        }

        return self
    }

    func mockSearch(photoCount: Int) -> Self {
        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            var resp = Auto_Api_OfferListingResponse(mockFile: "sale_list_by_fresh")
            resp.offers[0].services = [Auto_Api_PaidService.with({ $0.service = "all_sale_premium" })]
            resp.offers[0].state.imageUrls = Array(resp.offers[0].state.imageUrls.prefix(photoCount))
            resp.offers[0].sharkInfo = resp.offers[1].sharkInfo

            resp.offers[1].state.imageUrls = Array(resp.offers[1].state.imageUrls.prefix(photoCount))
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    func baseMock(offerId: String, isDealer: Bool = false) -> Self {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok")
        }

        server.addHandler("POST /search/cars *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "sale_list_by_fresh")
        }

        mockCalculator()

        server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_report_raw_GET_ok")
        }
        server.addHandler("GET /carfax/bought-reports/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_bought-reports_GET_ok")
        }

        server.addHandler("GET /carfax/offer/cars/\(offerId)/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_offer_cars_1090794514-915f196d_raw_GET_ok")
        }

        server.addHandler("GET /credits/tinkoff/claims/credits") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_tinkoff_claims_credits_ok")
        }
        server.addHandler("GET /credits/tinkoff/preliminary") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_TINKOFF_preliminary_GET_empty_ok")
        }

        server.addHandler("POST /shark/credit-application/create") { (_, _) -> Response? in
            var resp = Vertis_Shark_Api.CreditApplicationResponse(mockFile: "shark_credit-product_list")
            let applicationResp = Auto_Api_Shark_RichCreditApplicationResponse(mockFile: "shark_credit-application_active")
            resp.creditApplication = applicationResp.creditApplication
            resp.creditApplication.id = self.applicationId
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }

        server.addHandler("GET /geo/suggest *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "geo_suggest_1")
        }

        server.addHandler("POST /suggest/fms_unit") { (_, _) -> Response? in
            return Response.responseWithStatus(body: "{\"suggestions\":[{\"value\":\"УВД По Урюпинску\",\"data\":{}}]}".data(using: .utf8))
        }
        server.addHandler("POST /suggest/fio") { (_, _) -> Response? in
            return Response.responseWithStatus(body: "{\"suggestions\":[{\"value\":\"Тестовое имя\",\"data\":{\"patronymic\":\"тест\",\"gender\":\"UNKNOWN\",\"surname\":\"тест\",\"name\":\"тест\"}}]}".data(using: .utf8))
        }
        server.addHandler("POST /suggest/address") { (_, _) -> Response? in
            return Response.responseWithStatus(body: "{\"suggestions\":[{\"value\":\"г Москва\",\"data\":{\"city_with_type\":\"г Москва\",\"block_type_full\":\"корпус\"}},{\"value\":\"г Москва, Кутузовский проспект 17\",\"data\":{\"region_with_type\":\"г Москва\",\"street_with_type\":\"Кутузовский проспект\",\"city_with_type\":\"г Москва\",\"house\":\"17\",\"block_type_full\":\"корпус\"}}]}".data(using: .utf8))
        }
        server.addHandler("POST /suggest/party") { (_, _) -> Response? in
            return Response.responseWithStatus(body: "{\"suggestions\":[{\"value\":\"Рога и копыта\",\"data\":{\"inn\":\"12321321\",\"address\":{\"value\":\"г Москва, Кутузовский проспект 17\",\"data\":{\"region_with_type\":\"г Москва\",\"street_with_type\":\"Кутузовский проспект\",\"city_with_type\":\"г Москва\",\"house\":\"17\",\"block_type_full\":\"корпус\"}},\"employee_count\":50}}]}".data(using: .utf8))
        }
        server.addHandler("POST /findById/party") { (_, _) -> Response? in
            return Response.responseWithStatus(body: "{\"suggestions\":[{\"value\":\"Рога и копыта\",\"data\":{\"inn\":\"12321321\",\"address\":{\"value\":\"г Москва, Кутузовский проспект 17\",\"data\":{\"region_with_type\":\"г Москва\",\"street_with_type\":\"Кутузовский проспект\",\"city_with_type\":\"г Москва\",\"house\":\"17\",\"block_type_full\":\"корпус\"}},\"employee_count\":50}}]}".data(using: .utf8))
        }

        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.phones = [{
                var phone = Vertis_Passport_UserPhone()
                phone.phone = "+7 987 564-32-12"
                return phone
            }()]
            profile.user.emails = [{
                var email = Vertis_Passport_UserEmail()
                email.email = "pet@sa.ru"
                return email
            }()]
            profile.user.profile.autoru.about = ""
            profile.user.profile.autoru.fullName = "Вася ПЕТРОВ АЛЕК"
            if isDealer {
                profile.user.profile.autoru.clientID = "dealerId"
            }
            return profile
        }()

        server.addHandler("GET /user *") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data())
        }
        server.addHandler("POST /auth/login-or-register") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_auth_login-or-register")
        }
        server.addHandler("POST /user/confirm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_user_confirm")
        }

        return self
    }

    func mockCalculator() {
        server.addHandler("POST /shark/credit-product/calculator") { (_, _) -> Response? in
            return Response.okResponse(fileName: "shark_credit-product_param")
        }
    }

    func mockOffer(withSharkInfo: Bool) -> Self {
        var resp = Auto_Api_OfferResponse(mockFile: "offer_CARS_1098252972-99d8c274_ok")
        if withSharkInfo {
            resp.offer.sharkInfo = Auto_Api_SharkInfo.with({ (info) in
                info.suitableCreditProductIds = ["tinkoff-1", "tinkoff-2", "alfa499"]
            })
        }

        var historyResp = Auto_Api_OfferListingResponse(mockFile: "history_last_all_credit_ok")
        historyResp.offers = [resp.offer]

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! historyResp.jsonUTF8Data())
        }

        server.addHandler("GET /offer/CARS/1098252972-99d8c274 *") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mockProductList(products: [Product], appProduct: [Product]? = nil, isMany: Bool = false, isForPromo: Bool = false) -> Self {
        server.addHandler("POST /shark/credit-product/list *") { (requestEnt, _) -> Response? in
            var resp = Auto_Api_Shark_RichCreditProductsResponse(mockFile: "shark_credit-product_list")
            let tmpProduct = resp.creditProducts[0]
            let request = try! Vertis_Shark_Api.CreditProductsRequest(jsonUTF8Data: requestEnt.messageBody!)
            resp.creditProducts = []
            let respProducts: [Product]

            if !request.byCreditApplication.creditApplicationID.isEmpty {
                respProducts = appProduct ?? products
            } else {
                respProducts = products
            }

            let bank = resp.banks[0]
            resp.banks = []
            for (index, product) in respProducts.enumerated() {
                var prod = tmpProduct
                if isForPromo {
                    prod.isActive = true
                    prod.id = "\(product.rawValue)-1"
                } else {
                    prod.id = product.rawValue
                }
                switch product {
                case .tinkoff_creditCard:
                    prod.productType = .creditCard
                    prod.creditProductPayload.creditCard.gracePeriodDays = 100
                    prod.creditProductPayload.creditCard.yearlyFeeRub = 590
                    prod.interestRateRange.from = 12.4
                    prod.interestRateRange.to = 25
                    prod.amountRange.to = 500_000
                case .alfa499:
                    prod.productType = .auto
                    prod.interestRateRange.from = 4.99
                case .tinkoff_refin:
                    prod.productType = .refinancing
                case .tinkoff_cash:
                    prod.productType = .consumer
                case .sberbank:
                    prod.productType = .iframe
                    prod.bankType = .sberbank
                case .sravniru:
                    prod.productType = .iframe
                    prod.bankType = .sravniru
                default:
                    prod.productType = .auto
                }
                var prodBank = bank
                if isMany {
                    prodBank.name = product.rawValue
                }
                switch product {
                case .raif:
                    prodBank.bankType = .raiffeisen
                case .alfa499:
                    prodBank.bankType = .alfabank
                case .sberbank:
                    prodBank.bankType = .sberbank
                case .sravniru:
                    prodBank.bankType = .sravniru
                default:
                    prodBank.bankType = .tinkoff
                }
                prod.bankType = prodBank.bankType
                prodBank.id = isForPromo ? "\(prodBank.bankType)".lowercased() : "\(index)"
                prod.bankID = prodBank.id
                prodBank.logo184X42DarkURL = "asd.ru"
                prodBank.logo56X56RoundURL = "asd.ru"
                prodBank.colorHex = "000000"
                resp.banks.append(prodBank)
                resp.creditProducts.append(prod)
            }

            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mockNoApplication() -> Self {
        let resp = Auto_Api_Shark_RichCreditApplicationResponse()
        server.addHandler("GET /shark/credit-application/active *") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mockApplication(
        status: Vertis_Shark_CreditApplication.State,
        filled: Bool = false,
        claims: [Claim],
        offerType: ApplicationOfferType,
        hasArchive: Bool = false,
        modifyResp: ((Auto_Api_Shark_RichCreditApplicationResponse) -> Auto_Api_Shark_RichCreditApplicationResponse)? = nil) -> Self {
            var resp = Auto_Api_Shark_RichCreditApplicationResponse(mockFile: filled ? "shark_credit-application_active_filled" : "shark_credit-application_active")

            for (index, claimModel) in claims.enumerated() {
                var claim = Vertis_Shark_CreditApplication.Claim()
                claim.state = claimModel.status
                claim.id = "sad-\(index)"
                claim.creditProductID = claimModel.product.rawValue
                claim.approvedInterestRate = claimModel.rate
                claim.approvedMaxAmount = UInt64(claimModel.amount)
                claim.approvedTermMonths = UInt32(claimModel.term)
                claim.approvedMinInitialFeeRate = 0.2
                if case .sberbank = claimModel.product {
                    claim.bankPayload.sberbank.redirectURL = "test.test"
                } else if case .sravniru(let url) = claimModel.product, !url.isEmpty {
                    claim.bankPayload.sravniRu.redirectURL = url
                }
                resp.creditApplication.claims.append(claim)
            }

            resp.creditApplication.state = status
            resp.creditApplication.id = applicationId
            switch offerType {
            case .none:
                break
            case let .offer(id, isActive, allowedProducts):
                resp.offers = { () -> [Auto_Api_Offer] in
                    var resp = Auto_Api_OfferResponse(mockFile: "offer_CARS_1098252972-99d8c274_ok")
                    resp.offer.sharkInfo.suitableCreditProductIds = allowedProducts.map(\.rawValue)
                    resp.offer.status = isActive ? .active : .inactive
                    id.flatMap {
                        resp.offer.id = $0
                    }
                    return [resp.offer]
                }()
            }

            if let modifedResp = modifyResp?(resp) {
                resp = modifedResp
            }
            server.addHandler("GET /shark/credit-application/get/\(resp.creditApplication.id) *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }

            server.addHandler("GET /shark/credit-application/active *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }

            var updateResponse = Vertis_Shark_Api.CreditApplicationResponse()

            updateResponse.creditApplication = resp.creditApplication
            updateResponse.result.state = .ok(.init())

            server.addHandler("POST /shark/credit-application/update/\(updateResponse.creditApplication.id) *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! updateResponse.jsonUTF8Data())
            }

            server.addHandler("PUT /shark/credit-application/add-products/\(self.applicationId) *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! Vertis_Shark_Api.DefaultResponse().jsonUTF8Data())
            }
            server.addHandler("DELETE /shark/credit-application/cancel-products/\(self.applicationId)?credit_product_ids=tinkoff-1") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! Vertis_Shark_Api.DefaultResponse().jsonUTF8Data())
            }
            if hasArchive {
                server.addHandler("GET /shark/credit-application/list?page=1&page_size=20&with_offers=true") { (_, _) -> Response? in
                    var resp = Auto_Api_Shark_RichCreditApplicationsResponse()
                    var app = updateResponse.creditApplication
                    app.id = "archive"
                    app.payload.autoru.offers.append(.init())
                    app.payload.autoru.offers[0].id = "1098252972-99d8c274"
                    resp.offers = {
                        return [Auto_Api_OfferResponse(mockFile: "offer_CARS_1098252972-99d8c274_ok").offer]
                    }()
                    resp.creditApplications.append(app)
                    return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
                }
            }

            return self
        }

    func start() {
        try! server.start()
    }

    func stop() {
        server.stop()
    }
}
