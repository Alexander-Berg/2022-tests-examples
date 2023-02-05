import { idToString } from '../../../../../../code/api/common/id'
import { DeltaApiFolderCountersUpdateItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-folder-counters-update-item'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import folderCountersUpdates from './folder-counters-update.json'

describe(DeltaApiFolderCountersUpdateItem, () => {
  it('should parse the result from JSON', () => {
    const jsonItems = folderCountersUpdates.map((update) => JSONItemFromJSON(update))
    const results = jsonItems.map((item) => DeltaApiFolderCountersUpdateItem.fromJSONItem(item))
    expect(results).toHaveLength(2)
    expect(results[0]!.kind).toBe(DeltaApiItemKind.folderCountersUpdate)
    expect(idToString(results[0]!.fid)!).toEqual(folderCountersUpdates[0].fid)
    expect(results[0]!.tab).toBe(folderCountersUpdates[0].tab)
    expect(results[0]!.total).toBe(folderCountersUpdates[0].total)
    expect(results[0]!.unread).toBe(folderCountersUpdates[0].unread)

    expect(results[1]!.kind).toBe(DeltaApiItemKind.folderCountersUpdate)
    expect(idToString(results[1]!.fid)!).toEqual(folderCountersUpdates[1].fid)
    expect(results[1]!.tab).toBe(folderCountersUpdates[1].tab)
    expect(results[1]!.total).toBe(folderCountersUpdates[1].total)
    expect(results[1]!.unread).toBe(folderCountersUpdates[1].unread)
  })
  it('should return null if payload JSON is malformed', () => {
    const jsonItem = JSONItemFromJSON([folderCountersUpdates[0]])
    const result = DeltaApiFolderCountersUpdateItem.fromJSONItem(jsonItem)
    expect(result).toBeNull()
  })
})
