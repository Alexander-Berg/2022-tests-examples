backend_timeout = backend_timeout or "10s";

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
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {}
            }; -- http
        }; -- admin
        test = {
            errorlog = {
                log = errorlog;
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = accesslog;
                        report = {
                            uuid = "total";
                            cutter = {
                                bytes = cutter_bytes;
                                timeout = cutter_timeout;
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "1s"; backend_timeout = backend_timeout;
                                    resolve_timeout = "1s";
                                    keepalive_count = 1;
                                }; -- proxy
                            }; -- cutter
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
