import XCTest

final class SegmentSelectPicker: BaseSteps, UIElementProvider {
    enum Element {
        case segment(Int)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .segment(index):
            return "segmentControlSegmentLabel_\(index)"
        }
    }
}

