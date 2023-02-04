import Foundation
import AutoRuProtoModels

extension Responses {
    enum Device { }
}

extension Responses.Device {
    static func hello(for state: BackendState) -> MessageResponse<Auto_Api_HelloResponse> {
        MessageResponse(
            message:
                .with { msg in
                    msg.experimentsConfig = .with { msg in
                        msg.flags = state.experiments.flags
                    }
                    msg.status = .success
                },
            additionalHeaders: [
                "x-yandex-expflags": state.experiments.flags.joined(separator: ",")
            ]
        )
    }
}
