import { int64 } from '../../../common/ys'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { MessageDTO } from '../../../xpackages/eventus-common/code/objects/message'
import { Scenario } from '../../code/scenario'
import { checkScenario, refreshProvider } from '../utils/utils'

describe('Message list sync aggregator', () => {
  refreshProvider()

  it('should aggregate only sync events', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.messageViewEvents.backToMailList())
        .thenEvent(Eventus.messageViewEvents.backToMailList()),
      new Scenario()
        .thenEvent(Eventus.messageViewEvents.backToMailList())
        .thenEvent(Eventus.messageViewEvents.backToMailList()),
    )
    done()
  })
  it('should aggregate one sync event', (done) => {
    checkScenario(
      new Scenario().thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)])),
      new Scenario().thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)])),
    )
    done()
  })
  it('should aggregate distinct sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1), null)])),
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1), null)])),
    )
    done()
  })
  it('should not send duplicate sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1), null)]))
        .thenEvent(
          Eventus.modelSyncEvents.updateMessageList([
            new MessageDTO(int64(0), int64(2), null),
            new MessageDTO(int64(1), int64(2), null),
          ]),
        ),
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1), null)])),
    )
    done()
  })
  it('should send updates only for sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(
          Eventus.modelSyncEvents.updateMessageList([
            new MessageDTO(int64(1), int64(1), null),
            new MessageDTO(int64(0), int64(0), null),
          ]),
        ),
      new Scenario()
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0), null)]))
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1), null)])),
    )
    done()
  })
})
