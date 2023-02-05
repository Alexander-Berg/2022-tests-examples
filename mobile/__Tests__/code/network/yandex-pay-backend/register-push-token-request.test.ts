import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { PlatformType } from '../../../../../common/code/network/platform'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { PushTokenType } from '../../../../code/models/push-token-type'
import { RegisterPushTokenRequest } from '../../../../code/network/yandex-pay-backend/register-push-token-request'

describe(RegisterPushTokenRequest, () => {
  it.each([PlatformType.android, PlatformType.ios, PlatformType.electron])(
    'should represent "/api/mobile/v1/wallet/app/register_push_token" request for platform %s',
    (platform) => {
      const request = new RegisterPushTokenRequest(
        'APPID',
        'APPVERSION',
        'HARDWAREID',
        'PUSHTOKEN',
        PushTokenType.firebase,
        platform,
        'DEVICENAME',
        'ZONEID',
        'INSTALLID',
        'DEVICEID',
      )
      expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
      expect(request.method()).toBe(NetworkMethod.post)
      expect(request.targetPath()).toBe('api/mobile/v1/wallet/app/register_push_token')
      expect(request.urlExtra().asMap().size).toBe(0)
      expect(request.headersExtra().asMap().size).toBe(0)
      expect(request.params()).toStrictEqual(
        JSONItemFromJSON({
          app_id: 'APPID',
          app_version: 'APPVERSION',
          hardware_id: 'HARDWAREID',
          push_token: 'PUSHTOKEN',
          platform: platform === PlatformType.electron ? 'unknown' : platform,
          device_name: 'DEVICENAME',
          zone_id: 'ZONEID',
          notify_disabled: false,
          active: true,
          install_id: 'INSTALLID',
          device_id: 'DEVICEID',
          vendor_device_id: 'DEVICEID',
        }),
      )
    },
  )
  it('should add push token type to "/api/mobile/v1/wallet/app/register_push_token" request', () => {
    const request = new RegisterPushTokenRequest(
      'APPID',
      'APPVERSION',
      'HARDWAREID',
      'PUSHTOKEN',
      PushTokenType.huawei,
      PlatformType.android,
      'DEVICENAME',
      'ZONEID',
      'INSTALLID',
      'DEVICEID',
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/wallet/app/register_push_token')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        app_id: 'APPID',
        app_version: 'APPVERSION',
        hardware_id: 'HARDWAREID',
        push_token: 'PUSHTOKEN',
        is_huawei: true,
        platform: 'android',
        device_name: 'DEVICENAME',
        zone_id: 'ZONEID',
        notify_disabled: false,
        active: true,
        install_id: 'INSTALLID',
        device_id: 'DEVICEID',
        vendor_device_id: 'DEVICEID',
      }),
    )
  })
})
