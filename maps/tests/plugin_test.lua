-- mock ngx
ngx_logged_message = nil
ngx_reject_message = nil
ngx = {
    status = 200,
    say = function(text)
        ngx_reject_message = text
    end,
    exit = function() end,
    log = function(level, text)
        local message = level .. ': ' .. text
        print(message)
        ngx_logged_message = message
    end,
    ERR = 'err',
    WARN = 'warn',
    HTTP_OK = 200,
    HTTP_FORBIDDEN = 403,
    HTTP_TOO_MANY_REQUESTS = 429,
    HTTP_INTERNAL_SERVER_ERROR = 500,
    HTTP_SERVICE_UNAVAILABLE = 503
}

-- mock resty.http
mock_http = {
    connect = function() return true end,
    request = function(self, params)
        assert(params.query.tvm_id == self.expected_query.tvm_id)
        assert(params.query.endpoint == self.expected_query.endpoint)
        assert(params.query.resource_id == self.expected_query.resource_id)
        return self.response, self.err
    end,
    set_timeout = function() end,
    set_keepalive = function() return true end,
    close = function() end,

    expected_query = nil,
    response = {
        status = 200,
        read_body = function() end,
    },
    err = nil,
}

package.loaded['resty.http'] = {
    new = function() return mock_http end
}

local quotateka = require("quotateka-nginx-plugin")

-- access allowed, pass request through
mock_http.expected_query = {endpoint = '/whatever'}
quotateka.access(nil, '/whatever')
assert(ngx.status == 200)
assert(ngx_reject_message == nil)

mock_http.expected_query = {endpoint = '/whatever', resource_id = 'resource1', tvm_id = 42}
quotateka.access(42, '/whatever', 'resource1')
assert(ngx.status == 200)
assert(ngx_reject_message == nil)

-- limit exceeded, block request with 429
mock_http.expected_query = {endpoint = '/whatever', tvm_id = 42}
mock_http.response.status = 429
quotateka.access(42, '/whatever')
assert(ngx.status == 429)
assert(ngx_reject_message == 'Limit exceeded')
assert(ngx_logged_message == 'err: Limit EXCEEDED for tvm:42 to access /whatever', ngx_logged_message)

-- /access forbidden, block request with 403
mock_http.response.status = 403
mock_http.expected_query = {endpoint = '/restricted'}
quotateka.access(nil, '/restricted')
assert(ngx.status == 403)
assert(ngx_reject_message == 'Forbidden anonymous access', ngx_reject_message)
assert(ngx_logged_message == 'err: Forbidden anonym to access /restricted', ngx_logged_message)

mock_http.expected_query = {endpoint = '/restricted', tvm_id = 42}
quotateka.access(42, '/restricted')
assert(ngx.status == 403)
assert(ngx_reject_message == 'Forbidden tvm:42 access', ngx_reject_message)
assert(ngx_logged_message == 'err: Forbidden tvm:42 to access /restricted', ngx_logged_message)

-- on /access 503, we respond with 503
mock_http.response.status = ngx.HTTP_SERVICE_UNAVAILABLE
quotateka.access(42, '/restricted')
assert(ngx.status == ngx.HTTP_SERVICE_UNAVAILABLE)
assert(ngx_logged_message == 'err: Failed /access request: 503', ngx_logged_message)

-- on /access timeout, we respond with 503
mock_http.response = none
mock_http.err = 'timeout'
quotateka.access(42, '/restricted')
assert(ngx.status == ngx.HTTP_SERVICE_UNAVAILABLE)
assert(ngx_logged_message == 'err: Failed /access request: timeout', ngx_logged_message)

-- on /access 500, pass request through
mock_http.response = {status = ngx.HTTP_INTERNAL_SERVER_ERROR, read_body = function() end}
mock_http.err = nil
ngx.status = 200
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_logged_message == 'err: Failed /access request: 500', ngx_logged_message)

-- on other errors, pass request through
mock_http.response = nil
mock_http.err = 'unknown error'
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_logged_message == 'err: Failed /access request: unknown error', ngx_logged_message)


-- Dry run mode tests (requests are passed through)
quotateka.agent_settings.dry_run = true
ngx.status = 200
ngx_reject_message = nil
mock_http.response = {
    status = 200,
    read_body = function() end,
}
mock_http.err = nil

-- dry run: limit exceeded
mock_http.response.status = 429
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_reject_message == nil)
assert(ngx_logged_message == 'err: Limit EXCEEDED for tvm:42 to access /restricted')

-- dry run: forbidden
mock_http.response.status = 403
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_reject_message == nil)
assert(ngx_logged_message == 'err: Forbidden tvm:42 to access /restricted', ngx_logged_message)

-- dry run: 500 on /access
mock_http.response.status = ngx.HTTP_INTERNAL_SERVER_ERROR
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_logged_message == 'err: Failed /access request: 500', ngx_logged_message)

-- dry run: 503 on /access
mock_http.response.status = ngx.HTTP_SERVICE_UNAVAILABLE
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_logged_message == 'err: Failed /access request: 503', ngx_logged_message)

-- dry run: /access timout
mock_http.response = nil
mock_http.err = 'timeout'
quotateka.access(42, '/restricted')
assert(ngx.status == 200)
assert(ngx_logged_message == 'err: Failed /access request: timeout', ngx_logged_message)
