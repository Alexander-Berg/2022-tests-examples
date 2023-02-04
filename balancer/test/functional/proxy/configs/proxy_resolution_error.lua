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
            ip = "localhost";
            port = port;

            errorlog = {
                log = errorlog;
                log_level = log_level;

                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        proxy = {
                            host = "this-host-should-not-exist";
                            port = backend_port;
                            cached_ip = cached_ip;
                            connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                            watch_client_close = watch_client_close;
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
