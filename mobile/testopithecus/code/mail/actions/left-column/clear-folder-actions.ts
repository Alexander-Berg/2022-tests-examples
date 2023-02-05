import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { ClearFolderInFolderList, ClearFolderInFolderListFeature } from '../../feature/folder-list-features'

export class ClearTrashFolderAction extends BaseSimpleAction<ClearFolderInFolderList, MBTComponent> {
  public static readonly type: MBTActionType = 'ClearTrashFolderAction'

  public constructor(private confirmDeletionIfNeeded: boolean = true) {
    super(ClearTrashFolderAction.type)
  }
  public events(): EventusEvent[] {
    return []
  }

  public performImpl(
    modelOrApplication: ClearFolderInFolderList,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.clearTrash(this.confirmDeletionIfNeeded)
    return currentComponent
  }

  public requiredFeature(): Feature<ClearFolderInFolderList> {
    return ClearFolderInFolderListFeature.get
  }

  public canBePerformedImpl(model: ClearFolderInFolderList): Throwing<boolean> {
    return model.doesClearTrashButtonExist()
  }
}

export class ClearSpamFolderAction extends BaseSimpleAction<ClearFolderInFolderList, MBTComponent> {
  public static readonly type: MBTActionType = 'ClearSpamFolderAction'

  public constructor(private confirmDeletionIfNeeded: boolean = true) {
    super(ClearSpamFolderAction.type)
  }
  public events(): EventusEvent[] {
    return []
  }

  public performImpl(
    modelOrApplication: ClearFolderInFolderList,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.clearSpam(this.confirmDeletionIfNeeded)
    return currentComponent
  }

  public requiredFeature(): Feature<ClearFolderInFolderList> {
    return ClearFolderInFolderListFeature.get
  }

  public canBePerformedImpl(model: ClearFolderInFolderList): Throwing<boolean> {
    return model.doesClearSpamButtonExist()
  }
}
