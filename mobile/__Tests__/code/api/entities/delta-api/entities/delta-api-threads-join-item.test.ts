import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiThreadsJoinItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-threads-join-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiThreadsJoinItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiThreadsJoinItem.fromJSONItem(
      JSONItemFromJSON({
        mid: '301',
        tid: '401',
        labels: ['lid1', 'lid2'],
      }),
    )
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.threadsJoin)
    expect(result!.mid).toBe(int64(301))
    expect(result!.tid).toBe(int64(401))
    expect(result!.lids).toEqual(['lid1', 'lid2'])
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiThreadsJoinItem.fromJSONItem(
      JSONItemFromJSON([
        {
          mid: '301',
          tid: '401',
          labels: ['lid1', 'lid2'],
        },
      ]),
    )
    expect(result).toBeNull()
  })
  it('should return null if mid is invalid', () => {
    const result = DeltaApiThreadsJoinItem.fromJSONItem(
      JSONItemFromJSON({
        mid: 'invalid',
        tid: '401',
        labels: ['lid1', 'lid2'],
      }),
    )
    expect(result).toBeNull()
  })
  it('should return null if tid is invalid', () => {
    const result = DeltaApiThreadsJoinItem.fromJSONItem(
      JSONItemFromJSON({
        mid: '301',
        tid: 'invalid',
        labels: ['lid1', 'lid2'],
      }),
    )
    expect(result).toBeNull()
  })
})
