import { int64 } from '../../../../../common/ys'
import { maxDouble, maxInt32, maxInt64, minDouble, minInt32, minInt64 } from '../../../code/utils/math'

it('should evaluate max/min', () => {
  expect(minInt32(1, 2)).toBe(1)
  expect(minInt32(2, 1)).toBe(1)
  expect(minInt64(int64(1), int64(2))).toBe(int64(1))
  expect(minInt64(int64(2), int64(1))).toBe(int64(1))
  expect(minDouble(1.1, 2.2)).toBe(1.1)
  expect(minDouble(2.2, 1.1)).toBe(1.1)
  expect(maxInt32(1, 2)).toBe(2)
  expect(maxInt32(2, 1)).toBe(2)
  expect(maxInt64(int64(1), int64(2))).toBe(int64(2))
  expect(maxInt64(int64(2), int64(1))).toBe(int64(2))
  expect(maxDouble(1.1, 2.2)).toBe(2.2)
  expect(maxDouble(2.2, 1.1)).toBe(2.2)
})
