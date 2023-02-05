import { int64 } from '../../../../../../common/ys'
import { CursorMappers } from '../../../../code/api/storage/cursor-mappers'
import { MockCursorWithArray } from '../../../__helpers__/mock-patches'

describe(CursorMappers, () => {
  describe(CursorMappers.arrayMapper, () => {
    it('should process cursor and build an array', () => {
      const cursor = MockCursorWithArray([
        [true, 10, int64(20), 30.5, 'string', 'nonnull'],
        [false, -10, int64(-20), -30.5, 'another', null],
      ])
      const results = CursorMappers.arrayMapper(cursor, (extractor) => ({
        booleanValue: extractor.getBool(0),
        int32Value: extractor.getInt32(1),
        int64Value: extractor.getInt64(2),
        doubleValue: extractor.getDouble(3),
        stringValue: extractor.getString(4),
        countValue: extractor.getCount(),
        isNullValue: extractor.isNull(5),
      }))
      expect(results).toHaveLength(2)
      expect(results[0]).toStrictEqual({
        booleanValue: true,
        int32Value: 10,
        int64Value: int64(20),
        doubleValue: 30.5,
        stringValue: 'string',
        countValue: 6,
        isNullValue: false,
      })
      expect(results[1]).toStrictEqual({
        booleanValue: false,
        int32Value: -10,
        int64Value: int64(-20),
        doubleValue: -30.5,
        stringValue: 'another',
        countValue: 6,
        isNullValue: true,
      })
    })
  })

  describe(CursorMappers.singleStringColumn, () => {
    it('should extract string array from the first column', () => {
      const cursor = MockCursorWithArray([
        ['str1', 10],
        ['str2', 20],
      ])
      const results = CursorMappers.singleStringColumn(cursor)
      expect(results).toStrictEqual(['str1', 'str2'])
    })
  })

  describe(CursorMappers.singleIDColumn, () => {
    it('should extract ID array from the first column', () => {
      const cursor = MockCursorWithArray([
        [int64(1), 10],
        [int64(2), 20],
      ])
      const results = CursorMappers.singleStringColumn(cursor)
      expect(results).toStrictEqual([int64(1), int64(2)])
    })
  })

  describe(CursorMappers.singleInt32Column, () => {
    it('should extract Int32 array from the first column', () => {
      const cursor = MockCursorWithArray([
        [10, 'str1'],
        [20, 'str2'],
      ])
      const results = CursorMappers.singleInt32Column(cursor)
      expect(results).toStrictEqual([10, 20])
    })
  })

  describe(CursorMappers.singleInt64Column, () => {
    it('should extract Int64 array from the first column', () => {
      const cursor = MockCursorWithArray([
        [int64(10), 'str1'],
        [int64(20), 'str2'],
      ])
      const results = CursorMappers.singleInt64Column(cursor)
      expect(results).toStrictEqual([int64(10), int64(20)])
    })
  })
})
