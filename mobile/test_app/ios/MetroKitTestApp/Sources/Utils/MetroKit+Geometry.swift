import UIKit

extension CGPoint {

    init(_ point: YMLPoint) {
        self.init(x: CGFloat(point.x), y: CGFloat(point.y))
    }
    
    init(_ point: YMLScreenPoint) {
        self.init(x: CGFloat(point.x), y: CGFloat(point.y))
    }

}

extension YMLBox {
    
    var center: YMLPoint {
        return YMLPoint(x: (min.x + max.x) / 2.0, y: (min.y + max.y) / 2.0)
    }
    
}
