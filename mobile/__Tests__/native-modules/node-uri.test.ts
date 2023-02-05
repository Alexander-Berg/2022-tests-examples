import { Uris } from '../../native-modules/native-modules'
import { NodeUri } from '../../native-modules/node-uri'
import { UriQueryParameter } from '../../code/uri/uri'

describe(NodeUri, () => {
  it('should build file uri', () => {
    const uri = Uris.fromFilePath('/path/to/file')
    expect(uri.getAbsoluteString()).toBe('file:///path/to/file')
    expect(uri.getScheme()).toBe('file')
    expect(uri.getHost()).toBeNull()
    expect(uri.getPath()).toBe('/path/to/file')
    expect(uri.getPathSegments()).toEqual(['path', 'to', 'file'])
    expect(uri.getQuery()).toBeNull()
    expect(uri.getAllQueryParameters()).toEqual([])
    expect(uri.getFragment()).toBeNull()
    expect(uri.isFileUri()).toBe(true)
  })
  it('should build web uri', () => {
    const uri = Uris.fromString('https://ya.ru/path/to/resource?param=value&foo=bar#fragment')!
    expect(uri).not.toBeNull()
    expect(uri.getAbsoluteString()).toBe('https://ya.ru/path/to/resource?param=value&foo=bar#fragment')
    expect(uri.getScheme()).toBe('https')
    expect(uri.getHost()).toBe('ya.ru')
    expect(uri.getPath()).toBe('/path/to/resource')
    expect(uri.getPathSegments()).toEqual(['path', 'to', 'resource'])
    expect(uri.getQuery()).toBe('param=value&foo=bar')
    expect(uri.getAllQueryParameters()).toEqual([
      { name: 'param', value: 'value' },
      { name: 'foo', value: 'bar' },
    ])
    expect(uri.getFragment()).toBe('fragment')
    expect(uri.isFileUri()).toBe(false)
  })
  it('should build file uri from string', () => {
    const uri = Uris.fromString('file:///path/to/file')!
    expect(uri).not.toBeNull()
    expect(uri.getPath()).toBe('/path/to/file')
    expect(uri.getPathSegments()).toEqual(['path', 'to', 'file'])
    expect(uri.isFileUri()).toBe(true)
  })
  it('should fail to build web uri if the value is invalid', () => {
    const uri = Uris.fromString('invalid value')
    expect(uri).toBeNull()
  })
  it('should change uri', () => {
    const builder = Uris.fromString('https://ya.ru/path/to/resource?param=value&foo=bar#fragment')!.builder()
    expect(builder.setScheme('http').build().getAbsoluteString()).toBe(
      'http://ya.ru/path/to/resource?param=value&foo=bar#fragment',
    )
    expect(builder.setHost('yandex.ru').build().getAbsoluteString()).toBe(
      'http://yandex.ru/path/to/resource?param=value&foo=bar#fragment',
    )
    expect(builder.setPath('path/to/new-resource').build().getAbsoluteString()).toBe(
      'http://yandex.ru/path/to/new-resource?param=value&foo=bar#fragment',
    )
    expect(builder.setPath('/path/to/yet-another-resource').build().getAbsoluteString()).toBe(
      'http://yandex.ru/path/to/yet-another-resource?param=value&foo=bar#fragment',
    )
    expect(
      builder
        .setAllQueryParameters([new UriQueryParameter('new_param', 'value'), new UriQueryParameter('bar', 'bazz')])
        .build()
        .getAbsoluteString(),
    ).toBe('http://yandex.ru/path/to/yet-another-resource?new_param=value&bar=bazz#fragment')
    expect(builder.setAllQueryParameters([]).build().getAbsoluteString()).toBe(
      'http://yandex.ru/path/to/yet-another-resource#fragment',
    )
    expect(builder.setFragment('new-fragment').build().getAbsoluteString()).toBe(
      'http://yandex.ru/path/to/yet-another-resource#new-fragment',
    )
    expect(builder.setFragment('').build().getAbsoluteString()).toBe('http://yandex.ru/path/to/yet-another-resource')
    expect(builder.setPath('').build().getAbsoluteString()).toBe('http://yandex.ru/')
  })

  it('check uri query parameters', () => {
    let uri = Uris.fromString('https://ya.ru')!
    expect(uri.getAllQueryParameters()).toEqual([])
    expect(uri.getQueryParameter('foo')).toBeNull()
    expect(uri.getQueryParameters('foo')).toEqual([])
    expect(uri.getQueryParameterNames()).toEqual([])

    uri = uri
      .builder()
      .appendQueryParameter('foo', 'bar')
      .appendQueryParameter('param', 'val1')
      .appendQueryParameter('param', 'val2')
      .build()
    expect(uri.getAbsoluteString()).toBe('https://ya.ru/?foo=bar&param=val1&param=val2')
    expect(uri.getAllQueryParameters()).toEqual([
      { name: 'foo', value: 'bar' },
      { name: 'param', value: 'val1' },
      { name: 'param', value: 'val2' },
    ])
    expect(uri.getQueryParameter('foo')).toBe('bar')
    expect(uri.getQueryParameters('foo')).toEqual(['bar'])
    expect(uri.getQueryParameter('param')).toBe('val1')
    expect(uri.getQueryParameters('param')).toEqual(['val1', 'val2'])
    expect(uri.getQueryParameterNames()).toEqual(['foo', 'param'])

    uri = uri.builder().clearQuery().build()
    expect(uri.getAbsoluteString()).toBe('https://ya.ru/')
    expect(uri.getQueryParameterNames()).toEqual([])

    uri = uri
      .builder()
      .setAllQueryParameters([new UriQueryParameter('bar', 'bazz')])
      .build()
    expect(uri.getAbsoluteString()).toBe('https://ya.ru/?bar=bazz')
    expect(uri.getQueryParameterNames()).toEqual(['bar'])

    uri = Uris.fromString('https://ya.ru?foo')!
    expect(uri.getAllQueryParameters()).toEqual([{ name: 'foo', value: '' }])
    expect(uri.getQueryParameter('foo')).toBe('')
    expect(uri.getQueryParameters('foo')).toEqual([''])
    expect(uri.getQueryParameterNames()).toEqual(['foo'])
  })
})
