import { Application, SpectronClient } from 'spectron'
import { App, FeatureID, FeatureRegistry } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { YandexTeamLoginFeature } from '../../code/mail/feature/login-features'
import { DesktopYandexTeamLogin } from './feature/login'

export class DesktopApplication implements App {
  public static readonly allSupportedFeatures = [YandexTeamLoginFeature.get.name]

  public readonly supportedFeatures: FeatureID[] = DesktopApplication.allSupportedFeatures

  public constructor(
    private readonly app: Application, // TODO: remove
    private readonly client: SpectronClient,
  ) {}

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(YandexTeamLoginFeature.get, new DesktopYandexTeamLogin(this.app, this.client))
      .get(feature)
  }

  public async dump(model: App): Promise<string> {
    return ''
  }
}
