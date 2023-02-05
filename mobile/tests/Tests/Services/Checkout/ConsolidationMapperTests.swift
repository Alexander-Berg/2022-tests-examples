import MarketDTO
import MarketModels
import XCTest

@testable import BeruServices

class ConsolidationMapperTests: XCTestCase {

    // MARK: - Properties

    var mocksFactory: CheckoutMapperMocksFactory!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        mocksFactory = CheckoutMapperMocksFactory()
    }

    override func tearDown() {
        mocksFactory = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_consolidationMapping_whenSimpleResult() {
        // when
        let result = mocksFactory.makeConsolidation()
        let consolidation = mocksFactory.makeConsolidationResult().map {
            ConsolidationMapper.extractConsolidation(from: $0)
        }

        // then
        XCTAssertEqual(result, consolidation)
    }
}
