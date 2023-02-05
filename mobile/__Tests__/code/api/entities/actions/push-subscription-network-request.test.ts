import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { PushSubscriptionNetworkRequest } from '../../../../../code/api/entities/actions/push-subscription-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(PushSubscriptionNetworkRequest, () => {
  it('should build request to subscribe for pushes', () => {
    const request = new PushSubscriptionNetworkRequest(
      '0987654321',
      'veryRandomPushToken',
      'com.yandex.mail',
      'android',
      true,
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        device: '0987654321',
        push_token: 'veryRandomPushToken',
        app_name: 'com.yandex.mail',
        os: 'android',
      }),
    )
    expect(request.path()).toBe('push')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
  it('should build Unmark as Spam request', () => {
    const request = new PushSubscriptionNetworkRequest(
      '1234567890123456789',
      'somePushToken',
      'com.yandex.mail',
      'ios',
      false,
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        device: '1234567890123456789',
        push_token: 'somePushToken',
        app_name: 'com.yandex.mail',
        os: 'ios',
      }),
    )
    expect(request.path()).toBe('push')
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem().putString('unsubscribe', 'yes'))
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
