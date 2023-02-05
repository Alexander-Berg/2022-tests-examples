import UIKit

extension CGRect: FrameRepresentable {
    
    public var frame: CGRect {
        get {
            return self
        }
        set(newValue) {
            self = newValue
        }
    }
    
}
