import MarketFlexActions
import MarketFlexCore
import XCTest

extension AnyActionDecoderImpl {
    private static var decoderKey: CodingUserInfoKey {
        // swiftlint:disable:next force_unwrapping
        CodingUserInfoKey(rawValue: "decoder")!
    }

    private struct DecoderWrapper<T>: Decodable {
        let result: T

        init(from decoder: Decoder) throws {
            result = try XCTUnwrap(decoder.userInfo[AnyActionDecoderImpl.decoderKey] as? (Decoder) throws -> T)(decoder)
        }
    }

    func performDecode(_ json: String) throws -> AnyAction {
        let jsonDecoder = JSONDecoder()
        jsonDecoder.userInfo[AnyActionDecoderImpl.decoderKey] = decode
        let data = try XCTUnwrap(json.data(using: .utf8))
        let decoded = try jsonDecoder.decode(DecoderWrapper<AnyAction>.self, from: data)
        return decoded.result
    }
}
