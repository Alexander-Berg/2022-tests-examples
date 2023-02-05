import { Int32, int32ToInt64 } from '../../../../common/ys'
import { MockFileSystem, MockJSONSerializer, MockNetwork } from '../../../common/__tests__/__helpers__/mock-patches'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { ID } from '../../../mapi/code/api/common/id'
import { Network } from '../../../common/code/network/network'
import { IDSupport } from '../../code/api/common/id-support'
import { FileSystem } from '../../../common/code/file-system/file-system'
import { HighPrecisionTimer } from '../../code/api/logging/perf/high-precision-timer'
import { SharedPreferences } from '../../../common/code/shared-prefs/shared-preferences'
import { queryValuesFromIds } from '../../code/api/storage/query-helpers'
import { Storage } from '../../code/api/storage/storage'
import { AttachmentsManager } from '../../code/busilogics/attachments/attachments'
import { MessageBodyStore } from '../../code/busilogics/body/message-body-store'
import { DraftAttachments } from '../../code/busilogics/draft/draft-attachments'
import { Folders } from '../../code/busilogics/folders/folders'
import { Labels } from '../../code/busilogics/labels/labels'
import { Messages, MessagesSettings } from '../../code/busilogics/messages/messages'
import { SearchModel } from '../../code/busilogics/search/search-model'
import { Cleanup } from '../../code/busilogics/sync/cleanup'
import { Threads } from '../../code/busilogics/threads/threads'
import { Models } from '../../code/models'
import { MockDraftAttachments, MockHighPrecisionTimer, MockStorage } from '../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from './test-id-support'
import { Drafts } from '../../code/busilogics/draft/drafts'

const testIDSupport = new TestIDSupport()

export function makeFolders(patch?: {
  storage?: Storage
  sharedPreferences?: SharedPreferences
  idSupport?: IDSupport
  timer?: HighPrecisionTimer
}): Folders {
  const storage = patch?.storage ?? MockStorage()
  const prefs = patch?.sharedPreferences ?? new MockSharedPreferences()
  const idSupport = patch?.idSupport ?? testIDSupport
  const timer = patch?.timer ?? MockHighPrecisionTimer()
  return new Folders(storage, prefs, idSupport, timer)
}

export function makeMessages(patch?: {
  network?: Network
  storage?: Storage
  serializer?: JSONSerializer
  idSupport?: IDSupport
  settings?: MessagesSettings
}): Messages {
  return new Messages(
    patch?.network ?? MockNetwork(),
    patch?.storage ?? MockStorage(),
    patch?.serializer ?? MockJSONSerializer(),
    patch?.idSupport ?? new TestIDSupport(),
    patch?.settings ?? makeMessagesSettings(),
  )
}

export function makeLabels(patch?: { storage?: Storage; idSupport?: IDSupport }): Labels {
  return new Labels(patch?.storage ?? MockStorage(), patch?.idSupport ?? testIDSupport)
}

export function makeCleanup(patch?: {
  storage?: Storage
  folders?: Folders
  threads?: Threads
  idSupport?: IDSupport
}): Cleanup {
  return new Cleanup(
    patch?.storage ?? MockStorage(),
    patch?.threads ?? makeThreads(patch),
    patch?.folders ?? makeFolders(patch),
    patch?.idSupport ?? testIDSupport,
  )
}

export function makeThreads(patch?: {
  network?: Network
  storage?: Storage
  folders?: Folders
  threads?: Threads
  idSupport?: IDSupport
}): Threads {
  return new Threads(
    patch?.network ?? MockNetwork(),
    patch?.storage ?? MockStorage(),
    patch?.idSupport ?? testIDSupport,
  )
}

export function makeSearch(patch?: { network?: Network; storage?: Storage; idSupport?: IDSupport }): SearchModel {
  return new SearchModel(
    patch?.storage ?? MockStorage(),
    patch?.network ?? MockNetwork(),
    patch?.idSupport ?? testIDSupport,
  )
}

export function idstr(value: Int32 | ID): string {
  switch (typeof value) {
    case 'bigint':
      return queryValuesFromIds([value], testIDSupport)
    case 'number':
      return idstr(int32ToInt64(value))
  }
}

export function makeBodies(patch?: { fs?: FileSystem; directory?: string }): MessageBodyStore {
  return new MessageBodyStore(patch?.fs ?? MockFileSystem(), patch?.directory ?? '/account/folder')
}

export function makeMessagesSettings(patch?: {
  storage?: Storage
  network?: Network
  idSupport?: IDSupport
  params?: Partial<MessagesSettings>
}): MessagesSettings {
  const threads = patch?.params?.threads || makeThreads(patch)
  const folders = patch?.params?.folders || makeFolders(patch)
  return new MessagesSettings(
    patch?.params?.attachmentsManager || makeAttachmentsManager(patch),
    folders,
    patch?.params?.labels ?? makeLabels(patch),
    threads,
    patch?.params?.search ?? makeSearch(patch),
    patch?.params?.cleanup ??
      makeCleanup({
        ...patch,
        folders,
        threads,
      }),
    patch?.params?.bodies ?? makeBodies(),
  )
}

export function makeAttachmentsManager(patch?: {
  storage?: Storage
  idSupport?: IDSupport
  patch?: Partial<AttachmentsManager>
}): AttachmentsManager {
  return (Object.assign(
    {},
    new AttachmentsManager(patch?.storage ?? MockStorage(), patch?.idSupport ?? testIDSupport),
    {
      clearAttaches: jest.fn(),
      insertAttaches: jest.fn(),
    },
    patch?.patch,
  ) as any) as AttachmentsManager
}

export function MockModels(
  patch?: Partial<Models>,
  models?: {
    folders?: Folders
    labels?: Labels
    messages?: Messages
    threads?: Threads
    cleanup?: Cleanup
    settings?: MessagesSettings
    attachments?: AttachmentsManager
    search?: SearchModel
    bodyStore?: MessageBodyStore
    drafts?: Drafts
    draftAttachments?: DraftAttachments
  },
): Models {
  const result: Models = Object.assign(
    Object.create(Models.prototype),
    {
      bodiesDirectory: 'bodies',
      network: MockNetwork(),
      storage: MockStorage(),
      serializer: MockJSONSerializer(),
      fileSystem: MockFileSystem(),
      idSupport: new TestIDSupport(),
      highPrecisionTimer: MockHighPrecisionTimer(),
      sharedPreferences: new MockSharedPreferences(),
    },
    patch as any,
  )
  const res = result as any
  res._folders = models?.folders ?? null
  res._labels = models?.labels ?? null
  res._messages = models?.messages ?? null
  res._threads = models?.threads ?? null
  res._cleanup = models?.cleanup ?? null
  res._settings = models?.settings ?? null
  res._attachments = models?.attachments ?? null
  res._search = models?.search ?? null
  res._bodyStore = models?.bodyStore ?? null
  res._drafts = models?.drafts ?? null
  res._draftAttachments = models?.draftAttachments ?? MockDraftAttachments()
  return result
}
