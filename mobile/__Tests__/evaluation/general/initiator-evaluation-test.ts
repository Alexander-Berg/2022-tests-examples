import { int64 } from '../../../../common/ys'
import { EventNames } from '../../../../xpackages/eventus/code/events/event-names'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { InitiatorNameEventEvaluation } from '../../../code/evaluations/general-evaluations/function/default/initiator-name-event-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Initiator evaluation timestamp event evaluation', () => {
  it('should be correct for usual scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.startEvents.startWithMessageListShow())
      .thenEvent(Eventus.messageListEvents.markMessageAsRead(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.deleteMessage(0, int64(2)))

    const evaluations = [new InitiatorNameEventEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.START_WITH_MESSAGE_LIST])
    done()
  })
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new InitiatorNameEventEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  })
})
