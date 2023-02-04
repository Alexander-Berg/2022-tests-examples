//
//  PersonalizationServiceMocks.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Pavel Zhuravlev on 05.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils
@testable import YREServiceLayer

/// Here we almost replicate the behavior of `PersonalizationService`.
/// It's better to mock `WebServicesProtocol` and pass this mock to `PersonalizationService` instance,
/// but currently that's too complicated.
///
/// Differences from the real Service - here we support only one observer to simplify things.
final class PersonalizationServiceMock: PersonalizationServiceProtocol {
    typealias ResultType = TaskResult<Void, Error>

    init(predefinedResult: ResultType) {
        self.predefinedResult = predefinedResult
    }

    func upsertNote(
        offerID: String,
        note: String,
        completion: @escaping (ResultType) -> Void
    ) {
        DispatchQueue.main.async {
            switch self.predefinedResult {
                case .succeeded:
                    self.notifyUserNoteObservers(offerID: offerID, userNote: note)
                case .failed, .cancelled:
                    break
            }
            completion(self.predefinedResult)
        }
    }

    func deleteNote(offerID: String,
                    completion: @escaping (ResultType) -> Void) {
        DispatchQueue.main.async {
            switch self.predefinedResult {
                case .succeeded:
                    self.notifyUserNoteObservers(offerID: offerID, userNote: nil)
                case .failed, .cancelled:
                    break
            }
            completion(self.predefinedResult)
        }
    }

    func hideOffer(_ offerID: String,
                   completion: @escaping ((ResultType) -> Void)) {
        DispatchQueue.main.async {
            switch self.predefinedResult {
                case .succeeded:
                    self.notifyHiddenOfferObservers(offerID: offerID)
                case .failed, .cancelled:
                    break
            }
            completion(self.predefinedResult)
        }
    }

    // MARK: Private

    private let predefinedResult: ResultType
    private var observer: PersonalizationServiceObserver?
}

extension PersonalizationServiceMock: PersonalizationStateObservationService {
    func observe(by observer: PersonalizationServiceObserver) -> StateObservationProtocol? {
        self.observer = observer

        let observation = StateObservation { [weak self] in
            self?.observer = nil
        }
        return observation
    }

    private func notifyUserNoteObservers(offerID: String, userNote: String?) {
        DispatchQueue.main.async {
            self.observer?.onUserNoteChanged(for: offerID, to: userNote)
        }
    }

    private func notifyHiddenOfferObservers(offerID: String) {
        DispatchQueue.main.async {
            self.observer?.onOfferHidden(offerID)
        }
    }
}
