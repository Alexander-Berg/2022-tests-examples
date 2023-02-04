//
//  Created by Alexey Aleshkov on 03.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import ColorDifferenceKit

enum ScalarMath {
    static func rounded<T: FloatingPoint & ExpressibleByFloatLiteral>(_ value: T, upTo numbers: Int, pow: (T, T) -> T) -> T {
        let numberOfPlaces = T(numbers)
        let multiplier = pow(10.0, numberOfPlaces)
        let rounded = round(value * multiplier) / multiplier
        return rounded
    }
}
