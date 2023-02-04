instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    workers = workers;
    log = instance_log;
    enable_reuse_port = true;
    pinger_required = true;
    pinger_state_directory = "pingerfolder";
    pinger_log = pinger_log;

    sd = {
        host = "localhost";
        port = "12345";
        cache_dir = "./sd_cache";
        log = sd_log;
        client_name = "balancer_functional_test";
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            };
        }; -- ipdispatch/admin
        default = {
            errorlog = {
                log = error_log;
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = access_log;
                        regexp = {
                            error = {
                                match_fsm = {
                                    path = "/error.*"; case_insensitive = true; surround = false;
                                };
                                proxy = {
                                    host = "localhost"; port = dummy_backend_port;
                                    connect_timeout = "1s"; backend_timeout = "5s";
                                    resolve_timeout = "1s";
                                }; -- proxy
                            }; -- regexp/error
                            default = {
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "1s"; backend_timeout = "5s";
                                    resolve_timeout = "1s";
                                }; -- proxy
                            }; -- regexp/default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
