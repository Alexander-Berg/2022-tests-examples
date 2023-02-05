import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiLabelDeleteItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-label-delete-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiLabelDeleteItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiLabelDeleteItem.fromJSONItem(JSONItemFromJSON('lid1'))
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.labelDelete)
    expect(result!.lid).toBe('lid1')
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiLabelDeleteItem.fromJSONItem(JSONItemFromJSON(['lid1']))
    expect(result).toBeNull()
  })
})
