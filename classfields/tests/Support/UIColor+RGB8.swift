//
//  Created by Alexey Aleshkov on 03.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import UIKit
@testable import ImageDifferenceKit

extension UIColor {
    func toRGB8() -> RGB8ColorRep {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        self.getRed(&red, green: &green, blue: &blue, alpha: nil)
        return .init(red: UInt8(red * 255), green: UInt8(green * 255), blue: UInt8(blue * 255))
    }
}
