import { writeFileSync } from 'fs-extra'
import { KotlinSerializerEngine } from '../../../../src/generators/kotlin/config'
import { MoshiSerializationInfoGenerator } from '../../../../src/generators/kotlin/serialization/moshi-serialization-info-generator'
import { SerializerExtraInfoGeneratorFactory } from '../../../../src/generators/kotlin/serialization/serializer-extra-generator-factory'

describe(SerializerExtraInfoGeneratorFactory, () => {
  it('should throw if unsupported kotlin serializator is requested', () => {
    const serializerExtraInfoGeneratorFactory = new SerializerExtraInfoGeneratorFactory(writeFileSync)
    expect(() => {
      serializerExtraInfoGeneratorFactory.create(KotlinSerializerEngine.GSON)
    }).toThrowError("Requested Serializer engine 'gson' is not supported")
    expect(() => {
      serializerExtraInfoGeneratorFactory.create(KotlinSerializerEngine.JacksonXml)
    }).toThrowError("Requested Serializer engine 'jacksonxml' is not supported")
    expect(() => {
      serializerExtraInfoGeneratorFactory.create(KotlinSerializerEngine.Kotlinx)
    }).toThrowError("Requested Serializer engine 'kotlinx' is not supported")
    expect(serializerExtraInfoGeneratorFactory.create(KotlinSerializerEngine.Moshi)).toBeInstanceOf(
      MoshiSerializationInfoGenerator,
    )
  })
})
