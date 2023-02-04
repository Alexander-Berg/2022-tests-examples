limiter_agent = require 'ratelimiter2'

-- test helpers

local function tbl2string(T, sortedKeys)

    if not sortedKeys then
        sortedKeys = {}
        for key in pairs(T) do table.insert(sortedKeys, key) end
        table.sort(sortedKeys)
    end

    local str = ''
    for _, key in ipairs(sortedKeys) do
        local val = T[key]
        if val then
            if str ~= '' then str = str .. ','  end -- separator

            if type(val) == 'table' then
                str = str .. key .. '=' .. tbl2string(val)
            else
                str = str .. key .. '=' .. val
            end
        end
    end
    return '{' .. str .. '}'
end

local function req2string(reqParams)
    return'request('..tbl2string(reqParams)..')'
    -- return'request('..tbl2string(reqParams, {'method','path','query','headers','body'})..')'
end

local function str2codes(s)
    local codes = ''
    for c in s:gmatch'.' do
        if codes ~= '' then
            codes = codes .. ','    -- separator
        end
        codes = codes ..string.byte(c)
    end
end


local function response_method(self, params)
    if not self.log then self.log = '' end

    self.log = self.log .. req2string(params)

    if not self.response then
        return nil, 'HTTP 503'
    else
        self.status = self.response.status
        return self
    end
end

local function read_body_method(self)
    return self.response.body
end

-- set shard identifier
limiter_agent.shard_id = 'worker1'

---------------------------------------------------------------
-- test update_limits err result
local plush_http = {
    request = response_method,
    read_body = read_body_method,
    response = nil,
}

local ok, err = limiter_agent.update_limits(plush_http)
assert(ok == limiter_agent.codes.ERR)
assert(err == 'HTTP 503')
assert(plush_http.log == 'request({headers={Connection=keep-alive},method=GET,path=/resources/limits/get})',
    'Test sync_limits err failed, log:' .. plush_http.log)


---------------------------------------------------------------
-- test rate_access (while no limits loaded)
local ret = limiter_agent.rate_access('client.1', 'resource.5', 10)
assert(ret == limiter_agent.codes.OK)
local ret = limiter_agent.rate_access('some.client', 'some.resource', 1)
assert(ret == limiter_agent.codes.OK)

---------------------------------------------------------------
-- test update_counters
local plush_http = {
    request = response_method,
    read_body = read_body_method,
    response = {
        body = '', -- empty counters update from server
        status = 200
    },
}
local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.OK)
local expectedPayload = '\10\35\10\10resource.5\18\8\52\11\91\224\148\219\80\161\26\8\165\9\173\205\231\205\6\4\34\1'..
        '\10\10\38\10\13some.resource\18\8\88\74\128\93\190\59\94\148\26\8\146\73\93\199\137\125\82\218\34\1\1\24\1'
-- pre-serialized counters message {
-- {'resource.5', {{'client.1', 10 }}}
-- {'some.resource', {{ 'some.client', 1 }}}
-- } proto string
assert(plush_http.log == 'request({body='..expectedPayload..',headers={Connection=keep-alive},method=POST,' ..
        'path=/counters/sync,query={shard=worker1}})', --NB: no version parameter 'cause no limits set yet
    'Test failed, log:' .. plush_http.log)

---------------------------------------------------------------
-- test update_limits success
local plush_http = {
    request = response_method,
    read_body = read_body_method,
    response = {
-- pre-serialized 'any client' limits {
    -- {resource = 'resource.1', rps=10, burst=5, unit=1},
    -- {resource = 'resource.forbidden', rps=0, burst=0, unit=1}},
    -- version = 1 }
        body = '\10\18\10\10resource.1\32\10\40\5\48\1\10\26\10\18resource.forbidden\32\0\40\0\48\1\16\1',
        status = 200
    },
}

local ok, err = limiter_agent.update_limits(plush_http)
assert(plush_http.log ==
        'request({headers={Connection=keep-alive},method=GET,path=/resources/limits/get})',
        'Test failed, log:' .. plush_http.log)
assert(ok == limiter_agent.codes.OK)
assert(not err)

---------------------------------------------------------------
-- test rate_access
local ret = limiter_agent.rate_access('client.1', 'resource.1', 1)
assert(ret == limiter_agent.codes.OK)

local ret = limiter_agent.rate_access('client.1', 'resource.1', 2)
assert(ret == limiter_agent.codes.OK)

local ret = limiter_agent.rate_access('client.1', 'resource.1', 5)
assert(ret == limiter_agent.codes.LIMIT_EXCEEDED)

