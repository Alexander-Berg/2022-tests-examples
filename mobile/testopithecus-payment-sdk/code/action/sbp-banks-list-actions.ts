import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { SbpExtendedBanksListComponent } from '../component/sbp-extended-banks-list-component'
import {
  SbpBanksList,
  SbpBanksListFeature,
  SbpExtendedBanksList,
  SbpExtendedBanksListFeature,
} from '../feature/sbp-banks-list-feature'

export class SelectAnotherBankAction extends BaseSimpleAction<SbpBanksList, MBTComponent> {
  public constructor() {
    super('SelectAnotherBankAction')
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: SbpBanksList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.clickAnotherBank()
    return new SbpExtendedBanksListComponent()
  }

  public requiredFeature(): Feature<SbpBanksList> {
    return SbpBanksListFeature.get
  }
}

export class SearchQueryActionBankAction extends BaseSimpleAction<SbpExtendedBanksList, MBTComponent> {
  // Swift wants to override it, so change signature from base constructor
  public constructor(private readonly query: string = '', unused: boolean = false) {
    super('SearchQueryActionBankAction')
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: SbpExtendedBanksList, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSearch()
    modelOrApplication.enterSearchQuery(this.query)
    return currentComponent
  }

  public requiredFeature(): Feature<SbpExtendedBanksList> {
    return SbpExtendedBanksListFeature.get
  }
}
