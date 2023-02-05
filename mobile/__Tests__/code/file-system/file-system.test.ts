import { reject, resolve } from '../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../common/ys'
import { getVoid } from '../../../code/result/result'
import { FileSystem } from '../../../code/file-system/file-system'
import {
  CopyParameters,
  DeleteParameters,
  FileSystemImplementation,
  MakeDirectoryParameters,
  MoveParameters,
  ReadParameters,
  WriteParameters,
} from '../../../code/file-system/file-system-implementation'
import { Encoding, HashType, ItemInfo } from '../../../code/file-system/file-system-types'
import { MobileFileSystemPath } from '../../../code/file-system/mobile-file-system-path'

function MockFileSystemImplementation(patch: Partial<FileSystemImplementation> = {}): FileSystemImplementation {
  return Object.assign(
    {},
    {
      getItemInfo: jest.fn(),
      exists: jest.fn(),
      listDirectory: jest.fn(),
      readAsStringWithParams: jest.fn(),
      writeAsStringWithParams: jest.fn(),
      readArrayBufferWithParams: jest.fn(),
      writeArrayBufferWithParams: jest.fn(),
      deleteWithParams: jest.fn(),
      moveWithParams: jest.fn(),
      copyWithParams: jest.fn(),
      makeDirectoryWithParams: jest.fn(),
      hash: jest.fn(),
    },
    patch,
  )
}

function buildFileSystem(patchImpl: Partial<FileSystemImplementation> = {}): FileSystem {
  const impl = MockFileSystemImplementation(patchImpl)
  const fs = new FileSystem(
    {
      documentDirectory: '/documents',
      cachesDirectory: '/caches',
    },
    new MobileFileSystemPath(),
    impl,
  )
  return fs
}

