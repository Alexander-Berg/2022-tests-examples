instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

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
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    balancer2 = {
                        attempts = 2;
                        hashing = {
                            {
                                proxy = {
                                    buffering = 1;
                                    host = "localhost"; port = backend_port; connect_timeout = "3s";
                                    backend_timeout = "0.3s";
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                    watch_client_close = watch_client_close;
                                }; -- proxy
                            };
                            {
                                proxy = {
                                    buffering = 1;
                                    host = "localhost"; port = backend_port; connect_timeout = "3s";
                                    backend_timeout = "0.3s";
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                    watch_client_close = watch_client_close;
                                }; -- proxy
                            };
                        }; -- active
                    }; -- balancer2
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
