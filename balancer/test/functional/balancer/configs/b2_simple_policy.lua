instance = {
    set_no_file = false;
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        ipdispatch = {
            admin = {
                ip = "localhost"; port = admin_port;
                admin = {};
            };
            default = {
                hasher = {
                    mode = "request";
                    balancer2 = {
                        simple_policy = {};
                        attempts = 3;

                        rr = {
                            randomize_initial_state = false;
                            first = {
                                weight = 2;
                                proxy = {
                                    host = "localhost";
                                    port = first_backend_port;
                                    fail_on_5xx = 1;
                                    connect_timeout = "5s";
                                    backend_timeout = "5s";
                                    resolve_timeout = "1s";
                                }; -- proxy
                            }; -- first
                            second = {
                                weight = 1;
                                proxy = {
                                    host = "localhost";
                                    port = second_backend_port;
                                    fail_on_5xx = 1;
                                    connect_timeout = "5s";
                                    backend_timeout = "5s";
                                    resolve_timeout = "1s";
                                }; -- proxy
                            }; -- second
                        }; -- balancer2/rr

                        on_error = {
                            errordocument = {
                                status = 200;
                            }; -- errordocument
                        }; -- on_error
                    }; -- balancer2
                }; -- hasher
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
