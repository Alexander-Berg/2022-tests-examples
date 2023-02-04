instance = {
    set_no_file = false;
    workers = workers;

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
                errorlog = {
                    log = errorlog;
                    balancer2 = {
                        watermark_policy = {
                            lo = lo; hi = hi;
                            is_shared = shared;
                            params_file = params_file;
                            switch_key = switch_key;
                            switch_file = switch_file;
                            switch_default = switch_default;
                            retry_policy = {
                                unique_policy = {};
                            };
                        }; -- watermark_policy

                        attempts = attempts or 2;

                        rr = {
                            randomize_initial_state = false;
                            {
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "5s"; backend_timeout = backend_timeout or "5s";
                                    resolve_timeout = "1s";
                                };
                            };
                        }; -- balancer2/rr
                    }; -- balancer2
                }; -- errorlog
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
