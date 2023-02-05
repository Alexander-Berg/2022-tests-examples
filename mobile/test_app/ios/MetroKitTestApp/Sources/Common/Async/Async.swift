//
//  Async.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 24/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public final class Async<T> {

    
    public typealias OperationImpl = Any
    
    public typealias Fetch = (@escaping Fulfill) -> OperationImpl?
    public typealias Fulfill = (T) -> Void
    public typealias Cancel = (OperationImpl) -> Void
    public typealias Completion = (T) -> Void
    
    public private(set) var result: T? = nil
    
    public init(fetch: @escaping Fetch, cancel: Cancel? = nil) {
        _fetch = fetch
        _cancel = cancel
        _completions = []
    }
    
    deinit {
        if let op = _operation {
            _cancel?(op)
        }
    }
    
    public func start() {
        _operation = _fetch? { [weak self] res in
            guard let slf = self, slf._operation != nil else { return }
            
            slf.result = res
            slf._operation = nil
            slf._completions.forEach { $0(res) }
        }
    }
    
    public func cancel() {
        guard let op = _operation else { return }
        
        _cancel?(op)
        _operation = nil
    }
    
    public func onCompletion(completion: @escaping Completion) {
        _completions.append(completion)
    }
    
    // MARK: - Private
    
    private var _operation: OperationImpl? = nil
    private var _fetch: Fetch? = nil
    private var _cancel: ((OperationImpl) -> Void)? = nil
    private var _completions: [Completion]
}

public extension Async {
    
    public func map<U>(_ block: @escaping (T) -> U) -> Async<U> {
        return Async<U>(
            fetch: { fulfill in
                self.onCompletion { res in
                    fulfill(block(res))
                }
                self.start()
                return self
            },
            cancel: { impl in
                (impl as! Async<T>).cancel()
            }
        )
    }
    
    public func then<U>(_ block: @escaping (T) -> Async<U>?) -> Async<U> {
        return Async<U>(
            fetch: { fulfill in
                self.onCompletion { res in
                    let next = block(res)
                    next?.onCompletion {
                        fulfill($0)
                    }
                    
                    let prevCancel = next?._cancel
                    
                    next?._cancel = { op in
                        prevCancel?(op)
                        next?._cancel = nil
                    }
                    
                    next?.onCompletion { [weak next] _ in
                        next?._cancel = nil
                    }
                    
                    next?.start()
                }
                self.start()
                return self
            },
            cancel: { impl in
                (impl as! Async<T>).cancel()
            }
        )
    }
    
    public func then<U>(_ next: Async<U>) -> Async<U> {
        return Async<U>(
            fetch: { fulfill in
                next.onCompletion { res in
                    fulfill(res)
                }
                self.onCompletion { res in
                    next.start()
                }
                self.start()
                return (self, next)
            },
            cancel: { impl in
                let impl = impl as! (Async<T>, Async<U>)
                impl.0.cancel()
                impl.1.cancel()
            }
        )
    }
    
}
