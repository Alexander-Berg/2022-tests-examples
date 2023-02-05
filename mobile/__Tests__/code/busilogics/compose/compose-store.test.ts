import { resolve } from '../../../../../../common/xpromise-support'
import { Int32, int64, Nullable } from '../../../../../../common/ys'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../../common/code/result/result'
import { MockHighPrecisionTimer, MockStorage } from '../../../../../xmail/__tests__/__helpers__/mock-patches'
import { makeMessagesSettings } from '../../../../../xmail/__tests__/__helpers__/models'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../../../xmail/__tests__/__helpers__/test-id-support'
import { ID } from '../../../../../xmail/../mapi/code/api/common/id'
import { MessageBodyDescriptor } from '../../../../../xmail/../mapi/code/api/entities/body/message-body'

import { Messages } from '../../../../../xmail/code/busilogics/messages/messages'
import { Models } from '../../../../../xmail/code/models'
import { Registry } from '../../../../../xmail/code/registry'
import { ServiceLocatorItems } from '../../../../../xmail/code/utils/service-locator'

import { ComposeStore } from '../../../../code/busilogics/compose/compose-store'
import { NoOpMessageBodyLoaderPartial } from '../../../../code/busilogics/compose/message-body-loader-partial'
import { IConcurrentHashMap } from '../../../../code/service/concurrent-hash-map'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'

