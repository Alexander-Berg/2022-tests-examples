import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Actualization

extension CheckoutMapperMocksFactory {
    func makeActualization(
        parcels: [Parcel] = [],
        presetDeliverabilities: [PresetDeliverability] = [],
        summary: CheckoutSummary,
        commonPayment: CommonPaymentNew?,
        cashback: Cashback?,
        consolidation: [ConsolidationGroup] = [],
        hasCommonDelivery: Bool = true,
        errors: [CheckoutError] = []
    ) -> Actualization {
        Actualization(
            parcels: parcels,
            presetDeliverabilities: presetDeliverabilities,
            summary: summary,
            commonPayment: commonPayment,
            cashback: cashback,
            plusSubscriptionInfo: nil,
            coinInfo: nil,
            yandexCardInfo: nil,
            consolidation: consolidation,
            hasCommonDelivery: hasCommonDelivery,
            errors: errors
        )
    }

    // MARK: - DTO

    func makeOrderOptionsResult(
        presets: [PresetResult] = [],
        parcels: [ParcelResult] = [],
        summary: SummaryResult?,
        consolidation: [ConsolidationResult] = [],
        hasCommonDelivery: Bool = true,
        errors: [OrderErrorResult] = []
    ) -> OrderOptionsResult {
        OrderOptionsResult(
            presets: presets,
            parcels: parcels,
            commonDeliveryOptions: hasCommonDelivery ? [AnyCodable("Some")] : [],
            cashback: makeCashbackResult(),
            coinInfo: nil,
            consolidation: consolidation,
            commonPayment: makeCommonPaymentResult(),
            summary: summary,
            errors: errors
        )
    }
}
