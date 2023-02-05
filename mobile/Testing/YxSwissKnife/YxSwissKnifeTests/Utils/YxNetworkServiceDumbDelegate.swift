import Foundation
import XCTest
import Swifter

@testable import YxSwissKnife

class YxNetworkServiceDumbDelegate: YxNetworkServiceDelegate {
    var willPerform: ((_ req: YxNetworkRequest) -> Void)?
    var didPrepared: ((_ req: YxNetworkRequest, _ url: String) -> Void)?
    var didCheckedReachability: ((_ req: YxNetworkRequest, _ reachable: Bool) -> Void)?
    var didReceiveResponse: ((_ request: YxNetworkRequest) -> Void)?
    var didProcessedResponse: ((_ request: YxNetworkRequest) -> Void)?
    var didFail: ((_ request: YxNetworkRequest, _ error: Error) -> Void)?
    var didRedirected: ((_ request: YxNetworkRequest, _ code: Int, _ message: String?) -> Void)?
    var didCancelled: ((_ request: YxNetworkRequest) -> Void)?
    var didFinish: ((_ request: YxNetworkRequest) -> Void)?
    var debug: ((_ request: YxNetworkRequest, _ dump: String) -> Void)?

    func willPerform(request: YxNetworkRequest) {
        willPerform?(request)
    }

    func didPrepared(request: YxNetworkRequest, url: String) {
        didPrepared?(request, url)
    }

    func didCheckedReachability(request: YxNetworkRequest, reachable: Bool) {
        didCheckedReachability?(request, reachable)
    }

    func didReceiveResponse(request: YxNetworkRequest) {
        didReceiveResponse?(request)
    }

    func didProcessedResponse(request: YxNetworkRequest) {
        didProcessedResponse?(request)
    }

    func didFail(request: YxNetworkRequest, error: Error) {
        didFail?(request, error)
    }

    func didRedirected(request: YxNetworkRequest, code: Int, message: String?) {
        didRedirected?(request, code, message)
    }

    func didCancelled(request: YxNetworkRequest) {
        didCancelled?(request)
    }

    func didFinish(request: YxNetworkRequest) {
        didFinish?(request)
    }

    func debug(request: YxNetworkRequest, dump: String) {
        debug?(request, dump)
    }
}