describe(FileSystem, () => {
  it('has common attributes', () => {
    const fs = buildFileSystem()

    expect(fs.directories).toStrictEqual({
      documentDirectory: '/documents',
      cachesDirectory: '/caches',
    })
    expect(fs.path).toEqual(expect.any(MobileFileSystemPath))
  })
  it('passes "getItemInfo" result', (done) => {
    const expected = new ItemInfo('', false, int64(0), int64(0))
    const getItemInfo = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      getItemInfo,
    })

    fs.getItemInfo('path').then((result) => {
      expect(result).toBe(expected)
      expect(getItemInfo).toBeCalledWith('path')
      done()
    })
  })
  it('passes "exists" result', (done) => {
    const expected = true
    const exists = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      exists,
    })

    fs.exists('path').then((result) => {
      expect(result).toBe(expected)
      expect(exists).toBeCalledWith('path')
      done()
    })
  })
  it('passes "listDirectory" result', (done) => {
    const expected = ['a', 'b']
    const listDirectory = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      listDirectory,
    })

    fs.listDirectory('path').then((result) => {
      expect(result).toBe(expected)
      expect(listDirectory).toBeCalledWith('path')
      done()
    })
  })
  it('passes "readAsString" result', (done) => {
    const expected = 'content'
    const readAsStringWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      readAsStringWithParams,
    })

    fs.readAsString('path').then((result) => {
      expect(result).toBe(expected)
      expect(readAsStringWithParams).toBeCalledWith('path', new ReadParameters(null, null, Encoding.Utf8))
      done()
    })
  })
  it('passes "writeAsString" result', (done) => {
    const expected = getVoid()
    const writeAsStringWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      writeAsStringWithParams,
    })

    fs.writeAsString('path', 'content').then((result) => {
      expect(result).toBe(expected)
      expect(writeAsStringWithParams).toBeCalledWith('path', 'content', new WriteParameters(false, Encoding.Utf8))
      done()
    })
  })
  it('passes "readArrayBuffer" result', (done) => {
    const expected = 'content'
    const readArrayBufferWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      readArrayBufferWithParams,
    })

    fs.readArrayBuffer('path').then((result) => {
      expect(result).toBe(expected)
      expect(readArrayBufferWithParams).toBeCalledWith('path', new ReadParameters(null, null))
      done()
    })
  })
  it('passes "writeArrayBuffer" result', (done) => {
    const expected = getVoid()
    const writeArrayBufferWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      writeArrayBufferWithParams,
    })

    const str = 'content'
    const buf = new ArrayBuffer(str.length * 2) // 2 bytes for each char
    const bufView = new Uint16Array(buf)
    for (let i = 0, strLen = str.length; i < strLen; i++) {
      bufView[i] = str.charCodeAt(i)
    }

    fs.writeArrayBuffer('path', buf).then((result) => {
      expect(result).toBe(expected)
      expect(writeArrayBufferWithParams).toBeCalledWith('path', buf, new WriteParameters(false))
      done()
    })
  })
  it('passes "delete" result', (done) => {
    const expected = getVoid()
    const deleteWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      deleteWithParams,
    })

    fs.delete('path').then((result) => {
      expect(result).toBe(expected)
      expect(deleteWithParams).toBeCalledWith('path', new DeleteParameters(false))
      done()
    })
  })
  it('passes "move" result', (done) => {
    const expected = getVoid()
    const moveWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      moveWithParams,
    })

    fs.move('path1', 'path2').then((result) => {
      expect(result).toBe(expected)
      expect(moveWithParams).toBeCalledWith('path1', 'path2', new MoveParameters(true, false))
      done()
    })
  })
  it('passes "copy" result', (done) => {
    const expected = getVoid()
    const copyWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      copyWithParams,
    })

    fs.copy('path1', 'path2').then((result) => {
      expect(result).toBe(expected)
      expect(copyWithParams).toBeCalledWith('path1', 'path2', new CopyParameters(true))
      done()
    })
  })
  it('passes "makeDirectory" result', (done) => {
    const expected = getVoid()
    const makeDirectoryWithParams = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      makeDirectoryWithParams,
    })

    fs.makeDirectory('path').then((result) => {
      expect(result).toBe(expected)
      expect(makeDirectoryWithParams).toBeCalledWith('path', new MakeDirectoryParameters(true))
      done()
    })
  })
  it('passes "hash" result', (done) => {
    const expected = 'hash'
    const hash = jest.fn().mockReturnValue(resolve(expected))
    const fs = buildFileSystem({
      hash,
    })

    fs.hash('path', HashType.Md5).then((result) => {
      expect(result).toBe(expected)
      expect(hash).toBeCalledWith('path', HashType.Md5)
      done()
    })
  })
  describe('createNewFile', () => {
    it('does nothing if file exists', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(true))
      const fs = buildFileSystem({
        exists,
      })

      fs.createNewFile('path').then((result) => {
        expect(exists).toBeCalledWith('path')
        expect(result).toBe(false)
        done()
      })
    })
    it('creates file if "make directory" succeeds', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectoryWithParams = jest.fn().mockReturnValue(resolve(getVoid()))
      const writeAsStringWithParams = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = buildFileSystem({
        exists,
        makeDirectoryWithParams,
        writeAsStringWithParams,
      })

      fs.createNewFile('/parent/path').then((result) => {
        expect(exists).toBeCalledWith('/parent/path')
        expect(makeDirectoryWithParams).toBeCalledWith('/parent', new MakeDirectoryParameters(true))
        expect(writeAsStringWithParams).toBeCalledWith('/parent/path', '', new WriteParameters(false, Encoding.Utf8))
        expect(result).toBe(true)
        done()
      })
    })
    it('creates file if "make directory" fails', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectoryWithParams = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const writeAsStringWithParams = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = buildFileSystem({
        exists,
        makeDirectoryWithParams,
        writeAsStringWithParams,
      })

      fs.createNewFile('/parent/path').then((result) => {
        expect(exists).toBeCalledWith('/parent/path')
        expect(makeDirectoryWithParams).toBeCalledWith('/parent', new MakeDirectoryParameters(true))
        expect(writeAsStringWithParams).toBeCalledWith('/parent/path', '', new WriteParameters(false, Encoding.Utf8))
        expect(result).toBe(true)
        done()
      })
    })
  })
  describe('ensureParentFolderExists', () => {
    it('should skip folder creation if it exists', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(true))
      const makeDirectoryWithParams = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = buildFileSystem({
        exists,
        makeDirectoryWithParams,
      })

      expect.assertions(2)
      fs.ensureFolderExists('/dir').then(() => {
        expect(exists).toBeCalledWith('/dir')
        expect(makeDirectoryWithParams).not.toBeCalled()
        done()
      })
    })
    it('should create folder if it does not exist', (done) => {
      const exists = jest.fn().mockReturnValue(resolve(false))
      const makeDirectoryWithParams = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = buildFileSystem({
        exists,
        makeDirectoryWithParams,
      })

      expect.assertions(2)
      fs.ensureFolderExists('/dir').then(() => {
        expect(exists).toBeCalledWith('/dir')
        expect(makeDirectoryWithParams).toBeCalledWith('/dir', new MakeDirectoryParameters(true))
        done()
      })
    })
  })
})
