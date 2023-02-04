-- mock nginx
local ngx_request_headers = {}
ngx = {
    HTTP_FORBIDDEN = 403,
    HTTP_INTERNAL_SERVER_ERROR = 500,
    req = {
        get_headers = function() return ngx_request_headers end,
        set_header = function(name, value) ngx_request_headers[name] = value end,
        clear_header = function(name) ngx_request_headers[name] = nil end
    },
    status = 200,
    log = function() end,
    say = function() end,
    exit = function() end,
    worker = {exiting = function() end},
}

-- mock tvmtool
local mock_recipe = require 'mock_tvmtool'
local plugin = require 'tvm2-nginx'
plugin.tvmtool_config = {
    ['tvmtool.conf'] = mock_recipe.CONFIG_FILE,
    ['tvmtool.auth'] = mock_recipe.AUTH_FILE
}

-- configure_via validates initialization but state is still unitialized
local ok = plugin.configure_via_tvmtool('me')
assert(ok)

-- test uninitialized state
local ok = plugin.attach_service_ticket('A')
assert(not ok)
assert(not ngx_request_headers[plugin.SERVICE_TICKET_HEADER])
assert(ngx.status == 200) -- not rejected

ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = 'ticket content'}
local src_id = plugin.check_service_ticket()
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)


-- initialization
local ok = plugin.init_via_tvmtool()
assert(ok)

ngx.status = 200 -- reset status
ngx_request_headers = {}

-- test ticket issue invalid destination
local ok = plugin.attach_service_ticket('invalid_destination')
assert(not ok)
assert(not ngx_request_headers[plugin.SERVICE_TICKET_HEADER])
assert(ngx.status == 200) -- not rejected

-- test ticket issue success
local ok = plugin.attach_service_ticket('A')
assert(ok)
local valid_ticket_a = ngx_request_headers[plugin.SERVICE_TICKET_HEADER]
assert(valid_ticket_a)
assert(ngx.status == 200)

local ok = plugin.attach_service_ticket('B')
assert(ok)
local valid_ticket_b = ngx_request_headers[plugin.SERVICE_TICKET_HEADER]
assert(valid_ticket_b)
assert(ngx.status == 200)

-- test ticket check success
-- NB: 'A' dst id == self id, so the ticket validation passes
ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = valid_ticket_a}
local src_id = plugin.check_service_ticket()
assert(src_id == '100500')
assert(ngx_request_headers[plugin.SRC_TVM_ID_HEADER] == src_id) -- ticket set in header
assert(ngx.status == 200)

-- test ticket check invalid destination
ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = valid_ticket_b}
local src_id = plugin.check_service_ticket()
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == ngx.HTTP_FORBIDDEN)

-- test invalid ticket check
ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = 'invalid ticket'}
local src_id = plugin.check_service_ticket()
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == ngx.HTTP_FORBIDDEN)

-- test multiple tickets check
ngx.status = 200
ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = {valid_ticket_a, valid_ticket_b}}
local src_id = plugin.check_service_ticket()
assert(src_id == '100500')
assert(ngx_request_headers[plugin.SRC_TVM_ID_HEADER] == src_id) -- ticket set in header
assert(ngx.status == 200)

ngx_request_headers = {[plugin.SERVICE_TICKET_HEADER] = {valid_ticket_b, valid_ticket_a}}
local src_id = plugin.check_service_ticket()
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == ngx.HTTP_FORBIDDEN)

-- test allow/deny access with no ticket
ngx_request_headers = {}
ngx.status = 200

local allow_no_ticket = true
local src_id = plugin.check_service_ticket(allow_no_ticket)
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == 200)

local allow_no_ticket = false
local src_id = plugin.check_service_ticket(allow_no_ticket)
assert(not src_id)
assert(not ngx_request_headers[plugin.SRC_TVM_ID_HEADER])
assert(ngx.status == ngx.HTTP_FORBIDDEN)
