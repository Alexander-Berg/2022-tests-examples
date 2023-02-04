if backend_timeout == nil then backend_timeout = "5s"; end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    workers = workers;

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
            errorlog = {
                log = errorlog;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    keepalive = 1;
                    accesslog = {
                        log = accesslog;
                        threshold = {
                            pass_timeout = pass_timeout;
                            recv_timeout = recv_timeout;
                            lo_bytes = lo_bytes;
                            hi_bytes = hi_bytes;

                            on_pass_timeout_failure = {
                                proxy = {
                                    host = "localhost"; port = on_pass_failure_backend_port;
                                    backend_timeout = backend_timeout;
                                    connect_timeout = "1s"; resolve_timeout = "1s";
                                    fail_on_5xx = false;
                                }; -- proxy
                            }; -- on_pass_timeout_failure

                            proxy = {
                                host = 'localhost'; port = backend_port;
                                backend_timeout = backend_timeout;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                fail_on_5xx = false;
                            }; -- proxy
                        }; -- threshold
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
