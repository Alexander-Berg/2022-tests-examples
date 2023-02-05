import { DeltaApiFolder } from '../../../../../../code/api/entities/delta-api/entities/delta-api-folder'
import { DeltaApiFolderCreateItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-folder-create-item'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import folder from './folder.json'

describe(DeltaApiFolderCreateItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItem = JSONItemFromJSON(folder)
    const result = DeltaApiFolderCreateItem.fromJSONItem(jsonItem)
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.folderCreate)
    expect(result!.folder).toStrictEqual(DeltaApiFolder.fromJSONItem(jsonItem)!)
  })
  it('should return null if payload JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([folder])
    const result = DeltaApiFolderCreateItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
