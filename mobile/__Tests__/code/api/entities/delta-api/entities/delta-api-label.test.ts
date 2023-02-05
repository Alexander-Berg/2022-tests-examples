import {
  DeltaApiLabel,
  DeltaApiLabelSymbol,
  DeltaApiLabelType,
  stringToDeltaApiLabelSymbol,
  stringToDeltaApiLabelType,
} from '../../../../../../code/api/entities/delta-api/entities/delta-api-label'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import label from './label.json'

describe(DeltaApiLabel, () => {
  it('should parse the result from JSON', () => {
    const result = DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))
    expect(result).not.toBeNull()
    expect(result!.lid).toBe(label.lid)
    expect(result!.name).toBe(label.name)
    expect(result!.color).toBe(label.color)
    expect(result!.type).toBe(DeltaApiLabelType.system)
    expect(result!.symbol).toBe(DeltaApiLabelSymbol.important)
    expect(result!.messagesCount).toBe(label.messagesCount)
    expect(result!.creationTime).toBe(label.creationTime)
    expect(result!.revision).toBe(label.revision)
  })
  it('should return null if JSON is malformed', () => {
    const result = DeltaApiLabel.fromJSONItem(JSONItemFromJSON([label]))
    expect(result).toBeNull()
  })
})

test('stringToDeltaApiLabelSymbol', () => {
  expect(stringToDeltaApiLabelSymbol('attached_label')).toBe(DeltaApiLabelSymbol.attached)
  expect(stringToDeltaApiLabelSymbol('spam_label')).toBe(DeltaApiLabelSymbol.spam)
  expect(stringToDeltaApiLabelSymbol('answered_label')).toBe(DeltaApiLabelSymbol.answered)
  expect(stringToDeltaApiLabelSymbol('recent_label')).toBe(DeltaApiLabelSymbol.recent)
  expect(stringToDeltaApiLabelSymbol('draft_label')).toBe(DeltaApiLabelSymbol.draft)
  expect(stringToDeltaApiLabelSymbol('deleted_label')).toBe(DeltaApiLabelSymbol.deleted)
  expect(stringToDeltaApiLabelSymbol('forwarded_label')).toBe(DeltaApiLabelSymbol.forwarded)
  expect(stringToDeltaApiLabelSymbol('important_label')).toBe(DeltaApiLabelSymbol.important)
  expect(stringToDeltaApiLabelSymbol('forMe_label')).toBe(DeltaApiLabelSymbol.forMe)
  expect(stringToDeltaApiLabelSymbol('noBody_label')).toBe(DeltaApiLabelSymbol.noBody)
  expect(stringToDeltaApiLabelSymbol('noAnswer_label')).toBe(DeltaApiLabelSymbol.noAnswer)
  expect(stringToDeltaApiLabelSymbol('hasUserLabels_label')).toBe(DeltaApiLabelSymbol.hasUserLabels)
  expect(stringToDeltaApiLabelSymbol('seen_label')).toBe(DeltaApiLabelSymbol.seen)
  expect(stringToDeltaApiLabelSymbol('pinned_label')).toBe(DeltaApiLabelSymbol.pinned)
  expect(stringToDeltaApiLabelSymbol('postmaster_label')).toBe(DeltaApiLabelSymbol.postmaster)
  expect(stringToDeltaApiLabelSymbol('mulcaShared_label')).toBe(DeltaApiLabelSymbol.mulcaShared)
  expect(stringToDeltaApiLabelSymbol('imap_label')).toBe(DeltaApiLabelSymbol.imap)
  expect(stringToDeltaApiLabelSymbol('append_label')).toBe(DeltaApiLabelSymbol.append)
  expect(stringToDeltaApiLabelSymbol('copy_label')).toBe(DeltaApiLabelSymbol.copy)
  expect(stringToDeltaApiLabelSymbol('synced_label')).toBe(DeltaApiLabelSymbol.synced)
  expect(stringToDeltaApiLabelSymbol('remindNoAnswer_label')).toBe(DeltaApiLabelSymbol.remindNoAnswer)
  expect(stringToDeltaApiLabelSymbol('notifyNoAnswer_label')).toBe(DeltaApiLabelSymbol.notifyNoAnswer)
  expect(stringToDeltaApiLabelSymbol('remindMessage_label')).toBe(DeltaApiLabelSymbol.remindMessage)
  expect(stringToDeltaApiLabelSymbol('notifyMessage_label')).toBe(DeltaApiLabelSymbol.notifyMessage)
  expect(stringToDeltaApiLabelSymbol('mute_label')).toBe(DeltaApiLabelSymbol.mute)
  expect(stringToDeltaApiLabelSymbol('delayed_message')).toBe(DeltaApiLabelSymbol.delayedMessage)
  expect(stringToDeltaApiLabelSymbol('undo_message')).toBe(DeltaApiLabelSymbol.undoMessage)
  expect(stringToDeltaApiLabelSymbol('hamon_label')).toBe(DeltaApiLabelSymbol.hamon)
  expect(stringToDeltaApiLabelSymbol('encrypted_label')).toBe(DeltaApiLabelSymbol.encrypted)
  expect(stringToDeltaApiLabelSymbol('')).toBeNull()
  expect(stringToDeltaApiLabelSymbol('unknown')).toBeNull()
})
test('stringToDeltaApiLabelType', () => {
  expect(stringToDeltaApiLabelType('user')).toBe(DeltaApiLabelType.user)
  expect(stringToDeltaApiLabelType('social')).toBe(DeltaApiLabelType.social)
  expect(stringToDeltaApiLabelType('system')).toBe(DeltaApiLabelType.system)
  expect(stringToDeltaApiLabelType('status')).toBe(DeltaApiLabelType.status)
  expect(stringToDeltaApiLabelType('imap')).toBe(DeltaApiLabelType.imap)
  expect(stringToDeltaApiLabelType('threadWide')).toBe(DeltaApiLabelType.threadWide)
  expect(stringToDeltaApiLabelType('rpop')).toBe(DeltaApiLabelType.rpop)
  expect(stringToDeltaApiLabelType('phone')).toBe(DeltaApiLabelType.phone)
  expect(stringToDeltaApiLabelType('so')).toBe(DeltaApiLabelType.so)
  expect(stringToDeltaApiLabelType('so2')).toBe(DeltaApiLabelType.so2)
  expect(stringToDeltaApiLabelType('')).toBeNull()
  expect(stringToDeltaApiLabelType('unknown')).toBeNull()
})
