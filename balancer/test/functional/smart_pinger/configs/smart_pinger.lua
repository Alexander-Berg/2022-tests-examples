on_disable = on_disable and on_disable == "true";
function gen_smart_pinger()
    local result = {
        ping_request_data = ping_request_data;
        delay = delay;
        ttl = ttl;
        min_samples_to_disable = min_samples_to_disable;
        lo = lo;
        hi = hi;
        ping_disable_file = ping_disable_file;
        proxy = {
            host = "localhost"; port = backend_port;
            backend_timeout = "5s";
            connect_timeout = "5s";
            resolve_timeout = "1s";
            keepalive_count = keepalive_count;
            fail_on_5xx = true;
        }; -- proxy
    };
    if on_disable then
        result["on_disable"] = {
            errordocument = {
                status = 500;
                content = 'Error';
            }; -- errordocument
        }; -- on_disable
    end
    return result
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    };

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        ipdispatch = {
            admin = {
                ip = "localhost"; port = admin_port;
                admin = {}
            }; -- ipdispatch/default
            test = {
                smart_pinger = gen_smart_pinger();
            }; -- test
        }; -- ipdispatch
    }; -- http
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
