import { int64 } from '../../../common/ys'
import { Eventus } from '../../../xpackages/eventus/code/events/eventus'
import { Scenario } from '../../code/scenario'
import { checkScenario, refreshProvider } from '../utils/utils'

describe('Report should report events', () => {
  beforeEach(() => {
    refreshProvider()
  })

  it('in one empty scenario', (done) => {
    checkScenario(new Scenario(), new Scenario())
    done()
  })
  it('in one event scenario', (done) => {
    checkScenario(
      new Scenario().thenEvent(Eventus.messageListEvents.openMessage(1, int64(1))),
      new Scenario().thenEvent(Eventus.messageListEvents.openMessage(1, int64(1))),
    )
    done()
  })
  it('in two event scenario', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Eventus.messageListEvents.openMessage(1, int64(1)))
        .thenEvent(Eventus.messageViewEvents.reply(0)),
      new Scenario()
        .thenEvent(Eventus.messageListEvents.openMessage(1, int64(1)))
        .thenEvent(Eventus.messageViewEvents.reply(0)),
    )
    done()
  })
})