describe(ComposeStore, () => {
  beforeAll(() => {
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
  })
  afterAll(() => Models.drop())
  it('should be creatable', () => {
    jest.mock('../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        get: jest.fn().mockReturnValue(''),
        put: jest.fn().mockReturnValue(''),
        remove: jest.fn().mockReturnValue(true),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new ComposeStore(messages, partialBodyLoader)
    expect(composeStore).not.toBeNull()
  })
  it('should actually load message body from loadBody', (done) => {
    jest.mock('../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        get: jest.fn().mockReturnValue(null),
        put: jest.fn().mockReturnValue(''),
        remove: jest.fn().mockReturnValue(true),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    partialBodyLoader.getMessagesBodies = jest.fn().mockReturnValue(resolve(new Map<string, Nullable<string>>()))
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new ComposeStore(messages, partialBodyLoader)
    composeStore.loadBody(int64(111)).then((_) => {
      expect(partialBodyLoader.getMessagesBodies).toBeCalledWith([new MessageBodyDescriptor(int64(111), null, null)])
      done()
    })
  })
  it('should return cached value for mid if body is in cache', (done) => {
    jest.mock('../../../../code/registry')
    const mockGetFromConcurrentMap = jest.fn().mockReturnValue('some earlier cached body...')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        get: mockGetFromConcurrentMap,
        put: jest.fn().mockReturnValue(''),
        remove: jest.fn().mockReturnValue(true),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new ComposeStore(messages, partialBodyLoader)
    composeStore.loadBody(int64(111)).then((body) => {
      expect(mockGetFromConcurrentMap).toBeCalledWith('111')
      expect(body).toBe('some earlier cached body...')
      done()
    })
  })
  it('should remove message with mid from internal map when removeFromPendingSaveQueue called', () => {
    jest.mock('../../../../code/registry')
    const mockRemoveFromConcurrentMap = jest.fn().mockReturnValue(true)
    const mockGetFromConcurrentMap = jest.fn().mockReturnValue('')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        get: mockGetFromConcurrentMap,
        put: jest.fn().mockReturnValue(''),
        remove: mockRemoveFromConcurrentMap,
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new ComposeStore(messages, partialBodyLoader)
    const oldMid = int64(1234)
    composeStore.removeFromPendingSaveQueue(oldMid)
    expect(mockRemoveFromConcurrentMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
  })
  it('should immediately return from updateMid if no source mid in cache', (done) => {
    jest.mock('../../../../code/registry')
    const mockGetFromConcurrentHashMap = jest.fn().mockReturnValue(null as Nullable<string>)
    const mockPutToConcurrentHashMap = jest.fn().mockReturnValue('')
    const mockRemoveFromConcurrentHashMap = jest.fn().mockReturnValue(false)
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockImplementation((serviceLocatorItem) => {
        if (serviceLocatorItem === ServiceLocatorItems.handler) {
          return {
            destroy: jest.fn().mockReturnValue(getVoid()),
            hasMessages: jest.fn().mockReturnValue(false),
            post: jest.fn().mockReturnValue(resolve(getVoid())),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
          return {
            get: mockGetFromConcurrentHashMap,
            put: jest.fn().mockReturnValue(''),
            remove: jest.fn().mockReturnValue(true),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
          return {
            executeInLock: jest.fn().mockReturnValue(resolve(getVoid())),
          }
        }
        return null
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new TestComposeStore(messages, partialBodyLoader)
    const mockPostBodyUpdate = jest.spyOn(composeStore, 'postBodyUpdate')
    const mockPostBodyUpdateInternal = jest.spyOn(composeStore, 'postBodyUpdateInternal')
    const newMid: ID = int64(2222)
    const oldMid: ID = int64(1111)
    composeStore.updateMid(newMid, oldMid).then((_) => {
      expect(mockGetFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPutToConcurrentHashMap).not.toBeCalled()
      expect(mockRemoveFromConcurrentHashMap).not.toBeCalled()
      expect(mockPostBodyUpdate).not.toBeCalled()
      expect(mockPostBodyUpdateInternal).not.toBeCalled()
      done()
    })
  })
  it('should update mid and update body in messages', (done) => {
    jest.mock('../../../../code/registry')
    const storedBody = 'some stored body'
    const mockGetFromConcurrentHashMap = jest.fn().mockReturnValue(storedBody)
    const mockPutToConcurrentHashMap = jest.fn().mockReturnValue('')
    const mockRemoveFromConcurrentHashMap = jest.fn().mockReturnValue(false)
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockImplementation((serviceLocatorItem) => {
        if (serviceLocatorItem === ServiceLocatorItems.handler) {
          return {
            destroy: jest.fn().mockReturnValue(getVoid()),
            hasMessages: jest.fn().mockReturnValue(false),
            post: jest.fn().mockImplementation((toPost) => {
              toPost()
              return resolve(getVoid())
            }),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
          return {
            get: mockGetFromConcurrentHashMap,
            put: jest.fn().mockReturnValue(''),
            remove: mockRemoveFromConcurrentHashMap,
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
          return {
            executeInLock: jest.fn().mockImplementation((toWrap) => {
              toWrap()
              return resolve(getVoid())
            }),
          }
        }
        return null
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    messages.updateBody = jest.fn().mockReturnValue(resolve(getVoid()))
    const composeStore = new TestComposeStore(messages, partialBodyLoader)
    const mockPostBodyUpdate = jest.spyOn(composeStore, 'postBodyUpdate')
    const mockPostBodyUpdateInternal = jest.spyOn(composeStore, 'postBodyUpdateInternal')
    const newMid: ID = int64(2222)
    const oldMid: ID = int64(1111)
    composeStore.updateMid(newMid, oldMid).then((_) => {
      expect(mockGetFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPutToConcurrentHashMap).not.toBeCalled()
      expect(mockRemoveFromConcurrentHashMap).toBeCalled()
      expect(mockPostBodyUpdate).toBeCalledWith(newMid, storedBody)
      expect(mockPostBodyUpdateInternal).toBeCalledWith(newMid, storedBody)
      expect(messages.updateBody).toBeCalledWith(newMid, storedBody)
      done()
    })
  })
  it('should update internal map with bodies cache and return from postBodyUpdateInternal without updating message body and without invalidating cache in MessageBodyLoaderPartial', (done) => {
    jest.mock('../../../../code/registry')
    const testMap = new TestConcurrentMap()
    const mockGetFromConcurrentHashMap = jest.spyOn(testMap, 'get')
    const mockPutToConcurrentHashMap = jest.spyOn(testMap, 'put')
    const mockRemoveFromConcurrentHashMap = jest.spyOn(testMap, 'remove')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockImplementation((serviceLocatorItem) => {
        if (serviceLocatorItem === ServiceLocatorItems.handler) {
          return {
            destroy: jest.fn().mockReturnValue(getVoid()),
            hasMessages: jest.fn().mockReturnValue(false),
            post: jest.fn().mockImplementation((toPost) => {
              toPost()
              return resolve(getVoid())
            }),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
          return testMap
        } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
          return {
            executeInLock: jest.fn().mockImplementation((toWrap) => {
              toWrap()
              return resolve(getVoid())
            }),
          }
        }
        return null
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const mockInvalidateBodyCache = jest.spyOn(partialBodyLoader, 'invalidateBodyCache')
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    messages.updateBody = jest.fn().mockReturnValue(resolve(getVoid()))
    const composeStore = new TestComposeStore(messages, partialBodyLoader)
    const mockPostBodyUpdate = jest.spyOn(composeStore, 'postBodyUpdate')
    const mockPostBodyUpdateInternal = jest.spyOn(composeStore, 'postBodyUpdateInternal')
    const newMid: ID = int64(2222)
    const oldMid: ID = int64(1111)
    composeStore.updateMid(newMid, oldMid).then((_) => {
      expect(mockGetFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPutToConcurrentHashMap).toBeCalledWith(
        Models.instance().idSupport.toDBValue(newMid),
        'some stored body 1',
      )
      expect(mockRemoveFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPostBodyUpdate).toBeCalledWith(newMid, 'some stored body 1')
      expect(mockPostBodyUpdateInternal).toBeCalledWith(newMid, 'some stored body 1')
      expect(mockInvalidateBodyCache).not.toBeCalled()
      expect(messages.updateBody).not.toBeCalled()
      done()
    })
  })
  it('should not create internal handler if already created', (done) => {
    jest.mock('../../../../code/registry')
    const testMap = new TestConcurrentMap()
    const mockGetFromConcurrentHashMap = jest.spyOn(testMap, 'get')
    const mockPutToConcurrentHashMap = jest.spyOn(testMap, 'put')
    const mockRemoveFromConcurrentHashMap = jest.spyOn(testMap, 'remove')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockImplementation((serviceLocatorItem) => {
        if (serviceLocatorItem === ServiceLocatorItems.handler) {
          return {
            destroy: jest.fn().mockReturnValue(getVoid()),
            hasMessages: jest.fn().mockReturnValue(false),
            post: jest.fn().mockImplementation((toPost) => {
              toPost()
              return resolve(getVoid())
            }),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
          return testMap
        } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
          return {
            executeInLock: jest.fn().mockImplementation((toWrap) => {
              toWrap()
              return resolve(getVoid())
            }),
          }
        }
        return null
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const mockInvalidateBodyCache = jest.spyOn(partialBodyLoader, 'invalidateBodyCache')
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    messages.updateBody = jest.fn().mockReturnValue(resolve(getVoid()))
    const composeStore = new TestComposeStore(messages, partialBodyLoader)
    const mockPostBodyUpdate = jest.spyOn(composeStore, 'postBodyUpdate')
    const mockPostBodyUpdateInternal = jest.spyOn(composeStore, 'postBodyUpdateInternal')
    const newMid: ID = int64(2222)
    const oldMid: ID = int64(1111)
    composeStore.initHandler()
    composeStore.updateMid(newMid, oldMid).then((_) => {
      expect(mockGetFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPutToConcurrentHashMap).toBeCalledWith(
        Models.instance().idSupport.toDBValue(newMid),
        'some stored body 1',
      )
      expect(mockRemoveFromConcurrentHashMap).toBeCalledWith(Models.instance().idSupport.toDBValue(oldMid))
      expect(mockPostBodyUpdate).toBeCalledWith(newMid, 'some stored body 1')
      expect(mockPostBodyUpdateInternal).toBeCalledWith(newMid, 'some stored body 1')
      expect(mockInvalidateBodyCache).not.toBeCalled()
      expect(messages.updateBody).not.toBeCalled()
      done()
    })
  })
  it('should not create internal handler if already created', () => {
    jest.mock('../../../../code/registry')
    const testMap = new TestConcurrentMap()
    const mockDestroyHandler = jest.fn().mockReturnValue(getVoid())
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockImplementation((serviceLocatorItem) => {
        if (serviceLocatorItem === ServiceLocatorItems.handler) {
          return {
            destroy: mockDestroyHandler,
            hasMessages: jest.fn().mockReturnValue(false),
            post: jest.fn().mockImplementation((toPost) => {
              toPost()
              return resolve(getVoid())
            }),
          }
        } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
          return testMap
        } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
          return {
            executeInLock: jest.fn().mockImplementation((toWrap) => {
              toWrap()
              return resolve(getVoid())
            }),
          }
        }
        return null
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const testIDSupport = new TestIDSupport()
    const partialBodyLoader = new NoOpMessageBodyLoaderPartial()
    const storage = MockStorage()
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    const composeStore = new TestComposeStore(messages, partialBodyLoader)
    composeStore.destroyAndNullifyMessageLoop()
    expect(mockDestroyHandler).not.toBeCalled()
  })
  it('should create correct string key from descriptor with translation info', () => {
    const descriptor = new MessageBodyDescriptor(int64(4321), 'en', 'ru')
    expect(ComposeStore.toStringKey(descriptor)).toEqual('4321^^en^^ru')
  })
  it('should create correct string key from descriptor without translation info', () => {
    const descriptor = new MessageBodyDescriptor(int64(4321), null, null)
    expect(ComposeStore.toStringKey(descriptor)).toEqual('4321')
  })
  class TestConcurrentMap implements IConcurrentHashMap {
    private i: Int32 = 0
    public put(key: any, value: any): Nullable<any> {
      return ''
    }

    public get(key: any): Nullable<any> {
      this.i += 1
      return `some stored body ${this.i}`
    }

    public remove(key: any): Nullable<any> {
      return ''
    }
  }
  class TestComposeStore extends ComposeStore {
    public postBodyUpdate(mid: ID, body: string): XPromise<void> {
      return super.postBodyUpdate(mid, body)
    }

    public postBodyUpdateInternal(mid: ID, body: string): void {
      return super.postBodyUpdateInternal(mid, body)
    }

    public initHandler(): void {
      super.initHandler()
    }

    public destroyAndNullifyMessageLoop(): void {
      super.destroyAndNullifyMessageLoop()
    }
  }
})
