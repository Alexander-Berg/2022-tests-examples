//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

/// Assertion level
public enum Level: Equatable, Comparable {
    case assert
    case precondition
    case fatal
}
