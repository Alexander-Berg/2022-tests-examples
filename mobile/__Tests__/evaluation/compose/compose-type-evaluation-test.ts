import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { ComposeTypeEvaluation } from '../../../code/mail/scenarios/compose/compose-type-evaluation'
import { Scenario } from '../../../code/scenario'
import { checkEvaluationsResults } from '../../utils/utils'

describe('Compose type event evaluation', () => {
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, ['unknown'])
    done()
  })

  it('should not be overwritten', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.messageActionsEvents.reply())

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, ['compose'])
    done()
  })

  it('should be unknown for incorrect scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Eventus.startEvents.startFromMessageNotification())
        .thenEvent(Eventus.messageListEvents.writeNewMessage()),
      new Scenario().thenEvent(Eventus.messageListEvents.openMessage(1, int64(0))),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['unknown'])
    }

    done()
  })

  it('should be compose for compose scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Eventus.messageListEvents.writeNewMessage())
        .thenEvent(Eventus.messageListEvents.writeNewMessage()),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['compose'])
    }

    done()
  })

  it('should be reply for reply scenario', (done) => {
    const sessions = [
      new Scenario().thenEvent(Eventus.messageActionsEvents.reply()),
      new Scenario().thenEvent(Eventus.messageViewEvents.reply(0)),
      new Scenario().thenEvent(Eventus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0))),
      new Scenario().thenEvent(Eventus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 0)),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['reply'])
    }

    done()
  })

  it('should be reply_all for reply_all scenario', (done) => {
    const sessions = [
      new Scenario().thenEvent(Eventus.messageActionsEvents.replyAll()),
      new Scenario().thenEvent(Eventus.messageViewEvents.replyAll(0)),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['reply_all'])
    }

    done()
  })

  it('should be resume for resume scenario', (done) => {
    const sessions = [new Scenario().thenEvent(Eventus.messageViewEvents.editDraft(0))]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['resume'])
    }

    done()
  })

  it('should be forward for forward scenario', (done) => {
    const sessions = [new Scenario().thenEvent(Eventus.messageActionsEvents.forward())]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['forward'])
    }

    done()
  })
})
