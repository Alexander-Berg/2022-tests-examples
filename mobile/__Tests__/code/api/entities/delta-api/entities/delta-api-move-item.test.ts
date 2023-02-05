import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiMoveItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-move-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiMoveItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiMoveItem.fromJSONItem(
      JSONItemFromJSON({
        fid: '101',
        mid: '301',
        tid: '401',
        labels: ['lid1', 'lid2'],
      }),
    )
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.move)
    expect(result!.mid).toBe(int64(301))
    expect(result!.fid).toBe(int64(101))
    expect(result!.tid).toBe(int64(401))
    expect(result!.lids).toEqual(['lid1', 'lid2'])
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiMoveItem.fromJSONItem(
      JSONItemFromJSON([
        {
          fid: '101',
          mid: '301',
          tid: '401',
          labels: ['lid1', 'lid2'],
        },
      ]),
    )
    expect(result).toBeNull()
  })
})
