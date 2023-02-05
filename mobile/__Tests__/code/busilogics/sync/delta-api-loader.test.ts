import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { mockLogger, MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { DeltaApiCopyItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-copy-item'
import { DeltaApiDeleteItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-delete-item'
import { DeltaApiFolderCountersUpdateItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder-counters-update-item'
import { DeltaApiFolderCreateItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder-create-item'
import { DeltaApiFolderDeleteItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder-delete-item'
import { DeltaApiFolderModifyItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder-modify-item'
import { DeltaApiItemKind } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-item-kind'
import { DeltaApiLabelCreateItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-label-create-item'
import { DeltaApiLabelDeleteItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-label-delete-item'
import { DeltaApiLabelModifyItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-label-modify-item'
import { DeltaApiMoveItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-move-item'
import { DeltaApiMoveToTabItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-move-to-tab-item'
import { DeltaApiQuickSaveItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-quick-save-item'
import { DeltaApiStoreItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-store-item'
import { DeltaApiThreadsJoinItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-threads-join-item'
import { DeltaApiUpdateItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-update-item'
import { DeltaApiLoader } from '../../../../code/busilogics/sync/delta-api-loader'
import { Registry } from '../../../../code/registry'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { rejected } from '../../../__helpers__/test-failure'
import { clone } from '../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(DeltaApiLoader, () => {
  beforeAll(() => mockLogger())
  afterAll(Registry.drop)

  it('should load and parse Delta API data', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(sample))),
    })
    const jsonItem = (index: number) => JSONItemFromJSON(sample.changes[index].value[0])!

    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(47)
    loader.load(101, 100).then((res) => {
      expect(res).not.toBeNull()
      expect(res!.payload).toHaveLength(15)

      expect(res!.payload[0].kind).toBe(DeltaApiItemKind.store)
      expect(res!.payload[0].revision).toBe(101)
      expect(res!.payload[0].values).toStrictEqual([DeltaApiStoreItem.fromJSONItem(jsonItem(0))])

      expect(res!.payload[1].kind).toBe(DeltaApiItemKind.delete)
      expect(res!.payload[1].revision).toBe(102)
      expect(res!.payload[1].values).toStrictEqual([DeltaApiDeleteItem.fromJSONItem(jsonItem(1))])

      expect(res!.payload[2].kind).toBe(DeltaApiItemKind.update)
      expect(res!.payload[2].revision).toBe(103)
      expect(res!.payload[2].values).toStrictEqual([DeltaApiUpdateItem.fromJSONItem(jsonItem(2))])

      expect(res!.payload[3].kind).toBe(DeltaApiItemKind.copy)
      expect(res!.payload[3].revision).toBe(104)
      expect(res!.payload[3].values).toStrictEqual([DeltaApiCopyItem.fromJSONItem(jsonItem(3))])

      expect(res!.payload[4].kind).toBe(DeltaApiItemKind.move)
      expect(res!.payload[4].revision).toBe(105)
      expect(res!.payload[4].values).toStrictEqual([DeltaApiMoveItem.fromJSONItem(jsonItem(4))])

      expect(res!.payload[5].kind).toBe(DeltaApiItemKind.quickSave)
      expect(res!.payload[5].revision).toBe(106)
      expect(res!.payload[5].values).toStrictEqual([DeltaApiQuickSaveItem.fromJSONItem(jsonItem(5))])

      expect(res!.payload[6].kind).toBe(DeltaApiItemKind.threadsJoin)
      expect(res!.payload[6].revision).toBe(107)
      expect(res!.payload[6].values).toStrictEqual([DeltaApiThreadsJoinItem.fromJSONItem(jsonItem(6))])

      expect(res!.payload[7].kind).toBe(DeltaApiItemKind.labelCreate)
      expect(res!.payload[7].revision).toBe(108)
      expect(res!.payload[7].values).toStrictEqual([DeltaApiLabelCreateItem.fromJSONItem(jsonItem(7))])

      expect(res!.payload[8].kind).toBe(DeltaApiItemKind.labelDelete)
      expect(res!.payload[8].revision).toBe(109)
      expect(res!.payload[8].values).toStrictEqual([DeltaApiLabelDeleteItem.fromJSONItem(jsonItem(8))])

      expect(res!.payload[9].kind).toBe(DeltaApiItemKind.labelModify)
      expect(res!.payload[9].revision).toBe(110)
      expect(res!.payload[9].values).toStrictEqual([DeltaApiLabelModifyItem.fromJSONItem(jsonItem(9))])

      expect(res!.payload[10].kind).toBe(DeltaApiItemKind.folderCreate)
      expect(res!.payload[10].revision).toBe(111)
      expect(res!.payload[10].values).toStrictEqual([DeltaApiFolderCreateItem.fromJSONItem(jsonItem(10))])

      expect(res!.payload[11].kind).toBe(DeltaApiItemKind.folderDelete)
      expect(res!.payload[11].revision).toBe(112)
      expect(res!.payload[11].values).toStrictEqual([DeltaApiFolderDeleteItem.fromJSONItem(jsonItem(11))])

      expect(res!.payload[12].kind).toBe(DeltaApiItemKind.folderModify)
      expect(res!.payload[12].revision).toBe(113)
      expect(res!.payload[12].values).toStrictEqual([DeltaApiFolderModifyItem.fromJSONItem(jsonItem(12))])

      expect(res!.payload[13].kind).toBe(DeltaApiItemKind.moveToTab)
      expect(res!.payload[13].revision).toBe(114)
      expect(res!.payload[13].values).toStrictEqual([DeltaApiMoveToTabItem.fromJSONItem(jsonItem(13))])

      expect(res!.payload[14].kind).toBe(DeltaApiItemKind.folderCountersUpdate)
      expect(res!.payload[14].revision).toBe(114)
      expect(res!.payload[14].values).toStrictEqual([
        DeltaApiFolderCountersUpdateItem.fromJSONItem(JSONItemFromJSON(sample.changes[14].value[0])!),
        DeltaApiFolderCountersUpdateItem.fromJSONItem(JSONItemFromJSON(sample.changes[14].value[1])!),
      ])
      done()
    })
  })
  it('should return null if JSON is malformed', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON([sample]))),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(1)
    loader.load(101, 100).failed((e) => {
      expect(e!.message).toBe('JSON Item parsing failed for entity Delta API')
      done()
    })
  })
  it('should return empty payload if changes key is not found', (done) => {
    const malformed = clone(sample)
    delete malformed.changes
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(malformed))),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(2)
    loader.load(101, 100).then((res) => {
      expect(res).not.toBeNull()
      expect(res!.payload).toHaveLength(0)
      done()
    })
  })
  it('should skip malformed changes items if changes item is malformed', (done) => {
    const malformed = clone(sample)
    malformed.changes[11] = [malformed.changes[11]] // folder-delete
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(malformed))),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(3)
    loader.load(101, 100).then((res) => {
      expect(res).not.toBeNull()
      expect(res!.payload).toHaveLength(14)
      expect(res!.payload.find((item) => item.kind === DeltaApiItemKind.folderDelete)).toBeUndefined()
      done()
    })
  })
  it('should skip unknown types of change items', (done) => {
    const malformed = clone(sample)
    malformed.changes.push({
      revision: 115,
      type: 'unknown',
      value: ['lid1'],
    })
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(malformed))),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(2)
    loader.load(101, 100).then((res) => {
      expect(res).not.toBeNull()
      expect(res!.payload).toHaveLength(15)
      done()
    })
  })
  it('should skip malformed values if value item is malformed', (done) => {
    const malformed = clone(sample)
    malformed.changes[6].value[0].mid = 'invalid value' // threads-join
    malformed.changes[11].value = [malformed.changes[11].value] // folder-delete
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(malformed))),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(4)
    loader.load(101, 100).then((res) => {
      expect(res).not.toBeNull()
      expect(res!.payload).toHaveLength(15)
      expect(res!.payload.find((item) => item.kind === DeltaApiItemKind.threadsJoin)!.values).toHaveLength(0)
      expect(res!.payload.find((item) => item.kind === DeltaApiItemKind.folderDelete)!.values).toHaveLength(0)
      done()
    })
  })
  it('should fail if network fails', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(rejected('FAILED')),
    })
    const loader = new DeltaApiLoader(network, int64(1234))
    expect.assertions(1)
    loader.load(101, 100).failed((e) => {
      expect(e!.message).toBe('FAILED')
      done()
    })
  })
})