local ret = limiter_agent.rate_access('client.2', 'resource.1', 5)
assert(ret == limiter_agent.codes.OK)

local ret = limiter_agent.rate_access('client.2', 'resource.not.defined', 1)
assert(ret == limiter_agent.codes.LIMIT_UNDEFINED)

local ret = limiter_agent.rate_access('client.1', 'resource.forbidden', 1)
assert(ret == limiter_agent.codes.FORBIDDEN)

---------------------------------------------------------------
-- test update_counters success
local plush_http = {
    request = response_method,
    read_body = read_body_method,
    response = {
        body = '', -- empty counters update from server
        status = 200
    },
}

local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.OK)
-- pre-serialized counters {
--  {'resourse.1', {{ 'client.1', 3 }, {'client.2', 5}}
--  {'some.resource', {{ 'some.client', 1 }}}
--  {'resource.5', {'client.1', 10 }}
-- } proto string
local expectedPayload = '\10\35\10\10resource.5\18\8\52\11\91\224\148\219\80\161\26\8\165\9\173\205\231\205\6\4\34\1'..
        '\10\10\38\10\13some.resource\18\8\88\74\128\93\190\59\94\148\26\8\146\73\93\199\137\125\82\218\34\1\1\10\52'..
        '\10\10resource.1\18\16\52\11\91\224\148\219\80\161\2\42\198\77\154\232\93\186\26\16\165\9\173\205\231\205\6'..
        '\4\131\132\236\189\38\47\174\157\34\2\3\5\16\1\24\2'
assert(plush_http.log == 'request({body='..expectedPayload..',headers={Connection=keep-alive},method=POST,path=/counters/sync,query={shard=worker1}})',
    'Test failed, log:' .. plush_http.log)


---------------------------------------------------------------
-- test garbage collect counters
local ok = limiter_agent.garbage_collect_counters()
assert(ok == limiter_agent.codes.OK)
-- check no counters left (by calling update_counters)
local plush_http = {
    request = response_method,
    read_body = read_body_method,
    response = { body = '',  status = 200 },
}
local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.OK)
local expectedPayload = '\10\12\10\10resource.5\10\15\10\13some.resource\10\12\10\10resource.1\16\1\24\3'
-- pr-serialized empty resource entries
assert(plush_http.log == 'request({body='..expectedPayload..',headers={Connection=keep-alive},method=POST,path=/counters/sync,query={shard=worker1}})',
    'Test failed, log:' .. plush_http.log)


---------------------------------------------------------------
-- test update_counters server err
local plush_http = {
    request = response_method,
    response = nil,
}
local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.ERR)
assert(err == 'HTTP 503')
-- pr-serialized empty resource entries
local expectedPayload = '\10\12\10\10resource.5\10\15\10\13some.resource\10\12\10\10resource.1\16\1\24\4'
assert(plush_http.log ==
    'request({body='..expectedPayload..',headers={Connection=keep-alive},method=POST,path=/counters/sync,query={shard=worker1}})',
    'Test failed, log:' .. plush_http.log)

---------------------------------------------------------------
-- test limits update on 409 from server/counters/sync
local plush_http = {
    log = '',
    request = function(self, params)
        self.log = self.log .. req2string(params)
        if params.path == '/counters/sync' then
            self.status = 409
        else
            self.status = 200
        end
        return self
    end,
    -- serialized limitsVersion = 42
    read_body = function(self) return '\16\42' end,
}
local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.LIMITS_CONFLICT)

---------------------------------------------------------------
-- test lamport

local plush_http = {
    log = '',
    request = function(self, params)
        self.log = self.log .. req2string(params)
        self.status = 200
        return self
    end,
    -- serialized lamport 123
    read_body = function(self) return '\24\123' end,
}

-- agent gets new lamport, and next time sends incremented
local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.OK)

plush_http.request = function(self, params)
    self.log = self.log .. req2string(params)
    -- pr-serialized empty resource entries with limitsVersion = 1, lamport = 124
    local expectedPayload = '\10\12\10\10resource.5\10\15\10\13some.resource\10\12\10\10resource.1\16\1\24\124'
    assert(params.body == expectedPayload, 'Test failed, log:' .. plush_http.log)
    self.status = 200
    -- ratelimiter proxy accepted incremented lamport
    self.read_body = function(self) return '\24\124' end
    return self
end

local ok, err = limiter_agent.update_counters(plush_http)
assert(ok == limiter_agent.codes.OK)
