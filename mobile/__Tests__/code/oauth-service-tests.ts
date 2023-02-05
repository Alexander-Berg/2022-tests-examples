import * as assert from 'assert'
import { DefaultJSONSerializer } from '../../../common/__tests__/__helpers__/default-json'
import { PublicBackendConfig } from '../../../testopithecus/code/client/public-backend-config'
import { AccountType2 } from '../../code/mbt/test/mbt-test'
import { OauthService } from '../../code/users/oauth-service'
import { UserAccount } from '../../code/users/user-pool'
import { createSyncNetwork } from '../../../testopithecus/__tests__/test-utils'

describe('default oauth service', () => {
  it('should get oauth token', (done) => {
    const oauthService = new OauthService(
      PublicBackendConfig.mailApplicationCredentials,
      createSyncNetwork(),
      new DefaultJSONSerializer(),
    )
    const token = oauthService.getToken(new UserAccount('yandex-team-77175-41375', 'simple123456'), AccountType2.Yandex)
    assert.ok(token!.length > 0)
    done()
  })
})
