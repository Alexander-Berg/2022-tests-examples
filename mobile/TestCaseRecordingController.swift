//
//  TestCaseRecordingController.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 09.03.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
import UIKit
import YandexMapsNetwork

protocol TestCaseRecordingControllerDelegate: AnyObject {

    func testCaseRecordingController(_ recordingController: TestCaseRecordingController,
                                     didChangeState state: TestCaseRecordingController.State)
    
}

class TestCaseRecordingController: NSObject {

    // MARK: - Public Nested Types

    class TestCaseInfo {
        
        let identifier: String
        let creationTimestamp: String
        
        private struct Keys {
            static let identifier = "identifier"
            static let timestamp = "timestamp"
        }
        
        fileprivate init(identifier: String) {
            self.identifier = identifier
            
            let dateTimeFormatter = DateFormatter()
            dateTimeFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            self.creationTimestamp = dateTimeFormatter.string(from: Date())
        }
        
        fileprivate init?(dictionary: [String: Any]) {
            guard let identifier = dictionary[Keys.identifier] as? String,
                let timestamp = dictionary[Keys.timestamp] as? String else { return nil }
            
            self.identifier = identifier
            self.creationTimestamp = timestamp
        }
        
        fileprivate func toDictionary() -> [String: Any] {
            return [Keys.identifier: identifier, Keys.timestamp: creationTimestamp]
        }
        
    }

    enum State {
        case idle
        case recordingStarting(info: TestCaseInfo)
        case recording(info: TestCaseInfo)
        case recordingStopping(info: TestCaseInfo)
        case validating(session: URLSession?, info: TestCaseInfo)

        var isIdle: Bool {
            guard case .idle = self else { return false }
            return true
        }

        var isRecording: Bool {
            guard case .recording(_) = self else { return false }
            return true
        }
        
        var isValidating: Bool {
            guard case .validating(_, _) = self else { return false }
            return true
        }
    }

    // MARK: - Public Properties

    weak var container: UIView? { didSet{ updateUI() } }
    weak var delegate: TestCaseRecordingControllerDelegate?

    var state: State = .idle {
        didSet{
            switch state {
            case .idle: break
            case .recordingStarting(let info): UserDefaults.standard.set(info.toDictionary(), forKey: Static.userDefaultsStoredTestCaseKey)
            case .recording: break
            case .recordingStopping: UserDefaults.standard.removeObject(forKey: Static.userDefaultsStoredTestCaseKey)
            case .validating: break
            }

            updateUI()
            delegate?.testCaseRecordingController(self, didChangeState: state)
        }
    }

    // MARK: -

    var runningTestCase: String? {
        guard case .recording(let testCaseInfo) = state else { return nil }
        return testCaseInfo.identifier
    }

    // MARK: - Constructors

    init(uuid: String, testCaseIdentifierPreset: String = "") {
        self.uuid = uuid
        self.testCaseIdentifierPreset = testCaseIdentifierPreset
        super.init()

        defer {
            if let savedInfo = UserDefaults.standard.object(forKey: Static.userDefaultsStoredTestCaseKey) as? [String: Any] {
                if let testCaseInfo = TestCaseInfo(dictionary: savedInfo) {
                    state = .recordingStarting(info: testCaseInfo)
                    state = .recording(info: testCaseInfo)
                }
            }
        }
    }
    
    deinit {
        testCaseStatusBarView?.removeFromSuperview()
    }
    
    // MARK: - Public

    func setupValidation(url: URL, token: String) {
        validationURL = url
        validationToken = token
    }

    // MARK: -
    
    func run(testCase: String) {
        assert(state.isIdle)

        let testCaseInfo = TestCaseInfo(identifier: testCase)
        state = .recordingStarting(info: testCaseInfo)
        state = .recording(info: testCaseInfo)
    }
    
    func stopCurrentTestCase() {
        assert(state.isRecording)
        presentCompletionAlert()
    }

    // MARK: -

    func runWithAlertPrompt() {
        let alert: UIAlertView

        if state.isValidating {
            alert = UIAlertView(title: "Test Case", message: "Validation Still Running", delegate: self,
                                cancelButtonTitle: "Retry", otherButtonTitles: "Close")
        } else {
            alert = UIAlertView(title: "Test Case", message: "Enter test case identifier", delegate: self,
                                cancelButtonTitle: "Run", otherButtonTitles: "Close")
            alert.alertViewStyle = .plainTextInput
            alert.textField(at: 0)?.text = testCaseIdentifierPreset
        }

        alert.show()
    }

    // MARK: - Private Properties

    fileprivate let uuid: String
    fileprivate let testCaseIdentifierPreset: String

    fileprivate var validationURL: URL?
    fileprivate var validationToken: String?

    fileprivate var isValidationSupported: Bool { return validationURL != nil && validationToken != nil }
    
    fileprivate weak var testCaseStatusBarView: StatusBarView?

}

extension TestCaseRecordingController: UIAlertViewDelegate {

