import { AccountType2 } from '../../testopithecus-common/code/mbt/test/mbt-test'
import {
  OAuthApplicationCredentialsRegistry,
  OAuthCredentials,
} from '../../testopithecus-common/code/users/oauth-service'

export class PaymentSdkBackendConfig {
  public static applicationCredentials: OAuthApplicationCredentialsRegistry = new OAuthApplicationCredentialsRegistry().register(
    AccountType2.YandexTest,
    new OAuthCredentials('f171112adc6a40f59dd46c9822ef1168', '3f4267e7ff8f4e5a9359bc128c421824'),
  )
}
