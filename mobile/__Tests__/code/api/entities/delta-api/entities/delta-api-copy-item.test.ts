import { DeltaApiCopyItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-copy-item'
import { DeltaApiEnvelope } from '../../../../../../code/api/entities/delta-api/entities/delta-api-envelope'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import envelope from './envelope.json'

describe(DeltaApiCopyItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiCopyItem.fromJSONItem(JSONItemFromJSON(envelope))
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.copy)
    expect(result!.envelope).toStrictEqual(DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(envelope))!)
  })
  it('should return null if Envelope parsing failed', () => {
    const result = DeltaApiCopyItem.fromJSONItem(JSONItemFromJSON([envelope]))
    expect(result).toBeNull()
  })
})