    func alertView(_ alertView: UIAlertView, clickedButtonAt buttonIndex: Int) {
        switch state {
        case .idle:
            guard buttonIndex == 0 else { return }
            
            if let text = alertView.textField(at: 0)?.text, !text.isEmpty && text != testCaseIdentifierPreset {
                run(testCase: text)
            }

        case .recording, .recordingStarting, .recordingStopping:
            switch buttonIndex {
            case 1:
                if isValidationSupported {
                    sendTestCaseInfoForValidation()
                } else {
                    copyTestCaseInfoIntoPasteboard()
                }

            case 2:
                guard isValidationSupported else {
                    assert(false)
                    return
                }

                copyTestCaseInfoIntoPasteboard()
            default: return
            }
 
        case .validating(let session, _):
            if session == nil {
                if buttonIndex == 0 {
                    sendTestCaseInfoForValidation()
                } else if buttonIndex == 1 {
                    copyTestCaseInfoIntoPasteboard()
                }
            } else {
                if buttonIndex == 0 {
                    runWithAlertPrompt()
                }
            }
        }
    }

}

fileprivate extension TestCaseRecordingController {

    fileprivate struct Static {
        static let userDefaultsStoredTestCaseKey = "test_case_recording_controller_stored_test_case_key"
    }

    fileprivate class StatusBarView: UIView {

        init(title: String = "") {
            self.title = title
            super.init(frame: CGRect.zero)

            backgroundColor = UIColor.red

            addSubview(label)
            label.translatesAutoresizingMaskIntoConstraints = false

            label.fillContainer()

            label.textColor = UIColor.white
            label.textAlignment = .center
            label.font = UIFont.systemFont(ofSize: 14.0)
            label.text = title
        }

        required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        // MARK: -

        private let label = UILabel()
        fileprivate var title: String { didSet { label.text = title } }

    }

    // MARK: -

    fileprivate func presentCompletionAlert() {
        let alert: UIAlertView!

        if isValidationSupported {
            alert = UIAlertView(title: "Test Case", message: "Completion Option", delegate: self,
                                cancelButtonTitle: "Close", otherButtonTitles: "Validate", "Copy Info")
        } else {
            alert = UIAlertView(title: "Test Case", message: "Completion Option", delegate: self,
                                cancelButtonTitle: "Close", otherButtonTitles: "Copy Info")
        }

        alert.show()
    }

    fileprivate func updateUI() {
        guard let container = container else { return }

        func setupStatusViewIfNeeded() {
            guard testCaseStatusBarView == nil else { return }

            testCaseStatusBarView = { (obj: StatusBarView) -> StatusBarView in
                container.addSubview(obj)
                obj.translatesAutoresizingMaskIntoConstraints = false

                obj.fillContainer(withEdges: [.top, .left, .right])

                container.addConstraint(NSLayoutConstraint(item: obj, attribute: .bottom,
                                                           relatedBy: .equal, toItem: container, attribute: .top,
                                                           multiplier: 1.0, constant: 20.0))

                return obj
            }(StatusBarView())
        }

        switch state {
        case .idle:
            testCaseStatusBarView?.removeFromSuperview()
 
        case .recording(let info):
            setupStatusViewIfNeeded()
            testCaseStatusBarView?.title = info.identifier

        case  .validating(let session, _):
            setupStatusViewIfNeeded()
            testCaseStatusBarView?.title = session == nil ? "Validating error" : "Validating..."

        default:
            break
        }
    }

    fileprivate func copyTestCaseInfoIntoPasteboard() {
        var runningTestCaseInfo: TestCaseInfo!
        if case .recording(let info) = state {
            runningTestCaseInfo = info
        } else if case .validating(_, let info) = state {
            runningTestCaseInfo = info
        } else {
            assert(false)
            return
        }

        var infoString = ""
        infoString += "uuid=\(uuid) "
        infoString += "test_case_id=\(runningTestCaseInfo.identifier) "
        infoString += "start_datetime=\(runningTestCaseInfo.creationTimestamp)"
        UIPasteboard.general.string = infoString

        state = .recordingStopping(info: runningTestCaseInfo)
        state = .idle
    }

    fileprivate func sendTestCaseInfoForValidation() {
        guard let url = validationURL, let token = validationToken else {
            assert(false)
            return
        }

        let runningTestCaseInfo: TestCaseInfo!

        if case .recording(let info) = state {
            runningTestCaseInfo = info
        } else if case .validating(let session, let info) = state {
            assert(session == nil)
            runningTestCaseInfo = info
        } else {
            assert(false)
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let parameters = ["uuid": uuid,
                          "test_case_id": runningTestCaseInfo.identifier,
                          "start_datetime": runningTestCaseInfo.creationTimestamp,
                          "delay": "0sec",
                          "token": token]

        let parametersString = parameters.map{ (key, value) in return "\(key)=\(value)" }.reduce("", { $0 + "&" + $1 })
        request.httpBody = parametersString.data(using: .utf8)

        let session = URLSession.sessionWithSSLSystemServerTrustURLSessionDelegate(configuration: .default)
        let task = session.dataTask(with: request){ [weak self] data, response, error in
            guard let slf = self else { return }

            if error != nil {
                slf.state = .validating(session: nil, info: runningTestCaseInfo)
                let alert = UIAlertView(title: "Test Case", message: "Validation Error", delegate: self,
                                        cancelButtonTitle: "Retry", otherButtonTitles: "Copy Info")
                alert.show()
            } else {
                slf.state = .recordingStopping(info: runningTestCaseInfo)
                slf.state = .idle
            }
        }

        state = .validating(session: session, info: runningTestCaseInfo)
        task.resume()
    }

}
