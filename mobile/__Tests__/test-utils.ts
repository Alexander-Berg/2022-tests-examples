import { UserService } from '../../testopithecus-common/code/users/user-service'
import { Nullable } from '../../../common/ys'
import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import { DefaultJSONSerializer } from '../../common/__tests__/__helpers__/default-json'
import { MailboxClient, MailboxClientHandler } from '../code/client/mailbox-client'
import { PublicBackendConfig } from '../code/client/public-backend-config'
import { MobileMailBackend } from '../code/mail/backend/mobile-mail-backend'
import { MailAccountSpec, MailboxBuilder, MailboxPreparerProvider } from '../code/mail/mailbox-preparer'
import { AccountType2, MBTPlatform } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { OauthService } from '../../testopithecus-common/code/users/oauth-service'
import { OAuthUserAccount, UserAccount } from '../../testopithecus-common/code/users/user-pool'
import { DefaultSyncNetwork } from '../../testopithecus-common/__tests__/code/pod/default-http'
import { DefaultImapProvider } from './pod/default-imap'
import { SyncSleepImpl } from '../../testopithecus-common/__tests__/code/pod/sleep'
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config'
import * as fs from 'fs'

export function createSyncNetwork(): DefaultSyncNetwork {
  const jsonSerializer = new DefaultJSONSerializer()
  return new DefaultSyncNetwork(jsonSerializer, ConsoleLog.LOGGER)
}

export function createNetworkClient(oauthAccount: Nullable<OAuthUserAccount> = null): MailboxClient {
  let account: OAuthUserAccount
  if (oauthAccount === null) {
    const token = createOauthService().getToken(PRIVATE_BACKEND_CONFIG.account, PRIVATE_BACKEND_CONFIG.accountType)
    account = new OAuthUserAccount(PRIVATE_BACKEND_CONFIG.account, token, PRIVATE_BACKEND_CONFIG.accountType)
  } else {
    account = oauthAccount!
  }
  const jsonSerializer = new DefaultJSONSerializer()
  return new MailboxClient(MBTPlatform.MobileAPI, account, createSyncNetwork(), jsonSerializer, ConsoleLog.LOGGER)
}

export function createBackend(): MobileMailBackend {
  const client = createNetworkClient()
  const clientsHandler = new MailboxClientHandler([client])
  clientsHandler.clientsManager.currentAccount = 0
  return new MobileMailBackend(clientsHandler)
}

export function createMailboxPreparer(account: UserAccount, host: string): MailboxBuilder {
  const delegate = new MailboxPreparerProvider(
    MBTPlatform.MobileAPI,
    new DefaultJSONSerializer(),
    createSyncNetwork(),
    ConsoleLog.LOGGER,
    SyncSleepImpl.instance,
    new DefaultImapProvider(),
  )
  return new MailboxBuilder(MailAccountSpec.fromUserAccount(account, host), delegate)
}

export function getOAuthAccount(account: UserAccount, type: AccountType2): OAuthUserAccount {
  const token = createOauthService().getToken(account, type)
  return new OAuthUserAccount(account, token, type)
}

export function createOauthService(): OauthService {
  return new OauthService(
    PublicBackendConfig.mailApplicationCredentials,
    createSyncNetwork(),
    new DefaultJSONSerializer(),
  )
}

export function applyEnv(): void {
  if (process.env.MAIL_BASE_URL) {
    PublicBackendConfig.mailBaseUrl = process.env.MAIL_BASE_URL
  }
  if (process.env.XENO_BASE_URL) {
    PublicBackendConfig.xenoBaseUrl = process.env.XENO_BASE_URL
  }
  if (process.env.HOME) {
    const tusTokenPath = `${process.env.HOME}/.tus/token`
    if (fs.existsSync(tusTokenPath)) {
      UserService.userServiceOauthToken = fs.readFileSync(tusTokenPath).toString().trim()
    }
  }
  if (process.env.USER_SERVICE_OAUTH_TOKEN) {
    UserService.userServiceOauthToken = process.env.USER_SERVICE_OAUTH_TOKEN
  }
}
