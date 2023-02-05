import { resolve } from '../../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { AttachmentsManager } from '../../../../../xmail/code/busilogics/attachments/attachments'
import { Models } from '../../../../../xmail/code/models'
import { Registry } from '../../../../../xmail/code/registry'
import {
  MockCursorWithArray,
  MockHighPrecisionTimer,
  MockStorage,
  MockWithinTransaction,
} from '../../../../../xmail/__tests__/__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../../../xmail/__tests__/__helpers__/test-id-support'
import { StorageStatement } from '../../../../code/api/storage/storage-statement'
import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'
import { DiskAttachBundle, DraftAttachEntry } from '../../../../code/busilogics/draft/draft-attach-entry'
import { DraftAttachments } from '../../../../code/busilogics/draft/draft-attachments'
import { Drafts } from '../../../../code/busilogics/draft/drafts'
import { IMimeTypeMap } from '../../../../code/service/mime-type-map'
import { ServiceLocatorItems } from '../../../../code/utils/service-locator'
import { rejected } from '../../../__helpers__/test-failure'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'

describe(DraftAttachments, () => {
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
    const testIDSupport = new TestIDSupport()
    const storage = MockStorage()
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    expect(draftAttachments.formNarodAttachParameter([])?.replace(/[\n\r]+/g, '')).toStrictEqual(
      '<html><head><meta charset="utf-8"/></head><body></body></html>',
    )
  })
  it('should prepare correct Narod attachments html', () => {
    const testIDSupport = new TestIDSupport()
    const storage = MockStorage()
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    expect(
      draftAttachments
        .formNarodAttachParameter([
          new DiskAttachBundle('my disk attach', 'https://url.com/id=123', int64(32768)),
          new DiskAttachBundle('your disk attach', 'https://url.com/id=456', int64(32768)),
        ])
        ?.replace(/[\n\r]+/g, ''),
    ).toStrictEqual(
      '<html><head><meta charset="utf-8"/></head><body><br><a class="narod-attachment" target="_blank" href="https://url.com/id=123">my disk attach (32768)</a><br><br><a class="narod-attachment" target="_blank" href="https://url.com/id=456">your disk attach (32768)</a><br></body></html>',
    )
  })
  it('should run correct query when getting uploaded attach drafts', (done) => {
    const testIDSupport = new TestIDSupport()
    const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
    const storage = MockStorage({
      runQuery: mockRunQuery,
    })
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    const mockDraftAttachFromFromCursor = jest.spyOn(draftAttachments, 'draftAttachEntryFromCursor')
    draftAttachments.getUploadedDraftAttaches(int64(123)).then((_) => {
      expect(mockRunQuery).toBeCalledWith('SELECT * FROM draft_attach WHERE did = 123 AND uploaded = 1;', [])
      expect(mockDraftAttachFromFromCursor).toBeCalled()
      done()
    })
  })
  it('should prepare correct Narod attachments html', () => {
    const testIDSupport = new TestIDSupport()
    const storage = MockStorage()
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    expect(
      draftAttachments
        .formNarodAttachParameter([
          new DiskAttachBundle('my disk attach', 'https://url.com/id=123', int64(32768)),
          new DiskAttachBundle('your disk attach', 'https://url.com/id=456', int64(32768)),
        ])
        ?.replace(/[\n\r]+/g, ''),
    ).toStrictEqual(
      '<html><head><meta charset="utf-8"/></head><body><br><a class="narod-attachment" target="_blank" href="https://url.com/id=123">my disk attach (32768)</a><br><br><a class="narod-attachment" target="_blank" href="https://url.com/id=456">your disk attach (32768)</a><br></body></html>',
    )
  })
  it('should run correct query and create array of DraftAttachEntry from cursor', (done) => {
    const testIDSupport = new TestIDSupport()
    const mockRunQuery = jest
      .fn()
      .mockReturnValue(
        resolve(
          MockCursorWithArray([
            [
              int64(111),
              int64(123),
              'https://someUrl.org/id=123',
              'someFileUri',
              'some display name',
              int64(16000),
              null,
              false,
              false,
              false,
              null,
            ],
          ]),
        ),
      )
    const storage = MockStorage({
      runQuery: mockRunQuery,
    })
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    const mockDraftAttachFromFromCursor = jest.spyOn(draftAttachments, 'draftAttachEntryFromCursor')
    draftAttachments.getUploadedDraftAttaches(int64(123)).then((draftAttachEntries) => {
      expect(mockRunQuery).toBeCalledWith('SELECT * FROM draft_attach WHERE did = 123 AND uploaded = 1;', [])
      expect(mockDraftAttachFromFromCursor).toBeCalled()
      expect(draftAttachEntries).toStrictEqual([
        new DraftAttachEntry(
          int64(111),
          int64(123),
          'https://someUrl.org/id=123',
          'someFileUri',
          'some display name',
          int64(16000),
          null,
          false,
          false,
          false,
          null,
        ),
      ])
      done()
    })
  })
  describe('draftAttachEntryFromCursor', () => {
    it('should create DraftAttachEntry from cursor', () => {
      const mockCursor = MockCursorWithArray([
        [
          int64(111),
          int64(123),
          null,
          'someFileUri',
          'some display name',
          int64(16000),
          null,
          false,
          false,
          false,
          null,
        ],
        [
          int64(111),
          int64(123),
          'https://someUrl.org/?id=123',
          'someOtherFileUri',
          'some other display name',
          int64(16000),
          'someMimeType',
          true,
          true,
          true,
          'someLocalUri',
        ],
      ])
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )
      const attachDrafts = draftAttachments.draftAttachEntryFromCursor(mockCursor)
      expect(attachDrafts).toStrictEqual([
        new DraftAttachEntry(
          int64(111),
          int64(123),
          null,
          'someFileUri',
          'some display name',
          int64(16000),
          null,
          false,
          false,
          false,
          null,
        ),
        new DraftAttachEntry(
          int64(111),
          int64(123),
          'https://someUrl.org/?id=123',
          'someOtherFileUri',
          'some other display name',
          int64(16000),
          'someMimeType',
          true,
          true,
          true,
          'someLocalUri',
        ),
      ])
    })
  })
  describe('diskAttachBundleFromCursor', () => {
    it('should create DiskAttachBundle from cursor', () => {
      const mockCursor = MockCursorWithArray([
        ['some display name', null, int64(16000)],
        ['some other display name', 'https://somedownloadurl.com/?id=123', int64(32000)],
      ])
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )
      const attachDrafts = draftAttachments.diskAttachBundleFromCursor(mockCursor)
      expect(attachDrafts).toStrictEqual([
        new DiskAttachBundle('some display name', null, int64(16000)),
        new DiskAttachBundle('some other display name', 'https://somedownloadurl.com/?id=123', int64(32000)),
      ])
    })
  })
  describe('getDiskAttachesOfDraft', () => {
    it('should not run query if there is no draftId for specified messageId', (done) => {
      const testIDSupport = new TestIDSupport()
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const drafts = new Drafts(Models.instance())
      drafts.getMidByDid = jest.fn().mockImplementation(() => resolve(null))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      const mockDiskAttachBundleFromCursor = jest.spyOn(draftAttachments, 'diskAttachBundleFromCursor')
      draftAttachments.getDiskAttachesOfDraft(int64(123)).then((diskBundles) => {
        expect(mockRunQuery).not.toBeCalled()
        expect(diskBundles.length).toBe(0)
        expect(mockDiskAttachBundleFromCursor).not.toBeCalled()
        done()
      })
    })
    // tslint:disable-next-line:max-line-length
    it('should run query and create array of DiskAttachBundle if draftId for specified messageId was found', (done) => {
      const testIDSupport = new TestIDSupport()
      const mockRunQuery = jest.fn().mockReturnValue(
        resolve(
          MockCursorWithArray([
            ['some display name', null, int64(16000)],
            ['some other display name', 'https://somedownloadurl.com/?id=123', int64(32000)],
          ]),
        ),
      )
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const drafts = new Drafts(Models.instance())
      drafts.getMidByDid = jest.fn().mockImplementation(() => resolve(int64(123)))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      const mockDiskAttachBundleFromCursor = jest.spyOn(draftAttachments, 'diskAttachBundleFromCursor')
      draftAttachments.getDiskAttachesOfDraft(int64(123)).then((diskBundles) => {
        expect(mockRunQuery).toBeCalledWith(
          'SELECT display_name, download_url, size FROM attachment WHERE mid = 123 AND is_disk = 1 UNION SELECT display_name, download_url, size FROM referenced_attachment WHERE did = 123 AND is_disk = 1 UNION SELECT display_name, temp_mul_or_disk_url, size FROM draft_attach WHERE did = 123 AND is_disk = 1 AND uploaded = 1;',
          [],
        )
        expect(mockDiskAttachBundleFromCursor).toBeCalled()
        expect(diskBundles).toStrictEqual([
          new DiskAttachBundle('some display name', null, int64(16000)),
          new DiskAttachBundle('some other display name', 'https://somedownloadurl.com/?id=123', int64(32000)),
        ])
        done()
      })
    })
  })
  describe(DraftAttachments.prototype.deleteTempFiles, () => {
    it('should return immediately if input list of attachIds is empty', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage()
      const drafts = new Drafts(Models.instance())
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem({ delete: mockDelete }),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.deleteTempFiles([]).then((_) => {
        expect(mockDelete).not.toBeCalled()
        done()
      })
    })
    it('should delete attaches temp files', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage()
      const drafts = new Drafts(Models.instance())
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem({ delete: mockDelete }),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.deleteTempFiles([int64(123), int64(456)]).then((_) => {
        expect(mockDelete).nthCalledWith(1, 'attaches-tmp/123', true)
        expect(mockDelete).nthCalledWith(2, 'attaches-tmp/456', true)
        done()
      })
    })
  })
  describe(DraftAttachments.prototype.deleteByDids, () => {
    it('should return immediatelly if dids is empty', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage()
      const drafts = new Drafts(Models.instance())
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.deleteByDids([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should delete drafts with dids', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const drafts = new Drafts(Models.instance())
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.deleteByDids([int64(1), int64(2)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.draft_attach} WHERE did IN (1, 2);`)
        done()
      })
    })
  })
  describe('cleanDraftAttaches', () => {
    it('should call all methods that clear attaches', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve([])),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem({ delete: mockDelete }),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.cleanDraftAttaches(int64(123), int64(456), [int64(1111), int64(2222)]).then((_) => {
        expect(storage.runStatement).nthCalledWith(1, 'DELETE FROM attachment WHERE mid IN (123) AND is_disk = 0;')
        expect(storage.runStatement).nthCalledWith(2, 'DELETE FROM inline_attach WHERE mid IN (123);')
        expect(storage.notifyAboutChanges).nthCalledWith(1, ['attachment', 'inline_attach'])
        expect(storage.runStatement).nthCalledWith(3, 'DELETE FROM referenced_attachment WHERE did = 456;')
        expect(storage.notifyAboutChanges).nthCalledWith(2, ['referenced_attachment'])
        expect(storage.runStatement).nthCalledWith(4, 'DELETE FROM referenced_inline_attachment WHERE did = 456;')
        expect(storage.notifyAboutChanges).nthCalledWith(3, ['referenced_inline_attachment'])
        expect(storage.runStatement).nthCalledWith(5, 'DELETE FROM draft_attach WHERE attach_id IN (1111, 2222);')
        expect(storage.notifyAboutChanges).nthCalledWith(4, ['draft_attach'])
        done()
      })
    })
    it('should call all methods that clear attaches(case of empty list with attachIds)', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve([])),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem({ delete: mockDelete }),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.cleanDraftAttaches(int64(123), int64(456), []).then((_) => {
        expect(storage.runStatement).nthCalledWith(1, 'DELETE FROM attachment WHERE mid IN (123) AND is_disk = 0;')
        expect(storage.runStatement).nthCalledWith(2, 'DELETE FROM inline_attach WHERE mid IN (123);')
        expect(storage.notifyAboutChanges).nthCalledWith(1, ['attachment', 'inline_attach'])
        expect(storage.runStatement).nthCalledWith(3, 'DELETE FROM referenced_attachment WHERE did = 456;')
        expect(storage.notifyAboutChanges).nthCalledWith(2, ['referenced_attachment'])
        expect(storage.runStatement).nthCalledWith(4, 'DELETE FROM referenced_inline_attachment WHERE did = 456;')
        expect(storage.notifyAboutChanges).nthCalledWith(3, ['referenced_inline_attachment'])
        done()
      })
    })
  })
  it('should update mid for pinned disk attaches and notify attachment table from movePinnedDiskAttaches', (done) => {
    const testIDSupport = new TestIDSupport()
    const storage = MockStorage({
      notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
      runStatement: jest.fn().mockReturnValue(resolve([])),
    })
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    draftAttachments.movePinnedDiskAttaches(int64(123), int64(456)).then((_) => {
      expect(storage.runStatement).toBeCalledWith('UPDATE attachment SET mid = 456 WHERE mid = 123 AND is_disk = 1;')
      expect(storage.notifyAboutChanges).toHaveBeenNthCalledWith(1, ['attachment'])
      done()
    })
  })
  it('should delete all attach drafts for specified draftId and notify draft_attach table', (done) => {
    const testIDSupport = new TestIDSupport()
    const storage = MockStorage({
      notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
      runStatement: jest.fn().mockReturnValue(resolve([])),
    })
    const draftAttachments = new DraftAttachments(
      storage,
      new AttachmentsManager(storage, testIDSupport),
      testIDSupport,
      MockFileSystem(),
      'attaches-tmp',
      new Drafts(Models.instance()),
      createMockInstance(AttachmentSizes),
    )
    draftAttachments.cleanAllDraftAttachesByDid(int64(123)).then((_) => {
      expect(storage.runStatement).toBeCalledWith('DELETE FROM draft_attach WHERE did = 123;')
      expect(storage.notifyAboutChanges).toHaveBeenNthCalledWith(1, ['draft_attach'])
      done()
    })
  })
  describe('getPinnedNonDiskAttachesHids', () => {
    it('should not execute any queries if mid was not found by draftId, should rather throw error', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runStatement: jest.fn().mockReturnValue(resolve([])),
      })
      const mockDrafts = new Drafts(Models.instance())
      mockDrafts.getMidByDid = jest.fn().mockReturnValue(resolve(null))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        mockDrafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.getPinnedNonDiskAttachesHids(int64(123)).failed((err) => {
        expect(err).toStrictEqual(new YSError('Not found mid for did=123'))
        done()
      })
    })
    it('should not execute any queries if mid was not found by draftId, should rather throw error', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([['a'], ['b'], ['c']]))),
      })
      const mockDrafts = new Drafts(Models.instance())
      mockDrafts.getMidByDid = jest.fn().mockReturnValue(resolve(int64(456)))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        mockDrafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.getPinnedNonDiskAttachesHids(int64(123)).then((hids) => {
        expect(storage.runQuery).toBeCalledWith(
          'SELECT hid FROM attachment WHERE mid = 456 AND is_disk = 0 UNION SELECT hid FROM inline_attach WHERE mid = 456 UNION SELECT hid FROM referenced_attachment WHERE did = 123 AND is_disk = 0 UNION SELECT hid FROM referenced_inline_attachment WHERE did = 123;',
          [],
        )
        expect(hids).toStrictEqual(['a', 'b', 'c'])
        done()
      })
    })
  })
  describe('pinDiskAttaches', () => {
    it('should return immediately if input list of attach ids is empty', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runStatement: jest.fn().mockReturnValue(resolve([])),
      })
      const mockDrafts = new Drafts(Models.instance())
      mockDrafts.getMidByDid = jest.fn().mockReturnValue(resolve(int64(456)))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        mockDrafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.pinDiskAttaches(int64(123), int64(456), []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should execute query for pinning disk attaches and notify attachment table', (done) => {
      const testIDSupport = new TestIDSupport()
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runStatement: jest.fn().mockReturnValue(resolve([])),
      })
      const mockDrafts = new Drafts(Models.instance())
      mockDrafts.getMidByDid = jest.fn().mockReturnValue(resolve(int64(456)))
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        mockDrafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.pinDiskAttaches(int64(123), int64(456), [int64(11), int64(22)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          "INSERT INTO attachment (mid, hid, display_name, attachClass, size, mime_type, preview_support, is_disk, download_url, download_manager_id) SELECT (SELECT 123), 'fakehid_' || random() AS hid, display_name, NULL AS attachClass, size, mime_type, preview_support, is_disk, temp_mul_or_disk_url AS download_url, NULL AS download_manager_id FROM draft_attach WHERE is_disk = 1 AND attach_id IN (11, 22) AND temp_mul_or_disk_url IS NOT NULL UNION SELECT (SELECT 123), 'fakehid_' || random() AS hid, display_name, attachClass, size, mime_type, preview_support, is_disk, download_url, NULL AS download_manager_id FROM referenced_attachment WHERE is_disk = 1 AND did = 456 AND download_url IS NOT NULL;",
        )
        expect(storage.notifyAboutChanges).toBeCalledWith(['attachment'])
        done()
      })
    })
  })
  describe(DraftAttachments.prototype.getDraftAttachement, () => {
    it('should execute correct query and return null if no draft attach for specififed draftAttachId was found', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      draftAttachments.getDraftAttachement(int64(123)).then((draftAttach) => {
        expect(storage.runQuery).toBeCalledWith('SELECT * FROM draft_attach WHERE attach_id = 123;', [])
        expect(draftAttach).toBeNull()
        done()
      })
    })
    it('should execute correct query and return draft attach from cursor', (done) => {
      const mockRunQuery = jest
        .fn()
        .mockReturnValue(
          resolve(
            MockCursorWithArray([
              [
                int64(111),
                int64(123),
                'https://someUrl.org/id=123',
                'someFileUri',
                'some display name',
                int64(16000),
                null,
                false,
                false,
                false,
                null,
              ],
            ]),
          ),
        )
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      draftAttachments.getDraftAttachement(int64(123)).then((draftAttach) => {
        expect(storage.runQuery).toBeCalledWith('SELECT * FROM draft_attach WHERE attach_id = 123;', [])
        expect(draftAttach).toStrictEqual(
          new DraftAttachEntry(
            int64(111),
            int64(123),
            'https://someUrl.org/id=123',
            'someFileUri',
            'some display name',
            int64(16000),
            null,
            false,
            false,
            false,
            null,
          ),
        )
        done()
      })
    })
  })
  describe(DraftAttachments.prototype.getSummaryNonDiskAttachmentsSize, () => {
    it('should execute correct query and return 0 if no attachments were found', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      expect.assertions(2)
      draftAttachments.getSummaryNonDiskAttachmentsSize(int64(123), int64(456)).then((size) => {
        expect(storage.runQuery).toBeCalledWith(
          'SELECT (SELECT ifnull(sum(size), 0) FROM attachment WHERE mid = 123 AND is_disk = 0 AND size != -1) + (SELECT ifnull(sum(size), 0) FROM referenced_attachment WHERE did = 456 AND is_disk = 0 AND size != -1) + (SELECT ifnull(sum(size), 0) FROM draft_attach WHERE did = 456 AND is_disk = 0 AND size != -1);',
          [],
        )
        expect(size).toBe(int64(0))
        done()
      })
    })
    it('should execute correct query and return total size for all attachments', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(6)]])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      expect.assertions(2)
      draftAttachments.getSummaryNonDiskAttachmentsSize(int64(123), int64(456)).then((size) => {
        expect(storage.runQuery).toBeCalledWith(
          'SELECT (SELECT ifnull(sum(size), 0) FROM attachment WHERE mid = 123 AND is_disk = 0 AND size != -1) + (SELECT ifnull(sum(size), 0) FROM referenced_attachment WHERE did = 456 AND is_disk = 0 AND size != -1) + (SELECT ifnull(sum(size), 0) FROM draft_attach WHERE did = 456 AND is_disk = 0 AND size != -1);',
          [],
        )
        expect(size).toBe(int64(6))
        done()
      })
    })
  })

  describe(DraftAttachments.prototype.getSummaryNonDiskAttachmentsSizeByDid, () => {
    it('should fail if mid was not found', (done) => {
      const storage = MockStorage()
      const testIDSupport = new TestIDSupport()
      const drafts = createMockInstance(Drafts, {
        getMidByDidOrReject: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )

      expect.assertions(2)
      draftAttachments.getSummaryNonDiskAttachmentsSizeByDid(int64(123)).failed((e) => {
        expect(e!.message).toBe('FAILED')
        expect(drafts.getMidByDidOrReject).toBeCalledWith(int64(123))
        done()
      })
    })
    it('should succeed if mid was found', (done) => {
      const storage = MockStorage()
      const testIDSupport = new TestIDSupport()
      const drafts = createMockInstance(Drafts, {
        getMidByDidOrReject: jest.fn().mockReturnValue(resolve(int64(456))),
      })
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        drafts,
        createMockInstance(AttachmentSizes),
      )
      draftAttachments.getSummaryNonDiskAttachmentsSize = jest.fn().mockReturnValue(resolve(int64(100)))

      expect.assertions(2)
      draftAttachments.getSummaryNonDiskAttachmentsSizeByDid(int64(123)).then((size) => {
        expect(size).toBe(int64(100))
        expect(draftAttachments.getSummaryNonDiskAttachmentsSize).toBeCalledWith(int64(456), int64(123))
        done()
      })
    })
  })

  describe(DraftAttachments.prototype.formDraftAttachEntry, () => {
    it('should build DraftAttachEntry', (done) => {
      const storage = MockStorage()
      const testIDSupport = new TestIDSupport()
      const drafts = createMockInstance(Drafts, {
        getMidByDidOrReject: jest.fn().mockReturnValue(resolve(int64(456))),
      })
      const fs = MockFileSystem({
        getItemInfo: jest.fn().mockReturnValue(
          resolve({
            size: int64(100),
          }),
        ),
      })
      const attachSizes = createMockInstance(AttachmentSizes, {
        mustBeUploadedToDisk: jest.fn().mockReturnValue(true),
      })
      const mimeTypeMapStub: IMimeTypeMap = {
        getMimeType: jest.fn().mockReturnValue('image'),
      }
      Registry.registerServiceLocatorItems(
        new Map<ServiceLocatorItems.mimeTypeMap, () => any>([[ServiceLocatorItems.mimeTypeMap, () => mimeTypeMapStub]]),
      )
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        fs,
        'attaches-tmp',
        drafts,
        attachSizes,
      )
      draftAttachments.getSummaryNonDiskAttachmentsSizeByDid = jest.fn().mockReturnValue(resolve(int64(200)))

      draftAttachments.formDraftAttachEntry('/path/to/file.jpg', int64(123), 'file.jpg').then((attach) => {
        expect(attach).toStrictEqual(
          new DraftAttachEntry(
            DraftAttachments.NO_ATTACHMENT_ID,
            int64(123),
            null,
            'file:///path/to/file.jpg',
            'file.jpg',
            int64(100),
            'image',
            true,
            true,
            false,
            null,
          ),
        )
        done()
      })
    })
  })

  describe(DraftAttachments.prototype.insertNewAttachEntry, () => {
    it('should insert new attach entry and return its id', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(555)]])))
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close: StorageStatement['close'] = jest.fn()
      const mockPrepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        runQuery: mockRunQuery,
        runStatement: jest.fn().mockReturnValue(resolve([])),
        withinTransaction: MockWithinTransaction<any>(),
        prepareStatement: mockPrepareStatement,
      })

      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      const attach = new DraftAttachEntry(
        DraftAttachments.NO_ATTACHMENT_ID,
        int64(123),
        null,
        'file:///path/to/file.jpg',
        'file.jpg',
        int64(100),
        'image',
        true,
        true,
        false,
        null,
      )

      expect.assertions(6)
      draftAttachments.insertNewAttachEntry(attach).then((attachId) => {
        expect(attachId).toBe(int64(555))
        expect(mockPrepareStatement).toBeCalledWith(
          'INSERT INTO draft_attach (did, temp_mul_or_disk_url, file_uri, display_name, size, mime_type, preview_support, is_disk, uploaded, local_file_uri) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);',
        )
        expect(execute).toBeCalledWith([
          '123',
          null,
          'file:///path/to/file.jpg',
          'file.jpg',
          '100',
          'image',
          true,
          true,
          false,
          null,
        ])
        expect(close).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_attach])
        expect(storage.runQuery).toBeCalledWith('SELECT seq FROM sqlite_sequence WHERE name = "draft_attach"', [])
        done()
      })
    })
  })

  describe(DraftAttachments.prototype.setLocalFileUriForAttach, () => {
    it('should set local file uri for attach', (done) => {
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close: StorageStatement['close'] = jest.fn()
      const mockPrepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        prepareStatement: mockPrepareStatement,
      })

      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      expect.assertions(4)
      draftAttachments.setLocalFileUriForAttach('uri', int64(123)).then(() => {
        expect(mockPrepareStatement).toBeCalledWith('UPDATE draft_attach SET local_file_uri = ? WHERE attach_id = 123;')
        expect(execute).toBeCalledWith(['uri'])
        expect(close).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_attach])
        done()
      })
    })
    it('should reset local file uri for attach', (done) => {
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close: StorageStatement['close'] = jest.fn()
      const mockPrepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const storage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve([])),
        prepareStatement: mockPrepareStatement,
      })

      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem(),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      expect.assertions(4)
      draftAttachments.setLocalFileUriForAttach(null, int64(123)).then(() => {
        expect(mockPrepareStatement).toBeCalledWith(
          'UPDATE draft_attach SET local_file_uri = NULL WHERE attach_id = 123;',
        )
        expect(execute).toBeCalledWith([])
        expect(close).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_attach])
        done()
      })
    })
  })

  describe(DraftAttachments.prototype.insertNewAttach, () => {
    it('should insert new attach', (done) => {
      const mockFileSystemMove = jest.fn().mockReturnValue(resolve(getVoid()))

      const storage = MockStorage()
      const testIDSupport = new TestIDSupport()
      const draftAttachments = new DraftAttachments(
        storage,
        new AttachmentsManager(storage, testIDSupport),
        testIDSupport,
        MockFileSystem({
          move: mockFileSystemMove,
        }),
        'attaches-tmp',
        new Drafts(Models.instance()),
        createMockInstance(AttachmentSizes),
      )

      draftAttachments.formDraftAttachEntry = jest.fn().mockReturnValue(resolve({}))
      draftAttachments.insertNewAttachEntry = jest.fn().mockReturnValue(resolve(int64(456)))
      draftAttachments.setLocalFileUriForAttach = jest.fn().mockReturnValue(resolve(getVoid()))

      expect.assertions(5)
      draftAttachments.insertNewAttach('local file', int64(123), 'display name').then((attachId) => {
        expect(attachId).toBe(int64(456))
        expect(draftAttachments.formDraftAttachEntry).toBeCalledWith('local file', int64(123), 'display name')
        expect(draftAttachments.insertNewAttachEntry).toBeCalledWith({})
        expect(mockFileSystemMove).toBeCalledWith('local file', 'attaches-tmp/456')
        expect(draftAttachments.setLocalFileUriForAttach).toBeCalledWith('attaches-tmp/456', int64(456))
        done()
      })
    })
  })
})
