import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiUpdateItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-update-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiUpdateItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiUpdateItem.fromJSONItem(
      JSONItemFromJSON({
        mid: '301',
        labels: ['lid1', 'lid2'],
      }),
    )
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.update)
    expect(result!.mid).toBe(int64(301))
    expect(result!.lids).toEqual(['lid1', 'lid2'])
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiUpdateItem.fromJSONItem(
      JSONItemFromJSON([
        {
          mid: '301',
          labels: ['lid1', 'lid2'],
        },
      ]),
    )
    expect(result).toBeNull()
  })
})
