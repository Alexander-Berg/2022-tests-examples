function make_antirobot()
    local result = {
        cut_request = cut_request;
        no_cut_request_file = no_cut_request_file;
        file_switch = file_switch;
        checker = {
            proxy = {
                host = "localhost"; port = antirobot_port;
                connect_timeout = "0.3s"; backend_timeout = antirobot_backend_timeout;
                resolve_timeout = "0.3s";
                fail_on_5xx = 0;
            }; -- proxy
        }; -- checker
        module = {
            proxy = {
                host = "localhost"; port = backend_port;
                connect_timeout = "0.3s"; backend_timeout = "10s";
                resolve_timeout = "0.3s";
                fail_on_5xx = 0;
            }; -- proxy
        }; -- module
    }; -- antirobot
    return result
end

function make_subsections()
    if root_module == 'antirobot' then
        return make_antirobot()
    end;
    local result = {
        create_func = {
            ["X-Forwarded-For-Y"] = 'realip';
        };
        antirobot = make_antirobot();
    }; -- headers
    return result
end

instance = {
    thread_mode = thread_mode; set_no_file = false;
    ban_addresses_disable_file = ban_addresses_disable_file;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port=stats_port; }
        };
    }; -- unistat

    http2 = {
        allow_http2_without_ssl = true;
        http = {
            maxreq = 64 * 1024; maxlen = 64 * 1024;
            ipdispatch = {
                admin = {
                    ip = "localhost"; port = admin_port;
                    admin = {};
                };
                default = {
                    regexp = {
                        admin = {
                            match_fsm = { path = "/admin"; case_insensitive = false; surround = false; };
                            admin = {};
                        }; -- admin
                        default = {
                            accesslog = {
                                log = accesslog;
                                report = {
                                    uuid = "service_total";
                                    ranges = "7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms";
                                    disable_robotness = true; disable_sslness = true;
                                    [root_module] = make_subsections();
                                }; -- report
                            }; -- accesslog
                        }; -- default
                    }; -- regexp
                };
            }; -- ipdispatch
        }; -- http
    }; -- http2
}; -- instance
