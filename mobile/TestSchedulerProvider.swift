//
//  TestSchedulerProvider.swift
//  YandexMapsTests
//
//  Created by Vsevolod Mashinson on 22.11.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
import RxSwift
import RxTest

class TestSchedulerProvider: SchedulerProvider  {
    
    var mainSchedulerProvider: MainSchedulerProvider {
        return TestMainSchedulerProvider(scheduler: scheduler)
    }
    
    var concurrentDispatchQueueSchedulerProvider: ConcurrentDispatchQueueSchedulerProvider {
        return TestConcurrentDispatchQueueSchedulerProvider(scheduler: scheduler)
    }
    
    // MARK: - Constructors
    
    init(scheduler: TestScheduler) {
        self.scheduler = scheduler
    }
    
    // MARK: - Private Properties
    
    private let scheduler: TestScheduler
    
}

class TestConcurrentDispatchQueueSchedulerProvider: ConcurrentDispatchQueueSchedulerProvider {
    
    // MARK: ConcurrentDispatchQueueSchedulerProvider
    
    func scheduler(qos: DispatchQoS, leeway: DispatchTimeInterval) -> SchedulerType {
        return scheduler
    }
    
    func scheduler(queue: DispatchQueue, leeway: DispatchTimeInterval) -> SchedulerType {
        return scheduler
    }
    
    func scheduler(qos: DispatchQoS) -> SchedulerType {
        return scheduler
    }
    
    func scheduler(queue: DispatchQueue) -> SchedulerType {
        return scheduler
    }
    
    // MARK: - Constructors
    
    init(scheduler: TestScheduler) {
        self.scheduler = scheduler
    }
    
    // MARK: - Private Properties
    
    private let scheduler: TestScheduler
    
}

class TestMainSchedulerProvider: MainSchedulerProvider {
    
    var syncInstance: SchedulerType {
        return scheduler
    }
    
    var asyncInstance: SchedulerType {
        return scheduler
    }
    
    // MARK: - Constructors
    
    init(scheduler: TestScheduler) {
        self.scheduler = scheduler
    }
    
    // MARK: - Private Properties
    
    private let scheduler: TestScheduler
    
}
