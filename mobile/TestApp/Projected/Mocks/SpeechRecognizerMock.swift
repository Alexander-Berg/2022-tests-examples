//
//  SpeechRecognizerMock.swift
//  ProjectedLibTestApp
//
//  Created by Alexander Shchavrovskiy on 05.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YandexNaviProjectedUI
import YandexMapsUtils

final class SpeechRecognizerMock: NSObject, SpeechRecognizer {
    
    func startRecognizing(completion: @escaping (Bool) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5, execute: {
            completion(true)
        })
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 3, execute: { [weak self] in
            guard let slf = self else { return }
            slf.notifier.notify { $0.speechRecognizer(slf, didRecognizeText: "okhotniy", isFinished: false) }
        })

        DispatchQueue.main.asyncAfter(deadline: .now() + 5, execute: { [weak self] in
            guard let slf = self else { return }
            slf.notifier.notify { $0.speechRecognizer(slf, didRecognizeText: "okhotniy ryad", isFinished: true) }
        })
    }

    func cancelRecognizing() {
    }
    
    func addListener(_ listener: SpeechRecognizerListener) {
        notifier.addListener(listener)
    }
    
    func removeListener(_ listener: SpeechRecognizerListener) {
        notifier.removeListener(listener)
    }
    
    
    private let notifier = Notifier<SpeechRecognizerListener>()
}

