import { int64 } from '../../../../../../../common/ys'
import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import {
  DeltaApiFolderSymbol,
  DeltaApiFolderType,
} from '../../../../../code/api/entities/delta-api/entities/delta-api-folder'
import {
  deltaApiFolderTypeAndSymbolToFolderType,
  Folder,
  folderFromJSONItem,
  FolderSyncType,
  folderSyncTypeToInt32,
  FolderType,
  folderTypeToInt32,
  int32ToFolderSyncType,
  int32ToFolderType,
  isFolderOfTabType,
  isFolderOfThreadedType,
  tabTypeFromName,
} from '../../../../../code/api/entities/folder/folder'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import response from '../container/sample.json'

describe(Folder, () => {
  it('should convert FolderTypes to Int32', () => {
    expect(folderTypeToInt32(FolderType.inbox)).toBe(FolderType.inbox.valueOf())
    expect(folderTypeToInt32(FolderType.user)).toBe(FolderType.user.valueOf())
    expect(folderTypeToInt32(FolderType.outgoing)).toBe(FolderType.outgoing.valueOf())
    expect(folderTypeToInt32(FolderType.sent)).toBe(FolderType.sent.valueOf())
    expect(folderTypeToInt32(FolderType.draft)).toBe(FolderType.draft.valueOf())
    expect(folderTypeToInt32(FolderType.spam)).toBe(FolderType.spam.valueOf())
    expect(folderTypeToInt32(FolderType.trash)).toBe(FolderType.trash.valueOf())
    expect(folderTypeToInt32(FolderType.archive)).toBe(FolderType.archive.valueOf())
    expect(folderTypeToInt32(FolderType.templates)).toBe(FolderType.templates.valueOf())
    expect(folderTypeToInt32(FolderType.discount)).toBe(FolderType.discount.valueOf())
    expect(folderTypeToInt32(FolderType.other)).toBe(FolderType.other.valueOf())
    expect(folderTypeToInt32(FolderType.unsubscribe)).toBe(FolderType.unsubscribe.valueOf())
    expect(folderTypeToInt32(FolderType.tab_relevant)).toBe(FolderType.tab_relevant.valueOf())
    expect(folderTypeToInt32(FolderType.tab_news)).toBe(FolderType.tab_news.valueOf())
    expect(folderTypeToInt32(FolderType.tab_social)).toBe(FolderType.tab_social.valueOf())
  })
  it('should convert Int32 to FolderType', () => {
    expect(int32ToFolderType(FolderType.inbox.valueOf())).toBe(FolderType.inbox)
    expect(int32ToFolderType(FolderType.user.valueOf())).toBe(FolderType.user)
    expect(int32ToFolderType(FolderType.outgoing.valueOf())).toBe(FolderType.outgoing)
    expect(int32ToFolderType(FolderType.sent.valueOf())).toBe(FolderType.sent)
    expect(int32ToFolderType(FolderType.draft.valueOf())).toBe(FolderType.draft)
    expect(int32ToFolderType(FolderType.spam.valueOf())).toBe(FolderType.spam)
    expect(int32ToFolderType(FolderType.trash.valueOf())).toBe(FolderType.trash)
    expect(int32ToFolderType(FolderType.archive.valueOf())).toBe(FolderType.archive)
    expect(int32ToFolderType(FolderType.templates.valueOf())).toBe(FolderType.templates)
    expect(int32ToFolderType(FolderType.discount.valueOf())).toBe(FolderType.discount)
    expect(int32ToFolderType(FolderType.other.valueOf())).toBe(FolderType.other)
    expect(int32ToFolderType(FolderType.unsubscribe.valueOf())).toBe(FolderType.unsubscribe)
    expect(int32ToFolderType(FolderType.tab_relevant.valueOf())).toBe(FolderType.tab_relevant)
    expect(int32ToFolderType(FolderType.tab_news.valueOf())).toBe(FolderType.tab_news)
    expect(int32ToFolderType(FolderType.tab_social.valueOf())).toBe(FolderType.tab_social)
    expect(int32ToFolderType(1000)).toBe(FolderType.other)
  })
  it('should provide FolderTypes with specific values', () => {
    expect(FolderType.inbox).toBe(1)
    expect(FolderType.user).toBe(2)
    expect(FolderType.outgoing).toBe(3)
    expect(FolderType.sent).toBe(4)
    expect(FolderType.draft).toBe(5)
    expect(FolderType.spam).toBe(6)
    expect(FolderType.trash).toBe(7)
    expect(FolderType.archive).toBe(8)
    expect(FolderType.templates).toBe(9)
    expect(FolderType.discount).toBe(10)
    expect(FolderType.other).toBe(11)
    expect(FolderType.unsubscribe).toBe(12)
    expect(FolderType.tab_relevant).toBe(100)
    expect(FolderType.tab_news).toBe(101)
    expect(FolderType.tab_social).toBe(102)
  })
  it('should convert FolderSyncTypes to Int32', () => {
    expect(folderSyncTypeToInt32(FolderSyncType.doNotSync)).toBe(FolderSyncType.doNotSync.valueOf())
    expect(folderSyncTypeToInt32(FolderSyncType.silentSync)).toBe(FolderSyncType.silentSync.valueOf())
    expect(folderSyncTypeToInt32(FolderSyncType.pushSync)).toBe(FolderSyncType.pushSync.valueOf())
  })
  it('should convert Int32 to FolderSyncType', () => {
    expect(int32ToFolderSyncType(FolderSyncType.doNotSync.valueOf())).toBe(FolderSyncType.doNotSync)
    expect(int32ToFolderSyncType(FolderSyncType.silentSync.valueOf())).toBe(FolderSyncType.silentSync)
    expect(int32ToFolderSyncType(FolderSyncType.pushSync.valueOf())).toBe(FolderSyncType.pushSync)
    expect(() => int32ToFolderSyncType(100)).toThrowError('Unknown FolderSyncType for 100')
  })
  it('should convert tab names to FolderType, or null for unknown', () => {
    expect(tabTypeFromName('relevant')).toBe(FolderType.tab_relevant)
    expect(tabTypeFromName('news')).toBe(FolderType.tab_news)
    expect(tabTypeFromName('social')).toBe(FolderType.tab_social)
    expect(tabTypeFromName('')).toBeNull()
    expect(tabTypeFromName('12345')).toBeNull()
  })
  it('should provide FolderSyncTypes with specific values', () => {
    expect(FolderSyncType.doNotSync).toBe(0)
    expect(FolderSyncType.silentSync).toBe(1)
    expect(FolderSyncType.pushSync).toBe(2)
  })
  it('should be deserializable from JSONItem', () => {
    const element = response.find(({ fid }) => fid === '1')!
    expect(folderFromJSONItem(JSONItemFromJSON(element)! as MapJSONItem)).toStrictEqual(
      new Folder(int64(1), FolderType.inbox, 'Inbox', 3700, null, 2460, 6131),
    )
  })
  it('should return null if JSON is malformed', () => {
    const element = response.find(({ fid }) => fid === '1')!
    expect(folderFromJSONItem(JSONItemFromJSON([element]))).toBeNull()
  })
  it('should identify if a folder is threading', () => {
    expect(isFolderOfThreadedType(FolderType.inbox)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.user)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.outgoing)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.sent)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.draft)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.spam)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.trash)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.archive)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.templates)).toBe(false)
    expect(isFolderOfThreadedType(FolderType.discount)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.other)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.unsubscribe)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.tab_relevant)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.tab_news)).toBe(true)
    expect(isFolderOfThreadedType(FolderType.tab_social)).toBe(true)
  })
  it('should identify if a folder is of tab type', () => {
    expect(isFolderOfTabType(FolderType.inbox)).toBe(false)
    expect(isFolderOfTabType(FolderType.user)).toBe(false)
    expect(isFolderOfTabType(FolderType.outgoing)).toBe(false)
    expect(isFolderOfTabType(FolderType.sent)).toBe(false)
    expect(isFolderOfTabType(FolderType.draft)).toBe(false)
    expect(isFolderOfTabType(FolderType.spam)).toBe(false)
    expect(isFolderOfTabType(FolderType.trash)).toBe(false)
    expect(isFolderOfTabType(FolderType.archive)).toBe(false)
    expect(isFolderOfTabType(FolderType.templates)).toBe(false)
    expect(isFolderOfTabType(FolderType.discount)).toBe(false)
    expect(isFolderOfTabType(FolderType.other)).toBe(false)
    expect(isFolderOfTabType(FolderType.unsubscribe)).toBe(false)
    expect(isFolderOfTabType(FolderType.tab_relevant)).toBe(true)
    expect(isFolderOfTabType(FolderType.tab_news)).toBe(true)
    expect(isFolderOfTabType(FolderType.tab_social)).toBe(true)
  })
  it('should convert Delta API folder type into FolderType', () => {
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.inbox)).toBe(
      FolderType.inbox,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.sent)).toBe(
      FolderType.sent,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.trash)).toBe(
      FolderType.trash,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.spam)).toBe(
      FolderType.spam,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.draft)).toBe(
      FolderType.draft,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.outbox)).toBe(
      FolderType.outgoing,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.archive)).toBe(
      FolderType.archive,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.template)).toBe(
      FolderType.templates,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.discount)).toBe(
      FolderType.discount,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.unsubscribe)).toBe(
      FolderType.unsubscribe,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, DeltaApiFolderSymbol.zombie)).toBe(
      FolderType.other,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, null)).toBe(FolderType.other)
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.system, 'non-existent' as any)).toBe(
      FolderType.other,
    )

    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.inbox)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.sent)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.trash)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.spam)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.draft)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.outbox)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.archive)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.template)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.discount)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.unsubscribe)).toBe(
      FolderType.user,
    )
    expect(deltaApiFolderTypeAndSymbolToFolderType(DeltaApiFolderType.user, DeltaApiFolderSymbol.zombie)).toBe(
      FolderType.user,
    )
  })
})
