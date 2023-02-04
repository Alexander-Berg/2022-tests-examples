local test = require 'testing'
local describe, it = test.describe, test.it

require 'fixtures'

local routequid_util = require 'routequid-util'
local ngx = require 'ngx'

local ROUTEQUID_SET_COOKIE_REGEX = '^routequid=%d+; path=/; domain=%.routeq%.com; max%-age=31536000; Secure$'

describe('routequid_util', function()
    it('test_set_routequid_when_not_present', function()
        routequid_util.ensure_routequid_set_before_request()

        assert(ngx.var.routequid ~= nil)

        assert(#ngx.header['Set-Cookie'] == 1)
        local set_cookie = ngx.header['Set-Cookie'][1]
        print(set_cookie)
        assert(string.find(set_cookie, ROUTEQUID_SET_COOKIE_REGEX))

        assert(string.find(ngx.req.header['Cookie'], '^routequid=%d+$'))

        routequid_util.ensure_routequid_set_after_request()
        assert(#ngx.header['Set-Cookie'] == 1)
    end)

    it('test_set_routequid_when_other_cookies_are_present', function()
        ngx.var['cookie_x'] = '123'
        ngx.req.header['Cookie'] = 'x=123'
        ngx.header['Set-Cookie'] = {'y=345'}

        routequid_util.ensure_routequid_set_before_request()

        assert(ngx.var.routequid ~= nil)

        assert(#ngx.header['Set-Cookie'] == 2)

        table.sort(ngx.header['Set-Cookie'])
        local set_cookie = ngx.header['Set-Cookie'][1]
        assert(string.find(set_cookie, ROUTEQUID_SET_COOKIE_REGEX))
        assert(ngx.header['Set-Cookie'][2] == 'y=345')

        assert(string.find(ngx.req.header['Cookie'], '^routequid=%d+;x=123$'))

        routequid_util.ensure_routequid_set_after_request()
        assert(#ngx.header['Set-Cookie'] == 2)
    end)

    it('test_routequid_not_set_when_already_present', function()
        ngx.var['cookie_routequid'] = '123'
        ngx.req.header['Cookie'] = 'routequid=123'

        assert(not ngx.var.routequid)

        routequid_util.ensure_routequid_set_before_request()

        assert(#(ngx.header['Set-Cookie'] or {}) == 0)
        assert(ngx.req.header['Cookie'] == 'routequid=123')

        routequid_util.ensure_routequid_set_after_request()
        assert(#(ngx.header['Set-Cookie'] or {}) == 0)
    end)

    it('test_multiple_cookie_headers', function()
        ngx.var['cookie_x'] = '123'
        ngx.var['cookie_y'] = '234'
        ngx.req.header['Cookie'] = {'x=123', 'y=234'}

        routequid_util.ensure_routequid_set_before_request()

        assert(string.find(ngx.req.header['Cookie'], '^routequid=%d+;x=123$'))
    end)

    it('test_set_routequid_cookie_restored_after_request', function()
        routequid_util.ensure_routequid_set_before_request()

        -- request resets Set-Cookie header
        ngx.header['Set-Cookie'] = {'backend_cookie=abc'}

        routequid_util.ensure_routequid_set_after_request()
        assert(#ngx.header['Set-Cookie'] == 2)

        table.sort(ngx.header['Set-Cookie'])
        assert(ngx.header['Set-Cookie'][1] == 'backend_cookie=abc')
        local set_cookie = ngx.header['Set-Cookie'][2]
        assert(string.find(set_cookie, ROUTEQUID_SET_COOKIE_REGEX))
    end)
end)
