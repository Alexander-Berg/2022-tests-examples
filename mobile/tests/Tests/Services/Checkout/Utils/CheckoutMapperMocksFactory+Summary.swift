import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Summary

extension CheckoutMapperMocksFactory {
    func makeSummary(
        baseAmount: Int = Constants.Summary.baseAmount,
        discountAmount: Int? = nil,
        promoDiscount: Int? = nil,
        promoCodeDiscount: Int? = nil,
        coinDiscount: Int? = nil,
        totalAmount: Int = Constants.Summary.totalAmount,
        promos: [Promo] = [],
        delivery: CheckoutSummary.DeliveryInfo = Constants.Summary.delivery,
        serviceInfo: CheckoutSummary.ServiceInfo? = nil,
        creditInfo: CheckoutSummary.CreditInfo? = nil,
        installmentsInformation: InstallmentsInformation? = nil,
        helpIsNearInfo: CheckoutSummary.HelpIsNearInfo? = nil,
        bnplInfo: CheckoutSummary.BNPLInfo? = nil
    ) -> CheckoutSummary {
        CheckoutSummary(
            baseAmount: Decimal(baseAmount),
            discountAmount: discountAmount.map { Decimal($0) },
            promoDiscount: promoDiscount.map { Decimal($0) },
            promoCodeDiscount: promoCodeDiscount.map { Decimal($0) },
            coinDiscount: coinDiscount.map { Decimal($0) },
            totalAmount: Decimal(totalAmount),
            promos: promos,
            delivery: delivery,
            serviceInfo: serviceInfo,
            creditInfo: creditInfo,
            installmentsInformation: installmentsInformation,
            helpIsNearInfo: helpIsNearInfo,
            bnplInfo: bnplInfo
        )
    }

    // MARK: - DTO

    func makeSummaryResult(
        baseAmount: Int = Constants.Summary.baseAmount,
        discountAmount: Int? = nil,
        promoDiscount: Int? = nil,
        promoCodeDiscount: Int? = nil,
        coinDiscount: Int? = nil,
        totalAmount: Int = Constants.Summary.totalAmount,
        promos: [PromoResult] = [],
        delivery: DeliveryInfoResult? = Constants.Summary.deliveryResult,
        creditInformation: CreditInfoResult? = nil,
        serviceInfo: SummaryResult.ServiceInfoResult? = nil,
        yandexHelpInfo: HelpIsNearInfoResult? = nil
    ) -> SummaryResult {
        SummaryResult(
            baseAmount: baseAmount,
            discountAmount: discountAmount,
            promoDiscount: promoDiscount,
            promoCodeDiscount: promoCodeDiscount,
            coinDiscount: coinDiscount,
            totalAmount: totalAmount,
            promos: promos,
            delivery: delivery,
            creditInformation: creditInformation,
            serviceInfo: serviceInfo,
            yandexHelpInfo: yandexHelpInfo
        )
    }
}

// MARK: - Nested Types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        enum Summary {
            static let baseAmount = 9_999
            static let totalAmount = 9_999
            static let delivery = CheckoutSummary.DeliveryInfo(
                price: 0,
                freeDeliveryReason: .threshold,
                freeDeliveryStatus: .alreadyFree,
                liftingServices: []
            )
            static let deliveryResult = DeliveryInfoResult(
                price: 0,
                freeDeliveryReason: .threshold,
                freeDeliveryStatus: .alreadyFree,
                lifting: []
            )
        }
    }
}
