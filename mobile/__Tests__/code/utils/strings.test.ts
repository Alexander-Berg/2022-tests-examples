import {
  emptyStringIfNull,
  nullIfEmptyString,
  removeNewlines,
  stringReplaceAll,
  quote,
  isStringNullOrEmpty,
} from '../../../code/utils/strings'

describe(nullIfEmptyString, () => {
  it('should return correct values', () => {
    expect(nullIfEmptyString(null)).toBeNull()
    expect(nullIfEmptyString('')).toBeNull()
    expect(nullIfEmptyString('foo')).toBe('foo')
  })
})

describe(emptyStringIfNull, () => {
  it('should return correct values', () => {
    expect(emptyStringIfNull(null)).toBe('')
    expect(emptyStringIfNull('')).toBe('')
    expect(emptyStringIfNull('foo')).toBe('foo')
  })
})

describe(isStringNullOrEmpty, () => {
  it('should return correct values', () => {
    expect(isStringNullOrEmpty(null)).toBe(true)
    expect(isStringNullOrEmpty('')).toBe(true)
    expect(isStringNullOrEmpty('foo')).toBe(false)
  })
})

describe(quote, () => {
  it('should quote strings', () => {
    expect(quote('')).toBe('""')
    expect(quote('abc')).toBe('"abc"')
  })
})

describe(stringReplaceAll, () => {
  it('should replace all occurences of substring', () => {
    expect(stringReplaceAll('abb', 'ab', 'a')).toBe('ab')
    expect(stringReplaceAll('abc', 'a', 'ab')).toBe('abbc')
    expect(
      stringReplaceAll(
        'The quick brown fox jumps over the lazy dog. If the dog reacted, was it really lazy?',
        'dog',
        'monkey',
      ),
    ).toBe('The quick brown fox jumps over the lazy monkey. If the monkey reacted, was it really lazy?')
  })
})

describe(removeNewlines, () => {
  it('should remove new lines', () => {
    expect(removeNewlines('Quick\nbrown\nfox\njumps\nover\nthe\nlazy\ndog')).toBe(
      'Quick brown fox jumps over the lazy dog',
    )
  })
})
