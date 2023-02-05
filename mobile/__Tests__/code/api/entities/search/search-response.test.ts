import { int64 } from '../../../../../../../common/ys'
import { IntegerJSONItem } from '../../../../../../common/code/json/json-types'
import { MessageMeta } from '../../../../../code/api/entities/message/message-meta'
import { SearchResponse, searchResponseFromJSONItem } from '../../../../../code/api/entities/search/search-response'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(SearchResponse, () => {
  it('should return null if JSON Item is malformed', () => {
    expect(searchResponseFromJSONItem(IntegerJSONItem.fromInt32(10))).toBeNull()
    expect(searchResponseFromJSONItem(JSONItemFromJSON({}))).toBeNull()
  })
  it('should return error on error status', () => {
    const response = searchResponseFromJSONItem(
      JSONItemFromJSON({
        status: { status: 2, phrase: 'PHRASE', trace: 'TRACE' },
      }),
    )
    expect(response).not.toBeNull()
    expect(response!.networkStatus()).toStrictEqual(
      new NetworkStatus(NetworkStatusCode.temporaryError, 'TRACE', 'PHRASE'),
    )
    expect(response!.messages).toHaveLength(0)
  })
  it('should return empty messages payload if batch is empty', () => {
    const malformedResponse = clone(sample)
    malformedResponse.messages = {}
    const result = searchResponseFromJSONItem(JSONItemFromJSON(malformedResponse))
    expect(result).toStrictEqual(new SearchResponse(new NetworkStatus(NetworkStatusCode.ok), []))
  })
  it('should parse search response with metas', () => {
    const result = searchResponseFromJSONItem(JSONItemFromJSON(sample))
    expect(result).not.toBeNull()
    expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(result!.messages).toStrictEqual([
      new MessageMeta(
        int64(1),
        int64(1),
        int64(1),
        ['1'],
        false,
        'Re:',
        'Message subject',
        'Message first line',
        '"Ivan Ivanov" <email@example.com>',
        false,
        false,
        null,
        int64(1333976598000),
        true,
        null,
        1,
      ),
    ])
  })
})
