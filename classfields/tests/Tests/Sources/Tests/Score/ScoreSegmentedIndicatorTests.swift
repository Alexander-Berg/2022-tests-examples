import XCTest
import Snapshots
@testable import AutoRuOfferAdvantage
import CoreGraphics

final class ScoreSegmentedIndicatorTests: BaseUnitTest {
    func test_score0() {
        let chart = ScoreSegmentedIndicator.make(
            score: 0,
            size: CGSize(squareSize: 80),
            filledColor: UIColor.red,
            unfilledColor: UIColor.gray
        )!

        Snapshot.compareWithSnapshot(
            view: { $0.backgroundColor = .white; return $0 }(UIImageView(image: chart)),
            interfaceStyle: [.light],
            identifier: "segmented_score_0"
        )
    }

    func test_score6() {
        let chart = ScoreSegmentedIndicator.make(
            score: 6,
            size: CGSize(squareSize: 80),
            filledColor: UIColor.red,
            unfilledColor: UIColor.gray
        )!

        Snapshot.compareWithSnapshot(
            view: { $0.backgroundColor = .white; return $0 }(UIImageView(image: chart)),
            interfaceStyle: [.light],
            identifier: "segmented_score_6"
        )
    }

    func test_score10() {
        let chart = ScoreSegmentedIndicator.make(
            score: 10,
            size: CGSize(squareSize: 80),
            filledColor: UIColor.red,
            unfilledColor: UIColor.gray
        )!

        Snapshot.compareWithSnapshot(
            view: { $0.backgroundColor = .white; return $0 }(UIImageView(image: chart)),
            interfaceStyle: [.light],
            identifier: "segmented_score_10"
        )
    }

    func test_scoreInvalid() {
        let chart1 = ScoreSegmentedIndicator.make(
            score: -1,
            size: CGSize(squareSize: 80),
            filledColor: UIColor.red,
            unfilledColor: UIColor.gray
        )!

        Snapshot.compareWithSnapshot(
            view: { $0.backgroundColor = .white; return $0 }(UIImageView(image: chart1)),
            interfaceStyle: [.light],
            identifier: "segmented_score_0"
        )

        let chart2 = ScoreSegmentedIndicator.make(
            score: 100,
            size: CGSize(squareSize: 80),
            filledColor: UIColor.red,
            unfilledColor: UIColor.gray
        )!

        Snapshot.compareWithSnapshot(
            view: { $0.backgroundColor = .white; return $0 }(UIImageView(image: chart2)),
            interfaceStyle: [.light],
            identifier: "segmented_score_10"
        )
    }
}
