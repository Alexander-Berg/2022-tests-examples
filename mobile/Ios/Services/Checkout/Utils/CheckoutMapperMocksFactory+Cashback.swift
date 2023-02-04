import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Cashback

extension CheckoutMapperMocksFactory {

    func makeCashback() -> Cashback {
        Cashback(
            balance: 0,
            applicableOptions: nil,
            selectedOption: .emit,
            optionProfiles: [],
            paymentSystemCashback: nil,
            yandexCardCashback: nil
        )
    }

    func makeCashbackResult() -> CashbackResult {
        CashbackResult(
            selectedCashbackOption: .emit,
            cashbackBalance: 0,
            welcomeCashback: nil,
            cashbackOptionsProfiles: []
        )
    }
}
