//
//  SchemeManager.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

protocol SchemeManager {
    var lastSchemeId: YMLSchemeId? { get }

    func requestSchemeList() -> Async<YMLSchemeList?>
    func updateSchemeList() -> Async<YMLSchemeList?>
    func requestScheme(id: YMLSchemeId) -> Async<YMLScheme?>
    func updateScheme(id: YMLSchemeId) -> Async<YMLScheme?>
}

final class MetrokitSchemeManager {
    private let impl: YMLSchemeManager
    private let keyValueStorage: KeyValueStorage
    
    init(impl: YMLSchemeManager) {
        self.impl = impl
        keyValueStorage = UserDefaults.standard.makeScoped("MetrokitSchemeManager")
    }
}

extension MetrokitSchemeManager: SchemeManager {

    private struct Key {
        static var lastSchemeId = "lastSchemeId"
    }

    var lastSchemeId: YMLSchemeId? {
        get {
            return keyValueStorage[Key.lastSchemeId].map { YMLSchemeId(value: $0) }
        }
        set {
            keyValueStorage[Key.lastSchemeId] = newValue?.value
        }
    }
    
    func requestSchemeList() -> Async<YMLSchemeList?> {
        assert(Thread.isMainThread)
        
        typealias SessionListener = SchemeListObtainmentSessionListener
        typealias Session = YMLSchemeListObtainmentSession
        
        return Async<YMLSchemeList?>(
            fetch: { [weak self] fulfill -> (Session, SessionListener)? in
                guard let session = self?.impl.schemeList(withPublishedOnly: false) else { return nil }
                
                let listener = SessionListener { list, error in
                    if let error = error {
                        MetrokitSchemeManager.log(error: error)
                    }
                    fulfill(list)
                }
        
                session.addListener(withResultListener: listener)
                
                return (session, listener)
            },
            cancel: { operation in
                let operation = operation as? (Session, SessionListener)
                
                if let session = operation?.0, session.isValid, !session.isFinished() {
                    session.cancel()
                }
            }
        )
    }
    
    func updateSchemeList() -> Async<YMLSchemeList?> {
        assert(Thread.isMainThread)
        
        class State {
            var updatingSession: YMLSchemeListUpdatingSession? = nil
            var updatingListener: SchemeListUpdatingSessionListener? = nil
            var obtainmentSession: YMLSchemeListObtainmentSession? = nil
            var obtainmentListener: SchemeListObtainmentSessionListener? = nil
        }
        
        return Async<YMLSchemeList?>(
            fetch: { [weak self] fulfill in
                guard let slf = self else { return nil }
                
                let state = State()
                
                let session = slf.impl.updateSchemeList()
                
                let listener = SchemeListUpdatingSessionListener { [weak state, weak self] error in
                    if let error = error {
                        MetrokitSchemeManager.log(error: error)
                    }
                    
                    guard let slf = self else { return }
                    
                    let session = slf.impl.schemeList(withPublishedOnly: false)
                    
                    let listener = SchemeListObtainmentSessionListener { list, error in
                        if let error = error {
                            MetrokitSchemeManager.log(error: error)
                        }
                        fulfill(list)
                    }
                    
                    state?.obtainmentSession = session
                    state?.obtainmentListener = listener
                    
                    session.addListener(withResultListener: listener)
                }
                
                state.updatingSession = session
                state.updatingListener = listener

                session.addListener(withResultListener: listener)
                
                return state
            },
            cancel: { state_ in
                let state = state_ as! State
                
                if let s = state.updatingSession, s.isValid && !s.isFinished() {
                    s.cancel()
                }
                if let s = state.obtainmentSession, s.isValid && !s.isFinished() {
                    s.cancel()
                }
            }
        )
    }
    
    func requestScheme(id: YMLSchemeId) -> Async<YMLScheme?> {
        assert(Thread.isMainThread)
        
        typealias Session = YMLSchemeObtainmentSession
        typealias SessionListener = SchemeObtainmentSessionListener
        
        return Async<YMLScheme?>(
            fetch: { [weak self] fulfill -> (Session, SessionListener)? in
                guard let session = self?.impl.scheme(with: id) else { return nil }
    
                let listener = SessionListener { [weak self] scheme, error in
                    if let error = error {
                        MetrokitSchemeManager.log(error: error)
                    }
                    fulfill(scheme)
                    
                    if scheme != nil {
                        self?.lastSchemeId = id
                    }
                }
                
                session.addListener(withResultListener: listener)
            
                return (session, listener)
            },
            cancel: { operation in
                let operation = operation as? (Session, SessionListener)
                
                if let session = operation?.0, session.isValid, !session.isFinished() {
                    session.cancel()
                }
            }
        )
    }
    
