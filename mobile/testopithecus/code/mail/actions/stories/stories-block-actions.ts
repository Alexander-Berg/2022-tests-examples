import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  MBTActionType,
  MBTComponent,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { StoriesBlock, StoriesBlockFeature } from '../../feature/message-list/stories-block-feature'

export abstract class AbstractStoriesBlockAction extends BaseSimpleAction<StoriesBlock, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<StoriesBlock> {
    return StoriesBlockFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class HideStoriesBlockAction extends AbstractStoriesBlockAction {
  public static readonly type: MBTActionType = 'HideStoriesBlockAction'

  public constructor() {
    super(HideStoriesBlockAction.type)
  }

  public performImpl(modelOrApplication: StoriesBlock, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.hideStories()
    return currentComponent
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const storyModel = StoriesBlockFeature.get.castIfSupported(model)
    const isHidden = storyModel!.isHidden()
    return storyModel === null || !isHidden
  }
}

export class OpenStoryFromBlockAction extends AbstractStoriesBlockAction {
  public static readonly type: MBTActionType = 'OpenStoryFromBlockAction'

  public constructor(private position: Int32) {
    super(OpenStoryFromBlockAction.type)
  }

  public performImpl(modelOrApplication: StoriesBlock, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openStory(this.position)
    return currentComponent
  }
}
