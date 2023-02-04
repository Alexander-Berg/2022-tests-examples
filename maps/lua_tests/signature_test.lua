require 'http_signature'

sign_key = "AAECAwQFBgcICQoLDA0ODw=="

signature_for_default_body_tests = "VSE7n7SELBlqLYBItuNWUfnNcjI="
signature_for_file_test = "ISXXj+71Xjtx1Ig93cfceByyjnk="
signature_for_empty_body = "EyEjMjIEr8XB3KHa2t9FNklK/Bc="

function reset_ngx()
    _body_data = "Report contents"
    _body_file = nil
    ngx = {
        status = 200,
        say = function() end,
        exit = function() end,
        print = function() end,
        var = {
            scheme = 'http',
            host = 'auto-proxy.maps.yandex.net',
            request_method = 'POST',
            request_uri = '/reports/1.x/upload_report?head_id=ABCDEF123456&device_id=0123456789abcdef0123456789abcdef',
            http_x_yruntime_signature = signature_for_default_body_tests,
            http_user_agent = 'Mozilla/5.0',
            http_x_yruntime_timestamp = '1585671515'
        },
        req = {
            get_body_data = function() return _body_data end,
            get_body_file = function() return _body_file end,
            read_body = function() end,
        },
        HTTP_OK = 200,
        HTTP_FORBIDDEN = 403,
        HTTP_INTERNAL_SERVER_ERROR = 500,
    }
end

reset_ngx()
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
ngx.var.http_x_yruntime_signature = nil
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_user_agent = nil
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_x_yruntime_timestamp = nil
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_x_yruntime_signature = 'ZmZmZmZmZmZmZmZmZmZmZmZmZmY='
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_x_yruntime_signature = '012345-vwxyz'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_user_agent = 'NCSA_Mosaic/2.0'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_x_yruntime_timestamp = '1590000000'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.request_method = 'GET'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.request_uri = '/store/1.x/app_list'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.scheme = 'https'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.host = 'localhost'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_FORBIDDEN)

reset_ngx()
ngx.var.http_x_yruntime_signature = signature_for_file_test
_body_data = nil
_body_file = 'en_ru_jp_cn_emoji_0x00_0xff.txt'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_OK)

reset_ngx()
ngx.var.http_x_yruntime_signature = signature_for_file_test
_body_data = nil
_body_file = 'non-existent-file'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)

reset_ngx()
ngx.var.http_x_yruntime_signature = signature_for_empty_body
_body_data = nil
_body_file = 'non-existent-file'
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_INTERNAL_SERVER_ERROR)

reset_ngx()
ngx.var.http_x_yruntime_signature = signature_for_empty_body
_body_data = nil
_body_file = nil
check_http_signature(sign_key)
assert(ngx.status == ngx.HTTP_OK)
