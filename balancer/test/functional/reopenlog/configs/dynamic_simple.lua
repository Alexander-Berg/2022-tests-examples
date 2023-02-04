instance = {
    state_directory = "./state/";
    backends_blacklist = backends_blacklist;
    workers = 3;
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
                balancer2 = {
                    attempts = 2;
                    dynamic = {
                        first = {
                            proxy = {
                                host = "localhost"; port = backend1_port;
                                connect_timeout = "0.3s"; backend_timeout = 10;
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        };

                        second = {
                            proxy = {
                                host = "localhost"; port = backend2_port;
                                connect_timeout = "0.3s"; backend_timeout = 10;
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        };
                    }; -- rr
                }; -- balancer2
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
