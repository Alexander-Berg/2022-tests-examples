import { Throwing } from '../../../../../../common/ys'
import { Rotatable } from '../../feature/rotatable-feature'

export class RotatableModel implements Rotatable {
  public landscape: boolean = false

  private listeners: RotateListener[] = []

  public rotateToLandscape(): Throwing<void> {
    this.landscape = true
    this.notifyRotated()
  }

  public rotateToPortrait(): Throwing<void> {
    this.landscape = false
    this.notifyRotated()
  }

  public copy(): RotatableModel {
    const copy = new RotatableModel()
    copy.landscape = this.landscape
    return copy
  }

  public isInLandscape(): Throwing<boolean> {
    return this.landscape
  }

  public attach(listener: RotateListener): void {
    this.listeners.push(listener)
  }

  // public detach(listener: RotateListener): void {
  //   const observerIndex = this.listeners.indexOf(listener)
  //   this.listeners.splice(observerIndex, 1)
  // }

  public notifyRotated(): void {
    for (const listener of this.listeners) {
      listener.rotated(this.landscape)
    }
  }
}

export interface RotateListener {
  rotated(landscape: boolean): void
}
