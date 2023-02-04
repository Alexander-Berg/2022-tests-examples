backend_host = backend_host or "localhost"
instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

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
            http = {
                maxlen = 65536; maxreq = 65536;
                balancer2 = {
                    attempts = 2;
                    timeout = timeout;
                    attempts_file = attempts_file;
                    rewind_limit = rewind_limit;
                    weighted2 = {
                        {
                            proxy = {
                                host = "localhost"; port = backend1_port;
                                connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                resolve_timeout = "0.3s";
                                fail_on_5xx = fail_on_5xx;
                            }; -- proxy
                        },
                        {
                            proxy = {
                                host = "localhost"; port = backend2_port;
                                connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                resolve_timeout = "0.3s";
                                fail_on_5xx = fail_on_5xx;
                            }; -- proxy
                        },
                    }; -- rr
                }; -- balancer2
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
