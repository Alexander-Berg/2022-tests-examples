import Foundation
import AVFoundation
import YandexMapsMobile

extension YMKAnnotationLanguage {
    static func items() -> [YMKAnnotationLanguage] {
        return [
            .english,
            .russian,
            .french,
            .ukrainian,
            .italian,
            .turkish,
            .hebrew
        ]
    }

    func toString() -> String {
        switch (self) {
        case .english   : return "English"
        case .russian   : return "Russian"
        case .french    : return "French"
        case .ukrainian : return "Ukrainian"
        case .italian   : return "Italian"
        case .turkish   : return "Turkish"
        case .hebrew    : return "Hebrew"
        default         : assert(false)
        }
        return "UNKNOWN"
    }

    func toLanguageCode() -> String {
        switch (self) {
        case .english   : return "en-US"
        case .russian   : return "ru-RU"
        case .french    : return "fr-FR"
        case .ukrainian : return "uk-UA"
        case .italian   : return "it-IT"
        case .turkish   : return "tr-TR"
        case .hebrew    : return "he-IL"
        default         : assert(false)
        }
        return "ru-RU"
    }
}

class SpeakerImpl: NSObject, YMKSpeaker {
    let callback: (String?) -> Void
    let synthesizer: AVSpeechSynthesizer
    var voice: AVSpeechSynthesisVoice?

    init(withCallback callback: @escaping (String?) -> Void) {
        self.callback = callback
        synthesizer = AVSpeechSynthesizer()
    }

    func say(with phrase: YMKLocalizedPhrase) {
        let text = phrase.text
        if voice != nil {
            let utterance = AVSpeechUtterance(string: text)
            utterance.voice = voice
            synthesizer.speak(utterance)
        }
        callback(text)
    }

    func duration(with phrase: YMKLocalizedPhrase) -> Double {
        // simplified Russian formula from navikit/tts/ios/YNKTtsPlayerImpl.m
        return Double(phrase.text.count) * 0.061 + 0.65
    }

    func reset() {
        synthesizer.stopSpeaking(at: AVSpeechBoundary.word);
    }

    func setLanguage(_ language: YMKAnnotationLanguage) {
        voice = AVSpeechSynthesisVoice(language: language.toLanguageCode())
        if voice == nil {
            callback("Voice is not supported via TTS")
        }
    }
}
