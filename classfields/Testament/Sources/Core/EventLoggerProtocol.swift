//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public protocol EventLoggerProtocol: AnyObject {
    func log(_ event: Event)
}
