//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Testament

public protocol ErrorFactoryProtocol {
    func makeError(_ event: Event) -> NSError
}
