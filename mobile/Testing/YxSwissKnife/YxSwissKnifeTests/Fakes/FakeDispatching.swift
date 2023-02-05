//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

@testable import YxSwissKnife

final class FakeDispatching: Dispatching {
    @discardableResult
    func sync<T>(flags _: DispatchWorkItemFlags, execute work: () throws -> T) rethrows -> T {
        try work()
    }

    typealias AsyncWork = (
        group: DispatchGroup?,
        qos: DispatchQoS,
        flags: DispatchWorkItemFlags,
        work: @convention(block) () -> Void
    )

    var works: [AsyncWork] = []

    func async(
        group: DispatchGroup?,
        qos: DispatchQoS,
        flags: DispatchWorkItemFlags,
        execute work: @escaping @convention(block) () -> Void
    ) {
        works.append((group: group, qos: qos, flags: flags, work: work))
    }

    func executeAsyncWorks() {
        let copy = works
        works.removeAll()
        copy.forEach {
            $0.work()
        }
    }

    typealias TimedAsyncWork = (
        deadline: DispatchTime,
        qos: DispatchQoS,
        flags: DispatchWorkItemFlags,
        work: @convention(block) () -> Void
    )

    var timedWorks: [TimedAsyncWork] = []

    func asyncAfter(
        deadline: DispatchTime,
        qos: DispatchQoS,
        flags: DispatchWorkItemFlags,
        execute work: @escaping @convention(block) () -> Void
    ) {
        timedWorks.append((
            deadline: deadline,
            qos: qos,
            flags: flags,
            work: work
        ))
    }

    func executeTimedAsyncWorks() {
        let copy = timedWorks
        timedWorks.removeAll()
        copy.forEach { $0.work() }
    }
}

final class FakeDispatchingGroup: DispatchingGroup {
    var currentLoad: Int = 0

    func enter() {
        currentLoad += 1
    }

    func leave() {
        currentLoad -= 1
    }

    var notifies: [() -> Void] = []

    func notify(on _: Dispatching, work: @escaping () -> Void) {
        notifies.append(work)
    }

    func executeNotifies() {
        let copy = notifies
        notifies.removeAll()
        copy.forEach {
            $0()
        }
    }
}
