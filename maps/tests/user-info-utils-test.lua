local test = require 'testing'
local describe, it = test.describe, test.it

require 'fixtures'

local ngx = require 'ngx'
local user_info_utils = require 'user-info-utils'
local http = require 'resty.http'

describe('user_info_utils', function()
    it('test_get_issuer', function()
        local issuer = user_info_utils.get_issuer('test_realm')
        assert(issuer == 'http://test-keycloak.com/test_realm')
    end)

    it('test_get_user_info', function()
        http.response = '{"uid":"uid","login":"login"}'
        local info = user_info_utils.get_userinfo('test_realm')
        assert(http.log == 'Requesting http://test-keycloak.com/test_realm/protocol/openid-connect/userinfo')
        assert(type(info) == 'table')
        assert(info['login'] == 'login')
        assert(info['uid'] == 'uid')
        assert(ngx.status == ngx.HTTP_OK)
    end)

    it('test_get_identity_current_user', function()
        http.response = '{"id":42,"is_super":true,"role":"admin","company_users":[{"id":1,"role":"admin"}]}'
        local info = user_info_utils.get_identity_current_user('dummy_identity_token')

        assert(http.log == 'Requesting http://test-identity.com/current_user')
        assert(http.params.headers['Authorization'] == 'Bearer dummy_identity_token')
        assert(type(info) == 'table')
        assert(info['id'] == 42)
        assert(info['is_super'] == true)
        assert(info['role'] == 'admin')

        local company = info['company_users'][1]
        assert(company['id'] == 1)
        assert(company['role'] == 'admin')

        assert(ngx.status == ngx.HTTP_OK)
    end)

    it('test_get_user_info_bad_response', function()
        http.err = 'Bad keycloak response'
        local info = user_info_utils.get_userinfo('test_realm')
        local expected_url = 'http://test-keycloak.com/test_realm/protocol/openid-connect/userinfo'

        assert(http.log == 'Requesting ' .. expected_url)
        assert(info == nil)
        assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)
        assert(ngx.logs == 'ERROR: Fetch user info failed: Uri: ' .. expected_url .. ', status: nil, body: nil, error: Bad keycloak response')
    end)

    it('test_get_user_info_bad_access_token', function()
        http.response = 'Token verification failed'
        http.status = ngx.HTTP_UNAUTHORIZED
        local info = user_info_utils.get_userinfo('test_realm')
        local expected_url = 'http://test-keycloak.com/test_realm/protocol/openid-connect/userinfo'

        assert(http.log == 'Requesting ' .. expected_url)
        assert(info == nil)
        assert(ngx.status == ngx.HTTP_UNAUTHORIZED)
        assert(ngx.logs == 'ERROR: Invalid access token: Uri: ' .. expected_url .. ', status: 401, body: Token verification failed, error: nil')
    end)
end)
