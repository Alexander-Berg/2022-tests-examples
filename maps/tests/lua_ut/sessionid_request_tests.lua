local fixture = require 'tests.lua_ut.fixture'
local auth = require 'auth-nginx'

-- test No cookies header
fixture.mock_ngx:init_request({})
fixture.mock_http.response = { status = 500 }
local ticket, err = auth.exchange_sessionid_for_user_ticket()
assert(not ticket and not err)
assert(ngx.status == 200)
assert(not ngx.req.get_headers()[auth.USER_TICKET_HEADER]) -- no user ticket
assert(not fixture.mock_http.log) -- no auth request

-- test No sessionid in cookies
fixture.mock_ngx:init_request({ Cookie = 'somecookie=somevalue' })
fixture.mock_http.response = { status = 500 }
local ticket, err = auth.exchange_sessionid_for_user_ticket()
assert(not ticket and not err)
assert(ngx.status == 200)
assert(not ngx.req.get_headers()[auth.USER_TICKET_HEADER]) -- no user ticket
assert(not fixture.mock_http.log) -- no auth request

-- test Success
fixture.mock_ngx:init_request({ Cookie = 'Session_id=good-session; sessionid2=good-ssl-session; unrelated=cookie' })
fixture.mock_http.response = {
    status = 200,
    headers = {[auth.USER_TICKET_HEADER] = '3-Valid-User-Ticket'},
    read_body = function() end
}
fixture.mock_http.log = nil;

local ticket = auth.exchange_sessionid_for_user_ticket()
assert(ticket)
assert(ngx.status == 200)
assert(ngx.req.get_headers()[auth.USER_TICKET_HEADER] == '3-Valid-User-Ticket') -- got ticket
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')

-- test HTTP 401 auth response
fixture.mock_ngx:init_request({ Cookie = 'Session_id=expired; sessionid2=123' })
fixture.mock_http.response = {
    status = ngx.HTTP_UNAUTHORIZED,
    reason = 'Invalid cookie',
    read_body = function() end
}
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_sessionid_for_user_ticket()
assert(not ticket and err == ngx.HTTP_UNAUTHORIZED)
assert(ngx.status == ngx.HTTP_UNAUTHORIZED)  --request rejected by default on_unauthorized_error() handler
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')

-- test misc HTTP err auth response
fixture.mock_ngx:init_request({ Cookie = 'Session_id=good; sessionid2=good' })
fixture.mock_http.response = {status = 503, reason = 'whatever', read_body = function() end}
fixture.mock_http.log = nil;
local ticket, err = auth.exchange_sessionid_for_user_ticket()
assert(not ticket and err == ngx.HTTP_INTERNAL_SERVER_ERROR)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)  --request rejected by default on_internal_error() handler
assert(fixture.mock_http.log == 'connect();request();set_keepalive();')


-- test Remove sessionid from cookies
fixture.mock_ngx:init_request({ Cookie = 'Session_id=good; sessionid2=good-too' })
assert(ngx.req.get_headers()['Cookie'] == 'Session_id=good; sessionid2=good-too')
auth.remove_request_sessionid()
assert(not ngx.req.get_headers()['Cookie'])

fixture.mock_ngx:init_request({ Cookie = 'Cookie1=1; Session_id=good; sessionid2=good-too' })
auth.remove_request_sessionid()
assert(ngx.req.get_headers()['Cookie'] == 'Cookie1=1')

fixture.mock_ngx:init_request({ Cookie = 'sessionid2=good-too; Cookie1=1; Session_id=good; Cookie2=2' })
auth.remove_request_sessionid()
assert(ngx.req.get_headers()['Cookie'] == 'Cookie1=1; Cookie2=2')
