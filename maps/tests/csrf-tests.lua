local test = require 'testing'
local describe, it = test.describe, test.it

require 'fixtures'

local libroutequid_util = require 'libroutequid_util_cppmodule'
local csrf = require 'csrf'
local ngx = require 'ngx'

local function assert_no_ngx_error()
    assert(ngx.status == ngx.HTTP_OK)
    assert(ngx.msg == '')
    assert(ngx.exit_code == ngx.HTTP_OK)
end

local function assert_ngx_error(status, msg)
    assert(ngx.status == status)
    assert(ngx.msg == msg)
    assert(ngx.exit_code == status)
end

describe('csrf', function()
    it('test_create_and_check_csrf_token', function()
        ngx.var['cookie_routequid'] = '123'
        local uid = '1234'

        local token = csrf.create_csrf_token(uid)
        print(token)

        assert(token ~= nil)
        assert_no_ngx_error()

        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = token

        csrf.check_csrf_token(uid)

        assert_no_ngx_error()
    end)

    it('test_multiple_tokens_are_handled_correctly', function()
        ngx.var['cookie_routequid'] = '123'
        local uid = '1234'
        local token = csrf.create_csrf_token(uid)
        assert(token ~= nil)

        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = {token, token, token}
        csrf.check_csrf_token(uid)

        assert_no_ngx_error()
    end)

    it('test_create_token_without_routequid_fails', function()
        local uid = '1234'
        local token = csrf.create_csrf_token(uid)
        assert(token == nil)
        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'UID cookie is missing')
    end)

    it('test_post_without_csrf_token_fails', function()
        local uid = '1234'
        ngx.req.method = 'POST'
        csrf.check_csrf_token(uid)
        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'No CSRF header detected')
    end)

    it('test_get_without_csrf_token_is_ok', function()
        local uid = '1234'
        ngx.req.method = 'GET'
        csrf.check_csrf_token(uid)
        assert_no_ngx_error()
    end)

    it('test_post_with_invalid_csrf_token_format_fails', function()
        local token = 'invalid_csrf_token'
        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = token

        csrf.check_csrf_token(uid)

        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'CSRF header check failed')
    end)

    it('test_post_with_invalid_csrf_token_timestamp_fails', function()
        local token = 'invalid_csrf_token:abcdefgh'
        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = token

        csrf.check_csrf_token(uid)

        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'CSRF header check failed')
    end)

    it('test_expired_token_fails', function()
        ngx.var['cookie_routequid'] = '123'
        local uid = '1234'
        local timestamp = 946674000000 -- 2000-01-01 00:00:00
        local token = csrf.create_csrf_token(uid, timestamp)
        assert(token ~= nil)

        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = token
        csrf.check_csrf_token(uid)

        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'CSRF header expired')
    end)

    it('test_mismatched_token_fails', function()
        local uid = '1234'
        local timestamp = libroutequid_util.current_time_ms()
        local token = string.format('invalid_csrf_token:%d', timestamp)

        ngx.req.method = 'POST'
        ngx.req.header['X-CSRF-Token'] = token
        csrf.check_csrf_token(uid)

        assert_ngx_error(ngx.HTTP_UNAUTHORIZED, 'CSRF header check failed')
    end)
end)
