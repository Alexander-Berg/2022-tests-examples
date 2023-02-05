import { PseudoRandomProvider } from '../../xpackages/testopithecus-common/code/utils/pseudo-random'
import { App, FeatureID } from '../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import { EventListFeature, SyncronizationDelay, SyncronizationDelayFeature } from './model/calendar-features'

export class MultiCalendar implements App {
  constructor(private readonly backends: App[], private syncronizationDelaySec: number) {}

  public static readonly allSupportedFeatures = [EventListFeature.get.name, SyncronizationDelayFeature.get.name]

  supportedFeatures: FeatureID[] = MultiCalendar.allSupportedFeatures

  public getFeature(feature: FeatureID): any {
    if (feature === SyncronizationDelayFeature.get.name) {
      return new SyncronizationDelayImpl(this.syncronizationDelaySec)
    }
    const i = PseudoRandomProvider.INSTANCE.generate(this.backends.length)
    return this.backends[i].getFeature(feature)
  }

  public async dump(_: App): Promise<string> {
    return ''
  }
}

export class SyncronizationDelayImpl implements SyncronizationDelay {
  constructor(private readonly delaySec: number) {}

  getDelaySec(): number {
    return this.delaySec
  }
}
