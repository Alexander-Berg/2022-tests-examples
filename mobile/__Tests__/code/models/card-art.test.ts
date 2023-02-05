import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { CardArt } from '../../../code/models/card-art'

describe(CardArt, () => {
  it('should be deserializable from JSON', () => {
    const json = mapJSONItemFromObject({
      original: {
        uri: 'https://sample.com',
      },
      other: {
        uri: 'https://other.com',
      },
    })
    const actual = CardArt.fromJSONItem(json)
    expect(actual).toStrictEqual(
      new CardArt(
        new Map([
          ['original', 'https://sample.com'],
          ['other', 'https://other.com'],
        ]),
      ),
    )
  })
  it('should be able to provide origin', () => {
    const json = mapJSONItemFromObject({
      original: {
        uri: 'https://sample.com',
      },
      other: {
        uri: 'https://other.com',
      },
    })
    const actual = CardArt.fromJSONItem(json)
    expect(actual.original()).toBe('https://sample.com')
  })
})
