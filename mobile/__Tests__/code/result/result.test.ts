import { Int32 } from '../../../../../common/ys'
import { getVoid, Result, resultValue, resultError } from '../../../code/result/result'
import { toPromise } from '../../../code/utils/result-utils'

describe(Result, () => {
  it("should return true on 'isError' if error is set", () => {
    const error = new Error('SAMPLE ERROR')
    const result = new Result(null, error)
    expect(result.isError()).toBeTruthy()
  })
  it("should return false on 'isError' if error is not set", () => {
    const result = new Result(null, null)
    expect(result.isError()).toBeFalsy()
  })
  it("should return false on 'isValue' if error is set", () => {
    const error = new Error('SAMPLE ERROR')
    const result = new Result(null, error)
    expect(result.isValue()).toBeFalsy()
  })
  it("should return true on 'isValue' if error is not set", () => {
    const result = new Result(null, null)
    expect(result.isValue()).toBeTruthy()
  })
  it("should return value if 'getValue' is invoked and error is not set", () => {
    const value = 'SAMPLE VALUE'
    const result = new Result(value, null)
    expect(result.getValue()).toStrictEqual(value)
  })
  it("should return error if 'getError' is invoked and error set", () => {
    const error = new Error('SAMPLE ERROR')
    const result = new Result(null, error)
    expect(result.getError()).toStrictEqual(error)
  })
  it("should return null if 'getValue' is invoked and value is null", () => {
    const result = new Result(null, null)
    expect(result.getValue()).toBeNull()
  })
  it('withValue: should run function over value if not error', () => {
    const result = new Result('SAMPLE', null)
    const remapped = result.withValue((item) => item.toLowerCase())
    expect(remapped).toStrictEqual('sample')
  })
  it('withValue: should return null and not run a function over value if error', () => {
    const result = new Result('SAMPLE', new Error('ERROR'))
    const f = jest.fn((item: string) => item.toLowerCase())
    const remapped = result.withValue(f)
    expect(f).not.toHaveBeenCalled()
    expect(remapped).toBeNull()
  })
  it('map: should run function over value if not error', () => {
    const result = new Result('SAMPLE', null)
    const remapped = result.map((item) => item.toLowerCase())
    expect(remapped.getValue()).toStrictEqual('sample')
  })
  it('map: should return error and not run a function over value if error', () => {
    const result = new Result('SAMPLE', new Error('ERROR'))
    const f = jest.fn((item: string) => item.toLowerCase())
    const remapped = result.map(f)
    expect(f).not.toHaveBeenCalled()
    expect(remapped.getError().message).toBe('ERROR')
  })
  it('map: should convert to promise (SUCCESS)', (done) => {
    const result = new Result('SAMPLE', null)
    expect.assertions(1)
    toPromise(result).then((value) => {
      expect(value).toBe('SAMPLE')
      done()
    })
  })
  it('map: should convert to promise (FAILURE)', (done) => {
    const result = new Result(null, new Error('ERROR'))
    expect.assertions(1)
    toPromise(result).failed((error) => {
      expect(error.message).toBe('ERROR')
      done()
    })
  })
  it('map: should convert to nullable (SUCCESS)', () => {
    const result = new Result<Int32>(1, null)
    expect(result.asNullable()).toBe(1)
  })
  it('map: should convert to nullable (FAILURE)', () => {
    const result = new Result<Int32>(null, new Error('BAD'))
    expect(result.asNullable()).toBeNull()
  })
  it('flatMap: should return value', () => {
    const result = new Result<Int32>(100, null).flatMap((value) => new Result(String(value), null))
    expect(result.getValue()).toBe('100')
  })
  it('flatMap: should return error', () => {
    const result = new Result<Int32>(100, null).flatMap(() => new Result(null, new Error('ERROR')))
    expect(result.getError().message).toBe('ERROR')
  })
  it('flatMap: should propagate original error', () => {
    const result = new Result<Int32>(null, new Error('ERROR')).flatMap((value) => new Result(String(value), null))
    expect(result.getError().message).toBe('ERROR')
  })
  it('resultValue: should return value', () => {
    expect(resultValue('100')).toStrictEqual(new Result('100', null))
  })
  it('resultError: should return error', () => {
    expect(resultError(new Error('ERROR'))).toStrictEqual(new Result(null, new Error('ERROR')))
  })
  it('tryGetValue: should return value', () => {
    expect(resultValue('value').tryGetValue()).toStrictEqual('value')
  })
  it('tryGetValue: should throw error', () => {
    expect(() => resultError(new Error('ERROR')).tryGetValue()).toThrowError('ERROR')
  })
})

test('getVoid', () => {
  const v = getVoid()
  expect(v).toBeUndefined()
})
