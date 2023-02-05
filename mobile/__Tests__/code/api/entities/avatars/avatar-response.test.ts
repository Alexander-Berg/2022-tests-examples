import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { Ava, Avatar, AvaType } from '../../../../../code/api/entities/avatars/avatar'
import { avatarResponseFromJSONItem } from '../../../../../code/api/entities/avatars/avatar-response'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(avatarResponseFromJSONItem, () => {
  it('should return null if JSON Item is not map', () => {
    const item = JSONItemFromJSON([sample])
    const result = avatarResponseFromJSONItem(item)
    expect(result).toBeNull()
  })
  it('should return null if JSON Item does not contain status', () => {
    const copy = clone(sample)
    delete copy.status
    const item = JSONItemFromJSON(copy)
    const result = avatarResponseFromJSONItem(item)
    expect(result).toBeNull()
  })
  it('should return empty payload if status is not ok', () => {
    const copy = clone(sample)
    copy.status.status = 2
    const item = JSONItemFromJSON(copy)
    const result = avatarResponseFromJSONItem(item)
    expect(result).not.toBeNull()
    expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError))
    expect(result!.payload.size).toBe(0)
  })
  it('should return empty payload if status is malformed', () => {
    const copy = clone(sample)
    copy.status = []
    const item = JSONItemFromJSON(copy)
    const result = avatarResponseFromJSONItem(item)
    expect(result).not.toBeNull()
    expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError))
    expect(result!.payload.size).toBe(0)
  })
  it('should return empty payload if JSON has no catdog item', () => {
    const copy = clone(sample)
    delete copy.catdog
    const item = JSONItemFromJSON(copy)
    const result = avatarResponseFromJSONItem(item)!
    expect(result.status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(result.payload.size).toBe(0)
  })
  it('should return empty payload if catdog item is not map', () => {
    const copy = clone(sample)
    copy.catdog = []
    const item = JSONItemFromJSON(copy)
    const result = avatarResponseFromJSONItem(item)!
    expect(result.status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(result.payload.size).toBe(0)
  })
  it('should payload if it is properly formed', () => {
    const item = JSONItemFromJSON(sample)
    const result = avatarResponseFromJSONItem(item)!
    expect(result.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(result.payload).toStrictEqual(
      new Map([
        [
          'dzcomfly@gmail.com',
          [new Avatar('dzcomfly', null, 'dzcomfly@gmail.com', '', 'DZ', '#f39718', null, true, true)],
        ],
        ['comfly@mail.ru', [new Avatar('comfly', null, 'comfly@mail.ru', '', 'CO', '#f39718', null, true, true)]],
        [
          'dzcomfly@yandex.ru',
          [
            new Avatar(
              'dzcomfly',
              null,
              'dzcomfly@yandex.ru',
              'Dmitry Zakharov',
              'DZ',
              '#f39718',
              new Ava(
                AvaType.avatar,
                'https://avatars.mds.yandex.net/get-yapic/61207/LTPrMwv9ZFXNby39TL4a4r8hCk-1/islands-200',
              ),
              true,
              true,
            ),
          ],
        ],
        [
          '"testV D" <testdvf@yandex.ru>',
          [
            new Avatar(
              'testdvf',
              null,
              '"testV D" <testdvf@yandex.ru>',
              'testV D',
              'TD',
              '#45b0e6',
              new Ava(
                AvaType.avatar,
                'https://avatars.mds.yandex.net/get-yapic/61207/LTPrMwv9ZFXNby39TL4a4r8hCk-1/islands-200',
              ),
              true,
              true,
            ),
          ],
        ],
        ['$$$', []],
      ]),
    )
  })
})
