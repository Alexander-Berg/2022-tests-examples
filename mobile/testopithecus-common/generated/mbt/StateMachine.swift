// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/state-machine.ts >>>

import Foundation

open class StateMachine {
  private var model: App
  private var application: App
  private var walkStrategy: WalkStrategy
  private var logger: Logger
  public init(_ model: App, _ application: App, _ walkStrategy: WalkStrategy, _ logger: Logger) {
    self.model = model
    self.application = application
    self.walkStrategy = walkStrategy
    self.logger = logger
  }

  @discardableResult
  open func go(_ start: MBTComponent, _ limit: Int32! = nil) throws -> Bool {
    var currentComponent: MBTComponent! = start
    let history = HistoryBuilder(start)
    var i: Int32 = 0
    while currentComponent != nil {
      self.logger.info("Step №\(i):")
      currentComponent = (try self.step(history))
      if currentComponent != nil {
        history.next(currentComponent!)
      }
      i += 1
      if i == limit {
        break
      }
    }
    return i > 1
  }

  @discardableResult
  open func step(_ history: MBTHistory) throws -> MBTComponent! {
    let current = history.currentComponent
    let action: MBTAction! = (try self.walkStrategy.nextAction(self.model, self.application.supportedFeatures, current))
    if action == nil {
      self.logger.info("No possible actions available")
      return nil
    }
    self.logAction(action, "==========")
    if !action.supported(self.model.supportedFeatures, self.application.supportedFeatures) {
      fatalError("Не могу совершить действие \(action.tostring()), поскольку модель или приложение не поддерживают его")
    }
    var canPerform: Bool
    do {
      canPerform = (try action.canBePerformed(self.model))
    } catch {
      let e = error
      fatalError("Не получается проверить выполнимость экшена \(action.tostring()), что-то не так с моделью")
    }
    if !canPerform {
      fatalError("Can't perform \(action.tostring()), because it can't be performed in current model state")
    }
    self.logAction(action, "Performing action \(action.tostring()) on current component \(current.tostring())")
    let nextComponent = (try action.perform(self.model, self.application, history))
    self.logAction(action, "Action \(action.tostring()) on component \(current.tostring()) performed, new component is \(nextComponent.tostring())")
    self.logAction(action, "==========\n")
    return nextComponent
  }

  private func logAction(_ action: MBTAction, _ message: String) -> Void {
    if !fakeActions().includes(action.getActionType()) {
      self.logger.info(message)
    }
  }

}

open class HistoryBuilder: MBTHistory {
  public var previousDifferentComponent: MBTComponent! = nil
  public var allPreviousComponents: YSArray<MBTComponent> = YSArray()
  public var currentComponent: MBTComponent
  public init(_ currentComponent: MBTComponent) {
    self.currentComponent = currentComponent
  }

  @discardableResult
  open func next(_ component: MBTComponent) -> HistoryBuilder {
    if self.currentComponent.tostring() != component.tostring() {
      self.allPreviousComponents.push(self.currentComponent)
      self.previousDifferentComponent = self.currentComponent
      self.currentComponent = component
    }
    return self
  }

}

public protocol WalkStrategy {
  @discardableResult
  func nextAction(_ model: App, _ applicationFeatures: YSArray<FeatureID>, _ component: MBTComponent) throws -> MBTAction!
}

