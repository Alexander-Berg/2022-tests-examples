import { int64 } from '../../../common/ys'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { MessageDTO } from '../../../xpackages/eventus-common/code/objects/message'
import { Scenario } from '../../code/scenario'
import { checkScenario, refreshProvider } from '../utils/utils'

describe('Map aggregator', () => {
  refreshProvider()

  it('should ', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1))),
      new Scenario()
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1))),
    )
    done()
  })

  it('should change aggregator to default', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1))),
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1))),
    )
    done()
  })

  it('should change aggregator to default', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(2)))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(2), int64(0), null)])),
      new Scenario()
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(0)))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(0), null)]))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(1)))
        .thenEvent(Eventus.quickReplyEvents.closeAllSmartReplies(int64(2)))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(2), int64(0), null)])),
    )
    done()
  })
})
