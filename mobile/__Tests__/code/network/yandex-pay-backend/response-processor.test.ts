import { YSError } from '../../../../../../common/ys'
import { NetworkResponse } from '../../../../../common/code/network/network-response'
import { getVoid, resultError, resultValue } from '../../../../../common/code/result/result'
import { DefaultJSONSerializer } from '../../../../../common/__tests__/__helpers__/default-json'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { ResponseProcessor } from '../../../../code/network/yandex-pay-backend/response-processor'
import { TransportError, TransportErrorCodes } from '../../../../code/network/yandex-pay-backend/transport-errors'
import { TestResponse } from '../../../__helpers__/test-response'
import { TestSerializer } from '../../../__helpers__/test-serializer'

describe(ResponseProcessor, () => {
  describe('extractResponse', () => {
    it('should respond with Payload Error if body is null', (done) => {
      const processor = new ResponseProcessor(new TestSerializer())
      processor
        .extractResponse(
          'TAG',
          {
            code: jest.fn().mockReturnValue(200),
            body: jest.fn().mockReturnValue(null),
            headers: jest.fn().mockReturnValue(new Map()),
            isSuccessful: jest.fn().mockReturnValue(true),
          } as NetworkResponse,
          (_) => resultValue(getVoid()),
        )
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.noPayload)
          expect((err as TransportError).message).toBe('No payload in response on TAG')
          done()
        })
    })
    it('should respond with Serialization Error if body deserialization fails', (done) => {
      const processor = new ResponseProcessor(
        createMockInstance(TestSerializer, {
          deserialize: jest.fn().mockReturnValue(resultError(new YSError('ERROR'))),
        }),
      )
      processor
        .extractResponse('TAG', new TestResponse(200, '{sample: "OK"}'), (_) => resultValue(getVoid()))
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.jsonSerializationError)
          expect((err as TransportError).message).toBe('Error deserializing response on TAG: ERROR')
          done()
        })
    })
    it("should respond with Data Format Error if body doesn't have map in response", (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractResponse('TAG', new TestResponse(200, '[10, 20]'), (_) => resultValue(getVoid()))
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.dataFormatError)
          expect((err as TransportError).message).toBe('Map expected as response of request TAG')
          done()
        })
    })
    it("should respond with Data Format Error if body doesn't have status", (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractResponse('TAG', new TestResponse(200, JSON.stringify({ sample: 'OK', code: 200 })), (_) =>
          resultValue(getVoid()),
        )
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.dataFormatError)
          expect((err as TransportError).message).toBe(
            'Either Status or Code field is absent or of improper type in response on TAG',
          )
          done()
        })
    })
    it("should respond with Data Format Error if body doesn't have code", (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractResponse('TAG', new TestResponse(200, JSON.stringify({ status: 'success', hello: 10 })), (_) =>
          resultValue(getVoid()),
        )
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.dataFormatError)
          expect((err as TransportError).message).toBe(
            'Either Status or Code field is absent or of improper type in response on TAG',
          )
          done()
        })
    })
    it('should respond with Error if body Model object deserialization fails', (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractResponse('TAG', TestResponse.success({}), (_) => resultError(new YSError('ERROR')))
        .failed((err) => {
          expect(err).toBeInstanceOf(YSError)
          expect((err as YSError).message).toBe('ERROR')
          done()
        })
    })
    it('should return deserialized Model object if success', async () => {
      const obj = { item: 10 }
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      const result = await processor.extractResponse('TAG', TestResponse.success(obj), (_) => resultValue(obj))
      expect(result).toStrictEqual(obj)
    })
  })
  describe('extractUnvalidatedResponse', () => {
    it('should respond with Payload Error if body is null', (done) => {
      const processor = new ResponseProcessor(new TestSerializer())
      processor
        .extractUnvalidatedResponse(
          'TAG',
          {
            code: jest.fn().mockReturnValue(200),
            body: jest.fn().mockReturnValue(null),
            headers: jest.fn().mockReturnValue(new Map()),
            isSuccessful: jest.fn().mockReturnValue(true),
          } as NetworkResponse,
          (_) => resultValue(getVoid()),
        )
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.noPayload)
          expect((err as TransportError).message).toBe('No payload in response on TAG')
          done()
        })
    })
    it('should respond with Serialization Error if body deserialization fails', (done) => {
      const processor = new ResponseProcessor(
        createMockInstance(TestSerializer, {
          deserialize: jest.fn().mockReturnValue(resultError(new YSError('ERROR'))),
        }),
      )
      processor
        .extractUnvalidatedResponse('TAG', new TestResponse(200, '{sample: "OK"}'), (_) => resultValue(getVoid()))
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.jsonSerializationError)
          expect((err as TransportError).message).toBe('Error deserializing response on TAG: ERROR')
          done()
        })
    })
    it('should respond with Error if body Model object deserialization fails', (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractUnvalidatedResponse('TAG', TestResponse.success({}), (_) => resultError(new YSError('ERROR')))
        .failed((err) => {
          expect(err).toBeInstanceOf(YSError)
          expect((err as YSError).message).toBe('ERROR')
          done()
        })
    })
    it('should respond with Bad Status if the response is not of 2xx code', (done) => {
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      processor
        .extractUnvalidatedResponse('TAG', TestResponse.fail(501, 'ERROR'), (_) => resultValue(getVoid()))
        .failed((err) => {
          expect(err).toBeInstanceOf(TransportError)
          expect((err as TransportError).code).toBe(TransportErrorCodes.badStatusCode)
          expect((err as TransportError).message).toBe('Bad status code on TAG: 501')
          done()
        })
    })
    it('should return deserialized Model object if success', async () => {
      const obj = { item: 10 }
      const processor = new ResponseProcessor(new DefaultJSONSerializer())
      const result = await processor.extractUnvalidatedResponse('TAG', TestResponse.success(obj), (_) =>
        resultValue(obj),
      )
      expect(result).toStrictEqual(obj)
    })
  })
  it('should support extracting deserialized model deserialized Model object if success', async () => {
    const obj = { item: 10 }
    const processor = new ResponseProcessor(new DefaultJSONSerializer())
    const result = await processor.extractUnvalidatedResponse('TAG', TestResponse.success(obj), (_) => resultValue(obj))
    expect(result).toStrictEqual(obj)
  })
})
