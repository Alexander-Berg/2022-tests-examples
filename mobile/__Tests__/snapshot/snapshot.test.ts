import path from 'path'
import { Generator } from '../../src/generator'
import Generators from '../../src/generators'
import { KotlinGenerator } from '../../src/generators/kotlin/kotlin-generator'
import { SwiftGenerator } from '../../src/generators/swift/swift-generator'
import fs from 'fs'
import { Nullable } from '../../src/ys-tools/ys'

const updateSnapshot = true

describe('Snapshot testing for generation from YS', () => {
  const swiftGenerator = new SwiftGenerator({
    additionalImports: [],
    serializableEnums: false,
  })
  const kotlinGenerator = new KotlinGenerator({
    additionalImports: ['com.yandex.ys'],
    package: 'com.yandex.xplat',
    serializer: undefined,
  })

  it('sandbox to swift', () => {
    testGeneration('sandbox.ts', swiftGenerator, 'Sandbox.swift')
  })
  it('sandbox to kotlin', () => {
    testGeneration('sandbox.ts', kotlinGenerator, 'Sandbox.kt')
  })

  it('throwing to swift', () => {
    testGeneration('throwing.ts', swiftGenerator, 'Throwing.swift')
  })
  it('throwing to kotlin', () => {
    testGeneration('throwing.ts', kotlinGenerator, 'Throwing.kt')
  })

  it('async-await to swift', () => {
    testGeneration('async-await.ts', swiftGenerator, 'AsyncAwait.swift')
  })
  it('async-await to kotlin', () => {
    testGeneration('async-await.ts', kotlinGenerator, 'AsyncAwait.kt')
  })

  it('nullish to swift', () => {
    testGeneration('nullish.ts', swiftGenerator, 'Nullish.swift')
  })
  it('nullish to kotlin', () => {
    testGeneration('nullish.ts', kotlinGenerator, 'Nullish.kt')
  })

  it('parcelize to swift', () => {
    testGeneration('parcelize.ts', swiftGenerator, 'Parcelize.swift')
  })
  it('parcelize to kotlin', () => {
    testGeneration('parcelize.ts', kotlinGenerator, 'Parcelize.kt')
  })
  it('cant get value from any', () => {
    testGeneration('any-get.ts', swiftGenerator)
  })
})

function testGeneration(ysFileName: string, generator: Generator<any>, dstFileName: Nullable<string> = null): void {
  const filesBaseDir = path.join(__dirname, '__fixtures__')
  const fileList = [path.join(filesBaseDir, ysFileName)]
  const outputDir = path.join(filesBaseDir, 'actual')

  if (!Generators.generate(filesBaseDir, fileList, outputDir, generator, null)) {
    expect(dstFileName).toBeNull() // should generate without error
    return
  }

  expect(dstFileName).not.toBeNull() // generation should fail

  const actualFile = path.join(outputDir, dstFileName!)
  const actual = fs.readFileSync(actualFile, 'utf-8')
  const expectedFile = path.join(filesBaseDir, 'expected', dstFileName!)
  if (updateSnapshot || !fs.existsSync(expectedFile)) {
    fs.copyFileSync(actualFile, expectedFile)
  }
  const expected = fs.readFileSync(expectedFile, 'utf-8')
  expect(actual).toBe(expected)
}