    func updateScheme(id: YMLSchemeId) -> Async<YMLScheme?> {
        assert(Thread.isMainThread)
        
        class State {
            var updateSession: YMLSchemeUpdatingSession? = nil
            var updateListener: SchemeUpdatingSessionListener? = nil
            var getSession: YMLSchemeObtainmentSession? = nil
            var getListener: SchemeObtainmentSessionListener? = nil
        }
        
        return Async<YMLScheme?>(
            fetch: { [weak self] fulfill in
                guard let slf = self else { return nil }

                let state = State()
                
                let session = slf.impl.updateScheme(with: id)

                let listener = SchemeUpdatingSessionListener { [weak self] error in
                    guard let slf = self else { return }
                
                    if let error = error {
                        MetrokitSchemeManager.log(error: error)
                    }
                    
                    let session = slf.impl.scheme(with: id)
                    
                    let listener = SchemeObtainmentSessionListener { scheme, error in
                        if let error = error {
                            MetrokitSchemeManager.log(error: error)
                        }
                        fulfill(scheme)
                    }
                    
                    state.getSession = session
                    state.getListener = listener
                    
                    session.addListener(withResultListener: listener)
                }
                
                state.updateSession = session
                state.updateListener = listener

                session.addListener(withResultListener: listener)
                
                return state
            },
            cancel: { state_ in
                let state = state_ as! State
                
                if let s = state.updateSession, !s.isFinished() {
                    s.cancel()
                }
                if let s = state.getSession, !s.isFinished() {
                    s.cancel()
                }
            }
        )
    }
    
    private static func log(error: YMLSchemeManagerError) {
        let source: String
        
        switch error.source {
        case .filesystem:
            source = "filesystem"
        case .network:
            source = "network"
        case .client:
            source = "client"
        case .unknown:
            source = "unknown"
        }
        
        print("\(source): \(error.message)")
    }
    
}

private class SchemeListObtainmentSessionListener: NSObject, YMLSchemeListObtainmentSessionListener {
    typealias OnDone = (YMLSchemeList?, YMLSchemeManagerError?) -> Void
    
    private let onDone: OnDone
    
    init(onDone: @escaping OnDone) {
        self.onDone = onDone
    }
    
    // YMLSchemeListObtainmentSessionListener
    
    func onSchemeListObtainmentResult(withResult result: YMLSchemeList) {
        onDone(result, nil)
    }
    
    func onSchemeListObtainmentErrorWithError(_ error: YMLSchemeManagerError) {
        onDone(nil, error)
    }
}

private class SchemeListUpdatingSessionListener: NSObject, YMLSchemeListUpdatingSessionListener {
    typealias OnDone = (YMLSchemeManagerError?) -> Void
    
    private let onDone: OnDone
    
    init(onDone: @escaping OnDone) {
        self.onDone = onDone
    }
    
    // YMLSchemeListUpdatingSessionListener
    
    func onSchemeListUpdateResult() {
        onDone(nil)
    }
    
    func onSchemeListUpdateErrorWithError(_ error: YMLSchemeManagerError) {
        onDone(error)
    }
}

private class SchemeObtainmentSessionListener: NSObject, YMLSchemeObtainmentSessionListener {
    typealias OnDone = (YMLScheme?, YMLSchemeManagerError?) -> Void
    
    private let onDone: OnDone
    
    init(onDone: @escaping OnDone) {
        self.onDone = onDone
    }
    
    // YMLSchemeObtainmentSessionListener
    
    func onSchemeObtainmentResult(withResult result: YMLScheme) {
        onDone(result, nil)
    }
    
    func onSchemeObtainmentErrorWithError(_ error: YMLSchemeManagerError) {
        onDone(nil, error)
    }
}

private class SchemeUpdatingSessionListener: NSObject, YMLSchemeUpdatingSessionListener {
    typealias OnDone = (YMLSchemeManagerError?) -> Void
    
    private let onDone: OnDone
    
    init(onDone: @escaping OnDone) {
        self.onDone = onDone
    }
    
    // YMLSchemeUpdatingSessionListener

    func onSchemeUpdateResult(with summary: YMLSchemeSummary) {
        onDone(nil)
    }
    
    func onSchemeUpdateProgress(with progress: YMLProgress) {
        
    }
    
    func onSchemeUpdateErrorWithError(_ error: YMLSchemeManagerError) {
        onDone(error)
    }
}
