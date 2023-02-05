import { Logger } from '../../../common/code/logging/logger'
import { Int32, Nullable, Throwing } from '../../../../common/ys'
import { fakeActions } from './actions/fake-actions'
import { App, FeatureID, MBTAction, MBTComponent, MBTHistory } from './mbt-abstractions'

export class StateMachine {
  public constructor(
    private model: App,
    private application: App,
    private walkStrategy: WalkStrategy,
    private logger: Logger,
  ) {}

  public async go(start: MBTComponent, limit: Nullable<Int32> = null): Throwing<Promise<boolean>> {
    let currentComponent: Nullable<MBTComponent> = start
    const history = new HistoryBuilder(start)
    let i: Int32 = 0
    while (currentComponent !== null) {
      this.logger.info(`Step №${i}:`)
      currentComponent = await this.step(history)
      if (currentComponent !== null) {
        history.next(currentComponent!)
      }
      i += 1
      if (i === limit) {
        break
      }
    }
    return i > 1
  }

  public async step(history: MBTHistory): Throwing<Promise<Nullable<MBTComponent>>> {
    const current = history.currentComponent

    const action = this.walkStrategy.nextAction(this.model, this.application.supportedFeatures, current)
    if (action === null) {
      this.logger.info('No possible actions available')
      return null
    }

    this.logAction(action, '==========')

    // В теории, nextAction может вернуть вообще произвольное действие, не обязательно из списка possibleActions.
    // Например, в классических тестах при фиксированном сценарии.
    // Поэтому требуются следующие две проверки.
    if (!action.supported(this.model.supportedFeatures, this.application.supportedFeatures)) {
      throw new Error(
        `Не могу совершить действие ${action.tostring()}, поскольку модель или приложение не поддерживают его`,
      )
    }
    let canPerform: boolean
    try {
      canPerform = action.canBePerformed(this.model)
    } catch (e) {
      throw new Error(`Не получается проверить выполнимость экшена ${action.tostring()}, что-то не так с моделью`)
    }
    if (!canPerform) {
      throw new Error(`Can't perform ${action.tostring()}, because it can't be performed in current model state`)
    }

    this.logAction(action, `Performing action ${action.tostring()} on current component ${current.tostring()}`)
    const nextComponent = await action.perform(this.model, this.application, history)
    this.logAction(
      action,
      `Action ${action.tostring()} on component ${current.tostring()} performed, new component is ${nextComponent.tostring()}`,
    )
    this.logAction(action, '==========\n')
    return nextComponent
  }

  private logAction(action: MBTAction, message: string): void {
    if (!fakeActions().includes(action.getActionType())) {
      this.logger.info(message)
    }
  }
}

export class HistoryBuilder implements MBTHistory {
  public previousDifferentComponent: Nullable<MBTComponent> = null

  public constructor(public currentComponent: MBTComponent) {}

  public allPreviousComponents: MBTComponent[] = []

  public next(component: MBTComponent): HistoryBuilder {
    if (this.currentComponent.tostring() !== component.tostring()) {
      this.allPreviousComponents.push(this.currentComponent)
      this.previousDifferentComponent = this.currentComponent
      this.currentComponent = component
    }
    return this
  }
}

export interface WalkStrategy {
  nextAction(model: App, applicationFeatures: FeatureID[], component: MBTComponent): Throwing<Nullable<MBTAction>>
}
