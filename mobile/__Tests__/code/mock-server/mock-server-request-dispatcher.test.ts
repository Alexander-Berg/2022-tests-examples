import { Int32 } from '../../../../../common/ys'
import { Encoding } from '../../../../common/code/file-system/file-system-types'
import { HttpRequest, HttpResponse } from '../../../../common/code/network/http-layer'
import { ArrayBufferHelpers } from '../../../../common/native-modules/native-modules'
import { MockServerRequestDispatcher } from '../../../code/mock-server/mock-server-request-dispatcher'
import { MockServerRequestHandlers } from '../../../code/mock-server/mock-server-request-handlers'

describe(MockServerRequestDispatcher, () => {
  function request(url: string): HttpRequest {
    return new HttpRequest('GET', url, new Map(), new ArrayBuffer(0))
  }

  function response(code: Int32, text: string): HttpResponse {
    return new HttpResponse(
      code,
      new Map().set('Content-Type', 'text/plain; charset=utf-8'),
      ArrayBufferHelpers.arrayBufferFromString(text, Encoding.Utf8).getValue(),
    )
  }

  const dispatcher = new MockServerRequestDispatcher(
    MockServerRequestHandlers.callback((_) => response(404, 'page not found')),
  )
    .matchPath(
      'path',
      MockServerRequestHandlers.callback((request) => response(200, `handled request ${request.url}`)),
    )
    .matchPathPrefix(
      'prefix',
      MockServerRequestHandlers.stripPathPrefix(
        'prefix',
        new MockServerRequestDispatcher(
          MockServerRequestHandlers.callback((_) => response(404, 'inner page not found')),
        ).matchPath(
          'path',
          MockServerRequestHandlers.callback((request) => response(200, `handled inner request ${request.url}`)),
        ),
      ),
    )

  it('should match paths', () => {
    expect(dispatcher.handleRequest(request('https://example.com'))).toEqual(response(404, 'page not found'))
    expect(dispatcher.handleRequest(request('https://example.com/path'))).toEqual(
      response(200, 'handled request https://example.com/path'),
    )
    expect(dispatcher.handleRequest(request('https://example.com/prefix'))).toEqual(
      response(404, 'inner page not found'),
    )
    expect(dispatcher.handleRequest(request('https://example.com/prefix/path'))).toEqual(
      response(200, 'handled inner request https://example.com/path'),
    )
  })
})
