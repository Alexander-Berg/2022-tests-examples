import { Nullable, undefinedToNull } from '../../../../../common/ys'
import { Registry } from '../../../../xmail/code/registry'
import { ConcurrentHashMap, IConcurrentHashMap } from '../../../code/service/concurrent-hash-map'

class MockConcurrentMap implements IConcurrentHashMap {
  public items = new Map<any, any>()

  public put(key: any, value: any): Nullable<any> {
    const previousValue: any = undefinedToNull(this.items.get(key))
    this.items.set(key, value)
    return previousValue
  }

  public get(key: any): Nullable<any> {
    return undefinedToNull(this.items.get(key))
  }

  public remove(key: any): any {
    const prevValue = undefinedToNull(this.items.get(key))
    this.items.delete(key)
    return prevValue
  }
}

describe(ConcurrentHashMap, () => {
  it('should be creatable', () => {
    jest.mock('../../../code/registry')
    const mockInternalMap = new MockConcurrentMap()
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue(mockInternalMap),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const concurrentHashMap = new ConcurrentHashMap<string>()
    expect(concurrentHashMap).not.toBeNull()
  })
  describe('put', () => {
    it('should put value under the key', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      concurrentHashMap.put('someKey', 'someValue')
      expect(mockInternalMap.get('someKey')).toBe('someValue')
    })
    it('should override value under existing key and return previous stored value', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      const shouldBeNullValue = concurrentHashMap.put('someKey', 'someValue')
      const shouldBeNotNullValue = concurrentHashMap.put('someKey', 'someNewValue')
      expect(mockInternalMap.get('someKey')).toBe('someNewValue')
      expect(shouldBeNullValue).toBeNull()
      expect(shouldBeNotNullValue).toBe('someValue')
    })
  })
  describe('get', () => {
    it('should return value stored by key if exists', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      concurrentHashMap.put('someKey', 'someValue')
      expect(concurrentHashMap.get('someKey')).toBe('someValue')
    })
    it('should return null if there is no value for specified key', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      expect(concurrentHashMap.get('someKey')).toBeNull()
    })
  })
  describe('remove', () => {
    it('should remove value by key if in the map', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      concurrentHashMap.put('someKey', 'someValue')
      const shouldNotBeNull = concurrentHashMap.remove('someKey')
      expect(mockInternalMap.get('someKey')).toBeNull()
      expect(shouldNotBeNull).toBe('someValue')
    })
    it('should not remove value by key if value is not in the map', () => {
      jest.mock('../../../code/registry')
      const mockInternalMap = new MockConcurrentMap()
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue(mockInternalMap),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const concurrentHashMap = new ConcurrentHashMap<string>()
      concurrentHashMap.put('someKey', 'someValue')
      const shouldBeNull = concurrentHashMap.remove('someOtherKey')
      expect(mockInternalMap.get('someKey')).toBe('someValue')
      expect(shouldBeNull).toBeNull()
    })
  })
})
