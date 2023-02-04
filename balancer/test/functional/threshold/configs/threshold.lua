if backend_timeout == nil then backend_timeout = "5s"; end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    };

    workers = workers;

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        report = {
                            uuid = "total";
                            threshold = {
                                pass_timeout = pass_timeout;
                                recv_timeout = recv_timeout;
                                lo_bytes = lo_bytes;
                                hi_bytes = hi_bytes;
                                proxy = {
                                    host = 'localhost'; port = backend_port;
                                    backend_timeout = backend_timeout;
                                    connect_timeout = "1s"; resolve_timeout = "1s";
                                    fail_on_5xx = false;
                                }; -- proxy
                            }; -- threshold
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
