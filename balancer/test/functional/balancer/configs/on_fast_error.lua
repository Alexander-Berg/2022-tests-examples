instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

    ipdispatch = {
        default = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        balancer = {
                            fast_503 = fast_503;

                            unique_policy = {};
                            rr = {
                                backend = {
                                    weight = backend_weight;
                                    proxy = {
                                        host = "127.0.0.1";
                                        port = backend_port;
                                        backend_timeout = "10s";
                                        connect_timeout = "1s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = true;
                                    }; -- proxy
                                }; -- backend
                            }; -- rr
                            on_error = {
                                errordocument = {
                                    status = 404;
                                    content = "on_error";
                                }; -- errordocument
                            }; -- on_error
                            on_fast_error = {
                                errordocument = {
                                    status = 405;
                                    content = "on_fast_error";
                                }; -- errordocument
                            }; -- on_fast_error
                        }; -- balancer
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- default
    }; -- ipdispatch
}; -- instance
