import * as assert from 'assert'
import { MilesError } from '../../code/scenario/miles-error'
import { UnreadMailsScenario } from '../../code/scenario/mails/unread-mails'
import { IrrelevantScenarioResponse, RegularScenarioResponse } from '../../code/scenario/scenario-response'
import { SemanticFrame } from '../../code/scenario/semantic-frame'
import { RequestPerformerMock } from '../__mock__/request-performer.mock'
import { RunnerMock } from '../__mock__/runner.mock'
import { assertStrictEqualScenarioResponseToValue, defaultMid, messagesList } from '../utils.test'

describe('UnreadMails', () => {
  const sameFrames = [
    SemanticFrame.unreadCount,
    SemanticFrame.unreadFrom,
    SemanticFrame.unreadTheme,
    SemanticFrame.createNewMail,
  ]
  const positiveFrames = [SemanticFrame.goThroughMails, SemanticFrame.yesAnswer]
  const negativeFrames = [SemanticFrame.noAnswer, SemanticFrame.discardChanges]
  const knownFrames = positiveFrames.concat(negativeFrames).concat(sameFrames)
  const unknownFrames = Object.values(SemanticFrame).filter((element) => !knownFrames.includes(element))

  // MARK :- run

  it('run success -> load messages', async () => {
    const messages = messagesList(defaultMid, '', '', 10)
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const scenario = new UnreadMailsScenario(performer)

    try {
      const response = await scenario.run()
      assertStrictEqualScenarioResponseToValue(response, scenario.unreadMailsAndGoThrough(messages))
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('run fail -> error', async () => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.reject('500')
    }
    const scenario = new UnreadMailsScenario(performer)

    try {
      await scenario.run()
      assert.fail('UnreadMailsScenario scenario should fail')
    } catch (reason) {
      assert.strictEqual(MilesError.dataLoadingFailed, reason)
    }
  })

  // MARK :- handle

  it('handle unknown frames without forwarding -> irrelevant response', async () => {
    for (const frame of unknownFrames) {
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const scenario = new UnreadMailsScenario(performer)

      try {
        const value = await scenario.handle(frame)
        assert.ok(value instanceof IrrelevantScenarioResponse)
      } catch (e) {
        assert.fail('UnreadMailsScenario scenario has failed')
      }
    }
  })

  it('handle unknown frames with forwarding -> irrelevant response', async () => {
    for (const frame of unknownFrames) {
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const scenario = new UnreadMailsScenario(performer)
      const unknownFrame = 'unknown_frame'
      scenario.forwardInvocation = () => {
        return Promise.resolve(new RegularScenarioResponse(unknownFrame))
      }

      try {
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, unknownFrame)
      } catch (e) {
        assert.fail('UnreadMailsScenario scenario has failed')
      }
    }
  })

  it('handle positive frames -> run go thru scenario', async () => {
    for (const frame of positiveFrames) {
      const goThruMails = 'go_through_mails'
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const runner = new RunnerMock()
      runner.goThroughMailsScenarioPromise = () => {
        return Promise.resolve(new RegularScenarioResponse(goThruMails))
      }
      const scenario = new UnreadMailsScenario(performer)
      scenario.runner = runner

      try {
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, goThruMails)
      } catch (e) {
        assert.fail('UnreadMailsScenario scenario has failed')
      }
    }
  })

  it('handle same frame -> run unread scenario', async () => {
    const performer = new RequestPerformerMock()
    const messages = messagesList(defaultMid, '', '', 1)
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messages)
    }
    const scenario = new UnreadMailsScenario(performer)

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.unreadCount)
      assertStrictEqualScenarioResponseToValue(response, scenario.unreadMailsAndGoThrough(messages))
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
    const scenario = new UnreadMailsScenario(performer)
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
    const scenario = new UnreadMailsScenario(performer)
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
    const scenario = new UnreadMailsScenario(performer)
    scenario.runner = runner

    try {
      await scenario.run()
      const response = await scenario.handle(SemanticFrame.createNewMail)
      assertStrictEqualScenarioResponseToValue(response, responseMessage)
    } catch (e) {
      assert.fail('UnreadMailsScenario scenario has failed')
    }
  })

  it('handle negative frames -> cancel scenario', async () => {
    for (const frame of negativeFrames) {
      const performer = new RequestPerformerMock()
      performer.loadLastUnreadMessagesPromise = () => {
        return Promise.resolve(messagesList(defaultMid, '', '', 1))
      }
      const scenario = new UnreadMailsScenario(performer)

      try {
        const response = await scenario.handle(frame)
        assertStrictEqualScenarioResponseToValue(response, UnreadMailsScenario.discardScenarioText())
      } catch (e) {
        assert.fail('UnreadMailsScenario scenario has failed')
      }
    }
  })

  // MARK :- commit

  it('commit -> error', async () => {
    for (const frame of Object.values(SemanticFrame)) {
      const scenario = new UnreadMailsScenario(new RequestPerformerMock())

      try {
        await scenario.commit(frame)
        assert.fail('UnreadMailsScenario scenario should fail')
      } catch (reason) {
        assert.strictEqual(MilesError.commitDeprecated, reason)
      }
    }
  })

  // MARK :- cancellation

  it('handle positive frame -> cancel scenario', (done) => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 1))
    }
    const runner = new RunnerMock()
    runner.goThroughMailsScenarioPromise = () => {
      return Promise.resolve(new RegularScenarioResponse(''))
    }
    const scenario = new UnreadMailsScenario(performer)
    scenario.runner = runner
    scenario.cancellation = () => {
      done()
    }

    scenario.handle(SemanticFrame.yesAnswer).catch(() => {
      assert.fail('UnreadMailsScenario scenario has failed')
      done()
    })
  })

  it('handle negative frame -> cancel scenario', (done) => {
    const performer = new RequestPerformerMock()
    performer.loadLastUnreadMessagesPromise = () => {
      return Promise.resolve(messagesList(defaultMid, '', '', 1))
    }
    const scenario = new UnreadMailsScenario(performer)
    scenario.cancellation = () => {
      done()
    }

    scenario.handle(SemanticFrame.noAnswer).catch(() => {
      assert.fail('UnreadMailsScenario scenario has failed')
      done()
    })
  })
})
