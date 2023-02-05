import FileSystem, { PathLike } from 'fs'
import {
  buildOutputFileName,
  formatFileName,
  getGenerator,
  getGeneratorPath,
  getInputFileList,
} from '../../src/utils/fileutils'

describe('File Utils functions', () => {
  afterEach(jest.restoreAllMocks)

  it('returns list of ts-files in directory', () => {
    jest.spyOn(FileSystem, 'readdirSync').mockImplementation(((path: string): string[] => {
      if (path === 'inputDir') {
        return ['subdir1', 'subdir2', 'file.ts']
      }
      if (path === 'inputDir/subdir1') {
        return ['subdir11', 'subdir12']
      }
      if (path === 'inputDir/subdir1/subdir11') {
        return ['file11.ts', 'file12.ts']
      }
      if (path === 'inputDir/subdir1/subdir12') {
        return []
      }
      if (path === 'inputDir/subdir2') {
        return ['file21.ts', 'file22.ts']
      }
      return []
    }) as any)
    jest.spyOn(FileSystem, 'statSync').mockImplementation((path) => {
      const value =
        path === 'inputDir' ||
        path === 'inputDir/subdir1' ||
        path === 'inputDir/subdir1/subdir11' ||
        path === 'inputDir/subdir1/subdir12' ||
        path === 'inputDir/subdir2'

      return {
        isDirectory: () => value,
      } as FileSystem.Stats
    })

    expect(getInputFileList('inputDir').sort()).toEqual(
      [
        'inputDir/subdir1/subdir11/file11.ts',
        'inputDir/subdir1/subdir11/file12.ts',
        'inputDir/subdir2/file21.ts',
        'inputDir/subdir2/file22.ts',
        'inputDir/file.ts',
      ].sort(),
    )
  })

  it('returns empty list if no ts-files in directory', () => {
    jest.spyOn(FileSystem, 'readdirSync').mockImplementation(((path: PathLike): string[] => {
      if (path === 'inputDir') {
        return ['subdir1', 'subdir2', 'file.bin']
      }
      if (path === 'inputDir/subdir1') {
        return ['subdir11', 'subdir12']
      }
      if (path === 'inputDir/subdir1/subdir11') {
        return ['file11.js', 'file12.tsx']
      }
      if (path === 'inputDir/subdir1/subdir12') {
        return []
      }
      if (path === 'inputDir/subdir2') {
        return ['file21.json', 'file22.txt']
      }
      return []
    }) as any)

    jest.spyOn(FileSystem, 'statSync').mockImplementation((path) => {
      const value =
        path === 'inputDir' ||
        path === 'inputDir/subdir1' ||
        path === 'inputDir/subdir1/subdir11' ||
        path === 'inputDir/subdir1/subdir12' ||
        path === 'inputDir/subdir2'

      return {
        isDirectory: () => value,
      } as FileSystem.Stats
    })

    expect(getInputFileList('inputDir')).toHaveLength(0)
  })
})

describe(getGeneratorPath, () => {
  afterEach(jest.restoreAllMocks)
  afterAll(jest.restoreAllMocks)

  it('returns list of paths to use with `getGenerator`', () => {
    jest
      .spyOn(FileSystem, 'existsSync')
      .mockImplementation(
        (path) => path === 'generators/swift/generator-creator.js' || path === 'generators/kotlin/generator-creator.js',
      )
    expect([getGeneratorPath('generators/kotlin'), getGeneratorPath('generators/swift')]).toEqual([
      'generators/kotlin/generator-creator.js',
      'generators/swift/generator-creator.js',
    ])
  })

  it('skips paths that do not have `generator-creator.js` file in their root folder', () => {
    jest
      .spyOn(FileSystem, 'existsSync')
      .mockImplementation(
        (path) => path === 'generators/swift/hello.js' || path === 'generators/kotlin/generator-creator.js',
      )
    expect(getGeneratorPath('generators/swift')).toBeNull()
  })

  it('returns a generator by path', () => {
    const generator = getGenerator('path1/generator-creator.js', '{}', (id) => ({
      default: {
        create: (_: string): any => ({
          getName: (): string => id,
        }),
      },
    }))
    expect(generator.getName()).toBe('path1/generator-creator.js')
  })

  it('generates output file name', () => {
    const inputDir = '/dir/subdir1/subdir2/'
    const inputFile = '/dir/subdir1/subdir2/subdir3/subdir4/infile.in'
    const outputDir = '/dir/outdir1/outdir2'
    const outputFile = 'outfile.out'
    const result = buildOutputFileName(inputDir, inputFile, outputDir, outputFile)
    expect(result).toBe('/dir/outdir1/outdir2/subdir3/subdir4/outfile.out')
  })

  it("throws if input path doesn't contain input files", () => {
    const inputDir = '/dir/subdir1/subdir2/'
    const inputFile = '/dir/subdir1/otherdir/subdir3/infile.in'
    const outputDir = '/dir/outdir1/outdir2'
    const outputFile = 'outfile.out'
    expect(() => {
      buildOutputFileName(inputDir, inputFile, outputDir, outputFile)
    }).toThrowError(`Input file ${inputFile} seems to be not in ${inputDir}`)
  })

  it('formats file names', () => {
    expect(formatFileName('first-second-third.ts', '.swift')).toBe('FirstSecondThird.swift')
    expect(formatFileName('-second-third.ts', '.swift')).toBe('SecondThird.swift')
    expect(formatFileName('first.ts', '.swift')).toBe('First.swift')
    expect(formatFileName('first---second-third.ts', '.swift')).toBe('FirstSecondThird.swift')
    expect(formatFileName('f.ts', '.swift')).toBe('F.swift')
    expect(() => {
      formatFileName('', '.swift')
    }).toThrowError('Empty filename argument')
  })
})
