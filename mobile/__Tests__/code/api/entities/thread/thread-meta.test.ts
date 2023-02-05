import { int64, stringToInt64 } from '../../../../../../../common/ys'
import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import { idFromString } from '../../../../../code/api/common/id'
import { ThreadMeta, threadMetaFromJSONItem } from '../../../../../code/api/entities/thread/thread-meta'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import sample from './sample.json'

describe(ThreadMeta, () => {
  describe(threadMetaFromJSONItem, () => {
    it('should return null if malformed JSON Item is passed', () => {
      expect(threadMetaFromJSONItem(new ArrayJSONItem())).toBeNull()
    })
    it('should return Thread Meta from JSON Item having no thread count, setting 1 by default', () => {
      const item = sample[0].messageBatch.messages[0]
      const result = threadMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toStrictEqual(
        new ThreadMeta(
          stringToInt64(item.scn!)!,
          idFromString(item.tid)!,
          idFromString(item.fid)!,
          idFromString(item.mid)!,
          1,
        ),
      )
    })
    it('should return Message Meta from JSON Item with thread count', () => {
      const item: any = sample[1].messageBatch.messages[0]
      const result = threadMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toStrictEqual(
        new ThreadMeta(
          stringToInt64(item.scn!)!,
          idFromString(item.tid)!,
          idFromString(item.fid)!,
          idFromString(item.mid)!,
          Number.parseInt(item.threadCount!, 10),
        ),
      )
    })
  })
  it('should have thread count of 1 by default', () => {
    expect(new ThreadMeta(int64(12345), int64(401), int64(101), int64(301))).toStrictEqual(
      new ThreadMeta(int64(12345), int64(401), int64(101), int64(301), 1),
    )
  })
})
