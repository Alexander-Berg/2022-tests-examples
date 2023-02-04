port = port or 8081;
admin_port = admin_port or 8082;
backend_port = backend_port or 9765;
instance = {
    thread_mode = thread_mode; set_no_file = false;

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
                                ranges = "1ms,10ms";
                                disable_robotness = true; disable_sslness = true;
                                antirobot_wrapper = {
                                    cut_request = cut_request;
                                    no_cut_request_file = no_cut_request_file;
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = "10s";
                                        resolve_timeout = "0.3s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- antirobot_wrapper
                            }; -- report
                        }; -- accesslog
                    }; -- default
                }; -- regexp
            };
        }; -- ipdispatch
    }; -- http
}; -- instance
