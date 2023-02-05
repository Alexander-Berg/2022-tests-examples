//
//  RechabilityAdapterMock.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 16.05.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import RxSwift
import XCTest
#if !DEV_TEST
@testable import YandexDisk
#endif

final class YDReachabilityAdapterMock: ReachabilityAdapterProtocol {
    var connectionType = YDConnectionType.wiFi
    var hostIsReachable = true
    var isInitialized = true
    private(set) var observer: AnyObserver<Bool>?

    func onReachabilityDidChange() -> Observable<Bool> {
        return Observable<Bool>.create { [weak self] observer in
            self?.observer = observer
            return Disposables.create()
        }
    }

    private(set) var didSubscribed = false
    func subscribe(delegate _: ReachabilityAdapterNotifierProtocol) {
        didSubscribed = true
    }
}

final class YOReachabilityMock: YOReachability {
    override var hostIsReachable: Bool {
        return true
    }

    override var connectionType: YDConnectionType {
        return .wiFi
    }
}
