import XCTest
@testable import MarketCashback

final class PluralTests: XCTestCase {

    private let one = 1
    private let few = 3
    private let many = 25

    func testCashback() {
        // given
        let oneForm = "\(one) балл"
        let fewForm = "\(few) балла"
        let manyForm = "\(many) баллов"

        // when
        let onePlural = L10n.cashback(one)
        let fewPlural = L10n.cashback(few)
        let manyPlural = L10n.cashback(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCashbackFromPluralShort() {
        // given
        let oneForm = "\(one) балла"
        let fewForm = "\(few) баллов"
        let manyForm = "\(many) баллов"

        // when
        let onePlural = L10n.cashbackFromPluralShort(one)
        let fewPlural = L10n.cashbackFromPluralShort(few)
        let manyPlural = L10n.cashbackFromPluralShort(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }
}
