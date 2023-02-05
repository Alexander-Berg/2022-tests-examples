import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { Scheme, XmailStorageScheme } from '../../../../code/api/entities/scheme'
import { DB } from '../../../../code/api/storage/scheme-support/db-entity'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import { Result, getVoid } from '../../../../../common/code/result/result'
import { Int32, range } from '../../../../../../common/ys'

describe(Scheme, () => {
  describe('without ID Support', () => {
    it('should throw if IDSupport is not registered', () => {
      expect(() => Scheme.folder().asScript()).toThrowError('ID Support must be registered with DB before use')
    })
  })
  describe('setting ID Support', () => {
    it('should set ID Support', () => {
      const dbSpy = jest.spyOn(DB, 'setIDSupport')
      const testIDSupport = new TestIDSupport()
      Scheme.setIDSupport(testIDSupport)
      expect(dbSpy).toBeCalledWith(testIDSupport)
      dbSpy.mockRestore()
    })
  })
  describe('with ID Support', () => {
    beforeAll(() => {
      Scheme.setIDSupport(new TestIDSupport())
    })
    it('contains the abook scheme', () => {
      const script = Scheme.abookCache().asScript()
      const expected = `CREATE TABLE abook_cache (
_id INTEGER PRIMARY KEY AUTOINCREMENT,
cid TEXT NOT NULL UNIQUE ON CONFLICT REPLACE,
email TEXT,
first_name TEXT,
last_name TEXT
);`
      expect(script).toBe(expected)
    })
    it('contains the attachment scheme', () => {
      const script = Scheme.attachment().asScript()
      const expected = `CREATE TABLE attachment (
mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
attachClass TEXT,
size INTEGER NOT NULL,
mime_type TEXT NOT NULL DEFAULT "",
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
download_url TEXT NOT NULL,
download_manager_id INTEGER,
PRIMARY KEY (mid, hid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the draft attachment scheme', () => {
      const script = Scheme.draftAttach().asScript()
      const expected = `CREATE TABLE draft_attach (
attach_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
did INTEGER NOT NULL REFERENCES draft_entry(did),
temp_mul_or_disk_url TEXT,
file_uri TEXT NOT NULL,
display_name TEXT NOT NULL,
size INTEGER NOT NULL,
mime_type TEXT,
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
uploaded INTEGER NOT NULL DEFAULT 0,
local_file_uri TEXT
);`
      expect(script).toBe(expected)
    })
    it('contains the draft scheme', () => {
      const script = Scheme.draftEntry().asScript()
      const expected = `CREATE TABLE draft_entry (
did INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
mid INTEGER NOT NULL,
reply_type INTEGER NOT NULL DEFAULT 0,
reply_mid INTEGER NOT NULL DEFAULT -1,
revision INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains the email scheme', () => {
      const script = Scheme.email().asScript()
      const expected = `CREATE TABLE email (
login TEXT NOT NULL,
domain TEXT NOT NULL,
PRIMARY KEY (login, domain) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the folder scheme', () => {
      const script = Scheme.folder().asScript()
      const expected = `CREATE TABLE folder (
fid INTEGER PRIMARY KEY NOT NULL,
type INTEGER NOT NULL,
name TEXT NOT NULL,
position INTEGER NOT NULL,
parent INTEGER,
unread_counter INTEGER NOT NULL,
total_counter INTEGER NOT NULL
);`
      expect(script).toBe(expected)
    })
    it('contains the folder counters scheme', () => {
      const script = Scheme.folderCounters().asScript()
      const expected = `CREATE TABLE folder_counters (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT REPLACE,
overflow_total INTEGER NOT NULL,
overflow_unread INTEGER NOT NULL,
local_total INTEGER NOT NULL,
local_unread INTEGER NOT NULL
);`
      expect(script).toBe(expected)
    })
    it('contains the folder expand scheme', () => {
      const script = Scheme.folderExpand().asScript()
      const expected = `CREATE TABLE folder_expand (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
is_expanded INTEGER NOT NULL DEFAULT 1
);`
      expect(script).toBe(expected)
    })
    it('contains the folder last access time scheme', () => {
      const script = Scheme.folderLastAccessTime().asScript()
      const expected = `CREATE TABLE folder_lat (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
lat INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains the folder load more time scheme', () => {
      const script = Scheme.folderLoadMore().asScript()
      const expected = `CREATE TABLE folder_load_more (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
load_more_time INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains the folder-to-messages scheme', () => {
      const script = Scheme.folderMessages().asScript()
      const expected = `CREATE TABLE folder_messages (
fid INTEGER NOT NULL REFERENCES folder(fid),
mid INTEGER NOT NULL REFERENCES message_meta(mid) PRIMARY KEY ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the folder sync time scheme', () => {
      const script = Scheme.folderSyncType().asScript()
      const expected = `CREATE TABLE folder_synctype (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
sync_type INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains the inline attachment scheme', () => {
      const script = Scheme.inlineAttachment().asScript()
      const expected = `CREATE TABLE inline_attach (
mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
content_id TEXT NOT NULL,
PRIMARY KEY (mid, hid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the label scheme', () => {
      const script = Scheme.label().asScript()
      const expected = `CREATE TABLE label (
lid TEXT PRIMARY KEY NOT NULL,
type INTEGER NOT NULL,
name TEXT NOT NULL,
unread_counter INTEGER NOT NULL,
total_counter INTEGER NOT NULL,
color INTEGER NOT NULL,
symbol INTEGER
);`
      expect(script).toBe(expected)
    })
    it('contains the label-to-message scheme', () => {
      const script = Scheme.labelsMessages().asScript()
      const expected = `CREATE TABLE labels_messages (
lid TEXT NOT NULL REFERENCES label(lid),
mid INTEGER NOT NULL REFERENCES message_meta(mid),
tid INTEGER,
PRIMARY KEY (mid, lid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the message body scheme', () => {
      const script = Scheme.messageBodyMeta().asScript()
      const expected = `CREATE TABLE message_body_meta (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
recipients TEXT,
rfc_id TEXT,
reference TEXT,
contentType TEXT,
lang TEXT,
quick_reply_enabled INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains the message meta scheme', () => {
      const script = Scheme.messageMeta().asScript()
      const expected = `CREATE TABLE message_meta (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
fid INTEGER NOT NULL REFERENCES folder(fid),
tid INTEGER,
subj_empty INTEGER NOT NULL,
subj_prefix TEXT NOT NULL,
subj_text TEXT NOT NULL,
first_line TEXT NOT NULL,
sender TEXT NOT NULL,
unread INTEGER NOT NULL,
search_only INTEGER NOT NULL,
show_for TEXT,
timestamp INTEGER NOT NULL,
hasAttach INTEGER NOT NULL,
typeMask INTEGER NOT NULL
);`
      expect(script).toBe(expected)
    })
    it('contains the message timestamp scheme', () => {
      const script = Scheme.messageTimestamp().asScript()
      const expected = `CREATE TABLE message_timestamp (
mid INTEGER NOT NULL REFERENCES message_meta(mid) PRIMARY KEY ON CONFLICT IGNORE,
timestamp INTEGER NOT NULL
);`
      expect(script).toBe(expected)
    })
    it('contains the non-deleted command files scheme', () => {
      const script = Scheme.notDeletedCommandFiles().asScript()
      const expected = `CREATE TABLE IF NOT EXISTS not_deleted_command_files (
file TEXT NOT NULL PRIMARY KEY
);`
      expect(script).toBe(expected)
    })
    it('contains the non-synced messages scheme', () => {
      const script = Scheme.notSyncedMessages().asScript()
      const expected = `CREATE TABLE not_synced_messages (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
fid INTEGER NOT NULL,
tid INTEGER
);`
      expect(script).toBe(expected)
    })
    it('contains the pending compose operations scheme', () => {
      const script = Scheme.pendingComposeOperations().asScript()
      const expected = `CREATE TABLE pending_compose_ops (
did INTEGER NOT NULL,
revision INTEGER NOT NULL,
PRIMARY KEY (did, revision)
);`
      expect(script).toBe(expected)
    })
    it('contains the referenced attachments scheme', () => {
      const script = Scheme.referencedAttachment().asScript()
      const expected = `CREATE TABLE referenced_attachment (
did INTEGER NOT NULL,
reference_mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
attachClass TEXT,
size INTEGER NOT NULL,
mime_type TEXT NOT NULL,
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
download_url TEXT NOT NULL,
PRIMARY KEY (did, reference_mid, hid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the referenced inline attachments scheme', () => {
      const script = Scheme.referencedInlineAttachment().asScript()
      const expected = `CREATE TABLE referenced_inline_attachment (
did INTEGER NOT NULL,
reference_mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
content_id TEXT NOT NULL,
PRIMARY KEY (did, reference_mid, hid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the search cache scheme', () => {
      const script = Scheme.searchCache().asScript()
      const expected = `CREATE TABLE search_cache (
mid INTEGER NOT NULL,
show_for TEXT NOT NULL,
PRIMARY KEY (mid, show_for) ON CONFLICT IGNORE
);`
      expect(script).toBe(expected)
    })
    it('contains the thread scheme', () => {
      const script = Scheme.thread().asScript()
      const expected = `CREATE TABLE thread (
tid INTEGER NOT NULL,
fid INTEGER NOT NULL REFERENCES folder(fid),
top_mid INTEGER NOT NULL,
PRIMARY KEY (fid, tid) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains the thread counters scheme', () => {
      const script = Scheme.threadCounters().asScript()
      const expected = `CREATE TABLE thread_counters (
tid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
total_counter INTEGER NOT NULL,
unread INTEGER NOT NULL
);`
      expect(script).toBe(expected)
    })
    it('contains the thread scn scheme', () => {
      const script = Scheme.threadScn().asScript()
      const expected = `CREATE TABLE thread_scn (
tid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
scn INTEGER NOT NULL DEFAULT 0
);`
      expect(script).toBe(expected)
    })
    it('contains recipients scheme', () => {
      const script = Scheme.recipients().asScript()
      const expected = `CREATE TABLE IF NOT EXISTS recipients (
mid INTEGER NOT NULL,
email TEXT NOT NULL,
type INTEGER NOT NULL,
name TEXT,
PRIMARY KEY (mid, email, type) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains widgetsInfo scheme', () => {
      const script = Scheme.widgetsInfo().asScript()
      const expected = `CREATE TABLE IF NOT EXISTS widgets_info (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
type TEXT NOT NULL,
subtype TEXT NOT NULL,
PRIMARY KEY (mid, type) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('contains messageSmartReply scheme', () => {
      const script = Scheme.messageSmartReply().asScript()
      const expected = `CREATE TABLE IF NOT EXISTS message_smart_reply (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
reply_index INTEGER NOT NULL,
smart_reply TEXT NOT NULL,
PRIMARY KEY (mid, reply_index) ON CONFLICT REPLACE
);`
      expect(script).toBe(expected)
    })
    it('should contain all the tables', () => {
      const entities = Scheme.allEntities().map(({ name }) => name)
      const expected = [
        EntityKind.abook_cache,
        EntityKind.attachment,
        EntityKind.draft_attach,
        EntityKind.draft_entry,
        EntityKind.email,
        EntityKind.folder,
        EntityKind.folder_counters,
        EntityKind.folder_expand,
        EntityKind.folder_lat,
        EntityKind.folder_load_more,
        EntityKind.folder_messages,
        EntityKind.folder_synctype,
        EntityKind.inline_attach,
        EntityKind.label,
        EntityKind.labels_messages,
        EntityKind.message_body_meta,
        EntityKind.message_meta,
        EntityKind.message_timestamp,
        EntityKind.not_deleted_command_files,
        EntityKind.not_synced_messages,
        EntityKind.pending_compose_ops,
        EntityKind.referenced_attachment,
        EntityKind.referenced_inline_attachment,
        EntityKind.search_cache,
        EntityKind.thread,
        EntityKind.thread_counters,
        EntityKind.thread_scn,
        EntityKind.recipients,
        EntityKind.widgets_info,
        EntityKind.message_smart_reply,
      ]
      expect(entities).toHaveLength(expected.length)
      expect(entities).toStrictEqual(expected)
    })
  })
})

describe(XmailStorageScheme, () => {
  const xmailScheme = new XmailStorageScheme()

  it('should return version', () => {
    expect(xmailScheme.version()).toBe(2)
  })
  it('should initialize all tables', () => {
    const db = {
      execSQL: jest.fn().mockReturnValue(new Result(getVoid(), null)),
    }
    expect(xmailScheme.onCreate(db)).toStrictEqual(new Result(getVoid(), null))
    expect(db.execSQL.mock.calls).toEqual([
      [
        `CREATE TABLE abook_cache (
_id INTEGER PRIMARY KEY AUTOINCREMENT,
cid TEXT NOT NULL UNIQUE ON CONFLICT REPLACE,
email TEXT,
first_name TEXT,
last_name TEXT
);`,
      ],
      [
        `CREATE TABLE attachment (
mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
attachClass TEXT,
size INTEGER NOT NULL,
mime_type TEXT NOT NULL DEFAULT "",
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
download_url TEXT NOT NULL,
download_manager_id INTEGER,
PRIMARY KEY (mid, hid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE draft_attach (
attach_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
did INTEGER NOT NULL REFERENCES draft_entry(did),
temp_mul_or_disk_url TEXT,
file_uri TEXT NOT NULL,
display_name TEXT NOT NULL,
size INTEGER NOT NULL,
mime_type TEXT,
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
uploaded INTEGER NOT NULL DEFAULT 0,
local_file_uri TEXT
);`,
      ],
      [
        `CREATE TABLE draft_entry (
did INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
mid INTEGER NOT NULL,
reply_type INTEGER NOT NULL DEFAULT 0,
reply_mid INTEGER NOT NULL DEFAULT -1,
revision INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE email (
login TEXT NOT NULL,
domain TEXT NOT NULL,
PRIMARY KEY (login, domain) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE folder (
fid INTEGER PRIMARY KEY NOT NULL,
type INTEGER NOT NULL,
name TEXT NOT NULL,
position INTEGER NOT NULL,
parent INTEGER,
unread_counter INTEGER NOT NULL,
total_counter INTEGER NOT NULL
);`,
      ],
      [
        `CREATE TABLE folder_counters (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT REPLACE,
overflow_total INTEGER NOT NULL,
overflow_unread INTEGER NOT NULL,
local_total INTEGER NOT NULL,
local_unread INTEGER NOT NULL
);`,
      ],
      [
        `CREATE TABLE folder_expand (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
is_expanded INTEGER NOT NULL DEFAULT 1
);`,
      ],
      [
        `CREATE TABLE folder_lat (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
lat INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE folder_load_more (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
load_more_time INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE folder_messages (
fid INTEGER NOT NULL REFERENCES folder(fid),
mid INTEGER NOT NULL REFERENCES message_meta(mid) PRIMARY KEY ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE folder_synctype (
fid INTEGER NOT NULL REFERENCES folder(fid) PRIMARY KEY ON CONFLICT IGNORE,
sync_type INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE inline_attach (
mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
content_id TEXT NOT NULL,
PRIMARY KEY (mid, hid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE label (
lid TEXT PRIMARY KEY NOT NULL,
type INTEGER NOT NULL,
name TEXT NOT NULL,
unread_counter INTEGER NOT NULL,
total_counter INTEGER NOT NULL,
color INTEGER NOT NULL,
symbol INTEGER
);`,
      ],
      [
        `CREATE TABLE labels_messages (
lid TEXT NOT NULL REFERENCES label(lid),
mid INTEGER NOT NULL REFERENCES message_meta(mid),
tid INTEGER,
PRIMARY KEY (mid, lid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE message_body_meta (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
recipients TEXT,
rfc_id TEXT,
reference TEXT,
contentType TEXT,
lang TEXT,
quick_reply_enabled INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE message_meta (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
fid INTEGER NOT NULL REFERENCES folder(fid),
tid INTEGER,
subj_empty INTEGER NOT NULL,
subj_prefix TEXT NOT NULL,
subj_text TEXT NOT NULL,
first_line TEXT NOT NULL,
sender TEXT NOT NULL,
unread INTEGER NOT NULL,
search_only INTEGER NOT NULL,
show_for TEXT,
timestamp INTEGER NOT NULL,
hasAttach INTEGER NOT NULL,
typeMask INTEGER NOT NULL
);`,
      ],
      [
        `CREATE TABLE message_timestamp (
mid INTEGER NOT NULL REFERENCES message_meta(mid) PRIMARY KEY ON CONFLICT IGNORE,
timestamp INTEGER NOT NULL
);`,
      ],
      [
        `CREATE TABLE IF NOT EXISTS not_deleted_command_files (
file TEXT NOT NULL PRIMARY KEY
);`,
      ],
      [
        `CREATE TABLE not_synced_messages (
mid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
fid INTEGER NOT NULL,
tid INTEGER
);`,
      ],
      [
        `CREATE TABLE pending_compose_ops (
did INTEGER NOT NULL,
revision INTEGER NOT NULL,
PRIMARY KEY (did, revision)
);`,
      ],
      [
        `CREATE TABLE referenced_attachment (
did INTEGER NOT NULL,
reference_mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
attachClass TEXT,
size INTEGER NOT NULL,
mime_type TEXT NOT NULL,
preview_support INTEGER NOT NULL,
is_disk INTEGER NOT NULL DEFAULT 0,
download_url TEXT NOT NULL,
PRIMARY KEY (did, reference_mid, hid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE referenced_inline_attachment (
did INTEGER NOT NULL,
reference_mid INTEGER NOT NULL,
hid TEXT NOT NULL,
display_name TEXT NOT NULL,
content_id TEXT NOT NULL,
PRIMARY KEY (did, reference_mid, hid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE search_cache (
mid INTEGER NOT NULL,
show_for TEXT NOT NULL,
PRIMARY KEY (mid, show_for) ON CONFLICT IGNORE
);`,
      ],
      [
        `CREATE TABLE thread (
tid INTEGER NOT NULL,
fid INTEGER NOT NULL REFERENCES folder(fid),
top_mid INTEGER NOT NULL,
PRIMARY KEY (fid, tid) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE thread_counters (
tid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
total_counter INTEGER NOT NULL,
unread INTEGER NOT NULL
);`,
      ],
      [
        `CREATE TABLE thread_scn (
tid INTEGER NOT NULL PRIMARY KEY ON CONFLICT REPLACE,
scn INTEGER NOT NULL DEFAULT 0
);`,
      ],
      [
        `CREATE TABLE IF NOT EXISTS recipients (
mid INTEGER NOT NULL,
email TEXT NOT NULL,
type INTEGER NOT NULL,
name TEXT,
PRIMARY KEY (mid, email, type) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE IF NOT EXISTS widgets_info (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
type TEXT NOT NULL,
subtype TEXT NOT NULL,
PRIMARY KEY (mid, type) ON CONFLICT REPLACE
);`,
      ],
      [
        `CREATE TABLE IF NOT EXISTS message_smart_reply (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
reply_index INTEGER NOT NULL,
smart_reply TEXT NOT NULL,
PRIMARY KEY (mid, reply_index) ON CONFLICT REPLACE
);`,
      ],
    ])
  })

  describe(XmailStorageScheme.prototype.onUpgrade, () => {
    // A map between migration scripts by "scheme version" in which they were added.
    // If you upgraded the DB, make sure to add your scripts here.
    const migrationScriptsByVersion = new Map</* version */ Int32, /* scripts */ string[]>([
      [
        2,
        [
          `CREATE TABLE IF NOT EXISTS widgets_info (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
type TEXT NOT NULL,
subtype TEXT NOT NULL,
PRIMARY KEY (mid, type) ON CONFLICT REPLACE
);`,
          `CREATE TABLE IF NOT EXISTS message_smart_reply (
mid INTEGER NOT NULL REFERENCES message_meta(mid),
reply_index INTEGER NOT NULL,
smart_reply TEXT NOT NULL,
PRIMARY KEY (mid, reply_index) ON CONFLICT REPLACE
);`,
        ],
      ],
    ])

    // Checks DB upgrade against all possible migration ranges
    // Ex: v1 -> VERSION, v2 -> VERSION, v3 -> VERSION, ...
    // VERSION -> VERSION upgrade case is also verified just to make global coverage checker happy
    const updatesRange: readonly Int32[] = Array.from(range(1, xmailScheme.version() + 1))
    it.each(updatesRange)('should perform upgrade', (fromVersion) => {
      const db = {
        execSQL: jest.fn().mockReturnValue(new Result(getVoid(), null)),
      }

      const onUpgradeResult = xmailScheme.onUpgrade(db, fromVersion, xmailScheme.version() + 1)

      const expectedScripts = Array.from(range(fromVersion + 1, xmailScheme.version() + 1))
        .map((v) => migrationScriptsByVersion.get(v) ?? [])
        .flat()
        .map((script) => [script]) // <-- needed to adopt the expected format of "mock.calls" object

      expect(onUpgradeResult).toStrictEqual(new Result(getVoid(), null))
      expect(db.execSQL.mock.calls).toEqual(expectedScripts)
    })
  })
})
