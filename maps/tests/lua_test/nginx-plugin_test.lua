-- mock ngx
local scheduled_timers = {}
ngx = {
    worker = {
        pid = function() return 153 end,
        exiting = function() end
    },
    log = function() end,
    sleep = function() end,
    timer = {
        at = function(delay, callback, ...)
            -- store timer callback with arguments
            local args = {...}
            scheduled_timers[#scheduled_timers+1] = function()
                callback(false, unpack(args))
            end
            return true
        end
    },
    HTTP_FORBIDDEN = 403,
    say = function() end,
    exit = function() end,
    status = 200
}

-- mock resty.http
local http_log = ''
package.loaded['resty.http'] = {
    new = function()
        return {
            connect = function() return true end,
            request = function(self, params)
                http_log = http_log .. params.method .. '(' .. params.path .. ')'
                -- mock responses
                if params.path == '/resources/limits/get' then
                    return {
                        status = 200,
                        read_body = function()
                            -- pre-serialized limits {
                            --  {resource = 'resource.1', rps=10, burst=5, unit=1},
                            --  {resource = 'resource.forbidden', rps=0, burst=0, unit=1}},
                            -- version = 1 }
                            return '\10\18\10\10resource.1\32\10\40\5\48\1\10\26\10\18resource.forbidden\32\0\40\0\48\1\16\1'
                        end
                    }
                elseif params.path == '/counters/sync' then
                    return {
                        status = 200,
                        read_body = function() return '' end
                    }
                end
            end,
            set_timeout = function() end,
            set_keepalive = function() end
        }
    end
}

-- Initialization and background jobs tests

local plugin = require("ratelimiter2-nginx-plugin")

-- On load plugin schedules background jobs initialization with ngx.timer.at(0)
-- So right after start expect single timer registered
assert(#scheduled_timers == 1)
local run_callbacks = scheduled_timers
scheduled_timers = {}
-- run timer callbacks
for i = 1, #run_callbacks do run_callbacks[i]() end

-- on start expect /limits/get and /counters/sync requests
assert(http_log == 'GET(/resources/limits/get)POST(/counters/sync)')
http_log = ''

-- expect 2 background jobs scheduled as timers: garbage_collect and counters_sync
assert(#scheduled_timers == 2)
run_callbacks = scheduled_timers
scheduled_timers = {}

-- expect both jobs auto reschedule themselves each cycle
for i = 1, #run_callbacks do run_callbacks[i]() end
assert(#scheduled_timers == 2)

-- expect /counters/sync request from counters_sync job
assert(http_log == 'POST(/counters/sync)')
http_log = ''

-- rate_access tests

-- allowed
plugin.rate_access('client', 'resource.1', 1)
assert(ngx.status == 200)
-- limit exceeded
plugin.rate_access('client', 'resource.1', 10)
assert(ngx.status == 429)
-- access forbidden
plugin.rate_access('client', 'resource.forbidden', 1)
assert(ngx.status == ngx.HTTP_FORBIDDEN)
