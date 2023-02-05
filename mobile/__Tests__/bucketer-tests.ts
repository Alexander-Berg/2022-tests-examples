import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import * as assert from 'assert'
import { getSliceIndexesForBuckets } from '../../testopithecus-common/code/utils/utils'

describe('Bucketer tests', () => {
  ConsoleLog.setup()

  it('should split buckets correct if no reminder', (done) => {
    const indexes = getSliceIndexesForBuckets(8, 4)
    assert.deepStrictEqual(indexes, [0, 2, 4, 6, 8], 'indexes are incorrect')
    done()
  })

  it('should have bigger first buckets if have reminder', (done) => {
    const indexes = getSliceIndexesForBuckets(8, 5)
    assert.deepStrictEqual(indexes, [0, 2, 4, 6, 7, 8], 'indexes are incorrect')
    done()
  })

  it('should split buckets correct if there is more buckets then tests', (done) => {
    const indexes = getSliceIndexesForBuckets(3, 5)
    assert.deepStrictEqual(indexes, [0, 1, 2, 3, 3, 3], 'indexes are incorrect')
    done()
  })
})
