import { int64 } from '../../../../../../../common/ys'
import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import {
  DeltaApiLabelSymbol,
  DeltaApiLabelType,
} from '../../../../../code/api/entities/delta-api/entities/delta-api-label'
import {
  deltaApiLabelTypeAndSymbolToLabelType,
  int32ToLabelType,
  Label,
  labelFromJSONItem,
  LabelType,
  labelTypeToInt32,
} from '../../../../../code/api/entities/label/label'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import response from '../container/sample.json'

describe(Label, () => {
  it('should convert LabelTypes to Int32', () => {
    expect(labelTypeToInt32(LabelType.user)).toBe(LabelType.user.valueOf())
    expect(labelTypeToInt32(LabelType.system)).toBe(LabelType.system.valueOf())
    expect(labelTypeToInt32(LabelType.important)).toBe(LabelType.important.valueOf())
  })
  it('should convert Int32 to LabelType', () => {
    expect(int32ToLabelType(LabelType.user.valueOf())).toBe(LabelType.user)
    expect(int32ToLabelType(LabelType.system.valueOf())).toBe(LabelType.system)
    expect(int32ToLabelType(LabelType.important.valueOf())).toBe(LabelType.important)
    expect(int32ToLabelType(100)).toBe(LabelType.system)
  })
  it('should provide LabelTypes with specific values', () => {
    expect(LabelType.user).toBe(1)
    expect(LabelType.system).toBe(3)
    expect(LabelType.important).toBe(6)
  })
  it('should be deserializable from JSONItem', () => {
    const element = response.find((item) => item.lid)
    const result = labelFromJSONItem(JSONItemFromJSON(element)! as MapJSONItem)
    expect(result).toStrictEqual(new Label('6', LabelType.important, 'priority_high', 100, 1450, 0xaabbcc, int64(0)))
  })
  it("should use zero as color if it's incorrect", () => {
    const element = response.find((item) => item.lid)
    const badColor = { ...element, color: 'XXYYZZ' }
    const result = labelFromJSONItem(JSONItemFromJSON(badColor)! as MapJSONItem)
    expect(result).toStrictEqual(new Label('6', LabelType.important, 'priority_high', 100, 1450, 0, int64(0)))
  })
  it('should return null if item is malformed', () => {
    const element = response.find((item) => item.lid)
    const result = labelFromJSONItem(JSONItemFromJSON([element]))
    expect(result).toBeNull()
  })
  it('should convert Delta API folder type into FolderType', () => {
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.user, null)).toBe(LabelType.user)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.social, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.system, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.status, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.imap, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.threadWide, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.rpop, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.phone, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.so, null)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(DeltaApiLabelType.so2, null)).toBe(LabelType.system)

    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.attached)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.spam)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.answered)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.recent)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.draft)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.deleted)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.forwarded)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.important)).toBe(LabelType.important)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.forMe)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.noBody)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.noAnswer)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.hasUserLabels)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.seen)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.pinned)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.postmaster)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.mulcaShared)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.imap)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.append)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.copy)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.synced)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.remindNoAnswer)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.notifyNoAnswer)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.remindMessage)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.notifyMessage)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.mute)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.delayedMessage)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.undoMessage)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.hamon)).toBe(LabelType.system)
    expect(deltaApiLabelTypeAndSymbolToLabelType(null, DeltaApiLabelSymbol.encrypted)).toBe(LabelType.system)
  })
})
