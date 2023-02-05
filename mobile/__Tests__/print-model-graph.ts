import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import * as fs from 'fs'
import { MaillistComponent } from '../code/mail/components/maillist-component'
import { MailboxModel } from '../code/mail/model/mail-model'
import { singleAccountBehaviour } from '../code/mail/walk/mail-full-user-behaviour'
import { MultiRunner } from '../../testopithecus-common/code/mbt/walk/dfs-walk-strategy'
import { PersonalActionLimits } from '../../testopithecus-common/code/mbt/walk/limits/personal-action-limits'
import { MockMailboxProvider } from './mock-mailbox'

describe('Application Model graph', () => {
  it.skip('should be printed', async (done) => {
    const supportedFeatures = MailboxModel.allSupportedFeatures
    const modelProvider = MockMailboxProvider.emptyFoldersOneAccount()

    const limits = new PersonalActionLimits(100)

    const runner = new MultiRunner(
      new MaillistComponent(),
      singleAccountBehaviour(),
      limits,
      supportedFeatures,
      ConsoleLog.LOGGER,
    )
    runner.preparePath(await modelProvider.takeAppModel())

    const filename = 'graph.txt'

    fs.writeFileSync(filename, 'digraph g {\n')
    for (const vertex of runner.walkStrategyWithState.graph.adjList.keys()) {
      fs.appendFileSync(filename, `    ${vertex};\n`)
    }
    for (const edge of runner.walkStrategyWithState.graph.edges) {
      fs.appendFileSync(filename, `    ${edge.getFrom()} -> ${edge.getTo()} [label="${edge.getAction().tostring()}"]\n`)
    }
    fs.appendFileSync(filename, '}')
    done()
  })
})
