import {
  MockFileSystem,
  MockJSONSerializer,
  mockLogger,
  MockNetwork,
} from '../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../common/code/result/result'
import { Models } from '../../code/models'
import { Registry } from '../../code/registry'
import { IHandler } from '../../code/service/message-handler'
import { ServiceLocatorItems } from '../../code/utils/service-locator'
import { MockHighPrecisionTimer, MockStorage } from '../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../__helpers__/test-id-support'
import { createMockInstance } from '../../../common/__tests__/__helpers__/utils'
import { AttachmentSizes } from '../../code/busilogics/draft/attachment-sizes'

describe(Models, () => {
  afterEach(() => (Models as any).drop())
  it('should setup instance, dropping previous one', () => {
    const marker = {} as any
    // tslint:disable-next-line: no-string-literal
    ;(Models as any)._instance = marker
    expect(Models.instance()).toBeDefined()
    const instance = Models.setupInstance(
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
    expect(instance).not.toBe(marker)
    expect(Models.instance()).toBe(instance)
  })
  it('should throw if instance is requested but not setup', () => {
    expect(() => Models.instance()).toThrowError('Models instance should be `setup` before use.')
  })
  it('should create Folders (only once)', () => {
    const instance = Models.setupInstance(
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
    const folders = instance.folders()
    expect(folders).not.toBeNull()
    expect(instance.folders()).toBe(folders)
  })
  it('should create Labels (only once)', () => {
    const instance = Models.setupInstance(
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
    const labels = instance.labels()
    expect(labels).not.toBeNull()
    expect(instance.labels()).toBe(labels)
  })
  it('should create Messages (only once)', () => {
    const instance = Models.setupInstance(
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
    const messages = instance.messages()
    expect(messages).not.toBeNull()
    expect(instance.messages()).toBe(messages)
  })
  it('should create Threads (only once)', () => {
    const instance = Models.setupInstance(
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
    const threads = instance.threads()
    expect(threads).not.toBeNull()
    expect(instance.threads()).toBe(threads)
  })
  it('should create Cleanup (only once)', () => {
    const instance = Models.setupInstance(
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
    const cleanup = instance.cleanup()
    expect(cleanup).not.toBeNull()
    expect(instance.cleanup()).toBe(cleanup)
  })
  it('should create Settings (only once)', () => {
    const instance = Models.setupInstance(
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
    const settings = instance.settings()
    expect(settings).not.toBeNull()
    expect(instance.settings()).toBe(settings)
  })
  it('should create Attachments Manager (only once)', () => {
    const instance = Models.setupInstance(
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
    const attachmentsManager = instance.attachmentsManager()
    expect(attachmentsManager).not.toBeNull()
    expect(instance.attachmentsManager()).toBe(attachmentsManager)
  })
  it('should create Search (only once)', () => {
    const instance = Models.setupInstance(
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
    const search = instance.search()
    expect(search).not.toBeNull()
    expect(instance.search()).toBe(search)
  })
  it('should create Body Store (only once)', () => {
    const instance = Models.setupInstance(
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
    const bodyStore = instance.bodyStore()
    expect(bodyStore).not.toBeNull()
    expect(instance.bodyStore()).toBe(bodyStore)
  })
  it('should create Recipients (only once)', () => {
    const instance = Models.setupInstance(
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
    const recipients = instance.recipients()
    expect(recipients).not.toBeNull()
    expect(instance.recipients()).toBe(recipients)
  })
  it('should create Compose Store (only once)', () => {
    const fakeHandler: IHandler = {
      post: jest.fn(),
      destroy: jest.fn().mockReturnValue(getVoid()),
      hasMessages: jest.fn().mockReturnValue(false),
    }
    Registry.registerServiceLocatorItems(new Map([[ServiceLocatorItems.handler, () => fakeHandler]]))
    jest.mock('../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({}),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    const instance = Models.setupInstance(
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
    const composeStore = instance.composeStore()
    expect(composeStore).not.toBeNull()
    expect(instance.composeStore()).toBe(composeStore)
  })
  it('should create drafts (only once)', () => {
    const fakeHandler: IHandler = {
      post: jest.fn(),
      destroy: jest.fn().mockReturnValue(getVoid()),
      hasMessages: jest.fn().mockReturnValue(false),
    }
    Registry.registerServiceLocatorItems(new Map([[ServiceLocatorItems.handler, () => fakeHandler]]))
    mockLogger()
    const instance = Models.setupInstance(
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
    const drafts = instance.drafts()
    expect(drafts).not.toBeNull()
    expect(instance.drafts()).toBe(drafts)
  })
  it('should create draft attachments (only once)', () => {
    const fakeHandler: IHandler = {
      post: jest.fn(),
      destroy: jest.fn().mockReturnValue(getVoid()),
      hasMessages: jest.fn().mockReturnValue(false),
    }
    Registry.registerServiceLocatorItems(new Map([[ServiceLocatorItems.handler, () => fakeHandler]]))
    mockLogger()
    const instance = Models.setupInstance(
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
    const draftAttachments = instance.draftAttachments()
    expect(draftAttachments).not.toBeNull()
    expect(instance.draftAttachments()).toBe(draftAttachments)
  })
})
