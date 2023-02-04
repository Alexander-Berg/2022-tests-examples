instance = {
    set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; }
        };
    }; -- unistat

    ipdispatch = {
        default = {
            http = {
                maxlen = 65536; maxreq = 65536;
                accesslog = {
                    log = access_log;
                    rate_limiter = {
                        max_requests = max_requests;
                        interval = interval;
                        max_requests_in_queue = max_requests_in_queue;

                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "0.3s"; backend_timeout = 10;
                            resolve_timeout = "0.3s";
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- rate_limiter
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
