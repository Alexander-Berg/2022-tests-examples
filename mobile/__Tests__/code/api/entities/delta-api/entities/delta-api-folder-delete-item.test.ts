import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiFolderDeleteItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-folder-delete-item'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiFolderDeleteItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiFolderDeleteItem.fromJSONItem(JSONItemFromJSON('101'))
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.folderDelete)
    expect(result!.fid).toBe(int64(101))
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiFolderDeleteItem.fromJSONItem(JSONItemFromJSON(['101']))
    expect(result).toBeNull()
  })
})
