instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;

    config_uid = "1234567890";

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log_level = "DEBUG";
                log = errorlog;
                http = {
                    maxlen = 65536; maxreq = 65536;
                    enable_cycles_protection = enable_cycles_protection;
                    max_cycles = max_cycles;
                    cycles_header_len_alert = cycles_header_len_alert;
                    disable_cycles_protection_file = disable_cycles_protection_file;
                    accesslog = {
                        log = accesslog;
                        balancer2 = {
                            attempts = 2;
                            rr = {
                                randomize_initial_state = false;
                                backend1 = {
                                    proxy = {
                                        host = 'localhost';
                                        port = default_backend_port;
                                    }; -- proxy
                                }; -- backend1
                            }; -- rr
                        }; -- balancer2
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
