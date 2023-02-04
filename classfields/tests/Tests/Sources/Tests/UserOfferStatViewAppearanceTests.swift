import XCTest
import AutoRuUserOfferStat
import AutoRuFormatters
import Snapshots

final class UserOfferStatViewAppearanceTests: BaseUnitTest {
    func test_hasBothCounters() {
        checkView(model: OfferStatDiffModel(viewDiff: 19, searchPositionDiff: 70), method: #function)
    }

    func test_hasOnlyViewCounter() {
        checkView(model: OfferStatDiffModel(viewDiff: 11, searchPositionDiff: 0), method: #function)
    }

    func test_hasOnlySearchPositionCounter() {
        checkView(model: OfferStatDiffModel(viewDiff: 0, searchPositionDiff: -12), method: #function)
    }

    private func checkView(model: OfferStatDiffModel, method: String) {
        let view = UserOfferStatView(model)

        let container = UIView()
        container.backgroundColor = .black
        container.addSubview(view)
        container.frame = CGRect(width: 200, height: 60)

        view.pinCenter(to: container)

        container.setNeedsLayout()
        container.layoutIfNeeded()

        Snapshot.compareWithSnapshot(view: container, identifier: method)
    }
}
