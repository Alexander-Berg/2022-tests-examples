import { all, promise, race, reject, resolve, take } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { XPromise } from '../../../code/promise/xpromise'
import { getVoid } from '../../../code/result/result'
import { executeSequentially } from '../../../code/utils/xpromise-utils'

describe(XPromise, () => {
  it('should be creatable with promise function (resolved)', async () => {
    expect.assertions(1)
    const p = promise<number>((ok, err) => ok(10))
    await take(p).then((v) => expect(v).toBe(10))
  })
  it('should be creatable with promise function (rejected)', () => {
    expect.assertions(1)
    const p = promise<number>((ok, err) => err(new YSError('ERROR')))
    return take(p).catch((e) => expect(e).toEqual(new YSError('ERROR')))
  })
  it('should be creatable with resolve function', async () => {
    expect.assertions(1)
    const p = resolve(10)
    await take(p).then((v) => expect(v).toBe(10))
  })
  it('should be creatable with reject function', () => {
    expect.assertions(1)
    const p = reject(new YSError('ERROR'))
    return take(p).catch((e) => expect(e).toEqual(new YSError('ERROR')))
  })
  it('should be thennable', async () => {
    expect.assertions(1)
    const p = resolve(10)
      .then((item) => (item !== null ? item.toString() : 'unknown'))
      .then((item) => (item !== null ? item.startsWith('1') : false))
    await take(p).then((v) => expect(v).toBe(true))
  })
  it('should support multiple thens on one promise', async () => {
    expect.assertions(3)
    const p = resolve(10)
    p.then((item) => {
      expect(item).toBe(10)
      return '10'
    })
    p.then((item) => {
      expect(item).toBe(10)
      return true
    })
    await take(p).then((item) => expect(item).toBe(10))
  })
  it('should support multiple catches on one promise', () => {
    expect.assertions(3)
    const p = reject<number>(new YSError('ERROR'))
    p.catch((e) => {
      expect(e).toEqual(new YSError('ERROR'))
      return 10
    })
    p.catch((e) => {
      expect(e).toEqual(new YSError('ERROR'))
      return 20
    })
    return take(p).catch((e) => expect(e).toEqual(new YSError('ERROR')))
  })
  it('should be able to map other promises with flatThen', async () => {
    expect.assertions(1)
    const p = resolve(10)
      .flatThen((item) => resolve(item !== null ? item.toString() : 'unknown'))
      .flatThen((item) => resolve(item !== null && item.startsWith('1')))
    await take(p).then((v) => expect(v).toBe(true))
  })
  it('should be able to catch with catch and return new value', () => {
    expect.assertions(2)
    return reject<number>(new YSError('ERROR'))
      .catch((e) => {
        expect(e).toEqual(new YSError('ERROR'))
        return 10
      })
      .then((val) => {
        expect(val).toBe(10)
        return val
      })
  })
  it('should be able to catch inner rejections with flatCatch', (done) => {
    expect.assertions(2)
    reject(new YSError('ERROR 1'))
      .flatCatch((e) => {
        expect(e).toEqual(new YSError('ERROR 1'))
        return reject(new YSError('ERROR 2'))
      })
      .failed((e) => {
        expect(e).toEqual(new YSError('ERROR 2'))
        done()
      })
  })
  it('should be able to catch inner rejections with flatCatch and restore', () => {
    expect.assertions(2)
    return reject<string>(new YSError('ERROR 1'))
      .flatCatch((e) => {
        expect(e).toEqual(new YSError('ERROR 1'))
        return resolve('ok')
      })
      .then((res) => {
        expect(res).toBe('ok')
      })
  })
  it('should be able to catch rejections with failed', (done) => {
    expect.assertions(2)
    reject(new YSError('ERROR 1'))
      .flatCatch((e) => {
        expect(e).toEqual(new YSError('ERROR 1'))
        return reject(new YSError('ERROR 2'))
      })
      .failed((e) => {
        expect(e).toEqual(new YSError('ERROR 2'))
        done()
      })
  })
  it('should be able attach callbacks to both resolve and reject with both (resolve)', async () => {
    expect.assertions(1)
    await resolve(10).both(
      (item) => expect(item).toBe(10),
      (err) => fail('should not throw'),
    )
  })
  it('should be able attach callbacks to both resolve and reject with flatBoth (resolve)', () => {
    expect.assertions(1)
    return resolve(10)
      .flatBoth(
        (item) => resolve(item! + 10),
        (err) => reject(new YSError('ERROR')),
      )
      .both(
        (res) => expect(res).toBe(20),
        (err) => fail('should not be called'),
      )
  })
  it('should be able attach callbacks to both resolve and reject with both (reject)', () => {
    expect.assertions(1)
    return reject(new YSError('ERROR')).both(
      () => fail('should not be called'),
      (err) => expect(err).toEqual(new YSError('ERROR')),
    )
  })
  it('should be able attach callbacks to both resolve and reject with flatBoth (reject)', () => {
    expect.assertions(1)
    return reject<string>(new YSError('ERROR'))
      .flatBoth(
        (_) => {
          fail('should not throw')
          return resolve('resolved')
        },
        (_) => resolve('recovered'),
      )
      .both(
        (msg) => expect(msg).toBe('recovered'),
        (_) => fail('should not throw'),
      )
  })
  it('should be able to respond regardless of the result with finally (rejected)', () => {
    expect.assertions(2)
    return reject(new YSError('ERROR'))
      .finally(() => expect(true).toBe(true))
      .failed((e) => {
        expect(e).toEqual(new YSError('ERROR'))
      })
  })
  it('should be able to respond regardless of the result with finally (resolved)', () => {
    expect.assertions(2)
    return resolve(10)
      .then((v) => expect(v).toBe(10))
      .finally(() => expect(true).toBe(true))
  })
  it("should be able to make a promise dependent on a sequence of promises' completion (resolved)", async () => {
    expect.assertions(1)
    const promises = [resolve(10), resolve(20), resolve(30)]
    const p = all(promises).flatThen((items) =>
      resolve(items !== null ? items.reduce((acc, item) => acc! + item!, 0)!.toString() : ''),
    )
    await p.then((res) => expect(res).toBe('60'))
  })
  it("should be able to make a promise dependent on a sequence of promises' completion (rejected)", (done) => {
    expect.assertions(1)
    const promises = [resolve(10), reject<number>(new YSError('ERROR')), resolve(30)]
    all(promises)
      .then(() => fail('should not be called'))
      .failed((e) => {
        expect(e).toEqual(new YSError('ERROR'))
        done()
      })
  })
  it("should be able to make a promise pick the one completed from a sequence of promises' (resolved)", async () => {
    expect.assertions(1)
    const promises = [resolve(10), reject<number>(new YSError('ERROR 1')), reject<number>(new YSError('ERROR 2'))]
    const p = race(promises).flatThen((item) => resolve(item > 0 ? item + 5 : 0))
    await p.then((res) => expect(res).toBe(15))
  })
  it("should be able to make a promise pick the one completed from a sequence of promises' (rejected)", (done) => {
    expect.assertions(1)
    const promises = [
      reject<number>(new YSError('ERROR 1')),
      reject<number>(new YSError('ERROR 2')),
      reject<number>(new YSError('ERROR 3')),
    ]
    race(promises)
      .then(() => fail('should not be called'))
      .failed((e) => {
        expect(e).toEqual(new YSError('ERROR 1'))
        done()
      })
  })

  describe('with timers', () => {
    beforeEach(jest.useFakeTimers)
    afterEach(jest.clearAllTimers)
    afterEach(jest.useRealTimers)

    it('should reject race with the first rejection', (done) => {
      jest.useFakeTimers()
      expect.assertions(1)
      const promises = [
        promise((ok, err) => setTimeout(() => ok(10), 300)),
        promise<number>((ok, err) => setTimeout(() => err(new YSError('ERROR')), 200)),
        promise((ok, err) => setTimeout(() => ok(20), 400)),
      ]
      jest.advanceTimersByTime(250)
      race(promises)
        .then(() => fail('should not be called'))
        .failed((e) => {
          expect(e).toEqual(new YSError('ERROR'))
          done()
        })
    })

    it('should be creatable with promise function (resolved)', async () => {
      expect.assertions(1)
      const p = promise((ok, err) => {
        setTimeout(() => {
          ok(10)
        }, 100)
      })
      jest.runAllTimers()
      await take(p).then((item) => expect(item).toBe(10))
    })
    it('should be creatable with promise function (rejected)', () => {
      expect.assertions(1)
      const p = promise((ok, err) => {
        setTimeout(() => {
          err(new YSError('ERROR'))
        }, 100)
      })
      jest.runAllTimers()
      return take(p).catch((e) => expect(e).toEqual(new YSError('ERROR')))
    })
  })

  describe('executeSequentially', () => {
    it('should return empty array for empty input', async () => {
      const p = executeSequentially([])
      await take(p).then((item) => expect(item).toStrictEqual([]))
    })
    it('should execute promises in sequential order', async () => {
      function resolveAfter(timeout: number): XPromise<void> {
        return promise((ok, err) => {
          setTimeout(() => {
            ok(getVoid())
          }, timeout)
        })
      }

      const executionOrderTokens: any[] = []

      const p = executeSequentially([
        () =>
          resolveAfter(150)
            .then((_) => executionOrderTokens.push(1))
            .then(() => 'a'),
        () =>
          resolveAfter(100)
            .then((_) => executionOrderTokens.push(2))
            .then(() => 'b'),
        () =>
          resolveAfter(50)
            .then((_) => executionOrderTokens.push(3))
            .then(() => 'c'),
      ])

      const result = await take(p)
      expect(result).toStrictEqual(['a', 'b', 'c'])
      expect(executionOrderTokens).toStrictEqual([1, 2, 3])
    })
  })
})
