import XCTest
import Snapshots

final class DealerNewListingCategoryPickerScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var collectionView = findAll(.collectionView).firstMatch
    lazy var refreshingControl = self.scrollableElement.activityIndicators.firstMatch

    func hasExactlyPickerOptions(_ options: [String]) {
        let labels = scrollableElement.cells.staticTexts
        XCTAssertEqual(options.count, options.count)
        for opt in options {
            labels.matching(identifier: opt).firstMatch.shouldExist()
        }
    }
}
