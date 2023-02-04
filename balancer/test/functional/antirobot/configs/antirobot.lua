instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_port
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
                        hasher = {
                            mode = "subnet";
                            take_ip_from = "X-Real-Ip";
                            antirobot = {
                                checker = {
                                    balancer2 = {
                                        attempts = 2;
                                        hashing = {
                                            {
                                                weight = 1;
                                                proxy = {
                                                    host = "localhost"; port = antirobot_port1;
                                                    connect_timeout = "0.3s"; backend_timeout = "1s";
                                                    resolve_timeout = "0.3s";
                                                    fail_on_5xx = 0;
                                                }; -- proxy
                                            };
                                            {
                                                weight = 1;
                                                proxy = {
                                                    host = "localhost"; port = antirobot_port2;
                                                    connect_timeout = "0.3s"; backend_timeout = "1s";
                                                    resolve_timeout = "0.3s";
                                                    fail_on_5xx = 0;
                                                }; -- proxy
                                            };
                                        }; -- hashing
                                    }; -- balancer2
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
                        }; -- hasher
                    }; -- default
                }; -- regexp
            };
        }; -- ipdispatch
    }; -- http
}; -- instance
