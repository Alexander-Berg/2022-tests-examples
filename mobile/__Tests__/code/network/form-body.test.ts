import { FormBody } from '../../../code/network/form-body'

describe(FormBody, () => {
  it('should parse form body', () => {
    const result = FormBody.parse(
      'key=ABC&%D0%BA%D0%BB%D1%8E%D1%87=%D0%90%D0%91%D0%92&k%20e%20y=A%20B%20C&novalue1=&novalue2',
    )
    expect(result.getValue('key')).toBe('ABC')
    expect(result.getValue('novalue1')).toBe('')
    expect(result.getValue('novalue2')).toBe('')
    expect(result.getValue('ключ')).toBe('АБВ')
    expect(result.getValue('k e y')).toBe('A B C')
    expect(result.tryGetValue('ключ')).toBe('АБВ')
    expect(() => result.tryGetValue('unknown')).toThrowError(
      'Failed to query form body for key "unknown", body: "key=ABC&%D0%BA%D0%BB%D1%8E%D1%87=%D0%90%D0%91%D0%92&k%20e%20y=A%20B%20C&novalue1=&novalue2="',
    )
    expect(result.size()).toBe(5)
    expect(result.encodedName(1)).toBe('%D0%BA%D0%BB%D1%8E%D1%87')
    expect(result.encodedValue(1)).toBe('%D0%90%D0%91%D0%92')

    const resultFromBuilder = result.builder().add('фуу', 'бар').build()
    expect(resultFromBuilder.size()).toBe(result.size() + 1)
    expect(resultFromBuilder.tryGetValue('фуу')).toBe('бар')
  })
})
