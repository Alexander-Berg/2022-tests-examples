import { reject, resolve } from '../../../../../../common/xpromise-support'
import { YSError } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../../common/code/result/result'
import { containerFromJSONItem } from '../../../../../mapi/code/api/entities/container/container'
import { ContainersRequest } from '../../../../../mapi/code/api/entities/container/containers-request'
import { ContainersSync } from '../../../../code/busilogics/containers/containers-sync'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Labels } from '../../../../code/busilogics/labels/labels'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockHighPrecisionTimer, MockStorage, MockWithinTransaction } from '../../../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import response from '../../../../../mapi/__tests__/code/api/entities/container/sample.json'

describe(ContainersSync, () => {
  it('should make a network request', (done) => {
    const execute = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
    const network = MockNetwork({ execute })
    const storage = MockStorage()
    const idSupport = new TestIDSupport()
    const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
    const labelsManager = new Labels(storage, idSupport)
    const sync = new ContainersSync(network, storage, folders, labelsManager)
    expect.assertions(1)
    sync.synchronize().failed(() => {
      expect(execute).toBeCalledWith(new ContainersRequest())
      done()
    })
  })
  it('should fail if the network request fails', (done) => {
    const execute = jest.fn().mockReturnValue(reject(new YSError('REQUEST FAILED')))
    const network = MockNetwork({ execute })
    const storage = MockStorage()
    const idSupport = new TestIDSupport()
    const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
    const labelsManager = new Labels(storage, idSupport)
    const sync = new ContainersSync(network, storage, folders, labelsManager)
    expect.assertions(1)
    sync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('REQUEST FAILED'))
      done()
    })
  })
  it('should fail if the response is malformed', (done) => {
    const execute = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({ response })))
    const network = MockNetwork({ execute })
    const storage = MockStorage()
    const idSupport = new TestIDSupport()
    const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
    const labelsManager = new Labels(storage, idSupport)
    const sync = new ContainersSync(network, storage, folders, labelsManager)
    expect.assertions(1)
    sync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('JSON Item parsing failed for entity Container (xlist)'))
      done()
    })
  })
  it('should fail labels and folders storing operations if transaction was not created', (done) => {
    const jsonItem = JSONItemFromJSON(response)
    const execute = jest.fn().mockReturnValue(resolve(jsonItem))
    const network = MockNetwork({ execute })
    const storage = MockStorage({
      withinTransaction: jest
        .fn<XPromise<any>, [boolean, any]>((_, block) => block())
        .mockReturnValue(reject(new YSError('TRANSACTION CREATION FAILED'))),
    })

    const idSupport = new TestIDSupport()
    const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
    const labelsManager = new Labels(storage, idSupport)
    const sync = new ContainersSync(network, storage, folders, labelsManager)
    expect.assertions(1)
    sync.synchronize().failed((error) => {
      expect(error).toStrictEqual(new YSError('TRANSACTION CREATION FAILED'))
      done()
    })
  })
  it('should call labels, folders, and mailbox revision storing operations within transaction', (done) => {
    const jsonItem = JSONItemFromJSON(response)
    const container = containerFromJSONItem(jsonItem)!
    const execute = jest.fn().mockReturnValue(resolve(jsonItem))
    const network = MockNetwork({ execute })
    const storage = MockStorage({
      withinTransaction: MockWithinTransaction<any>(),
    })

    const folders = new Folders(storage, new MockSharedPreferences(), new TestIDSupport(), MockHighPrecisionTimer())
    const replaceFoldersSpy = jest.spyOn(folders, 'replaceFolders').mockReturnValue(resolve(getVoid()))
    const insertFolderDefaultsSpy = jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(resolve(getVoid()))
    const cleanUpOrphanFolderDefaultsSpy = jest
      .spyOn(folders, 'cleanupOrphanFoldersEntities')
      .mockReturnValue(resolve(getVoid()))
    const updateOverflowCountersForAllFoldersSpy = jest
      .spyOn(folders, 'updateOverflowCountersForAllFolders')
      .mockReturnValue(resolve(getVoid()))
    const cleanupOrphanFoldersEntitiesSpy = jest
      .spyOn(folders, 'cleanupThreadOrMessagesNotInXlist')
      .mockReturnValue(resolve(getVoid()))

    const labelsManager = new Labels(storage, new TestIDSupport())
    const replaceLabelsSpy = jest.spyOn(labelsManager, 'replaceLabels').mockReturnValue(resolve(getVoid()))

    const sync = new ContainersSync(network, storage, folders, labelsManager)
    expect.assertions(8)
    sync.synchronize().then((res) => {
      expect(res).not.toBeNull()
      expect(res!).toStrictEqual(container)
      expect(replaceLabelsSpy).toBeCalledWith(container.labels)
      expect(replaceFoldersSpy).toBeCalledWith(container.folders)
      expect(insertFolderDefaultsSpy).toBeCalledWith(container.folders, false, false)
      expect(cleanUpOrphanFolderDefaultsSpy).toBeCalledWith(container.folders)
      expect(updateOverflowCountersForAllFoldersSpy).toBeCalled()
      expect(cleanupOrphanFoldersEntitiesSpy).toBeCalledWith(container.folders)
      done()
    })
  })
})
