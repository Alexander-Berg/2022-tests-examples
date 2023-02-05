import OS from 'os'
import Path from 'path'
import { writeFile } from '../../src/utils/fileutils'

interface Results {
  mkdirpWith: string
  writeFileSyncWith: [string, string]
}

const mockResults: Results = {
  mkdirpWith: '',
  writeFileSyncWith: ['', ''],
}

jest.mock('fs-extra', () => ({
  mkdirpSync(path: string): void {
    mockResults.mkdirpWith = path
  },
}))
jest.mock('fs', () => ({
  writeFileSync(filename: string, contents: string): void {
    mockResults.writeFileSyncWith = [filename, contents]
  },
}))

beforeEach(() => {
  mockResults.mkdirpWith = ''
  mockResults.writeFileSyncWith = ['', '']
})

afterAll(jest.clearAllMocks)

test('should write files in directories', () => {
  const path = Path.join(OS.tmpdir(), 'subdir1/subdir2')
  const contents = 'hello world'
  writeFile(path, contents)
  expect(mockResults.mkdirpWith).toBe(Path.dirname(path))
  expect(mockResults.writeFileSyncWith).toStrictEqual([path, contents])
})
