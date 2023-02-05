import { ConsoleLog } from '../../../common/__tests__/__helpers__/console-log'
import { MaillistComponent } from '../../code/mail/components/maillist-component'
import { singleAccountBehaviour } from '../../code/mail/walk/mail-full-user-behaviour'
import { StateMachine } from '../../../testopithecus-common/code/mbt/state-machine'
import {
  RandomActionChooser,
  UserBehaviourWalkStrategy,
} from '../../../testopithecus-common/code/mbt/walk/user-behaviour-walk-strategy'
import { MockMailboxProvider } from '../mock-mailbox'
import { runAllTests, setupRegistry } from './mobile-mail-backend-tests'

describe('Run all tests on Model', async () => {
  await runAllTests(true)
})

describe('Test model inconsistensy', () => {
  it('model should be consistent', () => {
    setupRegistry()
    const model = MockMailboxProvider.emptyFoldersOneAccount().model
    const walkStrategy = new UserBehaviourWalkStrategy(singleAccountBehaviour(), new RandomActionChooser(), 1000)
    const stateMachine = new StateMachine(model, model.copy(), walkStrategy, ConsoleLog.LOGGER)
    stateMachine.go(new MaillistComponent())
  })
})
