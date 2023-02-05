import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { ComposeSendingEvaluation } from '../../../code/mail/scenarios/compose/compose-sending-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Compose sending event evaluation', () => {
  it('should be correct for successful scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.sendMessage())

    const evaluations = [new ComposeSendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [true])
    done()
  })

  it('should be correct for unsuccessful scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.pressBack(false))

    const evaluations = [new ComposeSendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  })

  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new ComposeSendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  })

  it('should be false for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.composeEvents.editBody())
      .thenEvent(Eventus.composeEvents.editBody())

    const evaluations = [new ComposeSendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  })
})
