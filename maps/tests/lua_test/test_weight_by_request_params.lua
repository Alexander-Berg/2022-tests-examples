-- mock ngx
ngx = {
    worker = { pid = function() return 153 end },
    log = function(level, message)
        print('ngx.log ' .. message)
    end,
    timer = { at = function() return true end },
    req = {}
}
-- mock resty.http
package.loaded['resty.http'] = {}
-- mock ratelimiter2
package.loaded['ratelimiter2'] = {}


local plugin = require("ratelimiter2-nginx-plugin")


-- params mathing tests

local weights_table = 'return {argA = {A3 = 3, A5 = 5}, argB = {["153"] = 10}, }'

-- no params in request
ngx.req.get_uri_args = function() return {} end
-- default is 1
assert(1 == plugin.weight_by_request_params(weights_table))

-- 1 match
ngx.req.get_uri_args = function() return {argA = 'A5'} end
assert(5 == plugin.weight_by_request_params(weights_table))

-- 2 matches multiplied
ngx.req.get_uri_args = function() return {argA = 'A3', argB = '153'} end
assert(3 * 10 == plugin.weight_by_request_params(weights_table))

-- no matches
ngx.req.get_uri_args = function() return {argA = 'B1', argB = 'A1'} end
assert(1 == plugin.weight_by_request_params(weights_table))

-- no matches
ngx.req.get_uri_args = function() return {argC = 'A1'} end
assert(1 == plugin.weight_by_request_params(weights_table))

-- empty table
ngx.req.get_uri_args = function() return {argC = 'A1'} end
assert(1 == plugin.weight_by_request_params(''))


-- table expression syntax tests
assert(1 == plugin.weight_by_request_params('WTF'))
ngx.req.get_uri_args = function() return {argA = 'A1'} end
assert(1 == plugin.weight_by_request_params('kekeke {argA = {A1 = 153}}'))


-- cache tests

-- put table into cache
ngx.req.get_uri_args = function() return {argB = '153'} end
assert(10 == plugin.weight_by_request_params(weights_table, 'cache_key1'))

-- repeate request, use cached table
assert(10 == plugin.weight_by_request_params('', 'cache_key1'))

-- same request, different key
assert(1 == plugin.weight_by_request_params('', 'cache_key2'))

-- one more key
assert(1 == plugin.weight_by_request_params('return {argC = {C1 = 33} }', 'cache_key3'))
ngx.req.get_uri_args = function() return {argC = 'C1'} end
assert(33 == plugin.weight_by_request_params('', 'cache_key3'))
