import { KotlinSerializerEngine } from '../../../../src/generators/kotlin/config'
import { SerializerExtraInfoGeneratorFactory } from '../../../../src/generators/kotlin/serialization/serializer-extra-generator-factory'

// TODO - delete this test once SerializerExtraInfoGeneratorFactory and serialization related classes are removed
describe(SerializerExtraInfoGeneratorFactory, () => {
  it('should generate moshi serialization support class for specified types', () => {
    let writtenContent = ''
    const fakeWriter = jest.fn((filename, content) => {
      writtenContent += content
    })
    const moshiSerializer = new SerializerExtraInfoGeneratorFactory(fakeWriter).create(KotlinSerializerEngine.Moshi)!
    const specializationClasses = new Set(['NetworkResponse', 'SomeMyClass'])
    moshiSerializer.generateSerializationExtraInfo('', 'someOutput', 'YSArray', specializationClasses)
    expect(writtenContent).toBe('')
  })
})
