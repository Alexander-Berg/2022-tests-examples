import { getVoid, Result } from '../../../../common/code/result/result'
import { NetworkStatus, NetworkStatusCode } from '../../../code/api/entities/status/network-status'
import { ResponseWithStatus } from '../../../code/api/entities/status/response-with-status'
import { MailApiAuthError, MailApiError, MailApiPermError, MailApiTempError } from '../../../code/api/mail-api-error'

describe(MailApiError, () => {
  describe('checkStatus', () => {
    it('should check ok status', () => {
      expect(MailApiError.checkStatus(new NetworkStatus(NetworkStatusCode.ok, null, null))).toStrictEqual(
        new Result(getVoid(), null),
      )
    })
    it('should check auth error status', () => {
      // tslint:disable-next-line:max-line-length
      const status = new NetworkStatus(
        NetworkStatusCode.permanentError,
        null,
        'PERM_FAIL folder_list: 2001, PERM_FAIL label_list_types: 2001, ',
      )
      expect(MailApiError.checkStatus(status)).toStrictEqual(new Result(null, new MailApiAuthError(status)))
    })
    it('should check perm error status', () => {
      const status = new NetworkStatus(NetworkStatusCode.permanentError, null, null)
      expect(MailApiError.checkStatus(status)).toStrictEqual(new Result(null, new MailApiPermError(status)))
    })
    it('should check temp error status', () => {
      const status = new NetworkStatus(NetworkStatusCode.temporaryError, null, null)
      expect(MailApiError.checkStatus(status)).toStrictEqual(new Result(null, new MailApiTempError(status)))
    })
    it('should check unkown status', () => {
      const status = new NetworkStatus(100, null, null)
      expect(MailApiError.checkStatus(status)).toStrictEqual(
        new Result(null, new MailApiError(`Unsupported type of status: ${status.getErrorMessage()}`)),
      )
    })
  })

  describe(MailApiError.checkStatusAsync.bind(MailApiError), () => {
    it('should check ok status async', (done) => {
      const status = new NetworkStatus(NetworkStatusCode.ok, null, null)
      expect.assertions(1)
      MailApiError.checkStatusAsync(status).then((result) => {
        expect(result).toBe(getVoid())
        done()
      })
    })
    it('should check auth error status async', (done) => {
      const status = new NetworkStatus(NetworkStatusCode.permanentError, null, null)
      expect.assertions(1)
      MailApiError.checkStatusAsync(status).failed((e) => {
        expect(e).toStrictEqual(new MailApiPermError(status))
        done()
      })
    })
  })
  describe(MailApiError.checkResponseWithStatus.bind(MailApiError), () => {
    it('should return the response if status is ok', (done) => {
      const response = new (class implements ResponseWithStatus {
        public kind: string = 'TEST_RESPONSE'
        public networkStatus(): NetworkStatus {
          return new NetworkStatus(NetworkStatusCode.ok)
        }
      })()
      MailApiError.checkResponseWithStatus(response).then((result) => {
        expect(result).toBe(response)
        expect(result.kind).toBe('TEST_RESPONSE')
        done()
      })
    })
    it('should reject if status is not ok', (done) => {
      const response = new (class implements ResponseWithStatus {
        public kind: string = 'TEST_RESPONSE'
        public networkStatus(): NetworkStatus {
          return new NetworkStatus(NetworkStatusCode.permanentError, 'TRACE', 'PHRASE')
        }
      })()
      MailApiError.checkResponseWithStatus(response).failed((e) => {
        expect(e.message).toBe('PERM error; Phrase = PHRASE; Trace = TRACE;')
        done()
      })
    })
  })
})

describe(MailApiAuthError, () => {
  it('should check auth error', () => {
    expect(MailApiAuthError.isAuthError(new NetworkStatus(NetworkStatusCode.ok, null, null))).toBe(false)
    expect(MailApiAuthError.isAuthError(new NetworkStatus(NetworkStatusCode.permanentError, null, null))).toBe(false)
    expect(MailApiAuthError.isAuthError(new NetworkStatus(NetworkStatusCode.permanentError, null, ''))).toBe(false)
    expect(
      MailApiAuthError.isAuthError(
        // tslint:disable-next-line:max-line-length
        new NetworkStatus(
          NetworkStatusCode.permanentError,
          null,
          'PERM_FAIL label_list_types: skip, PERM_FAIL folder_list: 2001',
        ),
      ),
    ).toBe(true)
  })
})
