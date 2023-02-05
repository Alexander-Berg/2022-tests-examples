import * as assert from 'assert'
import { MessageHeader } from '../../code/model/message-header'
import { GoThroughMailsScenario } from '../../code/scenario/mails/go-through-mails'
import { MilesError } from '../../code/scenario/miles-error'
import { IrrelevantScenarioResponse, RegularScenarioResponse } from '../../code/scenario/scenario-response'
import { SemanticFrame } from '../../code/scenario/semantic-frame'
import { RequestPerformerMock } from '../__mock__/request-performer.mock'
import { RunnerMock } from '../__mock__/runner.mock'
import { assertStrictEqualScenarioResponseToValue, buildMessage, defaultMid, messagesList } from '../utils.test'

describe('GoThroughMailsScenario', () => {
  const sameFrames = [
    SemanticFrame.goThroughMails,
    SemanticFrame.unreadCount,
    SemanticFrame.unreadFrom,
    SemanticFrame.unreadTheme,
    SemanticFrame.createNewMail,
  ]

  const mailProcessingFrames = [
    SemanticFrame.readMail,
    SemanticFrame.replyMail,
    SemanticFrame.deleteMail,
    SemanticFrame.markAsImportantMail,
    SemanticFrame.markAsReadMail,
  ]

  const knownFrames = [
    SemanticFrame.nextMail,
    SemanticFrame.yesAnswer,
    SemanticFrame.noAnswer,
    SemanticFrame.discardChanges,
  ]
    .concat(mailProcessingFrames)
    .concat(sameFrames)

  const unknownFrames = Object.values(SemanticFrame).filter((element) => !knownFrames.includes(element))

  // MARK :- run method (messages, empty, error)

  it('run success -> messages list', async () => {
    const subject = 'mail subject'
    const from = 'mail_from@ya.ru'
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, from, subject, 10))
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      const response = await scenario.run()
      assertStrictEqualScenarioResponseToValue(
        response,
        GoThroughMailsScenario.topUnreadMessagesText(from, subject, true),
      )
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('run success without messages -> empty', async () => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve([])
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      const response = await scenario.run()
      assertStrictEqualScenarioResponseToValue(response, GoThroughMailsScenario.noUnreadMessagesText())
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('run fail -> error', async () => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.reject('500')
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      await scenario.run()
      assert.fail('GoThroughMails scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
    }
  })

  // MARK :- handle semantic frames (wrong state, forwarding)

  it('handle frame without messages -> wrong state', async () => {
    const scenario = new GoThroughMailsScenario(new RequestPerformerMock())

    try {
      await scenario.handle(SemanticFrame.nextMail)
      assert.fail('GoThroughMails scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.wrongScenarioState, reason)
    }
  })

  // MARK :- handle unknown semantic frames

  it('handle unknown frame without forwarding -> irrelevant response', async () => {
    for (const frame of unknownFrames) {
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const scenario = new GoThroughMailsScenario(performer)

      try {
        await scenario.run()
        const response = await scenario.handle(frame)
        assert.ok(response instanceof IrrelevantScenarioResponse)
      } catch (e) {
        assert.fail('GoThroughMails scenario has failed')
      }
    }
  })

  // MARK :- handle unknown semantic frames with forwarding

  it('forward unknown frame -> custom response', async () => {
    for (const frame of unknownFrames) {
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const scenario = new GoThroughMailsScenario(performer)
      const unknownFrame = 'unknown_frame'
      scenario.forwardInvocation = () => {
        return Promise.resolve(new RegularScenarioResponse(unknownFrame))
      }

      try {
        await scenario.run()
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, unknownFrame)
      } catch (e) {
        assert.fail('GoThroughMails scenario has failed')
      }
    }
  })

  // MARK :- handle known semantic frames

  it('handle nextMail frame -> go to next', async () => {
    const from = 'mail_from@ya.ru'
    const subject = 'mail_subject'
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve([buildMessage(), buildMessage(defaultMid, from, subject)])
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.nextMail)
      assertStrictEqualScenarioResponseToValue(
        response,
        GoThroughMailsScenario.topUnreadMessagesText(from, subject, false),
      )
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('handle known frame -> run mail processing', async () => {
    for (const frame of mailProcessingFrames) {
      const messages = messagesList(defaultMid, '', '', 1)
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messages)
      }
      const runner = new RunnerMock()
      const mailProcessingResponse = 'run_mail_processing_scenario'
      runner.mailProcessingScenarioPromise = (message: MessageHeader, initialFrame: SemanticFrame) => {
        assert.strictEqual(message, messages[0])
        assert.strictEqual(initialFrame, frame)
        return Promise.resolve(new RegularScenarioResponse(mailProcessingResponse))
      }
      const scenario = new GoThroughMailsScenario(performer)
      scenario.runner = runner

      try {
        await scenario.run()
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, mailProcessingResponse)
      } catch (e) {
        assert.fail('GoThroughMails scenario has failed')
      }
    }
  })

  it('handle go_thru -> run go thru processing', async () => {
    const from = 'mail_from@ya.ru@ya.ru'
    const subject = 'mail_subject'
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve([buildMessage(defaultMid, from, subject)])
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.goThroughMails)
      assertStrictEqualScenarioResponseToValue(
        response,
        GoThroughMailsScenario.topUnreadMessagesText(from, subject, true),
      )
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('handle unread frame -> run unread scenario', async () => {
    const responseMessage = 'run unread-from scenario'
    const performer = new RequestPerformerMock()
    const messages = messagesList(defaultMid, '', '', 1)
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const runner = new RunnerMock()
    runner.totalUnreadMailsPromise = () => {
      return Promise.resolve(new RegularScenarioResponse(responseMessage))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.runner = runner

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.unreadCount)
      assertStrictEqualScenarioResponseToValue(response, responseMessage)
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('handle from frame -> run unread-from scenario', async () => {
    const responseMessage = 'run unread-from scenario'
    const performer = new RequestPerformerMock()
    const messages = messagesList(defaultMid, '', '', 1)
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const runner = new RunnerMock()
    runner.unreadMailsGroupedBySenderPromise = () => {
      return Promise.resolve(new RegularScenarioResponse(responseMessage))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.runner = runner

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.unreadFrom)
      assertStrictEqualScenarioResponseToValue(response, responseMessage)
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('handle theme frame -> run unread-theme scenario', async () => {
    const responseMessage = 'run unread-theme scenario'
    const performer = new RequestPerformerMock()
    const messages = messagesList(defaultMid, '', '', 1)
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const runner = new RunnerMock()
    runner.unreadMailsGroupedByTopicPromise = () => {
      return Promise.resolve(new RegularScenarioResponse(responseMessage))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.runner = runner

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.unreadTheme)
      assertStrictEqualScenarioResponseToValue(response, responseMessage)
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('handle new mail -> run create-new-mail scenario', async () => {
    const responseMessage = 'run create-new-mail scenario'
    const performer = new RequestPerformerMock()
    const messages = messagesList(defaultMid, '', '', 1)
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const runner = new RunnerMock()
    runner.newMailScenarioPromise = () => {
      return Promise.resolve(new RegularScenarioResponse(responseMessage))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.runner = runner

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.createNewMail)
      assertStrictEqualScenarioResponseToValue(response, responseMessage)
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('handle yes frame -> go to next', async () => {
    const from = 'mail_from@ya.ru'
    const subject = 'mail_subject'
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve([buildMessage(), buildMessage(defaultMid, from, subject)])
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.yesAnswer)
      assertStrictEqualScenarioResponseToValue(
        response,
        GoThroughMailsScenario.topUnreadMessagesText(from, subject, false),
      )
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('handle no frame -> go to next', async () => {
    for (const frame of [SemanticFrame.noAnswer, SemanticFrame.discardChanges]) {
      const from = 'mail_from@ya.ru'
      const subject = 'mail_subject'
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve([buildMessage(), buildMessage(defaultMid, from, subject)])
      }
      const scenario = new GoThroughMailsScenario(performer)

      try {
        await scenario.run()
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, GoThroughMailsScenario.discardScenarioText())
      } catch (e) {
        assert.fail('GoThroughMails scenario has failed')
      }
    }
  })

  // MARK :- commit

  it('commit -> error', async () => {
    for (const frame of Object.values(SemanticFrame)) {
      const scenario = new GoThroughMailsScenario(new RequestPerformerMock())

      try {
        await scenario.commit(frame)
        assert.fail('UnreadMailsScenario scenario should fail')
      } catch (reason) {
        assert.strictEqual(MilesError.commitDeprecated, reason)
      }
    }
  })

  // MARK :- cancellation

  it('next on last message -> cancel', (done) => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 1))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.cancellation = () => {
      done()
    }

    scenario.run().then(() => {
      scenario.handle(SemanticFrame.nextMail).catch(() => {
        assert.fail('GoThroughMails scenario has failed')
        done()
      })
    })
  })

  it('handle no frame -> cancel', (done) => {
    const from = 'mail_from@ya.ru'
    const subject = 'mail_subject'
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve([buildMessage(), buildMessage(defaultMid, from, subject)])
    }
    const scenario = new GoThroughMailsScenario(performer)

    scenario.cancellation = () => {
      done()
    }

    scenario.run().then(() => {
      scenario.handle(SemanticFrame.noAnswer).catch(() => {
        assert.fail('GoThroughMails scenario has failed')
        done()
      })
    })
  })

  it('next on non-last message -> cancel not called', async () => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 2))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.cancellation = () => {
      assert.fail('Cancellation callback should not be called')
    }

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.nextMail)
      assert.ok(response instanceof RegularScenarioResponse)
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  // MARK :- delete top mail

  it('delete non last message -> shift', async () => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 2))
    }
    const scenario = new GoThroughMailsScenario(performer)

    try {
      await scenario.run()
      scenario.cancellation = () => {
        assert.fail('Cancellation callback should not be called')
      }
      scenario.deleteTopMessage()
      // tslint:disable-next-line:no-empty
      scenario.cancellation = () => {}
      const value = await scenario.handle(SemanticFrame.nextMail)
      assert.ok(value instanceof RegularScenarioResponse)
    } catch (e) {
      assert.fail('GoThroughMails scenario has failed')
    }
  })

  it('delete message -> set flag wasTopMessageDeleted', (done) => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 1))
    }
    const scenario = new GoThroughMailsScenario(performer)
    scenario.run().then(() => {
      scenario.deleteTopMessage()
      if (scenario.topMessageDeleted()) {
        done()
      }
    })
  })
})
