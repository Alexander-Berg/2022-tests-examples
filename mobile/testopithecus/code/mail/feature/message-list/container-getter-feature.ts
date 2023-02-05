import { Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class ContainerGetterFeature extends Feature<ContainerGetter> {
  public static get: ContainerGetterFeature = new ContainerGetterFeature()

  private constructor() {
    super('ContainerGetter', 'Фича для получения текущего контейнера (папка, метка, фильтр)')
  }
}

export interface ContainerGetter {
  getCurrentContainer(): Throwing<MessageContainer>
}

export enum MessageContainerType {
  folder = 'Folder',
  label = 'Label',
  importantFilter = 'ImportantFilter',
  unreadFilter = 'UnreadFilter',
  withAttachmentsFilter = 'WithAttachmentsFilter',
  search = 'Search',
}

export class MessageContainer {
  public constructor(public readonly name: string, public readonly type: MessageContainerType) {}
}
