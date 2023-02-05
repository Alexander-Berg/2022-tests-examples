import { int64 } from '../../../../../../common/ys'
import { MessageBodyDescriptor } from '../../../../../xmail/../mapi/code/api/entities/body/message-body'
import {
  MessageBodyLoaderPartial,
  NoOpMessageBodyLoaderPartial,
} from '../../../../code/busilogics/compose/message-body-loader-partial'

describe(NoOpMessageBodyLoaderPartial, () => {
  it('should be creatable', (done) => {
    const partialBodyLoader: MessageBodyLoaderPartial = new NoOpMessageBodyLoaderPartial()
    const descr = new MessageBodyDescriptor(int64(123), null, null)
    partialBodyLoader.invalidateBodyCache(descr)
    partialBodyLoader.getMessagesBodies([descr]).then((bodiesMap) => {
      expect(bodiesMap.size).toBe(0)
      done()
    })
  })
})
