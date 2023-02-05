import 'mocha'
import { PathConfigurationParser } from '../code/core/path-configuration-parser'
import { IncomingMessage } from 'http'
import * as assert from 'assert'

describe('PathConfigurationParser should parse proxy options from request', () => {
  const parser = new PathConfigurationParser()

  it('should work without configuration', () => {
    const req = request('https://xp.yandex-team.ru/api/mobile')
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, null)
    assert.strictEqual(parsed.configurationName, null)
    assert.strictEqual(parsed.parameters, null)
    assert.strictEqual(parser.forwardedUrl(req, parsed), 'https://xp.yandex-team.ru/api/mobile')
  })

  it('should parse configuration', () => {
    const req = request('https://xp.yandex-team.ru/c/metro')
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, null)
    assert.strictEqual(parsed.configurationName, 'metro')
    assert.deepStrictEqual(parsed.parameters, [])
    assert.strictEqual(parser.forwardedUrl(req, parsed), 'https://xp.yandex-team.ru')
  })

  it('should parse forward host from host', () => {
    const req = {
      url: '/c/metro/api/mobile',
      headers: {
        host: 'pizza_com.xp.yandex-team.ru',
      },
    } as IncomingMessage
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, 'https://pizza.com')
    assert.strictEqual(parsed.configurationName, 'metro')
    assert.deepStrictEqual(parsed.parameters, [])
    assert.strictEqual(parser.forwardedUrl(req, parsed), '/api/mobile')
  })

  it('should parse forward host from path', () => {
    const req = request('/f/pizza.com/c/metro/api/mobile')
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, 'https://pizza.com')
    assert.strictEqual(parsed.configurationName, 'metro')
    assert.deepStrictEqual(parsed.parameters, [])
    assert.strictEqual(parser.forwardedUrl(req, parsed), '/api/mobile')
  })

  it('should parse parameters for configuration', () => {
    const req = request('/f/pizza.com/c/rps/5/api/mobile')
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, 'https://pizza.com')
    assert.strictEqual(parsed.configurationName, 'rps')
    assert.deepStrictEqual(parsed.parameters, ['5'])
    assert.strictEqual(parser.forwardedUrl(req, parsed), '/api/mobile')
  })

  it('should parse parameters for configuration with callback', () => {
    const req = request('http://localhost:8080/f/pizza.com/c/image/100/200/blue/file.png/api/mobile')
    const parsed = parser.parse(req)
    assert.strictEqual(parsed.forwardHost, 'https://pizza.com')
    assert.strictEqual(parsed.configurationName, 'image')
    assert.deepStrictEqual(parsed.parameters, ['100', '200', 'blue', 'file.png'])
    assert.strictEqual(parser.forwardedUrl(req, parsed), 'http://localhost:8080/api/mobile')
  })
})

function request(url: string): IncomingMessage {
  return {
    url,
    headers: {},
  } as IncomingMessage
}
