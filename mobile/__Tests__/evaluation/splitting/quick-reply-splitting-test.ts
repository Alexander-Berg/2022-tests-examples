import { int64 } from '../../../../common/ys'
import { Eventus } from '../../../../xpackages/eventus/code/events/eventus'
import { AnalyticsRunner } from '../../../code/analytics-runner'
import { FullScenarioEvaluation } from '../../../code/evaluations/general-evaluations/default/full-scenario-evaluation'
import { MailContextApplier } from '../../../code/mail/mail-context-applier'
import { QuickReplyScenarioSplitter } from '../../../code/mail/scenarios/compose/quick-reply-scenario-splitter'
import { Scenario } from '../../../code/scenario'
import { checkSplitterEvaluationResults } from '../../utils/utils'

describe('Quick splitting should split correctly', () => {
  it('for one full scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.quickReplyEvents.editBody())
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
      .thenEvent(Eventus.quickReplyEvents.closeSmartReplyMessage(1, int64(1)))
      .thenEvent(Eventus.messageViewEvents.backToMailList())

    const evaluations = [
      new QuickReplyScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
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
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Eventus.messageViewEvents.openMessageActions(1))
      .thenEvent(Eventus.messageViewEvents.reply(0))
      .thenEvent(Eventus.messageActionsEvents.delete())

    const evaluations = [
      new QuickReplyScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
          .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))
          .thenEvent(Eventus.messageViewEvents.openMessageActions(1))
          .thenEvent(Eventus.messageViewEvents.reply(0)),
      ],
    ])
    done()
  })

  it('for two scenarios', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.messageListEvents.markMessageAsUnread(1, int64(2)))
      .thenEvent(Eventus.messageListEvents.openMessageActions(1, int64(2)))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
      .thenEvent(Eventus.quickReplyEvents.editBody(123))
      .thenEvent(Eventus.quickReplyEvents.openCompose())
      .thenEvent(Eventus.composeEvents.pressBack(true))
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.quickReplyEvents.editBody(34))
      .thenEvent(Eventus.quickReplyEvents.editBody(56))
      .thenEvent(Eventus.quickReplyEvents.sendMessage())
      .thenEvent(Eventus.messageViewEvents.backToMailList())

    const evaluations = [
      new QuickReplyScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
          .thenEvent(Eventus.quickReplyEvents.editBody(123))
          .thenEvent(Eventus.quickReplyEvents.openCompose()),
      ],
      [
        new Scenario()
          .thenEvent(Eventus.quickReplyEvents.clicked())
          .thenEvent(Eventus.quickReplyEvents.editBody(34))
          .thenEvent(Eventus.quickReplyEvents.editBody(56))
          .thenEvent(Eventus.quickReplyEvents.sendMessage()),
      ],
    ])
    done()
  })

  it('for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.quickReplyEvents.clicked())
      .thenEvent(Eventus.quickReplyEvents.editBody())
      .thenEvent(Eventus.groupActionsEvents.selectMessage(1, int64(1)))
      .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [
      new QuickReplyScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.quickReplyEvents.clicked())
          .thenEvent(Eventus.quickReplyEvents.editBody())
          .thenEvent(Eventus.groupActionsEvents.selectMessage(1, int64(1)))
          .thenEvent(Eventus.messageListEvents.openMessage(0, int64(1))),
      ],
    ])
    done()
  })

  it('for scenario with multiple starting events', (done) => {
    const session = new Scenario()
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
      .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(2))

    const evaluations = [
      new QuickReplyScenarioSplitter([(): FullScenarioEvaluation<unknown> => new FullScenarioEvaluation()]),
    ]
    const runner = new AnalyticsRunner()
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [
        new Scenario()
          .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(0))
          .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(1))
          .thenEvent(Eventus.quickReplyEvents.smartReplyMessageClicked(2)),
      ],
    ])
    done()
  })
})
