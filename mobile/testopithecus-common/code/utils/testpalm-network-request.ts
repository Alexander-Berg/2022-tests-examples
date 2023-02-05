import { MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../../../common/code/network/network-request'
import { MBTPlatform } from '../mbt/test/mbt-test'

export class TestpalmTrustedNetworkRequest extends BaseNetworkRequest {
  public constructor(private readonly platform: MBTPlatform) {
    super()
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public targetPath(): string {
    return `testcases/${TestpalmTrustedNetworkRequest.getPathByPlatform(this.platform)}`
  }

  public urlExtra(): NetworkParams {
    return new MapJSONItem()
      .putString('expression', TestpalmTrustedNetworkRequest.getFilterByPlatform(this.platform))
      .putString('include', 'id')
  }

  private static getFilterByPlatform(platform: MBTPlatform): string {
    switch (platform) {
      case MBTPlatform.IOS:
        return '{"type":"AND","left":{"type":"EQ","key":"attributes.5c8abacebb580fb2967a0e23","value":"Trusted"},"right":{"type":"EQ","key":"attributes.5c4acdf4d477414d135c4d29","value":"iOS"}}'
      case MBTPlatform.Android:
        return '{"type":"AND","left":{"type":"EQ","key":"attributes.5c8abacebb580fb2967a0e23","value":"Trusted"},"right":{"type":"EQ","key":"attributes.5c4acdf4d477414d135c4d29","value":"Android"}}'
      default:
        return ''
    }
  }

  private static getPathByPlatform(platform: MBTPlatform): string {
    switch (platform) {
      case MBTPlatform.IOS:
        return 'mobilemail'
      case MBTPlatform.Android:
        return 'mobilemail'
      default:
        return ''
    }
  }
}
