local fixture = require 'tests.lua_ut.fixture'
local auth = require 'auth-nginx'

-- test response OK with OAuth and no phones
fixture.mock_http.response = {
    status = 200,
    read_body = function()
        return '{"uid": 1, "login": "lgn", "has_bug_icon": false}'
    end
}
local get_phones = false
local resp, code = auth.get_user_info(get_phones)
assert(resp['uid'] == 1)
assert(resp['login'] == 'lgn')
assert(resp['has_bug_icon'] == false)
assert(not resp['phones'])
assert(code == 200)

-- test response OK with OAuth and phones
fixture.mock_http.response = {
    status = 200,
    read_body = function()
        return '{"uid": 1, "login": "lgn", "has_bug_icon": false, "phones": [{"attributes": {"2": "79161111111"}, "id": "3"}]}'
    end
}
local get_phones = true
local resp, code = auth.get_user_info(get_phones)
assert(resp['uid'] == 1)
assert(resp['login'] == 'lgn')
assert(resp['has_bug_icon'] == false)
assert(resp['phones'][1].attributes["2"] == "79161111111")
assert(resp['phones'][1].id == "3")
assert(code == 200)

local error_handlers_log = ''
-- override default behavior for some handlers
local custom_handlers = {
    on_internal_error = function() error_handlers_log = 'on_internal_error' end,
    on_unauthorized_error = function() error_handlers_log = 'on_unauthorized_error' end,
}

-- test bad response
fixture.mock_http.response = {
    status = 200,
    read_body = function()
        return 'not json at all'
    end
}
local get_phones = true
local resp, code = auth.get_user_info(get_phones, custom_handlers)
assert(not resp)
assert(code == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(error_handlers_log == 'on_internal_error')

-- test unauthorized
fixture.mock_http.response = {
    status = ngx.HTTP_UNAUTHORIZED,
    reason = 'Unauthorized',
    read_body = function()
        return 'Unauthorized'
    end
}
local get_phones = true
local resp, code = auth.get_user_info(get_phones, custom_handlers)
assert(not resp)
assert(code == ngx.HTTP_UNAUTHORIZED)
assert(error_handlers_log == 'on_unauthorized_error')

-- test internal error
fixture.mock_http.response = {
    status = 503,
    reason = 'Internal error',
    read_body = function()
        return 'Internal error'
    end
}
local get_phones = true
local resp, code = auth.get_user_info(get_phones, custom_handlers)
assert(not resp)
assert(code == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(error_handlers_log == 'on_internal_error')
