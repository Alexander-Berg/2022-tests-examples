import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - CommonPaymentNew

extension CheckoutMapperMocksFactory {

    func makeCommonPayment(
        prepayForAll: Bool = true,
        postpayForAll: Bool = true,
        maxPrepay: CommonPaymentNew.MaxPrepay? = nil
    ) -> CommonPaymentNew {
        CommonPaymentNew(
            prepayForAll: prepayForAll,
            postpayForAll: postpayForAll,
            maxPrepay: maxPrepay
        )
    }

    func makeCommonPaymentResult() -> CommonPaymentResult {
        CommonPaymentResult(prepayForAll: true, postpayForAll: true, maxPrepay: nil)
    }
}

// MARK: - Nested Types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        enum CommonPayment {
            static let prepayForAll = true
            static let postpayForAll = true
        }
    }
}
