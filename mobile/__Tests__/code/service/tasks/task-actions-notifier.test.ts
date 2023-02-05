import { int64 } from '../../../../../../common/ys'
import { EmptyTaskActionsNotifier } from '../../../../code/service/tasks/task-actions-notifier'

describe(EmptyTaskActionsNotifier, () => {
  it('just a dummy test to fix test coverage', () => {
    const notifier = new EmptyTaskActionsNotifier()
    notifier.notifyUpdateNotifications(int64(1), [])
    notifier.notifyWidgetsForFolders(int64(1), [])
    notifier.notifyTicketsAboutMoveToFolder(int64(1), [], int64(1), int64(1))
    notifier.notifyFTSMailAboutSendMailTaskSuccess(int64(1), int64(1111))
    notifier.notifyScheduleCheckAttachesInLastSend(int64(1111), int64(2222), 1)
  })
})
