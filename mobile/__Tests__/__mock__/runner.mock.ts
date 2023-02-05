import { MessageBody } from '../../code/model/message-body'
import { MessageHeader } from '../../code/model/message-header'
import { Runner } from '../../code/scenario/runner'
import { ScenarioResponse } from '../../code/scenario/scenario-response'
import { SemanticFrame } from '../../code/scenario/semantic-frame'

export class RunnerMock implements Runner {
  public goThroughMailsScenarioPromise: () => Promise<ScenarioResponse> = () =>
    Promise.reject('`runGoThroughMailsScenario` should not be called accidentally')
  public unreadMailsGroupedByTopicPromise: () => Promise<ScenarioResponse> = () =>
    Promise.reject('`runUnreadMailsGroupedByTopicScenario` should not be called accidentally')
  public unreadMailsGroupedBySenderPromise: () => Promise<ScenarioResponse> = () =>
    Promise.reject('`runUnreadMailsGroupedBySenderScenario` should not be called accidentally')
  public totalUnreadMailsPromise: () => Promise<ScenarioResponse> = () =>
    Promise.reject('`runTotalUnreadMailsScenario` should not be called accidentally')
  public mailProcessingScenarioPromise: (
    message: MessageHeader,
    initialFrame: SemanticFrame,
  ) => Promise<ScenarioResponse> = () => Promise.reject('`runMailProcessingScenario` should not be called accidentally')
  public newMailScenarioPromise: (message: MessageHeader | null) => Promise<ScenarioResponse> = () =>
    Promise.reject('`runNewMailScenario` should not be called accidentally')

  public runGoThroughMailsScenario(_input: string): Promise<ScenarioResponse> {
    return this.goThroughMailsScenarioPromise()
  }

  public runUnreadMailsGroupedByTopicScenario(_input: string): Promise<ScenarioResponse> {
    return this.unreadMailsGroupedByTopicPromise()
  }

  public runUnreadMailsGroupedBySenderScenario(_input: string): Promise<ScenarioResponse> {
    return this.unreadMailsGroupedBySenderPromise()
  }

  public runTotalUnreadMailsScenario(_input: string): Promise<ScenarioResponse> {
    return this.totalUnreadMailsPromise()
  }

  public runMailProcessingScenario(
    message: MessageHeader,
    initialFrame: SemanticFrame,
    _input: string,
  ): Promise<ScenarioResponse> {
    return this.mailProcessingScenarioPromise(message, initialFrame)
  }

  public runNewMailScenario(
    message: MessageHeader | null,
    _body: MessageBody | null,
    _input: string,
  ): Promise<ScenarioResponse> {
    return this.newMailScenarioPromise(message)
  }
}
