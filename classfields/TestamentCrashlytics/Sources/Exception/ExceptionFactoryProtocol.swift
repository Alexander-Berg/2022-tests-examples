//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament
import FirebaseCrashlytics

public protocol ExceptionFactoryProtocol {
    func makeException(_ event: Event) -> ExceptionModel
}
