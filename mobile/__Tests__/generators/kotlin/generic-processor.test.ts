import FileProcessor from '../../../src/fileprocessor'
import { TSLocalTypeKind } from '../../../src/generators-model/basic-types'
import { KotlinSerializerEngine } from '../../../src/generators/kotlin/config'
import { GenericProcessor } from '../../../src/generators/kotlin/generic-processor'
import { SerializerExtraInfoGeneratorFactory } from '../../../src/generators/kotlin/serialization/serializer-extra-generator-factory'
import { TestGenerator } from '../../__helpers__/generator'
import { testTypeMappingsProviderGenerator } from '../../__helpers__/test-type-mapping'

describe(GenericProcessor, () => {
  // TODO - delete this test once SerializerExtraInfoGeneratorFactory and serialization related classes are removed
  it('should generate moshi support class for array of passed type', () => {
    const fakeWriter = jest.fn()
    const serializerExtraInfoGeneratorFactory = new SerializerExtraInfoGeneratorFactory(fakeWriter)
    const moshiSerializer = serializerExtraInfoGeneratorFactory.create(KotlinSerializerEngine.Moshi)
    const genericProcessor = new GenericProcessor(moshiSerializer, testTypeMappingsProviderGenerator())
    const localType = {
      kind: TSLocalTypeKind.Reference,
      name: 'MyType1',
    }
    genericProcessor.onGenericDetected(localType)
    genericProcessor.afterAllFilesProcessed('', '')
    const expectedContents = ''
    const actualContents = fakeWriter.mock.calls.map(([, contents]) => contents).join('')
    expect(actualContents).toStrictEqual(expectedContents)
  })

  it('should call allFilesProcessed when all files are processed', () => {
    const testGenerator = new TestGenerator()
    testGenerator.allFilesProcessed = jest.fn()
    const fileProcessor = new FileProcessor<{}>(testGenerator, '', '')
    fileProcessor.allFilesProcessed('a1', 'a2')
    expect(testGenerator.allFilesProcessed).toBeCalledWith('a1', 'a2')
  })
})
