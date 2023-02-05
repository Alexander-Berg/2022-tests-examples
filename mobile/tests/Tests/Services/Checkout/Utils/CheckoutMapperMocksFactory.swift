import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

final class CheckoutMapperMocksFactory {}

// MARK: - Nested Types

extension CheckoutMapperMocksFactory {
    typealias DeliveryPartnerType = DeliveryOption.DeliveryPartnerType
    typealias DeliveryExtraCharge = DeliveryOptionResult.ExtraCharge
}
