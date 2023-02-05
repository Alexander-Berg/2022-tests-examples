import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { MessageDTO } from '../../../../xpackages/eventus-common/code/objects/message'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { DefaultScenarios } from '../../../code/mail/scenarios/default-scenarios'
import { Scenario } from '../../../code/scenario'
import { setIncrementalTimeline } from '../../utils/utils'

describe('Compose scenario', () => {
  it('should evaluate all fields', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Eventus.startEvents.startWithMessageListShow())
        .thenEvent(Eventus.messageListEvents.writeNewMessage())
        .thenEvent(Eventus.composeEvents.editBody())
        .thenEvent(Eventus.composeEvents.addAttachments(2))
        .thenEvent(Eventus.composeEvents.pressBack(false)),
      new Scenario()
        .thenEvent(Eventus.startEvents.startWithMessageListShow())
        .thenEvent(Eventus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10), null)]))
        .thenEvent(Eventus.messageListEvents.openMessage(0, int64(0)))
        .thenEvent(Eventus.messageViewEvents.replyAll(1))
        .thenEvent(Eventus.composeEvents.editBody())
        .thenEvent(Eventus.composeEvents.addAttachments(2))
        .thenEvent(Eventus.composeEvents.editBody())
        .thenEvent(Eventus.composeEvents.sendMessage())
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
      const evaluations = [DefaultScenarios.composeScenario()]

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
