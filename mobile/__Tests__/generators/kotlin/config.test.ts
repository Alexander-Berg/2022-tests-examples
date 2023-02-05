import { fromJSON, KotlinSerializerEngine } from '../../../src/generators/kotlin/config'

describe('Config', () => {
  it('should create config instance from json', () => {
    const configString = `
{
  "package": "com.yandex.mail",
  "additionalImports": [
    "com.yandex.xplat.common",
    "com.squareup.moshi"
  ],
  "serializer": "moshi"
}
`
    const configInstance = fromJSON(configString)
    expect(configInstance.serializer).toBe(KotlinSerializerEngine.Moshi)
    expect(configInstance.package).toBe('com.yandex.mail')
    expect(configInstance.additionalImports).toStrictEqual(['com.yandex.xplat.common', 'com.squareup.moshi'])
  })
})
