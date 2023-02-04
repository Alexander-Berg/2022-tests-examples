local function trim(str)
    return str:gsub('^%s*', ''):gsub('%s*$', '')
end

local function parse_cookies(cookies_header)
	local res = {}
	for cookie in cookies_header:gmatch('[^;]+') do
        for key, value in cookie:gmatch('([^=]+)=([^=]+)') do
            res[trim(key)] = trim(value)
        end
	end
	return res
end

local fixture = {
    mock_http = {
        set_timeout = function() end,
        connect = function(self) self.log = (self.log or '') .. 'connect();' return 1 end,
        request = function(self)
            self.log = self.log .. 'request();'
            return self.response
        end,
        set_keepalive = function(self) self.log = self.log .. 'set_keepalive();' return 1 end,
        close = function(self) self.log = self.log .. 'close();' end,

        response = nil,
        log = nil
    },

    mock_ngx = {
        request_headers = {},
        init_request = function(self, headers_table)
            ngx.status = 200  -- reset request status
            self.request_headers = {}
            ngx.var = {
                remote_address = '::1'
            }
            for header, value in pairs(headers_table) do
                self:set_request_header(header, value)
            end
        end,
        set_request_header = function(self, header, value)
            self.request_headers[header] = value
            -- put header and cookies into ngx.var entries
            local var_name = 'http_'..header:lower():gsub('-', '_')
            ngx.var[var_name] = value
            if header == 'Cookie' and value then
                for k, v in pairs(parse_cookies(value)) do
                    ngx.var['cookie_'..k] = v
                end
            end
        end
    },
}

-- mock ngx
ngx = {
    HTTP_INTERNAL_SERVER_ERROR = 500,
    HTTP_UNAUTHORIZED = 401,
    HTTP_FORBIDDEN = 403,
    req = {
        get_headers = function() return fixture.mock_ngx.request_headers end,
        set_header = function(name, value) fixture.mock_ngx:set_request_header(name, value) end,
    },
    var = {},
    log = function() end,
    say = function() end,
    exit = function() end,
    status = 200,
}

-- mock resty.http
package.loaded['resty.http'] = {
    new = function() return fixture.mock_http end
}


return fixture