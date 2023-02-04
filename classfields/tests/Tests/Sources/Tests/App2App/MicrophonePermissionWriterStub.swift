import AutoRuSystemContracts
import RxSwift

final class MicrophonePermissionWriterStub: MicrophonePermissionWriter {
    var microphoneAuthorizationStatus: AuthorizationStatus {
        .authorized
    }

    var microphoneAuthorizationStatusChanged: Observable<AuthorizationStatus> {
        .empty()
    }

    func requestMicrophoneAccess(_ completion: @escaping (Bool) -> Void) {
        completion(true)
    }
}
