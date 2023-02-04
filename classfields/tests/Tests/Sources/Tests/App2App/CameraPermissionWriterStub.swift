import AutoRuSystemContracts
import RxSwift

final class CameraPermissionWriterStub: CameraPermissionWriter {
    var cameraAuthorizationStatus: AuthorizationStatus {
        .authorized
    }

    var cameraAuthorizationStatusChanged: Observable<AuthorizationStatus> {
        .empty()
    }

    func requestCameraAccess(_ completion: @escaping (Bool) -> Void) {
        completion(true)
    }
}
