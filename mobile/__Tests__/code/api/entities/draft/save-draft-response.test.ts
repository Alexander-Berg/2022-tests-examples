import { int64 } from '../../../../../../../common/ys'
import { IntegerJSONItem } from '../../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { MessageBodyAttach } from '../../../../../code/api/entities/body/message-body'
import {
  NetworkStatus,
  NetworkStatusCode,
  networkStatusFromJSONItem,
} from '../../../../../code/api/entities/status/network-status'
import {
  SaveDraftResponse,
  saveDraftResponseFromJSONItem,
} from '../../../../../code/api/entities/draft/save-draft-response'

describe(SaveDraftResponse, () => {
  describe(networkStatusFromJSONItem, () => {
    it('should return null if JSON Item is malformed', () => {
      expect(saveDraftResponseFromJSONItem(IntegerJSONItem.fromInt32(10))).toBeNull()
      expect(saveDraftResponseFromJSONItem(JSONItemFromJSON({}))).toBeNull()
    })
    it('should return ok status if JSON Item is valid', () => {
      expect(
        saveDraftResponseFromJSONItem(
          JSONItemFromJSON({
            attachments: {
              attachment: [],
            },
            fid: int64(2),
            mid: int64(2),
            status: {
              status: NetworkStatusCode.ok,
            },
          }),
        )!.networkStatus().code,
      ).toBe(NetworkStatusCode.ok)
    })
    it('should return null in attachments field if JSON does not have attachments key', () => {
      expect(
        saveDraftResponseFromJSONItem(
          JSONItemFromJSON({
            fid: int64(1),
            mid: int64(2),
            status: {
              status: NetworkStatusCode.ok,
            },
          }),
        ),
      ).toStrictEqual(new SaveDraftResponse(new NetworkStatus(NetworkStatusCode.ok), int64(2), int64(1), null))
    })
    it('should return null in attachments field if JSON have attachments key but does not have inner attachment key', () => {
      expect(
        saveDraftResponseFromJSONItem(
          JSONItemFromJSON({
            attachments: {},
            fid: int64(1),
            mid: int64(2),
            status: {
              status: NetworkStatusCode.ok,
            },
          }),
        ),
      ).toStrictEqual(new SaveDraftResponse(new NetworkStatus(NetworkStatusCode.ok), int64(2), int64(1), null))
    })
    it('should return error if JSON Item is one of an error', () => {
      expect(
        saveDraftResponseFromJSONItem(
          JSONItemFromJSON({
            attachments: [],
            fid: int64(-1),
            mid: int64(-1),
            status: {
              phrase: 'PHRASE',
              status: 2,
              trace: 'TRACE',
            },
          }),
        ),
      ).toStrictEqual(
        new SaveDraftResponse(
          new NetworkStatus(NetworkStatusCode.temporaryError, 'TRACE', 'PHRASE'),
          int64(-1),
          int64(-1),
          [],
        ),
      )
    })
    it('should parse JSON Item as SaveDraftResponse without attachments', () => {
      const result = saveDraftResponseFromJSONItem(
        JSONItemFromJSON({
          attachments: {
            attachment: [],
          },
          fid: int64(1),
          mid: int64(1),
          status: {
            status: NetworkStatusCode.ok,
          },
        }),
      )
      expect(result).toStrictEqual(
        new SaveDraftResponse(new NetworkStatus(NetworkStatusCode.ok), int64(1), int64(1), []),
      )
    })
    it('should parse JSON Item as SaveDraftResponse with attachments', () => {
      const result = saveDraftResponseFromJSONItem(
        JSONItemFromJSON({
          attachments: {
            attachment: [
              {
                attachClass: null,
                content_id: '111111',
                display_name: 'someAttach.pdf',
                download_url: 'https://yandex.ru',
                hid: '1.1',
                isDisk: false,
                is_inline: false,
                mime_type: 'pdf',
                preview_supported: false,
                size: int64(32000),
              },
            ],
          },
          fid: int64(1),
          mid: int64(1),
          status: {
            status: NetworkStatusCode.ok,
          },
        }),
      )
      expect(result).toStrictEqual(
        new SaveDraftResponse(new NetworkStatus(NetworkStatusCode.ok), int64(1), int64(1), [
          new MessageBodyAttach(
            '1.1',
            'someAttach.pdf',
            int64(32000),
            'https://yandex.ru',
            false,
            false,
            false,
            null,
            'pdf',
            '111111',
          ),
        ]),
      )
    })
  })
})
