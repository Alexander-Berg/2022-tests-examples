import { Logger } from '../../../../common/code/logging/logger'
import { default as syncRequest } from 'sync-request'
import { HttpVerb, Options } from 'sync-request'
import { Int32, Nullable, undefinedToNull, YSError } from '../../../../../common/ys'
import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { MapJSONItem, StringJSONItem } from '../../../../common/code/json/json-types'
import { NetworkMethod, NetworkRequest, RequestEncodingKind } from '../../../../common/code/network/network-request'
import { NetworkAPIVersions } from '../../../../mapi/code/api/mail-network-request'
import { SyncNetwork } from '../../../code/client/network/sync-network'
import { Result, resultValue, resultError } from '../../../../common/code/result/result'

export class DefaultSyncNetwork implements SyncNetwork {
  public constructor(private jsonSerializer: JSONSerializer, private logger: Logger) {}

  private static buildUrlParams(request: NetworkRequest): string {
    const args: string[] = []
    const clientMap = new MapJSONItem()
    clientMap.putString('client', 'iphone')
    DefaultSyncNetwork.addUrlParams(args, clientMap)
    DefaultSyncNetwork.addUrlParams(args, request.urlExtra())
    if (request.encoding().kind === RequestEncodingKind.url) {
      DefaultSyncNetwork.addUrlParams(args, request.params())
    }
    if (args.length === 0) {
      return ''
    }
    return args.join('&')
  }

  private static addUrlParams(args: string[], params: MapJSONItem): void {
    const map = params.asMap()
    for (const key of map.keys()) {
      args.push(key.toString() + '=' + (map.get(key) as StringJSONItem).value)
    }
  }

  private static requestToString(method: NetworkMethod, fullUrl: string, opts: Options): string {
    const data = opts.json !== null ? JSON.stringify(opts.json) : opts.body
    const dataString = undefinedToNull(data) !== null && data!.length > 0 ? `-d '${data}'` : ''
    const headers = opts.headers ?? {}
    let result = `curl -X ${DefaultSyncNetwork.toHttpVerb(method)} '${fullUrl}' ${dataString}`
    for (const key of Object.keys(headers)) {
      result += ` -H '${key}: ${headers[key]}'`
    }
    return result
  }

  private static toHttpVerb(method: NetworkMethod): HttpVerb {
    switch (method) {
      case NetworkMethod.get:
        return 'GET'
      case NetworkMethod.post:
        return 'POST'
    }
  }

  public syncExecute(baseUrl: string, networkRequest: NetworkRequest, oauthToken: Nullable<string>): Result<string> {
    const opts = this.encodeRequest(networkRequest, oauthToken)
    const fullUrl = this.buildUrl(baseUrl, networkRequest)
    this.logger.info(`${DefaultSyncNetwork.requestToString(networkRequest.method(), fullUrl, opts)}`)
    const response = syncRequest(DefaultSyncNetwork.toHttpVerb(networkRequest.method()), fullUrl, opts)
    const statusCode = response.statusCode
    const body = response.body.toString()
    if (body.length === 0) {
      // TODO: get json response from Imap Sync
      return resultValue('')
    }
    let parsed: any
    try {
      parsed = JSON.parse(body)
    } catch (e) {
      return resultError(new YSError(`Bad response body ${body}`))
    }
    if (networkRequest.targetPath().startsWith(NetworkAPIVersions.v2)) {
      switch (statusCode) {
        case 400:
          return resultError(new YSError(`Request error. Error: ${parsed.error}. Message: ${parsed.message}`))
        case 401:
          return resultError(new YSError(`Unauthorized error. Error: ${parsed.error}. Message: ${parsed.message}`))
        case 500:
          return resultError(new YSError(`Backend error. Error: ${parsed.error}. Message: ${parsed.message}`))
      }
    } else {
      if (!(parsed instanceof Array)) {
        if (
          parsed.status !== undefined &&
          parsed.status !== 1 &&
          parsed.status !== 'ok' &&
          parsed.status.status !== undefined &&
          parsed.status.status !== 1
        ) {
          return resultError(new YSError(`Bad response status ${body}`))
        }
      } else {
        const status = parsed[0].status
        if (undefinedToNull(status) !== null && status.status !== 1) {
          return resultError(new YSError(`Bad response status ${status.phrase}`))
        }
      }
    }
    return resultValue(response.body.toString())
  }

  private buildUrl(baseUrl: string, req: NetworkRequest): string {
    const url = `${baseUrl}/${req.targetPath()}`
    return `${url}?${DefaultSyncNetwork.buildUrlParams(req)}`
  }

  private encodeRequest(request: NetworkRequest, oauthToken: Nullable<string>): Options {
    const opts: Options = {
      retry: true,
    }
    opts.headers = {
      'Content-type': 'application/json',
      'User-Agent': 'testopithecus',
    }
    if (oauthToken !== null && oauthToken.length > 0) {
      opts.headers.Authorization = `OAuth ${oauthToken}`
    }
    if (request.encoding().kind === RequestEncodingKind.url) {
      if (request.targetPath() === 'token') {
        // TODO
        opts.body = DefaultSyncNetwork.buildUrlParams(request)
        return opts
      }
      return opts
    }
    const json = this.jsonSerializer.serialize(request.params()).getValue()
    opts.json = JSON.parse(json)
    return opts
  }

  public syncExecuteWithRetries(
    retries: Int32,
    baseUrl: string,
    request: NetworkRequest,
    oauthToken: Nullable<string>,
  ): Result<string> {
    let result: Result<string> = new Result<string>(null, null)
    while (retries >= 0) {
      result = this.syncExecute(baseUrl, request, oauthToken)
      if (result.isValue()) {
        return result
      }
      retries = retries - 1
    }
    throw result.getError()
  }
}
