import { ClientInfo, ClientPlatform } from '../../../../../code/network/mobile-backend/entities/client-info'

describe(ClientInfo, () => {
  it('build ClientInfo', () => {
    const info = new ClientInfo(ClientPlatform.android, '123')
    expect(info.platform).toStrictEqual(ClientPlatform.android)
    expect(info.version).toStrictEqual('123')
  })
})
