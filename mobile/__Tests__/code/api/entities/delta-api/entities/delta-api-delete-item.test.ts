import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiDeleteItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-delete-item'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiDeleteItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiDeleteItem.fromJSONItem(JSONItemFromJSON('301'))
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.delete)
    expect(result!.mid).toBe(int64(301))
  })
  it('should return null if Envelope parsing failed', () => {
    const result = DeltaApiDeleteItem.fromJSONItem(JSONItemFromJSON(['1']))
    expect(result).toBeNull()
  })
})
