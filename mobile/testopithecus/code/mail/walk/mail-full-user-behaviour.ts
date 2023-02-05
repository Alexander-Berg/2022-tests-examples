import { UserBehaviour } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import { MarkAsRead, MarkAsUnread } from '../actions/base-actions/markable-actions'
import { AllGroupOperationsActions, GroupOperationsComponent } from '../components/group-operations-component'
import { AllLoginActions, LoginComponent } from '../components/login-component'
import { AllMaillistActions, MaillistComponent } from '../components/maillist-component'
import { AllMessageActions, MessageComponent } from '../components/message-component'

export function allActionsBehaviour(accounts: UserAccount[]): UserBehaviour {
  return singleAccountBehaviour().setActionProvider(LoginComponent.type, new AllLoginActions(accounts))
}

export function singleAccountBehaviour(): UserBehaviour {
  return new UserBehaviour()
    .setActionProvider(MaillistComponent.type, new AllMaillistActions())
    .setActionProvider(GroupOperationsComponent.type, new AllGroupOperationsActions())
    .setActionProvider(MessageComponent.type, new AllMessageActions())
}

export function readUnreadUser(account: UserAccount): UserBehaviour {
  return allActionsBehaviour([account])
    .setActionProvider(LoginComponent.type, new AllLoginActions([account]))
    .whitelistFor(MaillistComponent.type, MarkAsRead.type)
    .whitelistFor(MaillistComponent.type, MarkAsUnread.type)
}
