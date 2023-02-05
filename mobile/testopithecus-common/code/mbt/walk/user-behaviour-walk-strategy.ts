import { Int32, Nullable, Throwing } from '../../../../../common/ys'
import { PseudoRandomProvider } from '../../utils/pseudo-random'
import { RandomProvider } from '../../utils/random'
import { App, FeatureID, MBTAction, MBTComponent } from '../mbt-abstractions'
import { WalkStrategy } from '../state-machine'
import { UserBehaviour } from './behaviour/user-behaviour'

export class UserBehaviourWalkStrategy implements WalkStrategy {
  public readonly history: MBTAction[] = []
  private currentStep: Int32 = 0

  public constructor(
    private behaviour: UserBehaviour,
    private chooser: ActionChooser = new RandomActionChooser(),
    private stepsLimit: Nullable<Int32> = null,
  ) {}

  public nextAction(
    model: App,
    applicationFeatures: FeatureID[],
    component: MBTComponent,
  ): Throwing<Nullable<MBTAction>> {
    const possibleActions = this.behaviour
      .getActions(model, component)
      .filter((mbtAction) => mbtAction.supported(model.supportedFeatures, applicationFeatures))
      .filter((mbtAction) => {
        try {
          return mbtAction.canBePerformed(model)
        } catch (e) {
          throw new Error(
            `Не получается проверить выполнимость экшена ${mbtAction.tostring()}, что-то не так с моделью`,
          )
        }
      })
    if (possibleActions.length === 0) {
      return null
    }
    if (this.currentStep === this.stepsLimit) {
      return null
    }
    const action = this.chooser.choose(possibleActions, component)
    if (action === null) {
      return null
    }
    this.history.push(action)
    this.currentStep += 1
    return action
  }
}

export interface ActionChooser {
  choose(actions: MBTAction[], component: MBTComponent): Nullable<MBTAction>
}

export class RandomActionChooser implements ActionChooser {
  public constructor(private random: RandomProvider = PseudoRandomProvider.INSTANCE) {}

  public choose(actions: MBTAction[], _component: MBTComponent): Nullable<MBTAction> {
    if (actions.length === 0) {
      return null
    }
    const order = this.random.generate(actions.length)
    return actions[order]
  }
}
