local fixture = require 'tests.fixture'

local ffi = require('ffi')
ffi.cdef([=[
int setenv(const char*, const char*, int);
]=])

local function setenv(name, value)
    if ffi.C.setenv(name, value, 1) == -1 then
        return nil, ffi.C.strerror(ffi.errno())
    else
        return value
    end
end

local function read_test_config()
    local f = assert(io.open('tests/data/masks.json', 'r'))
    local t = f:read('*all')
    f:close()
    return t
end

-- put json config to environment to setup masquerade
assert(setenv('MASQUERADE_FAKE_ACCOUNTS', read_test_config()))

local masquerade = require 'masquerade-nginx'

function table_compare(tbl1, tbl2)
    for k, v in pairs(tbl1) do
        if (type(v) == "table" and type(tbl2[k]) == "table") then
            if (not table_compare(v, tbl2[k])) then return false end
        else
            if (v ~= tbl2[k]) then return false end
        end
    end
    for k, v in pairs(tbl2) do
        if (type(v) == "table" and type(tbl1[k]) == "table") then
            if (not table_compare(v, tbl1[k])) then return false end
        else
            if (v ~= tbl1[k]) then return false end
        end
    end
    return true
end

function dump(o)
    if type(o) == 'table' then
        local s = '{ '
        for k,v in pairs(o) do
            if type(k) ~= 'number' then k = '"'..k..'"' end
            s = s .. '['..k..'] = ' .. dump(v) .. ','
        end
        return s .. '} '
    else
        return tostring(o)
    end
end

fixture.mock_ngx:init_request({Authorization = 'OAuth token'}, {foo = 'bar'})
assert(table_compare(ngx.req.get_headers(), {Authorization = 'OAuth token'}))
assert(table_compare(ngx.req.get_uri_args(), {foo = 'bar'}))
masquerade.replace_credentials()
assert(ngx.req.get_headers(), {[masquerade.AUTHORIZATION_HEADER] = 'OAuth o1'})
assert(table_compare(ngx.req.get_uri_args(), {foo = 'bar'}))


fixture.mock_ngx:init_request({test_header = 'test_value'}, {uuid = 'some_test_uuid', foo = 'bar'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {test_header = 'test_value'}))
assert(table_compare(ngx.req.get_uri_args(), {uuid = 'u1', foo = 'bar'}))

fixture.mock_ngx:init_request({test_header = 'test_value'}, {deviceid = 'some_test_deviceid', foo = 'bar'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {test_header = 'test_value'}))
assert(table_compare(ngx.req.get_uri_args(), {deviceid = 'd1', foo = 'bar'}))

fixture.mock_ngx:init_request({test_header = 'test_value'}, {miid = 'some_test_miid', foo = 'bar'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {test_header = 'test_value'}))
assert(table_compare(ngx.req.get_uri_args(), {miid = 'm1', foo = 'bar'}))

fixture.mock_ngx:init_request({[masquerade.USER_TICKET_HEADER] = 'user_ticket'}, {uuid = 'some_test_uuid'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {[masquerade.AUTHORIZATION_HEADER] = 'OAuth o1'}))
assert(table_compare(ngx.req.get_uri_args(), {uuid = 'u1'}))

fixture.mock_ngx:init_request({Host = 'host-name.which.is.also.a.tvmtool.alias'}, {uuid = 'some_test_uuid'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {
    Host = 'host-name.which.is.also.a.tvmtool.alias',
    ['Fake-Service-Ticket'] = 'fake service ticket',
}))
assert(table_compare(ngx.req.get_uri_args(), {uuid = 'u1'}))

fixture.mock_ngx:init_request({Host = 'host-name.which.is.not.a.tvmtool.alias'}, {uuid = 'some_test_uuid'})
masquerade.replace_credentials()
assert(table_compare(ngx.req.get_headers(), {
    Host = 'host-name.which.is.not.a.tvmtool.alias',
}))
assert(table_compare(ngx.req.get_uri_args(), {uuid = 'u1'}))
