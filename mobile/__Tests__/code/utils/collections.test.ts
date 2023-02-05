/* eslint-disable @typescript-eslint/unbound-method */
import { arrayToSet, int64 } from '../../../../../common/ys'
import { Collections } from '../../../code/utils/collections'

describe(Collections, () => {
  describe(Collections.flatten, () => {
    it('should return empty array', () => {
      expect(Collections.flatten([])).toStrictEqual([])
    })
    it('should actually flatten array', () => {
      expect(Collections.flatten([[1, 2], [3, 4], [], [5, 6]])).toStrictEqual([1, 2, 3, 4, 5, 6])
    })
  })
  describe(Collections.setMinus, () => {
    it('should return empty set', () => {
      expect(Collections.setMinus(new Set(), new Set())).toStrictEqual(new Set())
    })
    it('should actually return set difference', () => {
      expect(Collections.setMinus(new Set([1, 2, 3]), new Set([2, 3, 4]))).toStrictEqual(new Set([1]))
    })
  })
  describe(Collections.maxInt64, () => {
    it('should pick maximum Int64 value from an array with the picker', () => {
      expect(Collections.maxInt64(['1', '3', '5', '4', '2'], BigInt)).toBe(int64(5))
      expect(Collections.maxInt64([], BigInt)).toBeNull()
      expect(Collections.maxInt64(['5'], BigInt)).toBe(int64(5))
    })
  })
  describe(Collections.maxInt32, () => {
    it('should pick maximum Int32 value from an array with the picker', () => {
      expect(Collections.maxInt32(['1', '3', '5', '4', '2'], Number)).toBe(5)
      expect(Collections.maxInt32([], Number)).toBeNull()
      expect(Collections.maxInt32(['5'], Number)).toBe(5)
    })
  })
  describe(Collections.sumBy, () => {
    it('should sum values in array', () => {
      expect(Collections.sumBy(['1', '3', '5', '4', '2'], Number)).toBe(15)
      expect(Collections.sumBy([], Number)).toBe(0)
      expect(Collections.sumBy(['5'], Number)).toBe(5)
    })
  })
  describe(Collections.zip2With, () => {
    it('should zip two arrays having length of the shortest (excessive values are dropped)', () => {
      expect(Collections.zip2With([], [], (a, b) => [a, b])).toEqual([])
      expect(Collections.zip2With([1, 2], [], (a, b) => [a, b])).toEqual([])
      expect(Collections.zip2With([], [1, 2], (a, b) => [a, b])).toEqual([])
      expect(Collections.zip2With([1, 2, 3], [4, 5], (a, b) => [a, b])).toEqual([
        [1, 4],
        [2, 5],
      ])
      expect(Collections.zip2With([1, 2], [3, 4, 5], (a, b) => [a, b])).toEqual([
        [1, 3],
        [2, 4],
      ])
      expect(Collections.zip2With([1, 2, 3], [4, 5, 6], (a, b) => [a, b])).toEqual([
        [1, 4],
        [2, 5],
        [3, 6],
      ])
    })
  })

  describe(Collections.setIntersect, () => {
    it('should return empty set if sets do not overlap', () => {
      expect(Collections.setIntersect(arrayToSet([1, 2, 3, 4]), arrayToSet([5, 6, 7, 8])).size).toBe(0)
    })
    it('should actually return sets intersection', () => {
      expect(Collections.setIntersect(arrayToSet([1, 2, 3]), arrayToSet([2, 3, 4]))).toStrictEqual(arrayToSet([2, 3]))
    })
    it('should return empty set if left set is empty', () => {
      expect(Collections.setIntersect(new Set(), arrayToSet([2, 3, 4])).size).toBe(0)
    })
    it('should return empty set if right set is empty', () => {
      expect(Collections.setIntersect(arrayToSet([2, 3, 4]), new Set()).size).toBe(0)
    })
  })

  describe(Collections.setSubtract, () => {
    it('should return first set if sets do not overlap', () => {
      expect(Collections.setSubtract(arrayToSet([1, 2, 3, 4]), arrayToSet([5, 6, 7, 8]))).toStrictEqual(
        arrayToSet([1, 2, 3, 4]),
      )
    })
    it('should actually return sets subtraction', () => {
      expect(Collections.setSubtract(arrayToSet([1, 2, 3]), arrayToSet([2, 3, 4]))).toStrictEqual(arrayToSet([1]))
    })
    it('should return empty set if left set is empty', () => {
      expect(Collections.setSubtract(new Set(), arrayToSet([2, 3, 4])).size).toBe(0)
    })
    it('should return first set if right set is empty', () => {
      expect(Collections.setSubtract(arrayToSet([2, 3, 4]), new Set())).toStrictEqual(arrayToSet([2, 3, 4]))
    })
  })

  describe(Collections.mapKeys, () => {
    it('should map keys', () => {
      expect(
        Collections.mapKeys(
          new Map([
            ['key1', 'value1'],
            ['key2', 'value2'],
          ]),
          (key) => 'prefix.' + key,
        ),
      ).toStrictEqual(
        new Map([
          ['prefix.key1', 'value1'],
          ['prefix.key2', 'value2'],
        ]),
      )
    })
  })

  describe(Collections.mapNotNull, () => {
    it('should return array of not null elements', () => {
      expect(
        Collections.mapNotNull([1, 2, 1, 3], (value) => {
          if (value === 1) {
            return 1
          } else {
            return null
          }
        }),
      ).toStrictEqual([1, 1])
    })
  })
})
