instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                accesslog = {
                    log = accesslog;

                    balancer2 = {
                        attempts = 2;
                        hashing = {
                            {
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "9s"; backend_timeout = "9s";
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            };
                        }; -- hashing
                        on_error = {
                            errordocument = {
                                status = 200;
                                content = "on_error";
                            }; -- errordocument
                        }; -- on_error
                    }; -- balancer
                } -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
