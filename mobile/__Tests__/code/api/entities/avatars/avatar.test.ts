import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import {
  Ava,
  Avatar,
  avatarFromJSONItem,
  AvaType,
  avaTypeFromInt32,
  avaTypeFromServerValue,
  avaTypeToInt32,
} from '../../../../../code/api/entities/avatars/avatar'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'

describe(avaTypeFromServerValue, () => {
  it('should map server avatar type to local', () => {
    expect(avaTypeFromServerValue('avatar')).toBe(AvaType.avatar)
    expect(avaTypeFromServerValue('icon')).toBe(AvaType.icon)
    expect(avaTypeFromServerValue('none')).toBe(AvaType.none)
    expect(avaTypeFromServerValue('other')).toBe(AvaType.unknown)
  })
})

describe(avatarFromJSONItem, () => {
  it('should return null if JSON Item is not map', () => {
    expect(avatarFromJSONItem(new ArrayJSONItem(), 'sample@yandex.ru')).toBeNull()
  })
  it('should return null if valid is not true', () => {
    expect(avatarFromJSONItem(JSONItemFromJSON({ valid: false }), 'sample@yandex.ru')).toBeNull()
  })
  it('should return Avatar parsed from JSON Item', () => {
    expect(
      avatarFromJSONItem(
        JSONItemFromJSON({
          local: 'local',
          domain: 'domain.com',
          display_name: 'Local Domain',
          mono: 'LD',
          color: '#aabbccdd',
          ava: {
            type: 'avatar',
            url_mobile: 'https://sample.com/',
          },
          valid: true,
        }),
        'sample@yandex.ru',
      ),
    ).toStrictEqual(
      new Avatar(
        'local',
        'domain.com',
        'sample@yandex.ru',
        'Local Domain',
        'LD',
        '#aabbccdd',
        new Ava(AvaType.avatar, 'https://sample.com/'),
        true,
        true,
      ),
    )
  })
  it('should return null ava if it is malformed', () => {
    expect(
      avatarFromJSONItem(
        JSONItemFromJSON({
          local: 'local',
          domain: 'domain.com',
          display_name: 'Local Domain',
          mono: 'LD',
          color: '#aabbccdd',
          ava: [],
          valid: true,
        }),
        'sample@yandex.ru',
      ),
    ).toStrictEqual(
      new Avatar('local', 'domain.com', 'sample@yandex.ru', 'Local Domain', 'LD', '#aabbccdd', null, true, true),
    )
  })
  it('should return null ava if it is absent', () => {
    expect(
      avatarFromJSONItem(
        JSONItemFromJSON({
          local: 'local',
          domain: 'domain.com',
          display_name: 'Local Domain',
          mono: 'LD',
          color: '#aabbccdd',
          valid: true,
        }),
        'sample@yandex.ru',
      ),
    ).toStrictEqual(
      new Avatar('local', 'domain.com', 'sample@yandex.ru', 'Local Domain', 'LD', '#aabbccdd', null, true, true),
    )
  })
})

describe(avaTypeToInt32, () => {
  it('should convert AvaType to integer', () => {
    expect(avaTypeToInt32(AvaType.avatar)).toBe(0)
    expect(avaTypeToInt32(AvaType.icon)).toBe(1)
    expect(avaTypeToInt32(AvaType.none)).toBe(2)
    expect(avaTypeToInt32(AvaType.unknown)).toBe(3)
  })
})

describe(avaTypeFromInt32, () => {
  it('should convert integer to AvaType', () => {
    expect(avaTypeFromInt32(0)).toEqual(AvaType.avatar)
    expect(avaTypeFromInt32(1)).toEqual(AvaType.icon)
    expect(avaTypeFromInt32(2)).toEqual(AvaType.none)
    expect(avaTypeFromInt32(3)).toEqual(AvaType.unknown)
    expect(avaTypeFromInt32(4)).toEqual(AvaType.unknown)
  })
})
