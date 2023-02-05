import { NetworkMethod, UrlRequestEncoding } from '../../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { PaymentsHistoryRequest } from '../../../../../../code/network/mobile-backend/entities/history/payments-history-request'
import { int64 } from '../../../../../../../../common/ys'

describe(PaymentsHistoryRequest, () => {
  it('should build PaymentsHistoryRequest request', () => {
    const request = new PaymentsHistoryRequest(
      'uid',
      'token',
      'appID',
      'merchantID',
      'status',
      int64(0),
      int64(1),
      2,
      3,
    )
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        uid: 'uid',
        token: 'token',
        app_id: 'appID',
        merchant_id: 'merchantID',
        status: 'status',
        date_start: int64(0),
        date_end: int64(1),
        skip: 2,
        count: 3,
      }),
    )
    expect(request.targetPath()).toBe('payments_history')
    expect(request.urlExtra().asMap().size).toBe(0)
  })

  it('should build empty PaymentsHistoryRequest request', () => {
    const request = new PaymentsHistoryRequest(null, null, null, null, null, null, null, null, null)
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params()).toStrictEqual(JSONItemFromJSON({}))
    expect(request.targetPath()).toBe('payments_history')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
