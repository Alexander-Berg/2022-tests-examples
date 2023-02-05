import { ContainerGetter, MessageContainer } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplayModel } from './message-list-display-model'

export class ContainerGetterModel implements ContainerGetter {
  public constructor(public model: MessageListDisplayModel) {}

  public getCurrentContainer(): MessageContainer {
    return this.model.getCurrentContainer() // todo Дублирование кода?
  }
}
