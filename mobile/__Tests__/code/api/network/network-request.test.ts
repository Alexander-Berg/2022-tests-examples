import { MailNetworkRequest, NetworkAPIVersions } from '../../../../code/api/mail-network-request'
import {
  JsonRequestEncoding,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
  RequestEncodingKind,
  UrlRequestEncoding,
} from '../../../../../common/code/network/network-request'
import { MapJSONItem } from '../../../../../common/code/json/json-types'

describe(MailNetworkRequest, () => {
  it('should provide default extra values for request', () => {
    const sample = new (class extends MailNetworkRequest {
      public version(): NetworkAPIVersions {
        throw new Error('Not implemented')
      }
      public method(): NetworkMethod {
        throw new Error('Not implemented')
      }
      public path(): string {
        throw new Error('Not implemented')
      }
      public params(): NetworkParams {
        throw new Error('Not implemented')
      }
      public encoding(): RequestEncoding {
        throw new Error('Not implemented')
      }
    })()
    expect(sample.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(sample.headersExtra()).toStrictEqual(new MapJSONItem())
  })
  it('should return expected URL encoding kind', () => {
    expect(new UrlRequestEncoding().kind).toBe(RequestEncodingKind.url)
    expect(new JsonRequestEncoding().kind).toBe(RequestEncodingKind.json)
  })
})
