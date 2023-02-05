import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { ComposeComponent } from '../../components/compose-component'
import { WYSIWIG, WysiwygFeature } from '../../feature/compose/compose-features'

export abstract class WysiwygBaseAction extends BaseSimpleAction<WYSIWIG, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<WYSIWIG> {
    return WysiwygFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ClearFormatting extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'ClearFormatting'

  public constructor(private from: Int32, private to: Int32) {
    super(ClearFormatting.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, _currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.clearFormatting(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `ClearFormatting(from=${this.from}, to=${this.to})`
  }
}

export class SetStrong extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'SetStrong'

  public constructor(private from: Int32, private to: Int32) {
    super(SetStrong.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, _currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setStrong(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `SetStrong(from=${this.from}, to=${this.to})`
  }
}

export class SetItalic extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'SetItalic'

  public constructor(private from: Int32, private to: Int32) {
    super(SetItalic.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, _currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setItalic(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `SetItalic(from=${this.from}, to=${this.to})`
  }
}

export class AppendToBody extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'AppendToBody'

  public constructor(private index: Int32, private text: string) {
    super(AppendToBody.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, _currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.appendText(this.index, this.text)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `AppendToBody(index=${this.index}, text=${this.text})`
  }
}
