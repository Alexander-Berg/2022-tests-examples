import { DeltaApiEnvelope } from '../../../../../../code/api/entities/delta-api/entities/delta-api-envelope'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiQuickSaveItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-quick-save-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import envelope from './envelope.json'

describe(DeltaApiQuickSaveItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItem = JSONItemFromJSON(envelope)
    const result = DeltaApiQuickSaveItem.fromJSONItem(jsonItem)
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.quickSave)
    expect(result!.envelope).toStrictEqual(DeltaApiEnvelope.fromJSONItem(jsonItem)!)
  })
  it('should return null if JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([envelope])
    const result = DeltaApiQuickSaveItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
