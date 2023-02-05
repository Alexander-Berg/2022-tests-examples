import MarketModels
import XCTest

@testable import BeruServices
@testable import MarketDTO

class ProductScrollboxWidgetMapperTests: XCTestCase {
    func test_dtoMapping() throws {
        // given
        let dtoItem = ProductWidgetItemResult(
            title: "Набор оснастки Makita B-53768, 56 предм.",
            pictures: ["imageUrl"],
            price: "5000 ₽",
            oldPrice: "7000 ₽",
            promocodeBadge: "- 15%",
            discount: .init(description: "- 5%", isPersonalDiscount: true),
            rating: 4.5,
            opinionsCount: 50,
            cashback: .init(value: "500", isExtra: true)
        )

        let dtoModel = ProductScrollboxWidgetResult(
            id: 0,
            reloadable: false,
            title: "",
            content: [dtoItem]
        )

        // when
        let domainModel = try XCTUnwrap(
            ProductScrollboxWidgetMapper.map(result: dtoModel, context: .stub)
        )

        // then

        let domainItem = try XCTUnwrap(domainModel.content.first)

        XCTAssertEqual(dtoModel.content.count, domainModel.content.count)
        XCTAssertEqual(dtoItem.title, domainItem.title)
        XCTAssertEqual(domainItem.title, "Набор оснастки Makita B-53768, 56 предм.")
        XCTAssertEqual(domainItem.pictures, ["imageUrl"])
        XCTAssertEqual(domainItem.price, "5000 ₽")
        XCTAssertEqual(domainItem.oldPrice, "7000 ₽")
        XCTAssertEqual(domainItem.promocodeBadge, "- 15%")
        XCTAssertEqual(domainItem.discount, .init(description: "- 5%", isPersonalDiscount: true))
        XCTAssertEqual(domainItem.rating, 4.5)
        XCTAssertEqual(domainItem.opinionsCount, 50)
        XCTAssertEqual(domainItem.cashback, .init(value: "500", isExtra: true))
    }
}
