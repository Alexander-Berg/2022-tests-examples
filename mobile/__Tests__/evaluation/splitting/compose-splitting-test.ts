import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { FullScenarioEvaluation } from '../../../code/evaluations/general-evaluations/default/full-scenario-evaluation'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { ComposeScenarioSplitter } from '../../../code/mail/scenarios/compose/compose-scenario-splitter'
import { Scenario } from '../../../code/scenario'
import { checkSplitterEvaluationResults } from '../../utils/utils'

describe('Compose splitting should split correctly', () => {
  it('for one full scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.composeEvents.pressBack(true))

    const evaluations = [
      new ComposeScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  })

  it('for one scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.deleteMessage(1, int64(2)))
      .thenEvent(Eventus.messageListEvents.deleteMessage(1, int64(2)))
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.composeEvents.pressBack(true))
      .thenEvent(Eventus.messageViewEvents.backToMailList())
      .thenEvent(Eventus.messageViewEvents.backToMailList())

    const evaluations = [
      new ComposeScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.messageListEvents.writeNewMessage())
          .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
          .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
          .thenEvent(Eventus.composeEvents.pressBack(true)),
      ],
    ])
    done()
  })

  it('for scenario interaction', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.writeNewMessage())
      .thenEvent(Eventus.startEvents.startFromMessageNotification())

    const evaluations = [
      new ComposeScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [new Scenario().thenEvent(Eventus.messageListEvents.writeNewMessage())],
    ])
    done()
  })

  it('for two scenarios', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.markMessageAsUnread(1, int64(2)))
      .thenEvent(Eventus.messageListEvents.openMessageActions(1, int64(2)))
      .thenEvent(Eventus.messageActionsEvents.reply())
      .thenEvent(Eventus.messageActionsEvents.cancel())
      .thenEvent(Eventus.messageActionsEvents.delete())
      .thenEvent(Eventus.composeEvents.pressBack(true))
      .thenEvent(Eventus.messageActionsEvents.replyAll())
      .thenEvent(Eventus.composeEvents.editBody(14))
      .thenEvent(Eventus.composeEvents.editBody(44))
      .thenEvent(Eventus.composeEvents.sendMessage())
      .thenEvent(Eventus.messageViewEvents.backToMailList())

    const evaluations = [
      new ComposeScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.messageActionsEvents.reply())
          .thenEvent(Eventus.messageActionsEvents.cancel())
          .thenEvent(Eventus.messageActionsEvents.delete())
          .thenEvent(Eventus.composeEvents.pressBack(true)),
      ],
      [
        new Scenario()
          .thenEvent(Eventus.messageActionsEvents.replyAll())
          .thenEvent(Eventus.composeEvents.editBody(14))
          .thenEvent(Eventus.composeEvents.editBody(44))
          .thenEvent(Eventus.composeEvents.sendMessage()),
      ],
    ])
    done()
  })

  it('for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageViewEvents.editDraft(1))
      .thenEvent(Eventus.groupActionsEvents.selectMessage(0, int64(1)))
      .thenEvent(Eventus.groupActionsEvents.selectMessage(1, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [
      new ComposeScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.messageViewEvents.editDraft(1))
          .thenEvent(Eventus.groupActionsEvents.selectMessage(0, int64(1)))
          .thenEvent(Eventus.groupActionsEvents.selectMessage(1, int64(1)))
          .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1))),
      ],
    ])
    done()
  })
})
