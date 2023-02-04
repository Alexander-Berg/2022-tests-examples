//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public protocol EventInterrupterProtocol {
    func triggerInterrupt(_ event: Event)
}
