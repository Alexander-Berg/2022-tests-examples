//
//  Notifier.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 24/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public final class Notifier<Listener> {

    public init() {
        listeners = WeakCollection<Listener>()
    }
    
    public func subscribe(_ listener: Listener) {
        listeners.insert(listener)
    }
    
    public func unsubscribe(_ listener: Listener) {
        listeners.remove(listener)
    }
    
    public func forEach(_ block: (Listener) -> Void) {
        listeners.forEach(block)
    }

    public var hasNoListeners: Bool {
        return listeners.isEmpty()
    }

    // MARK: - Private
    
    private var listeners: WeakCollection<Listener>

}
