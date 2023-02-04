backend_host = backend_host or "localhost"
instance = {
    thread_mode = thread_mode; set_no_file = false;

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
                    rr = {
                        randomize_initial_state = false;
                        {
                            proxy = {
                                host = backend_host; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = 10;
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        },
                    }; -- rr
                }; -- balancer
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
