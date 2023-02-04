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
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        default = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    request_replier = {
                        rate = rate;
                        rate_file = rate_file;
                        enable_failed_requests_replication = enable_failed_requests_replication;
                        sink = {
                            proxy = {
                                host = "localhost"; port = sink_backend_port; connect_timeout = connect_timeout; backend_timeout = sink_backend_timeout;
                                resolve_timeout = "1s";
                            };
                        }; -- request_replier/sink
                        proxy = {
                            host = "localhost"; port = main_backend_port; connect_timeout = connect_timeout; backend_timeout = main_backend_timeout;
                            resolve_timeout = "1s";
                        };
                    }; -- request_replier
                }; -- http
            }; -- errorlog
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
