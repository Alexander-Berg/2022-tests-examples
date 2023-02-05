//
//  Created by Timur Turaev on 24.08.2021.
//

import XCTest
import TestUtils
import NetworkLayer
@testable import Backup
import Combine

internal final class BackupStateSynchronizerTest: XCTestCase {
    private var synchronizer: BackupStateSynchronizer!
    private var loader: TestRestoreBackupStatusLoader!
    private var storage: TestRestoreBackupStateStorage!
    private var cancellables: Set<AnyCancellable> = .empty

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.storage = TestRestoreBackupStateStorage()
        self.loader = TestRestoreBackupStatusLoader()
        self.synchronizer = BackupStateSynchronizer(storage: self.storage,
                                                    statusLoader: self.loader,
                                                    statusReloadingDelay: .milliseconds(50))
    }

    func testListeningCreating() throws {
        var statuses: [BackupStatus] = .empty
        let statusExpectation = self.expectation(description: #function)
        statusExpectation.expectedFulfillmentCount = 3
        self.synchronizer.backupStatus
            .print("🕸", to: nil)
            .sink { backupStatus in
                statuses.append(try! XCTUnwrap(backupStatus.toOptional(), backupStatus.toError()!.localizedDescription))
                statusExpectation.fulfill()
            }
            .store(in: &self.cancellables)

        var states: [BackupState] = .empty
        let stateExpectation = self.expectation(description: #function)
        stateExpectation.expectedFulfillmentCount = 4
        self.storage.newStatePublisher
            .print("💥", to: nil)
            .sink { backupState in
                states.append(backupState)
                stateExpectation.fulfill()
            }
            .store(in: &self.cancellables)

        self.synchronizer.stateStorage.setNewBackupState(.pending(action: .create))
        self.synchronizer.syncBackupStatus(restartPolicy: .init(restartOnError: true, restartOnIncompleteStatus: true))

        self.wait(for: [statusExpectation, stateExpectation], timeout: 1)

        XCTAssertEqual(statuses, self.loader.statuses)
        XCTAssertEqual(states, [.pending(action: .create)] + self.loader.statuses.map { .receivedStatus(status: $0) })
    }

    func testRestoreStateIsCodable() {
        func validate(_ status: BackupState) {
            let data = try! PropertyListEncoder().encode(status)
            let decodedValue = try! PropertyListDecoder().decode(BackupState.self, from: data)
            XCTAssertEqual(decodedValue, status)
        }

        validate(.pending(action: .create))
        validate(.pending(action: .restore))
        validate(.receivedStatus(status: .noRestoringBackup))
        validate(.receivedStatus(status: .creatingBackup))
        validate(.receivedStatus(status: .restoringInProgress))
        validate(.receivedStatus(status: .restoringCompleted))
    }
}

private final class TestRestoreBackupStateStorage: BackupWritableStateStoring {
    @Published
    fileprivate var currentBackupState: BackupState?

    func setNewBackupState(_ newState: BackupState) {
        self.currentBackupState = newState
    }

    var newStatePublisher: AnyPublisher<BackupState, Never> {
        return self.$currentBackupState
            .dropFirst()
            .compactMap { $0 }
            .eraseToAnyPublisher()
    }
}

private final class TestRestoreBackupStatusLoader: BackupStatusLoader {
    let statuses = [
        BackupStatus.creatingBackup,
        .restoringInProgress,
        .restoringCompleted
    ]

    private var currentStatusIndex = 0

    func loadStatus(completion: @escaping (Result<BackupStatus>) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(50)) {
            completion(.success(self.statuses[self.currentStatusIndex]))
            self.currentStatusIndex += 1
        }
    }
}

private extension BackupStatus {
    static var noRestoringBackup: Self {
        return BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 15)), hasBeingCreatedBackup: false, restoringBackup: .nothing)
    }

    static var creatingBackup: Self {
        return BackupStatus(currentBackup: .nothing, hasBeingCreatedBackup: true, restoringBackup: .nothing)
    }

    static var restoringInProgress: Self {
        return BackupStatus(currentBackup: .nothing,
                            hasBeingCreatedBackup: false,
                            restoringBackup: .exist(restoredMessages: 1, totalMessages: 10, inProgress: true, method: .fullHierarchy))
    }

    static var restoringCompleted: Self {
        return BackupStatus(currentBackup: .nothing,
                            hasBeingCreatedBackup: false,
                            restoringBackup: .exist(restoredMessages: 0, totalMessages: 0, inProgress: false, method: .fullHierarchy))
    }
}
