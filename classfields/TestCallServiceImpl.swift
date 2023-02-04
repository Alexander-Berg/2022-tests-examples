#if DEBUG

import UIKit
import AutoRuCalls
import AutoRuCallsCore
import AutoRuProtoModels
import AutoRuSystemContracts

public final class TestCallServiceImpl: TestCallService {
    private let window: UIWindow?
    private let offerCallOutput: OfferCallOutput?
    private let commonOutgoingCallStrategyOutput: CommonOutgoingCallStrategyOutput?
    private let microphonePermissionWriter: MicrophonePermissionWriter
    private let cameraPermissionWriter: CameraPermissionWriter

    public init(
        window: UIWindow?,
        offerCallOutput: OfferCallOutput?,
        commonOutgoingCallStrategyOutput: CommonOutgoingCallStrategyOutput?,
        microphonePermissionWriter: MicrophonePermissionWriter,
        cameraPermissionWriter: CameraPermissionWriter
    ) {
        self.window = window
        self.offerCallOutput = offerCallOutput
        self.commonOutgoingCallStrategyOutput = commonOutgoingCallStrategyOutput
        self.microphonePermissionWriter = microphonePermissionWriter
        self.cameraPermissionWriter = cameraPermissionWriter
    }

    public func makeCall(_ preset: TestCallPreset) {
        let options = makeStartCallOptionSet(preset)
        let optionSet = StartCallOptionSet(voxImplant: [options], pstn: [])

        switch preset.mode {
        case .offer:
            CallService.makeCallForOffer(
                optionSet,
                environment: OutgoingCallForOfferStrategyEnvironment(
                    offer: Auto_Api_Offer(),
                    dialingSource: .offerCard,
                    microphonePermissionWriter: microphonePermissionWriter,
                    cameraPermissionWriter: cameraPermissionWriter,
                    flowObserver: nil,
                    offerCallOutput: offerCallOutput,
                    window: window
                )
            )

        case .generic, .raw:
            CallService.makeCall(
                optionSet,
                environment: CommonOutgoingCallStrategyEnvironment(
                    dialingSource: nil,
                    microphonePermissionWriter: microphonePermissionWriter,
                    cameraPermissionWriter: cameraPermissionWriter,
                    flowObserver: nil,
                    output: commonOutgoingCallStrategyOutput,
                    window: window
                )
            )
        }
    }

    private func makeStartCallOptionSet(_ preset: TestCallPreset) -> VoxImplantStartCallOptions {
        let headers: [String: String]

        switch preset.mode {
        case .offer:
            let payload = preset.offerPayload

            headers = [
                "redirect_id": payload.redirectID,
                "alias": payload.alias,
                "alias_and_subject": payload.aliasAndSubject,
                "user_pic": payload.userPic,
                "offer_mark": payload.mark,
                "offer_model": payload.model,
                "offer_generation": payload.generation,
                "offer_year": payload.year,
                "offer_pic": payload.pic,
                "offer_link": payload.link,
                "offer_price": payload.price,
                "offer_price_currency": payload.currency
            ]

        case .generic:
            let payload = preset.genericPayload
            headers = [
                "redirect_id": payload.redirectID,
                "alias": payload.alias,
                "alias_and_subject": payload.aliasAndSubject,
                "user_pic": payload.userPic,
                "image": payload.image,
                "url": payload.url,
                "line1": payload.line1,
                "line2": payload.line2,
                "handle": payload.handle,
                "subjectType": payload.subjectType
            ]

        case .raw:
            let payload = preset.rawPayload
            headers = payload.items.reduce(into: [:], { dict, item in
                let name = item.header.trimmingCharacters(in: .whitespaces)
                guard !name.isEmpty else { return }

                dict["X-" + name] = item.value
            })
        }

        return VoxImplantStartCallOptions(
            voxUsername: preset.voxUsername,
            payload: headers,
            isVideo: false,
            customData: nil
        )
    }
}

#endif
