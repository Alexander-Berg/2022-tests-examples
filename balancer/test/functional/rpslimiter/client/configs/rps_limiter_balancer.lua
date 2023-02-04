instance = {
    thread_mode = thread_mode; set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; }
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; }
    };
    ipdispatch = {
        default = {
            http = {
                maxlen = 65536; maxreq = 65536;
                rps_limiter = {
                    disable_file = disable_file;
                    skip_on_error = skip_on_error;
                    namespace = namespace;
                    register_only = register_only;
                    register_backend_attempts = register_backend_attempts;
                    checker = {
                        proxy = {
                            host = "localhost"; port = checker_port;
                            connect_timeout = "10s"; backend_timeout = "10s"; resolve_timeout = "10s";
                        }; -- proxy
                    }; -- checker
                    module = {
                        balancer2 = {
                            attempts = attempts;
                            simple_policy = {};
                            rr = {
                                randomize_initial_state = false;
                                {
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "10s"; backend_timeout = "10s"; resolve_timeout = "10s";
                                    };
                                };
                            };
                            on_error = {
                                errordocument = {
                                    status = 404;
                                    content = "error";
                                };
                            };
                        }; -- balancer2
                    }; -- module
                }; -- rps_limiter
            }; -- http
        }; -- default
    }; -- ipdispatch
}; -- instance
