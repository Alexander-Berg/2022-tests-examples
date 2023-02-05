import { reject, resolve } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { HistoryItem } from '../../../code/network/mobile-backend/entities/history/history-entities'
import { HistoryQuery } from '../../../code/models/history-query'
import { Payer } from '../../../code/models/payer'
import { HistoryService } from '../../../code/busilogics/history-service'
import { MobileBackendApi } from '../../../code/network/mobile-backend/mobile-backend-api'
import { PaymentsHistoryResponse } from '../../../code/network/mobile-backend/entities/history/payments-history-response'

const payer = new Payer('token', '123', 'test@ya.ru')
const query = new HistoryQuery('id', 'token', 'merchant', 'success', null, null, null, null)

describe(HistoryService, () => {
  it('should get items on success history', (done) => {
    const items = [new HistoryItem('key', 'token', '1234', 'success', [], 'id', 'merchant', 'url', 'uid')]
    const mobile = createMockInstance(MobileBackendApi, {
      paymentHistory: jest.fn().mockReturnValue(resolve(new PaymentsHistoryResponse(items))),
    })
    const historyService = new HistoryService(payer, mobile)

    expect.assertions(1)
    historyService.history(query).then((res) => {
      expect(res).toStrictEqual(items)
      done()
    })
  })

  it('should get error on history failed', (done) => {
    const error = new YSError('error')
    const mobile = createMockInstance(MobileBackendApi, {
      paymentHistory: jest.fn().mockReturnValue(reject(error)),
    })
    const historyService = new HistoryService(payer, mobile)

    expect.assertions(1)
    historyService.history(query).failed((res) => {
      expect(res).toStrictEqual(error)
      done()
    })
  })
})
