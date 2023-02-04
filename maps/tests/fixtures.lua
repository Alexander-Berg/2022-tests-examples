local test = require 'testing'

package.loaded['ngx'] = test.Mock:new{
    header = {},
    var = {},
    req = {
        header = { Authorization = 'Bearer keycloak_token' },
        get_headers = function()
            local ngx = require 'ngx'
            return ngx.req.header
        end,
        set_header = function(key, value)
            local ngx = require 'ngx'
            ngx.req.header[key] = value
        end,

        method = nil,
        get_method = function()
            local ngx = require 'ngx'
            return ngx.req.method
        end,
    },

    status = 200,
    msg = '',
    print = function(msg)
        local ngx = require 'ngx'
        ngx.msg = msg
        print(string.format('Rejected: %s', msg))
    end,
    exit_code = 200,
    exit = function(code)
        local ngx = require 'ngx'
        ngx.exit_code = code
    end,

    logs = '',
    log = function(level, message)
        local ngx = require 'ngx'
        ngx.logs = level .. ': ' .. message
    end,

    HTTP_OK = 200,
    HTTP_UNAUTHORIZED = 401,
    HTTP_INTERNAL_SERVER_ERROR = 500,

    ERR = 'ERROR',
}

package.loaded['resty.http'] = test.Mock:new{
    new = function()
        return {
            request_uri = function(self, url, params)
                local http = require 'resty.http'
                http.log = 'Requesting ' .. url
                http.params = params
                if http.response ~= '' then
                    local resp = {
                        ['body'] = http.response,
                        ['status'] = http.status
                    }
                    return resp, nil
                else
                    return nil, http.err
                end
            end,
        }
    end,

    response = '',
    status = 200,
    err = '',
    log = '',
}
