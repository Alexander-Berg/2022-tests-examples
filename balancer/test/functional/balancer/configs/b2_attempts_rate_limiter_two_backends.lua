instance = {
    workers = workers;
    set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
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
                        attempts_rate_limiter = {
                            coeff = coeff;
                            limit = limit;
                            max_budget = max_budget;
                        }; -- attempts_rate_limiter

                        attempts = attempts or 2;
                        fast_503 = true;
                        fast_attempts = fast_attempts;
                        connection_attempts = connection_attempts;
                        hedged_delay = hedged_delay;

                        by_name_policy = {
                            name = "b1";
                            unique_policy = {};
                        };

                        rr = {
                            randomize_initial_state = false;
                            b1 = {
                                proxy = {
                                    host = "localhost"; port = backend1_port;
                                    connect_timeout = "5s"; backend_timeout = backend_timeout or "5s";
                                    resolve_timeout = "1s";
                                };
                            },

                            b2 = {
                                proxy = {
                                    host = "localhost"; port = backend2_port;
                                    connect_timeout = "5s"; backend_timeout = backend_timeout or "5s";
                                    resolve_timeout = "1s";
                                };
                            }
                        }; -- balancer2/rr
                    }; -- balancer2
                }; -- errorlog
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
