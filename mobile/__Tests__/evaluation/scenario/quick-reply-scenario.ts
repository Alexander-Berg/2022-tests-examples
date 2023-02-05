import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { MessageDTO } from '../../../../xpackages/eventus-common/code/objects/message'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { DefaultScenarios } from '../../../code/mail/scenarios/default-scenarios'
import { Scenario } from '../../../code/scenario'
import { setIncrementalTimeline } from '../../utils/utils'

describe('Quick reply scenario', () => {
  it('should evaluate all fields', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Eventus.startEvents.startWithMessageListShow())
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), null)]))
        .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
        .thenEvent(Eventus.quickReplyEvents.clicked())
        .thenEvent(Eventus.quickReplyEvents.editBody())
        .thenEvent(Eventus.messageViewEvents.backToMailList()),
      new Scenario()
        .thenEvent(Eventus.startEvents.startWithMessageListShow())
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), null)]))
        .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
        .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
        .thenEvent(Eventus.quickReplyEvents.editBody())
        .thenEvent(Eventus.quickReplyEvents.sendMessage())
        .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(0))),
    ]
    const runner = new AnalyticsRunner()
    const requiredAttributes = [
      'start_timestamp_ms',
      'finish_timestamp_ms',
      'duration_scenario_ms',
      'scenario_type',
      'mid',
      'receive_timestamp',
      'sending',
    ]

    for (const session of sessions) {
      setIncrementalTimeline(session)
      const evaluations = [DefaultScenarios.quickReplyScenario()]

      const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())
      const attributes = results.get(evaluations[0].name())!
      for (const scenarioAttribute of attributes) {
        for (const attribute of requiredAttributes) {
          scenarioAttribute.attributes.has(attribute)
        }
      }
    }
    done()
  })
})
