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
                        log = accesslog;
                        report = {
                            uuid = "default";
                            balancer2 = {
                                simple_policy = {};
                                attempts = attempts;
                                hedged_delay = hedged_delay;
                                rr = {
                                    randomize_initial_state = false;
                                    backend1 = {
                                        proxy = {
                                            host = "localhost"; port = backend1_port;
                                            connect_timeout = "1s"; backend_timeout = "1s";
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                        }; -- proxy
                                    },
                                    backend2 = {
                                        proxy = {
                                            host = "localhost"; port = backend2_port;
                                            connect_timeout = "1s"; backend_timeout = "1s";
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                        }; -- proxy
                                    }
                                }; -- rr
                            }; -- balancer
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
