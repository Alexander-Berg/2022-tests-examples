import { int64 } from '../../../../../../../../common/ys'
import { DeltaApiItemKind } from '../../../../../../code/api/entities/delta-api/entities/delta-api-item-kind'
// tslint:disable-next-line: max-line-length
import { DeltaApiMoveToTabItem } from '../../../../../../code/api/entities/delta-api/entities/delta-api-move-to-tab-item'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(DeltaApiMoveToTabItem, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiMoveToTabItem.fromJSONItem(
      JSONItemFromJSON({
        mid: '101',
        tab: 'news',
      }),
    )
    expect(result).not.toBeNull()
    expect(result!.kind).toBe(DeltaApiItemKind.moveToTab)
    expect(result!.mid).toBe(int64(101))
    expect(result!.tab).toBe('news')
  })
  it('should return null on malformed input', () => {
    const result = DeltaApiMoveToTabItem.fromJSONItem(
      JSONItemFromJSON([
        {
          mid: '101',
          tab: 'news',
        },
      ]),
    )
    expect(result).toBeNull()
  })
})
