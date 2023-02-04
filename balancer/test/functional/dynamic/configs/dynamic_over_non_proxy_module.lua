instance = {
    state_directory = "./state";
    workers = 4;
    thread_mode = thread_mode; set_no_file = false;

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
                            max_pessimized_share = 0.2;

                            first = {
                                debug = {
                                    proxy = {
                                        host = "localhost"; port = 8080;
                                        connect_timeout = "0.3s"; backend_timeout = 10;
                                        resolve_timeout = "0.3s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }
                            };

                            second = {
                                proxy = {
                                    host = "localhost"; port = 8081;
                                    connect_timeout = "0.3s"; backend_timeout = 10;
                                    resolve_timeout = "0.3s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            };
                        }; -- rr
                    }; -- balancer2
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
