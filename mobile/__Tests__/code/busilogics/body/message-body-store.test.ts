import { MockFileSystem, mockLogger } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { Log } from '../../../../../common/code/logging/logger'
import { reject, resolve } from '../../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { MessageBodyInfo, MessageBodyPayload } from '../../../../../mapi/code/api/entities/body/message-body'
import { Encoding } from '../../../../../common/code/file-system/file-system-types'
import {
  EmptyMessageBodyStoreNotifier,
  MessageBodyStore,
  MessageBodyStoreNotifier,
} from '../../../../code/busilogics/body/message-body-store'
import { rejected } from '../../../__helpers__/test-failure'

describe(MessageBodyStore, () => {
  beforeEach(() => {
    mockLogger()
  })
  afterEach(jest.restoreAllMocks)

  describe('getMessageDirectoryPath & getMessageBodyPath', () => {
    it('should return message body file & directory', () => {
      const fs = MockFileSystem()
      const bodyStore = new MessageBodyStore(fs, '/bodies')
      expect(bodyStore.getMessageDirectoryPath(int64(1))).toBe('/bodies/1')
      expect(bodyStore.getMessageBodyPath(int64(1))).toBe('/bodies/1/body')
    })
  })

  describe('getMessageBodyContent', () => {
    it('should return body content', (done) => {
      const fs = MockFileSystem({
        exists: jest.fn().mockReturnValue(resolve(true)),
        readAsString: jest.fn().mockReturnValue(resolve('content')),
      })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.getMessageBodyContent(int64(1)).then((content) => {
        expect(content).toBe('content')
        done()
      })
    })
    it('should return null body content if file is missing', (done) => {
      const fs = MockFileSystem({
        exists: jest.fn().mockReturnValue(resolve(false)),
        readAsString: jest.fn().mockReturnValue(resolve('content')),
      })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.getMessageBodyContent(int64(1)).then((content) => {
        expect(content).toBeNull()
        done()
      })
    })
  })

  describe('storeMessageBodies', () => {
    it('should return immediatelly if empty list is passed', (done) => {
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({
        writeAsString,
      })
      const bodyStore = new MessageBodyStore(fs, '/bodies')
      bodyStore.storeMessageBodies([]).then(() => {
        expect(writeAsString).not.toBeCalled()
        done()
      })
    })
    it('should store empty message body', (done) => {
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({
        makeDirectory: jest.fn().mockReturnValue(resolve(getVoid())),
        exists: jest.fn().mockReturnValue(resolve(false)),
        writeAsString,
      })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore
        .storeMessageBodies([
          new MessageBodyPayload(new MessageBodyInfo(int64(1), [], [], '', ''), [], '', '', false, []),
        ])
        .then(() => {
          expect(writeAsString).toBeCalledWith('/bodies/1/body', '', Encoding.Utf8, true)
          done()
        })
    })
    it('should store message bodies', (done) => {
      const notify = jest.fn()
      const notifier: MessageBodyStoreNotifier = { notifyUpdateOfMessageContent: notify }
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({
        makeDirectory: jest
          .fn()
          .mockReturnValueOnce(resolve(getVoid()))
          .mockReturnValueOnce(reject(new YSError('NO MATTER'))),
        exists: jest.fn().mockReturnValue(resolve(false)),
        writeAsString,
      })
      const bodyStore = new MessageBodyStore(fs, '/bodies', notifier)

      expect.assertions(4)
      bodyStore
        .storeMessageBodies([
          new MessageBodyPayload(
            new MessageBodyInfo(int64(301), [], [], '', ''),
            [
              { hid: '', content: 'a', contentType: '', lang: '' },
              { hid: '', content: null, contentType: '', lang: '' },
              { hid: '', content: 'b', contentType: '', lang: '' },
            ],
            '',
            '',
            false,
            [],
          ),
          new MessageBodyPayload(
            new MessageBodyInfo(int64(302), [], [], '', ''),
            [
              { hid: '', content: 'c', contentType: '', lang: '' },
              { hid: '', content: 'd', contentType: '', lang: '' },
              { hid: '', content: null, contentType: '', lang: '' },
            ],
            '',
            '',
            false,
            [],
          ),
        ])
        .then(() => {
          expect(writeAsString.mock.calls[0]).toEqual(['/bodies/301/body', 'ab', Encoding.Utf8, true])
          expect(writeAsString.mock.calls[1]).toEqual(['/bodies/302/body', 'cd', Encoding.Utf8, true])
          expect(notify.mock.calls[0]).toEqual(['/bodies/301/body'])
          expect(notify.mock.calls[1]).toEqual(['/bodies/302/body'])
          done()
        })
    })
  })

  describe('deleteMessageDirectory', () => {
    it('should handle successfull message directory deletion', (done) => {
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ delete: mockDelete })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.deleteMessageDirectory(int64(1)).then((result) => {
        expect(mockDelete).toHaveBeenCalledWith('/bodies/1', true)
        done()
      })
    })
  })

  describe('deleteMessageBody', () => {
    it('should handle successfull message body deletion', (done) => {
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ delete: mockDelete })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.deleteMessageBody(int64(1)).then(() => {
        expect(mockDelete).toHaveBeenCalledWith('/bodies/1/body', true)
        done()
      })
    })
  })

  describe('updateBodiesTimestamps', () => {
    beforeEach(() => {
      mockLogger()
    })
    afterEach(jest.restoreAllMocks)

    it('should move existing timestamp file if timestamp value was updated', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve(['/bodies/1/timestamp_1']))
      const move = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ listDirectory, move })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore
        .updateBodiesTimestamps(
          [
            new MessageBodyPayload(new MessageBodyInfo(int64(1), [], [], '', ''), [], '', '', false, []),
            new MessageBodyPayload(new MessageBodyInfo(int64(2), [], [], '', ''), [], '', '', false, []),
          ],
          new Map([[int64(1), int64(100)]]),
        )
        .then((_) => {
          expect(move).toBeCalledWith('/bodies/1/timestamp_1', '/bodies/1/timestamp_100')
          done()
        })
    })
    it('should create new timestamp file if did not exist', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve([]))
      const createNewFile = jest.fn().mockReturnValue(resolve(true))
      const fs = MockFileSystem({ listDirectory, createNewFile })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore
        .updateBodiesTimestamps(
          [
            new MessageBodyPayload(new MessageBodyInfo(int64(3), [], [], '', ''), [], '', '', false, []),
            new MessageBodyPayload(new MessageBodyInfo(int64(4), [], [], '', ''), [], '', '', false, []),
          ],
          new Map([[int64(3), int64(300)]]),
        )
        .then((_) => {
          expect(createNewFile).toBeCalledWith('/bodies/3/timestamp_300')
          done()
        })
    })
    it('should not fail if unable to create timestamp file', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve([]))
      const createNewFile = jest.fn().mockReturnValue(rejected('FAILED'))
      const fs = MockFileSystem({ listDirectory, createNewFile })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore
        .updateBodiesTimestamps(
          [new MessageBodyPayload(new MessageBodyInfo(int64(3), [], [], '', ''), [], '', '', false, [])],
          new Map([[int64(3), int64(300)]]),
        )
        .then((_) => {
          expect(Log.getDefaultLogger()!.error).toBeCalledWith(expect.stringContaining('Error creating file: '))
          expect(createNewFile).toBeCalledWith('/bodies/3/timestamp_300')
          done()
        })
    })
    it('should not fail if unable to move timestamp file', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve(['/bodies/1/timestamp_1']))
      const move = jest.fn().mockReturnValue(rejected('FAILED'))
      const fs = MockFileSystem({ listDirectory, move })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore
        .updateBodiesTimestamps(
          [
            new MessageBodyPayload(new MessageBodyInfo(int64(1), [], [], '', ''), [], '', '', false, []),
            new MessageBodyPayload(new MessageBodyInfo(int64(2), [], [], '', ''), [], '', '', false, []),
          ],
          new Map([[int64(1), int64(100)]]),
        )
        .then((_) => {
          expect(Log.getDefaultLogger()!.error).toBeCalledWith(expect.stringContaining('Error moving file: '))
          done()
        })
    })
  })
  describe('storeMessageBodyContent', () => {
    it('should check message directory existence and create if does not exist', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectory = jest.fn().mockReturnValue(resolve(getVoid()))
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ exists, makeDirectory, writeAsString })

      const notifier: MessageBodyStoreNotifier = {
        notifyUpdateOfMessageContent: jest.fn(),
      }
      const store = new MessageBodyStore(fs, '/bodies', notifier)
      const directory = store.getMessageDirectoryPath(int64(1))
      expect.assertions(4)
      store.storeMessageBodyContent(int64(1), 'content').then((_) => {
        expect(exists).toBeCalledWith(directory)
        expect(makeDirectory).toBeCalledWith(directory, true)
        expect(writeAsString).toBeCalledWith(store.getMessageBodyPath(int64(1)), 'content', Encoding.Utf8, true)
        expect(notifier.notifyUpdateOfMessageContent).toBeCalledWith(store.getMessageBodyPath(int64(1)))
        done()
      })
    })
    it('should check message directory existence and skip storing if exists', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(true))
      const makeDirectory = jest.fn().mockReturnValue(resolve(getVoid()))
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ exists, makeDirectory, writeAsString })

      const store = new MessageBodyStore(fs, '/bodies')
      const directory = store.getMessageDirectoryPath(int64(1))
      expect.assertions(3)
      store.storeMessageBodyContent(int64(1), 'content').then((_) => {
        expect(exists).toBeCalledWith(directory)
        expect(makeDirectory).not.toBeCalled()
        expect(writeAsString).not.toBeCalledWith()
        done()
      })
    })
    it('should not fail if unable to make a directory', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectory = jest.fn().mockReturnValue(rejected('FAILED'))
      const writeAsString = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ exists, makeDirectory, writeAsString })

      const store = new MessageBodyStore(fs, '/bodies')
      const directory = store.getMessageDirectoryPath(int64(1))
      expect.assertions(3)
      store.storeMessageBodyContent(int64(1), 'content').then((_) => {
        expect(exists).toBeCalledWith(directory)
        expect(makeDirectory).toBeCalledWith(directory, true)
        expect(writeAsString).not.toBeCalledWith()
        done()
      })
    })
    it('should fail if unable to write the file', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectory = jest.fn().mockReturnValue(resolve(getVoid()))
      const writeAsString = jest.fn().mockReturnValue(rejected('FAILED'))
      const fs = MockFileSystem({ exists, makeDirectory, writeAsString })

      const store = new MessageBodyStore(fs, '/bodies')
      const directory = store.getMessageDirectoryPath(int64(1))
      expect.assertions(4)
      store.storeMessageBodyContent(int64(1), 'content').failed((err) => {
        expect(exists).toBeCalledWith(directory)
        expect(makeDirectory).toBeCalledWith(directory, true)
        expect(writeAsString).toBeCalledWith(store.getMessageBodyPath(int64(1)), 'content', Encoding.Utf8, true)
        expect(err.message).toBe('FAILED')
        done()
      })
    })
  })

  describe('deleteOldDraftBodies', () => {
    it('should do nothing if timestamp did not change', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve(['/bodies/mid/timestamp_1']))
      const mockDelete = jest.fn()
      const fs = MockFileSystem({ listDirectory, delete: mockDelete })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.deleteOldDraftBodies(new Map([[int64(1), int64(1)]])).then((mids) => {
        expect(mids).toEqual([])
        expect(mockDelete).not.toBeCalled()
        done()
      })
    })
    it('should delete body if timestamp was updated', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve(['/bodies/mid/timestamp_1']))
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ listDirectory, delete: mockDelete })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.deleteOldDraftBodies(new Map([[int64(1), int64(2)]])).then((mids) => {
        expect(mids).toEqual([int64(1)])
        expect(mockDelete).toBeCalledWith('/bodies/1', true)
        done()
      })
    })
    it('should delete body if timestamp was not found', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve([]))
      const mockDelete = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({ listDirectory, delete: mockDelete })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.deleteOldDraftBodies(new Map([[int64(1), int64(2)]])).then((mids) => {
        expect(mids).toEqual([int64(1)])
        expect(mockDelete).toBeCalledWith('/bodies/1', true)
        done()
      })
    })
  })

  describe('findTimestampFile', () => {
    it('should return timestamp file if it exists', (done) => {
      const listDirectory = jest
        .fn()
        .mockReturnValue(resolve(['/bodies/1/foo', '/bodies/1/bar', '/bodies/1/timestamp_1']))
      const fs = MockFileSystem({ listDirectory })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.findTimestampFile(int64(1)).then((file) => {
        expect(file).toBe('/bodies/1/timestamp_1')
        done()
      })
    })
    it('should return null if timestamp file does not exist', (done) => {
      const listDirectory = jest.fn().mockReturnValue(resolve(['/bodies/1/foo', '/bodies/1/bar']))
      const fs = MockFileSystem({ listDirectory })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.findTimestampFile(int64(1)).then((file) => {
        expect(file).toBe(null)
        done()
      })
    })
    it('should return null if "listDirectory" rejects', (done) => {
      const listDirectory = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const fs = MockFileSystem({ listDirectory })
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      bodyStore.findTimestampFile(int64(1)).then((file) => {
        expect(file).toBe(null)
        done()
      })
    })
  })

  describe('getTimestampFilePath', () => {
    it('should return timestamp file path', () => {
      const fs = MockFileSystem()
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      expect(bodyStore.getTimestampFilePath(int64(1), int64(1))).toBe('/bodies/1/timestamp_1')
    })
  })

  describe('getTimestampFromFile', () => {
    it('should return "0" for null input value', () => {
      const fs = MockFileSystem()
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      expect(bodyStore.getTimestampFromFile(null)).toBe(int64(0))
    })
    it('should return "0" for not expected file names', () => {
      const fs = MockFileSystem()
      const bodyStore = new MessageBodyStore(fs, '/bodies')

      expect(bodyStore.getTimestampFromFile('unexpected')).toBe(int64(0))
    })
    it('should actually parse timestamp from file name', () => {
      const fs = MockFileSystem()
      const bodyStore = new MessageBodyStore(fs, '/bodies')
      expect(bodyStore.getTimestampFromFile('/bodies/1/timestamp_1')).toBe(int64(1))
    })
  })
})

describe(EmptyMessageBodyStoreNotifier, () => {
  it('should have empty notification method', () => {
    const notifier = new EmptyMessageBodyStoreNotifier()
    expect(notifier.notifyUpdateOfMessageContent('')).toBe(getVoid())
  })
})
