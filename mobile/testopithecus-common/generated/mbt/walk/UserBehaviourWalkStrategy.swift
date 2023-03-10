// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/user-behaviour-walk-strategy.ts >>>

import Foundation

open class UserBehaviourWalkStrategy: WalkStrategy {
  public let history: YSArray<MBTAction> = YSArray()
  private var currentStep: Int32 = 0
  private var behaviour: UserBehaviour
  private var chooser: ActionChooser
  private var stepsLimit: Int32!
  public init(_ behaviour: UserBehaviour, _ chooser: ActionChooser = RandomActionChooser(), _ stepsLimit: Int32! = nil) {
    self.behaviour = behaviour
    self.chooser = chooser
    self.stepsLimit = stepsLimit
  }

  @discardableResult
  open func nextAction(_ model: App, _ applicationFeatures: YSArray<FeatureID>, _ component: MBTComponent) throws -> MBTAction! {
    let possibleActions = self.behaviour.getActions(model, component).filter({
      (mbtAction) in
      mbtAction.supported(model.supportedFeatures, applicationFeatures)
    }).filter({
      (mbtAction) in
      do {
        return (try mbtAction.canBePerformed(model))
      } catch {
        let e = error
        fatalError("Не получается проверить выполнимость экшена \(mbtAction.tostring()), что-то не так с моделью")
      }
    })
    if possibleActions.length == 0 {
      return nil
    }
    if self.currentStep == self.stepsLimit {
      return nil
    }
    let action: MBTAction! = self.chooser.choose(possibleActions, component)
    if action == nil {
      return nil
    }
    self.history.push(action)
    self.currentStep += 1
    return action
  }

}

public protocol ActionChooser {
  @discardableResult
  func choose(_ actions: YSArray<MBTAction>, _ component: MBTComponent) -> MBTAction!
}

open class RandomActionChooser: ActionChooser {
  private var random: RandomProvider
  public init(_ random: RandomProvider = PseudoRandomProvider.INSTANCE) {
    self.random = random
  }

  @discardableResult
  open func choose(_ actions: YSArray<MBTAction>, _ _component: MBTComponent) -> MBTAction! {
    if actions.length == 0 {
      return nil
    }
    let order = self.random.generate(actions.length)
    return actions[order]
  }

}

