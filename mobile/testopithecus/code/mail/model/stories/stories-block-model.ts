import { Log } from '../../../../../common/code/logging/logger'
import { Int32, Nullable, Throwing } from '../../../../../../common/ys'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { StoriesBlock } from '../../feature/message-list/stories-block-feature'
import { DefaultFolderName } from '../folder-data-model'
import { RotateListener } from '../general/rotatable-model'
import { ContainerListener, MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class StoriesBlockModel implements StoriesBlock {
  public hideClickedFlag: boolean = false

  public displaysLeft: Int32 = 10

  public container: Nullable<MessageContainer> = null

  // public currentOpenedStory

  public constructor() {}

  public hideStories(): Throwing<void> {
    Log.info('Hiding all stories')
    this.hideClickedFlag = true
  }

  public openStory(position: Int32): Throwing<void> {
    Log.info(`Opening story at ${position} position`)
  }

  public isHidden(): Throwing<boolean> {
    return (
      this.hideClickedFlag ||
      this.displaysLeft < 0 ||
      !(
        this.container !== null &&
        this.container!.type === MessageContainerType.folder &&
        this.container!.name === DefaultFolderName.inbox
      )
    )
  }

  public blockShown(): void {
    if (this.displaysLeft >= 0) {
      this.displaysLeft = this.displaysLeft - 1
    }
  }
}

export class StoriesBlockViewCounter implements RotateListener, ContainerListener {
  public constructor(private model: StoriesBlockModel, private messageListDisplayModel: MessageListDisplayModel) {
    this.model.container = messageListDisplayModel.getCurrentContainer()
  }

  public blockShown(): void {
    this.model.blockShown()
  }

  public folderOpened(container: MessageContainer): void {
    this.model.container = container
    if (container.type === MessageContainerType.folder && container.name === DefaultFolderName.inbox) {
      this.blockShown()
    }
  }

  public rotated(landscape: boolean): void {
    this.folderOpened(this.messageListDisplayModel.getCurrentContainer())
  }

  public containerChanged(container: MessageContainer): void {
    this.folderOpened(container)
  }
}
