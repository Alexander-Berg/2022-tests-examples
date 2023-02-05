import { int64 } from '../../../../../../../../common/ys'
import { idFromString } from '../../../../../../code/api/common/id'
import {
  DeltaApiFolder,
  DeltaApiFolderSymbol,
  DeltaApiFolderType,
  stringToDeltaApiFolderSymbol,
  stringToDeltaApiFolderType,
} from '../../../../../../code/api/entities/delta-api/entities/delta-api-folder'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import folder from './folder.json'

describe(DeltaApiFolder, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))
    expect(result).not.toBeNull()
    expect(result!.fid).toBe(idFromString(folder.id))
    expect(result!.name).toBe(folder.name)
    expect(result!.parentId).toBe(idFromString(folder.parentId))
    expect(result!.type).toBe(DeltaApiFolderType.system)
    expect(result!.symbol).toBe(DeltaApiFolderSymbol.inbox)
    expect(result!.recentMessagesCount).toBe(folder.recentMessagesCount)
    expect(result!.messagesCount).toBe(folder.messagesCount)
    expect(result!.createDate).toBe(folder.createDate)
    expect(result!.isThreadable).toBe(folder.isThreadable)
    expect(result!.isSubscribedForSharedFolder).toBe(folder.isSubscribedForSharedFolder)
    expect(result!.unreadMessagesCount).toBe(folder.unreadMessagesCount)
    expect(result!.bytes).toBe(int64(folder.bytes))
    expect(result!.revision).toBe(folder.revision)
    expect(result!.isUnvisited).toBe(folder.isUnvisited)
  })
  it('should parse null if JSON is malformed', () => {
    const result = DeltaApiFolder.fromJSONItem(JSONItemFromJSON([folder]))
    expect(result).toBeNull()
  })
})
test('stringToDeltaApiFolderSymbol', () => {
  expect(stringToDeltaApiFolderSymbol('inbox')).toBe(DeltaApiFolderSymbol.inbox)
  expect(stringToDeltaApiFolderSymbol('sent')).toBe(DeltaApiFolderSymbol.sent)
  expect(stringToDeltaApiFolderSymbol('trash')).toBe(DeltaApiFolderSymbol.trash)
  expect(stringToDeltaApiFolderSymbol('spam')).toBe(DeltaApiFolderSymbol.spam)
  expect(stringToDeltaApiFolderSymbol('draft')).toBe(DeltaApiFolderSymbol.draft)
  expect(stringToDeltaApiFolderSymbol('outbox')).toBe(DeltaApiFolderSymbol.outbox)
  expect(stringToDeltaApiFolderSymbol('archive')).toBe(DeltaApiFolderSymbol.archive)
  expect(stringToDeltaApiFolderSymbol('template')).toBe(DeltaApiFolderSymbol.template)
  expect(stringToDeltaApiFolderSymbol('discount')).toBe(DeltaApiFolderSymbol.discount)
  expect(stringToDeltaApiFolderSymbol('unsubscribe')).toBe(DeltaApiFolderSymbol.unsubscribe)
  expect(stringToDeltaApiFolderSymbol('zombie')).toBe(DeltaApiFolderSymbol.zombie)
  expect(stringToDeltaApiFolderSymbol('')).toBeNull()
  expect(stringToDeltaApiFolderSymbol('unknown')).toBeNull()
})
test('stringToDeltaApiFolderType', () => {
  expect(stringToDeltaApiFolderType('system')).toBe(DeltaApiFolderType.system)
  expect(stringToDeltaApiFolderType('user')).toBe(DeltaApiFolderType.user)
  expect(stringToDeltaApiFolderType('')).toBeNull()
  expect(stringToDeltaApiFolderType('unknown')).toBeNull()
})
