import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiLabel } from '../../../../../../code/api/entities/delta-api/entities/delta-api-label'
import { DeltaApiLabelCreateItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-label-create-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import label from './label.json'

describe(DeltaApiLabelCreateItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItem = JSONItemFromJSON(label)
    const result = DeltaApiLabelCreateItem.fromJSONItem(jsonItem)
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.labelCreate)
    expect(result!.label).toStrictEqual(DeltaApiLabel.fromJSONItem(jsonItem)!)
  })
  it('should return null if payload JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([label])
    const result = DeltaApiLabelCreateItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
