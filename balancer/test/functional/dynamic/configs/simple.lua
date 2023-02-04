instance = {
    backends_blacklist = backends_blacklist;
    workers = workers;
    thread_mode = thread_mode; set_no_file = false;
    dynamic_balancing_log = dynamic_balancing_log;

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
                    balancer2 = {
                        attempts = 2;
                        dynamic = {
                            max_pessimized_share = max_pessimized_share;
                            min_pessimization_coeff = min_pessimization_coeff;
                            weight_increase_step = weight_increase_step;

                            backend0 = {
                                proxy = {
                                    host = "localhost"; port = backend0_port;
                                    connect_timeout = "0.3s"; backend_timeout = 10;
                                    resolve_timeout = "0.3s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            };

                            backend1 = {
                                proxy = {
                                    host = "localhost"; port = backend1_port;
                                    connect_timeout = "0.3s"; backend_timeout = 10;
                                    resolve_timeout = "0.3s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            };
                        }; -- dynamic
                    }; -- balancer2
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
