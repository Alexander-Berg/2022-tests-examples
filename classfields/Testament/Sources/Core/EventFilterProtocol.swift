//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public enum FilterResult {
    /// Stop filtering chain and **Append**
    case accept
    /// Continue filter chain
    case neutral
    /// Stop filtering chain and **Not Append**
    case deny
}

/// Entity that decides whether to send an event further down the chain or not.
public protocol EventFilterProtocol: AnyObject {
    func filter(_ event: Event) -> FilterResult
}
