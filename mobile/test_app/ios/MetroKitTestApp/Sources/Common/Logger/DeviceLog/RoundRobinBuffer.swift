//
//  RoundRobinBuffer.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 27.04.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation

final class RoundRobinBuffer<T> {

    init(size: Int) {
        assert(size > 0)
        self.size = Int(size)
        buf.reserveCapacity(self.size)
    }
    
    private(set) var buf = [T]()

    var count: Int {
        if head > tail {
            return head - tail + 1
        } else if head == tail {
            return buf.isEmpty ? 0 : 1
        } else {
            return size
        }
    }

    subscript(index: Int) -> T? {
        if index < 0 || index >= count {
            return nil
        }
        
        let tempIndex = head - index
        if tempIndex < 0 {
            return buf[size + tempIndex]
        } else {
            return buf[tempIndex]
        }
    }

    func clear() {
        head = 0
        tail = 0
        buf.removeAll(keepingCapacity: true)
    }

    func put(_ item: T) {
        let appending = count < size
        
        if !buf.isEmpty {
            head += 1
            if head >= size {
                head = 0
            }
            
            if head == tail {
                tail = head + 1
                if tail >= size {
                    tail = 0
                }
            }
        }
        
        if appending {
            buf.append(item)
        } else {
            buf[head] = item
        }
    }

    // MARK: - Private Properties

    private let size: Int

    private var head: Int = 0
    private var tail: Int = 0

}
