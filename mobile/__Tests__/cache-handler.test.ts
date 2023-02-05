import FileSystem from 'fs'
import FileSystemExtra from 'fs-extra'
import { CacheHandler } from '../src/cache-handler'

describe(CacheHandler, () => {
  const fakeWriter = jest.fn()
  const fakeReader = jest.fn().mockReturnValue('header')
  let originalExistsSync: any
  let originalMkDirpSync: any
  beforeAll(() => {
    originalMkDirpSync = FileSystemExtra.mkdirpSync
    originalExistsSync = FileSystem.existsSync
    FileSystemExtra.mkdirpSync = jest.fn()
  })
  afterEach(() => {
    FileSystem.existsSync = originalExistsSync
    jest.clearAllMocks()
  })
  afterAll(() => {
    FileSystemExtra.mkdirpSync = originalMkDirpSync
  })
  it('should tell that file without cache was changed', () => {
    const cacheHandler = new CacheHandler('test_dir', 'TestGenerator', fakeReader, fakeWriter)
    FileSystem.existsSync = jest.fn((path) => path === 'test_dir/test_file1.ts')
    expect(cacheHandler.sourceWasChanged('test_dir/test_file1.ts')).toBe(true)
    expect(fakeWriter).toBeCalledTimes(1)
    expect(fakeWriter).toBeCalledWith(
      'test_dir/.cache/TestGenerator/test_file1.ts.idx',
      '099fb995346f31c749f6e40db0f395e3',
    )
  })
  it('should create cache dir in constructor', () => {
    const _ = new CacheHandler('1111', '222', fakeReader, fakeWriter)
    expect(FileSystemExtra.mkdirpSync).toBeCalled()
  })
  it('should tell that file with cache was not changed', () => {
    const fakeReader2 = jest.fn((path: string): string =>
      path.endsWith('.idx') ? '099fb995346f31c749f6e40db0f395e3' : 'header',
    )
    FileSystem.existsSync = jest.fn(
      (path) => path === 'test_dir/test_file2.ts' || path === 'test_dir/.cache/TestGenerator/test_file2.ts.idx',
    )
    const cacheHandler = new CacheHandler('test_dir', 'TestGenerator', fakeReader2, fakeWriter)
    expect(cacheHandler.sourceWasChanged('test_dir/test_file2.ts')).toBe(false)
  })

  it('should tell that file with changes and cache was changed if file contents was changed', () => {
    FileSystem.existsSync = jest.fn(
      (path) => path === 'test_dir/test_file3.ts' || path === 'test_dir/.cache/TestGenerator/test_file2.ts.idx',
    )
    const cacheHandler = new CacheHandler('test_dir', 'TestGenerator', fakeReader, fakeWriter)
    expect(cacheHandler.sourceWasChanged('test_dir/test_file3.ts')).toBe(true)
  })

  it('should correctly transform source file name into cachefile name', () => {
    const fileName = 'test_dir/test_sub_dir/test_file1'
    const cacheHandler = new CacheHandler('test_dir', 'TestGenerator', fakeReader, fakeWriter)
    expect(cacheHandler.transformPath(fileName)).toBe('test_sub_dir_test_file1.idx')
  })

  it('should correctly transform source file name into cachefile name if basepath ends with slash', () => {
    const fileName = 'test_dir/test_sub_dir/test_file1'
    const cacheHandler = new CacheHandler('test_dir/', 'TestGenerator', fakeReader, fakeWriter)
    expect(cacheHandler.transformPath(fileName)).toBe('test_sub_dir_test_file1.idx')
  })

  it('should correctly transform source file name into cachefile name', () => {
    const fileName = 'test_dir/test_file1'
    const cacheHandler = new CacheHandler('test_dir', 'TestGenerator', fakeReader, fakeWriter)
    expect(cacheHandler.transformPath(fileName)).toBe('test_file1.idx')
  })
})
