import Foundation
import Snapshots
import AutoRuCommonViews
import AutoRuColorSchema
import CoreGraphics

final class SegmentsCircleGraphTests: BaseUnitTest {
    func test_segmentsCircleGraph() {
        let segments = [
            Segment(start: 0, end: 90, color: .red),
            Segment(start: 100, end: 270, color: .red),
            Segment(start: 30, end: 120, color: .red),
        ]
        let model = CircleSegmentsGraphModel(segments: segments, lineWidth: 10, backgroundLineColor: ColorSchema.Background.background)
        let graph = CircleSegmentsGraph(model: model)
        graph.frame = CGRect(x: 0, y: 0, size: .init(width: 100, height: 100))
        graph.backgroundColor = ColorSchema.Background.surface

        Snapshot.compareWithSnapshot(view: graph)
    }
}
