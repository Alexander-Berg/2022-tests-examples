local fixture = require 'tests.lua_ut.fixture'
local auth = require 'auth-nginx'

-- test Success auth response
fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
fixture.mock_http.response = {
    status = 200,
    headers = {[auth.USER_TICKET_HEADER] = '3-Valid-User-Ticket'},
    read_body = function() end
}
fixture.mock_http.log = nil;
local ticket = auth.exchange_oauth_for_user_ticket()
assert(ticket)
assert(ngx.status == 200)
assert(ngx.req.get_headers()[auth.USER_TICKET_HEADER] == '3-Valid-User-Ticket') -- ticket in headers
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')

-- test No 'Authorization' header
fixture.mock_ngx:init_request({})
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket()
assert(not ticket and not err)
assert(ngx.status == 200)
assert(not ngx.req.get_headers()[auth.USER_TICKET_HEADER]) -- no user ticket
assert(not fixture.mock_http.log) -- no auth request

-- test HTTP 401 auth response
fixture.mock_ngx:init_request({ Authorization ='OAuth Invalid-Token' })
fixture.mock_http.response = {
    status = ngx.HTTP_UNAUTHORIZED,
    reason = 'Invalid token',
    read_body = function() end
}
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket()
assert(not ticket and err == ngx.HTTP_UNAUTHORIZED)
assert(ngx.status == ngx.HTTP_UNAUTHORIZED) -- request rejected by default on_unauthorized_error() handler
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')

-- test misc HTTP err status auth response
fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
fixture.mock_http.response = {status = 503, reason = 'whatever', read_body = function() end}
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket()
assert(not ticket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)  -- request rejected by default on_internal_error() handler
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')

-- test timed out request to auth_agent
fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
fixture.mock_http.response = nil  -- so http::request will return nil
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket()
assert(not ticket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)  -- request rejected by default on_internal_error() handler
assert(fixture.mock_http.log == 'connect();request();close();')

-- test connection failed
fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
-- override http mock connect method so it will fail
fixture.mock_http.connect = function () return nil, 'Shit happens' end
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket()
assert(not ticket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)  -- request rejected by default on_internal_error() handler
assert(not fixture.mock_http.log)
-- restore http mock connect method
fixture.mock_http.connect = function(self) self.log = (self.log or '') .. 'connect();' return 1 end


-- test custom error handlers
local error_handlers_log = ''
-- override default behavior for all handlers
local custom_handlers = {
    -- reject instead of ignore if auth is empty
    on_empty_auth = function()
        error_handlers_log = error_handlers_log .. 'on_empty_auth;'
        auth.reject(401)
    end,
    -- for internal and unauthorized ignore instead of reject
    on_internal_error = function() error_handlers_log = error_handlers_log .. 'on_internal_error;' end,
    on_unauthorized_error = function() error_handlers_log = error_handlers_log .. 'on_unauthorized_error;' end,
}
-- 503 response from blackbox
fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
fixture.mock_http.response = {status = 503, reason = 'noreason', read_body = function() end}
local tiket, err = auth.exchange_oauth_for_user_ticket(custom_handlers)
assert(not tiket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == 200)  -- request not rejected
assert(error_handlers_log == 'on_internal_error;')

-- 401 response from blackbox
fixture.mock_ngx:init_request({ Authorization ='OAuth Invalid-Token' })
fixture.mock_http.response = {status = ngx.HTTP_UNAUTHORIZED, reason = 'Invalid Token', read_body = function() end}
local tiket, err = auth.exchange_oauth_for_user_ticket(custom_handlers)
assert(not tiket and err == ngx.HTTP_UNAUTHORIZED)
assert(ngx.status == 200)  -- request not rejected
assert(error_handlers_log == 'on_internal_error;on_unauthorized_error;')

-- no 'Authorization' header
fixture.mock_ngx:init_request({})
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_oauth_for_user_ticket(custom_handlers)
assert(not ticket and not err)
assert(ngx.status == 401)   -- request rejected with 401
assert(error_handlers_log == 'on_internal_error;on_unauthorized_error;on_empty_auth;')


-- test override global builtin on_internal_error handler
auth.error_handlers.on_internal_error = function() end  -- override builtin auto-reject behavior

fixture.mock_ngx:init_request({ Authorization ='OAuth 3-Valid-Token' })
fixture.mock_http.response = {status = 503, reason = 'noreason', read_body = function() end}
local tiket, err = auth.exchange_oauth_for_user_ticket()
assert(not tiket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == 200)  -- request not rejected


-- test Remove OAuth header
fixture.mock_ngx:init_request({ Authorization ='OAuth 123-Token' })
assert(ngx.req.get_headers()['Authorization'] == 'OAuth 123-Token')
auth.remove_request_oauth()
assert(not ngx.req.get_headers()['Authorization'])
