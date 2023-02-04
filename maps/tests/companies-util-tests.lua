require "companies_util"

local function reset_ngx()
    ngx = {
        status = 200,
        print = function() end,
        exit = function() end,
        HTTP_OK = 200,
        HTTP_FORBIDDEN = 403,
    }
end


reset_ngx()
init_companies("1:key1")
check_company_has_apikey("1", "key1")
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
init_companies("1:key1,2:key2")
check_company_has_apikey("1", "key1")
check_company_has_apikey("2", "key2")
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
init_companies("1:key1,2:key2,3:key3")
check_company_has_apikey("1", "key1")
check_company_has_apikey("2", "key2")
check_company_has_apikey("3", "key3")
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
init_companies("1:key1,1:key2")
check_company_has_apikey("1", "key1")
assert(ngx.status == ngx.HTTP_OK)
check_company_has_apikey("1", "key2")
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
init_companies("1:key1,2:key2")
check_company_has_apikey("2", "key1")
assert(ngx.status == ngx.HTTP_FORBIDDEN)
check_company_has_apikey("1", "key2")
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
init_companies("")
check_company_has_apikey("any", "any")
assert(ngx.status == ngx.HTTP_FORBIDDEN)
