@testable import AutoRuDealerFilters
import AutoRuUtils
import AutoRuNetworkUtils
import XCTest

final class DealerFiltersTests: BaseUnitTest {
    func test_StatusFilter() {
        var filters = DealerFilterModel.default
        let statusParam: () -> QueryParameterValue? = { filters.serialized["status"] }

        XCTAssertEqual(statusParam(), .value("ACTIVE"), "Фильтр по-умолчанию должен быть `Активные`")

        filters.listingStatus = .all
        XCTAssertEqual(statusParam(), .empty, "`Все` listing status shouldn't be serialized")

        filters.listingStatus = .active
        XCTAssertEqual(statusParam(), .value("ACTIVE"), "`Активные` listing status should be serialized to `ACTIVE`")

        filters.listingStatus = .pending
        XCTAssertEqual(statusParam(), .value("NEED_ACTIVATION"), "`Ждут активации` listing status should be serialized to `BANNED`")

        filters.listingStatus = .inactive
        XCTAssertEqual(statusParam(), .value("INACTIVE"), "`Неактивные` listing status should be serialized to `INACTIVE`")

        filters.listingStatus = .blocked
        XCTAssertEqual(statusParam(), .value("BANNED"), "`Заблокированные` listing status should be serialized to `BANNED`")
    }

    func test_PriceFilter() {
        var filters = DealerFilterModel.default
        let priceFromParam: () -> QueryParameterValue? = { filters.serialized["price_from"] }
        let priceToParam: () -> QueryParameterValue? = { filters.serialized["price_to"] }

        XCTAssertEqual(priceFromParam(), nil, "`Цена от` не должна быть указана в фильтре по-умолчанию")
        XCTAssertEqual(priceToParam(), nil, "`Цена до` не должна быть указана в фильтре по-умолчанию")

        filters.priceFrom = 3_333
        XCTAssertEqual(priceFromParam(), .value("3333"), "`Цена от 3333` should be serialized to `3333`")
        XCTAssertEqual(priceToParam(), nil, "`Цена до` should stay unset")

        filters.priceTo = 7_777
        XCTAssertEqual(priceFromParam(), .value("3333"), "`Цена от` should stay the same")
        XCTAssertEqual(priceToParam(), .value("7777"), "`Цена до 7777` should be serialized to `7_77`")

        filters.priceFrom = 5_555
        XCTAssertEqual(priceFromParam(), .value("5555"), "`Цена от` should be updated to `5555`")
        XCTAssertEqual(priceToParam(), .value("7777"), "While `Цена до` should be untouched")

        filters.priceTo = 12_345
        XCTAssertEqual(priceFromParam(), .value("5555"), "While `Цена от` should be untouched")
        XCTAssertEqual(priceToParam(), .value("12345"), "`Цена до` should be updated to `12345`")

        filters = DealerFilterModel.default
        filters.priceTo = 9_999
        XCTAssertEqual(priceFromParam(), nil, "`Цена от` should stay unset")
        XCTAssertEqual(priceToParam(), .value("9999"), "`Цена до 9999` should be serialized to `9999`")
    }
}
