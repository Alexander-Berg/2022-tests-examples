import AutoRuFormatters
@testable import AutoRuFavoriteSaleList
import XCTest

final class FavoriteSaleListUpdatesTests: BaseUnitTest {
    func test_formatter() {
        let fmt = FavoritesUpdatesFormatter.self
        var updates: FavoritesUpdates!

        do {
            updates = FavoritesUpdates(soldCount: 1, changedPriceCount: 0)
            XCTAssertEqual(fmt.format(updates: updates), "1 объявление было снято с продажи")

            updates = FavoritesUpdates(soldCount: 2, changedPriceCount: 0)
            XCTAssertEqual(fmt.format(updates: updates), "2 объявления были сняты с продажи")

            updates = FavoritesUpdates(soldCount: 3, changedPriceCount: 0)
            XCTAssertEqual(fmt.format(updates: updates), "3 объявления были сняты с продажи")

            updates = FavoritesUpdates(soldCount: 5, changedPriceCount: 0)
            XCTAssertEqual(fmt.format(updates: updates), "5 объявлений были сняты с продажи")

            updates = FavoritesUpdates(soldCount: 21, changedPriceCount: 0)
            XCTAssertEqual(fmt.format(updates: updates), "21 объявление было снято с продажи")
        }
        do {
            updates = FavoritesUpdates(soldCount: 0, changedPriceCount: 1)
            XCTAssertEqual(fmt.format(updates: updates), "У 1 объявления изменилась цена")

            updates = FavoritesUpdates(soldCount: 0, changedPriceCount: 2)
            XCTAssertEqual(fmt.format(updates: updates), "У 2 объявлений изменилась цена")

            updates = FavoritesUpdates(soldCount: 0, changedPriceCount: 3)
            XCTAssertEqual(fmt.format(updates: updates), "У 3 объявлений изменилась цена")

            updates = FavoritesUpdates(soldCount: 0, changedPriceCount: 5)
            XCTAssertEqual(fmt.format(updates: updates), "У 5 объявлений изменилась цена")

            updates = FavoritesUpdates(soldCount: 0, changedPriceCount: 21)
            XCTAssertEqual(fmt.format(updates: updates), "У 21 объявления изменилась цена")
        }
        do {
            updates = FavoritesUpdates(soldCount: 1, changedPriceCount: 1)
            XCTAssertEqual(fmt.format(updates: updates), "1 объявление было снято с продажи, у 1 изменилась цена")

            updates = FavoritesUpdates(soldCount: 2, changedPriceCount: 2)
            XCTAssertEqual(fmt.format(updates: updates), "2 объявления были сняты с продажи, у 2 изменилась цена")

            updates = FavoritesUpdates(soldCount: 3, changedPriceCount: 3)
            XCTAssertEqual(fmt.format(updates: updates), "3 объявления были сняты с продажи, у 3 изменилась цена")

            updates = FavoritesUpdates(soldCount: 5, changedPriceCount: 5)
            XCTAssertEqual(fmt.format(updates: updates), "5 объявлений были сняты с продажи, у 5 изменилась цена")

            updates = FavoritesUpdates(soldCount: 21, changedPriceCount: 21)
            XCTAssertEqual(fmt.format(updates: updates), "21 объявление было снято с продажи, у 21 изменилась цена")
        }
    }
}
