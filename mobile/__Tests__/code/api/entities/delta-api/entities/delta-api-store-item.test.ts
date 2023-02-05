import { DeltaApiEnvelope } from '../../../../../../code/api/entities/delta-api/entities/delta-api-envelope'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiStoreItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-store-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import envelope from './envelope.json'

describe(DeltaApiStoreItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItem = JSONItemFromJSON(envelope)
    const result = DeltaApiStoreItem.fromJSONItem(jsonItem)
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.store)
    expect(result!.envelope).toStrictEqual(DeltaApiEnvelope.fromJSONItem(jsonItem)!)
  })
  it('should return null if JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([envelope])
    const result = DeltaApiStoreItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
