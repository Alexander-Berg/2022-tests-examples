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

function gen_on_error(status_code)
    if status_code == nil then
        return nil
    end

    return {
        errordocument = {
            status = status_code;
            content = "on_error";
        }; -- errordocument
    };
end

rr = {
    randomize_initial_state = false;
}

for i = 1, backend_count do
    rr["backend" .. i] = {
        proxy = {
            host = "localhost"; port = _G["backend" .. i .. "_port"];
            connect_timeout = "0.3s"; backend_timeout = backend_timeout;
            resolve_timeout = "0.3s";
            use_only_ipv6 = true; -- don't duplicate conn_refused stat for v4 and v6 addresses
            fail_on_5xx = 0;
        }; -- proxy
    };
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port=stats_port; }
        };
    };

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
            errorlog = {
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        report = {
                            uuid = "default";
                            balancer2 = {
                                by_name_policy = {
                                    name = "backend1";
                                    unique_policy = {};
                                };
                                retry_non_idempotent = retry_non_idempotent;
                                not_retryable_methods = not_retryable_methods;
                                attempts = attempts;
                                connection_attempts = connection_attempts;
                                hedged_delay = hedged_delay;
                                status_code_blacklist = prepare_blacklist(status_code_blacklist);
                                return_last_5xx = return_last_5xx;
                                rr = rr;
                                on_error = gen_on_error(on_error_status);
                                use_on_error_for_non_idempotent = use_on_error_for_non_idempotent;
                            }; -- balancer
                        }; --report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
