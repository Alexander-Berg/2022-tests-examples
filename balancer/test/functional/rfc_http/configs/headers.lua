instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    log = log;
    thread_mode = thread_mode; set_no_file = false;

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
                log_level = "INFO";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    keepalive = keepalive;
                    headers = {
                        accesslog = {
                            log = accesslog;
                            proxy = {
                                host = 'localhost'; port = backend_port;
                                keepalive_count = keepalive_count;
                                backend_timeout = backend_timeout or "60s";
                                resolve_timeout = "1s";
                                connect_timeout = "1s";
                            }; -- proxy
                        }; -- accesslog
                    }; -- headers
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end
