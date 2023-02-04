function gen_admin_error_replier()
    if admin_error_replier_status ~= nil then
        return {
            errordocument = {
                status = admin_error_replier_status;
                content = "admin_error_erplier";
            }
        }
    end
    return nil;
end

function split(s)
    local elements = {}
    local pattern = '([^,]+)'
    string.gsub(s, pattern, function(value) elements[#elements + 1] = value end)
    return elements
end

function parse_status_codes(s)
    if s == nil then
        return nil
    end

    return split(s)
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
        keepalive = keepalive or 0;
        ipdispatch = {
            default = {
                pinger = {
                    admin_request_uri = admin_uri;
                    delay = delay;
                    histtime = histtime;
                    ping_request_data = ping_request;
                    enable_tcp_check_file = check_file;
                    lo = lo;
                    hi = hi;
                    switch_off_file = switch_off_file;
                    switch_off_key = switch_off_key;
                    status_codes = parse_status_codes(status_codes);
                    status_codes_exceptions = parse_status_codes(status_codes_exceptions);
                    admin_error_replier = gen_admin_error_replier();
                    module = {
                        balancer2 = {
                            hashing = {
                                {
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        backend_timeout = backend_timeout;
                                        connect_timeout = "1s"; resolve_timeout = "1s";
                                        keepalive_count = keepalive_count;
                                    }; -- proxy
                                };
                            }; -- hashing
                        }; -- balancer2
                    }; -- module
                }; -- pinger
            }; -- default
        }; -- ipdispatch
    }; -- http
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
