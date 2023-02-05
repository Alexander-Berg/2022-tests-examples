import CoreGraphics

public final class FrameRef {
    public var frame: CGRect
    
    public init(_ frame: CGRect = .zero) {
        self.frame = frame
    }
}

extension FrameRef: SingleFrameRepresentable {

}

public extension FrameRef {

    public var bounds: CGRect {
        return frame.bounds
    }
    
    public convenience init(size: CGSize) {
        self.init(CGRect(size: size))
    }

}
