import { Int32, int64 } from '../../../../../../common/ys'
import { CursorValueExtractor } from '../../../../code/api/storage/cursor-value-extractor'
import { NullableCursorValueExtractor } from '../../../../code/api/storage/nullable-cursor-value-extractor'

describe(NullableCursorValueExtractor, () => {
  let nullCursor: CursorValueExtractor
  let nonNullCursor: CursorValueExtractor
  let nullSpy: jest.SpyInstance<boolean, [number]>
  let nonNullSpy: jest.SpyInstance<boolean, [number]>
  beforeEach(() => {
    nullCursor = {
      getInt64: jest.fn().mockReturnValue(int64(1000)),
      getInt32: jest.fn().mockReturnValue(100 as Int32),
      getString: jest.fn().mockReturnValue('STRING'),
      getBool: jest.fn().mockReturnValue(true),
      getDouble: jest.fn().mockReturnValue(10.5),
      isNull: jest.fn().mockReturnValue(true),
      getCount: jest.fn().mockReturnValue(5),
    }
    nonNullCursor = {
      getInt64: jest.fn().mockReturnValue(int64(1000)),
      getInt32: jest.fn().mockReturnValue(100 as Int32),
      getString: jest.fn().mockReturnValue('STRING'),
      getBool: jest.fn().mockReturnValue(true),
      getDouble: jest.fn().mockReturnValue(10.5),
      isNull: jest.fn().mockReturnValue(false),
      getCount: jest.fn().mockReturnValue(5),
    }
    nullSpy = jest.spyOn(nullCursor, 'isNull')
    nonNullSpy = jest.spyOn(nonNullCursor, 'isNull')
  })

  it('should extract nullable strings', () => {
    expect(new NullableCursorValueExtractor(nullCursor).getString(0)).toBeNull()
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).getString(0)).toBe('STRING')
    expect(nonNullSpy).toBeCalled()
  })
  it('should extract nullable Int32 values', () => {
    expect(new NullableCursorValueExtractor(nullCursor).getInt32(0)).toBeNull()
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).getInt32(0)).toBe(100)
    expect(nonNullSpy).toBeCalled()
  })
  it('should extract nullable Int64 values', () => {
    expect(new NullableCursorValueExtractor(nullCursor).getInt64(0)).toBeNull()
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).getInt64(0)).toBe(int64(1000))
    expect(nonNullSpy).toBeCalled()
  })
  it('should extract nullable Double values', () => {
    expect(new NullableCursorValueExtractor(nullCursor).getDouble(0)).toBeNull()
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).getDouble(0)).toBeCloseTo(10.5)
    expect(nonNullSpy).toBeCalled()
  })
  it('should extract nullable Bool values', () => {
    expect(new NullableCursorValueExtractor(nullCursor).getBool(0)).toBeNull()
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).getBool(0)).toBe(true)
    expect(nonNullSpy).toBeCalled()
  })
  it('should check if null', () => {
    expect(new NullableCursorValueExtractor(nullCursor).isNull(0)).toBe(true)
    expect(nullSpy).toBeCalled()
    expect(new NullableCursorValueExtractor(nonNullCursor).isNull(0)).toBe(false)
    expect(nonNullSpy).toBeCalled()
  })
})
