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
                    cgi_hasher = {
                        parameters = {
                            "uid";
                        }; -- parameters
                        request_replier = {
                            rate = 1;
                            sink = {
                                balancer2 = {
                                    rendezvous_hashing = {
                                        {
                                            weight = 1;
                                            proxy = {
                                                host = "localhost";
                                                port = sink_backend1_port;
                                                connect_timeout = connect_timeout;
                                                backend_timeout = sink_backend_timeout;
                                                resolve_timeout = "1s";
                                            };
                                        };
                                        {
                                            weight = 1;
                                            proxy = {
                                                host = "localhost";
                                                port = sink_backend2_port;
                                                connect_timeout = connect_timeout;
                                                backend_timeout = sink_backend_timeout;
                                                resolve_timeout = "1s";
                                            };
                                        };
                                    }; -- rendezvous_hashing
                                }; -- balancer2
                            }; -- request_replier/sink
                            proxy = {
                                host = "localhost"; port = main_backend_port; connect_timeout = connect_timeout; backend_timeout = main_backend_timeout;
                                resolve_timeout = "1s";
                            };
                        }; -- request_replier
                    };
                }; -- http
            }; -- errorlog
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance
