if on_error_status == nil then on_error_status = "200"; end
if return_last_5xx == nil then return_last_5xx = false; end
if return_last_blacklisted_http_code == nil then return_last_blacklisted_http_code = false; end
if fast_503 == nil then fast_503 = false; end

function prepare_blacklist(str)
    if not str then
        return nil
    end

    local retval = {}
    for w in string.gmatch(str, ",?([^,]+)") do
        retval[#retval + 1] = w
    end
    return retval
end

function gen_on_status_code(code, content)
    if code == nil or content == nil then
        return nil
    end

    return {
        [code] = {
            errordocument = {
                status = 200;
                content = content;
            }; -- errordocument
        };
    };
end

balancer2 = {
    attempts = 3;
    status_code_blacklist = prepare_blacklist(status_code_blacklist);
    status_code_blacklist_exceptions = prepare_blacklist(status_code_blacklist_exceptions);
    return_last_5xx = return_last_5xx;
    return_last_blacklisted_http_code = return_last_blacklisted_http_code;
    fast_503 = fast_503;
    on_status_code=gen_on_status_code(on_status_code, on_status_code_content);
    rr = {
        randomize_initial_state = false;
        {
            errordocument = {
                status = status;
                content = ":(";
            }; -- errordocument
        },
    }; -- rr
    on_error = {
        errordocument = {
            status = on_error_status;
            content = "on_error";
        }; -- errordocument
    }; -- on_error
}; -- balancer

if policy == "simple_policy" then balancer2["simple_policy"] = {}; end
if policy == "unique_policy" then balancer2["unique_policy"] = {}; end
if policy == "unique_retry_policy" then balancer2["unique_retry_policy"] = {}; end

backend_host = backend_host or "localhost"
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            http = {
                maxlen = 65536; maxreq = 65536;
                balancer2 = balancer2;
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

