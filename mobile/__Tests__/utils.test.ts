import * as assert from 'assert'
import { ID, idFromString } from '../../xpackages/mapi/code/api/common/id'
import { RequestPerformer } from '../code/mobapi/request-performer'
import { MessageBody } from '../code/model/message-body'
import { MessageHeader } from '../code/model/message-header'
import { CreateNewMailScenario } from '../code/scenario/mails/create-new-mail'
import { GoThroughMailsScenario } from '../code/scenario/mails/go-through-mails'
import { MailProcessingScenario } from '../code/scenario/mails/mail-processing'
import { RegularScenarioResponse, ScenarioResponse } from '../code/scenario/scenario-response'
import { Scenario } from '../code/scenario/scenario'

export const defaultMid = idFromString('13')!

export const buildMessage = (mid: ID = idFromString('1')!, from = '', subject = ''): MessageHeader =>
  new MessageHeader(mid, from, subject)

export const messagesList = (
  mid: ID = idFromString('1')!,
  from = '',
  subject = '',
  count = 1,
): readonly MessageHeader[] => {
  const messages: MessageHeader[] = [buildMessage(mid, from, subject)]
  for (let i = 0; i < count - 1; i++) {
    messages.push(buildMessage())
  }
  return messages
}

export const messageBodies = (content = ''): MessageBody => new MessageBody(content, 'rfcId')

export const scenariosStack = (requestPerformer: RequestPerformer): readonly Scenario[] => {
  const messages = messagesList(defaultMid, 'Asya', 'New Year')
  const goThroughMailsScenario = GoThroughMailsScenario.build(requestPerformer, messages, messages.length, false)
  const createNewMailScenario = CreateNewMailScenario.build(
    CreateNewMailScenario.State.waitingForBody,
    defaultMid,
    buildMessage(defaultMid, 'Asya', 'Ho-ho-ho'),
    new MessageBody('content', 'rfcId'),
    'Anna',
    'Christmas',
    'text',
    [],
    requestPerformer,
  )
  const mailProcessingScenario = MailProcessingScenario.build(
    defaultMid,
    buildMessage(defaultMid, 'Asya', 'Bad Santa'),
    messageBodies('text'),
    false,
    false,
    requestPerformer,
  )

  return [createNewMailScenario, mailProcessingScenario, goThroughMailsScenario]
}

export function assertStrictEqualScenarioResponseToValue(response: ScenarioResponse, value: string): void {
  if (response instanceof RegularScenarioResponse) {
    assert.strictEqual(value, response.outputSpeech())
  } else {
    assert.fail('Wrong resolve value type')
  }
}
