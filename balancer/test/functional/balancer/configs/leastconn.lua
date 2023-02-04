instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
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
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                balancer2 = {
                    leastconn = {
                        {
                            proxy = {
                                host = "localhost"; port = backend1_port;
                                connect_timeout = "0.3s"; backend_timeout = "1s";
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        },
                        {
                            proxy = {
                                host = "localhost"; port = backend2_port;
                                connect_timeout = "0.3s"; backend_timeout = "1s";
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        },
                    }; --leastconn
                }; -- balancer2
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
