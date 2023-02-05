import { Int32, Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class StoriesBlockFeature extends Feature<StoriesBlock> {
  public static get: StoriesBlockFeature = new StoriesBlockFeature()

  public constructor() {
    super('StoriesBlock', 'Фича Блок сторис на экране списка писем')
  }
}

export interface StoriesBlock {
  hideStories(): Throwing<void>

  openStory(position: Int32): Throwing<void>

  isHidden(): Throwing<boolean>
}
