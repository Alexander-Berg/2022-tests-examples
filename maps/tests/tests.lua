local test = require 'testing'
local describe, it = test.describe, test.it

package.loaded['resty.http'] = test.Mock:new{
    _mock = {
        body = nil,
        error = nil,
        status = 200,
    },
    _client = test.Mock:new{
        request_uri = test.stub(function()
            local http = require 'resty.http'
            if http._mock.error ~= nil then
                return nil, http._mock.error
            else
                local response = {
                    body = http._mock.body,
                    status = http._mock.status,
                }
                return response, nil
            end
        end),
    },
    new = test.stub(function()
        local http = require 'resty.http'
        return http._client
    end),
}

package.loaded['ngx'] = test.Mock:new{
    req = {
        get_headers = function()
            return {
                Authorization = 'Bearer keycloak_token'
            }
        end
    },
    HTTP_OK = 200,
    HTTP_INTERNAL_SERVER_ERROR = 500,
}

function dump(o, depth)
    depth = depth or 0
    if type(o) == 'table' then
        local s = '{\n'
        for k,v in pairs(o) do
            if type(k) ~= 'number' then k = '"'..tostring(k, 0)..'"' end
            if depth <= 10 then
                v = dump(v, depth + 1, already_seen)
            else
                v = tostring(v)
            end
            s = s .. string.rep('  ', depth + 1) .. '['..k..'] = ' .. v .. ',\n'
        end
        return s .. string.rep('  ', depth) .. '}'
    else
        return tostring(o)
    end
end

local exchange = require 'jwt_exchange'
local http = require 'resty.http'
local ngx = require 'ngx'

local TEST_IDENTITY_URL = 'https://identity.routeq.test.com'
local TEST_IDENTITY_HOST = 'test.identity.b2bgeo.com'

exchange.settings.identity_url = TEST_IDENTITY_URL
exchange.settings.identity_host = TEST_IDENTITY_HOST

local DUMMY_APIKEY = 'abcd1234'

describe('exchange', function()
    it('test_exchange_apikey', function()
        local dummy_token = 'abcdefgh'
        http._mock.body = string.format('{"token": "%s"}', dummy_token)

        local status, jwt, err = exchange.exchange_apikey(DUMMY_APIKEY)
        assert(status == ngx.HTTP_OK)
        assert(err == nil)
        assert(jwt == dummy_token)

        local request_args = test.called(http._client.request_uri)
        assert(#request_args == 1)
        assert(request_args[1][2] == TEST_IDENTITY_URL)
        assert(dump(request_args[1][3]) == dump({
            headers = { Host = TEST_IDENTITY_HOST },
            method = 'GET',
            path = '/internal/tokens/company',
            query = {
                apikey = DUMMY_APIKEY,
            },
        }))
    end)

    it('test_exchange_user_token', function()
        local dummy_token = 'abcdefgh'
        http._mock.body = string.format('{"token": "%s"}', dummy_token)

        local status, jwt, err = exchange.exchange_user_token()
        assert(status == ngx.HTTP_OK)
        assert(err == nil)
        assert(jwt == dummy_token)

        local request_args = test.called(http._client.request_uri)
        assert(#request_args == 1)
        assert(request_args[1][2] == TEST_IDENTITY_URL)

        local sent_request = request_args[1][3]
        assert(sent_request.method == 'GET')
        assert(sent_request.path == '/internal/tokens/user')
        assert(sent_request.query == nil)

        local sent_headers = sent_request.headers
        assert(sent_headers ~= nil)
        assert(sent_headers.Host == TEST_IDENTITY_HOST)
        assert(sent_headers.Authorization == 'Bearer keycloak_token')
    end)

    it('test_request_failure', function()
        http._mock.status = 404
        local status, _, err = exchange.exchange_apikey(DUMMY_APIKEY)
        assert(status == 404)
        assert(err == 'Identity response error code: 404, body: nil')
    end)

    it('test_read_body_failure', function()
        http._mock.error = 'Error reading body'
        local status, _, err = exchange.exchange_apikey(DUMMY_APIKEY)
        assert(status == ngx.HTTP_INTERNAL_SERVER_ERROR)
        assert(err == 'Error reading body')
    end)

    it('test_json_parse_failure', function()
        http._mock.body = '2 + 2'
        local status, _, err = exchange.exchange_apikey(DUMMY_APIKEY)
        assert(status == ngx.HTTP_INTERNAL_SERVER_ERROR)
        assert(err == 'Expected the end but found invalid number at character 3')
    end)

    it('test_missing_token_field', function()
        http._mock.body = '{"a": 3}'
        local status, _, err = exchange.exchange_apikey(DUMMY_APIKEY)
        assert(status == ngx.HTTP_INTERNAL_SERVER_ERROR)
        assert(err == 'Field "token" is missing in Identity response')
    end)
end)
