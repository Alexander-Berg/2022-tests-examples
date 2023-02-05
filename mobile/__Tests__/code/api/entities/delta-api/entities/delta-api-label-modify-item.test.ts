import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiLabel } from '../../../../../../code/api/entities/delta-api/entities/delta-api-label'
import { DeltaApiLabelModifyItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-label-modify-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import label from './label.json'

describe(DeltaApiLabelModifyItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItem = JSONItemFromJSON(label)
    const result = DeltaApiLabelModifyItem.fromJSONItem(jsonItem)
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.labelModify)
    expect(result!.label).toStrictEqual(DeltaApiLabel.fromJSONItem(jsonItem)!)
  })
  it('should return null if JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([label])
    const result = DeltaApiLabelModifyItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
