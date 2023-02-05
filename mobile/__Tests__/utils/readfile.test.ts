import FileSystem from 'fs'
import { readFile } from '../../src/utils/fileutils'

describe(readFile, () => {
  afterEach(jest.restoreAllMocks)

  it('should read files in directories', () => {
    const path = 'test_dir4/test_file3.ts'
    const existsSpy = jest.spyOn(FileSystem, 'existsSync').mockImplementation((filePath) => filePath === path)
    const readFileSpy = jest.spyOn(FileSystem, 'readFileSync').mockReturnValue('hello')
    expect(readFile(path)).toBe('hello')
    expect(existsSpy).toBeCalledWith(path)
    expect(existsSpy).toReturnWith(true)
    expect(readFileSpy).toBeCalledWith(path, 'utf8')
    expect(readFileSpy).toReturnWith('hello')
  })

  it('should throw error if file cannot be read', () => {
    const path = 'test_dir4/test_file18.ts'
    const existsSpy = jest.spyOn(FileSystem, 'existsSync').mockImplementation((filePath) => filePath !== path)
    expect(() => {
      readFile(path)
    }).toThrowError(`Input file ${path} does not exist`)
    expect(existsSpy).toBeCalledWith(path)
    expect(existsSpy).toReturnWith(false)
  })
})
