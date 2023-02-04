if return_last_5xx then
    status_code_blacklist = {"5xx";};
    fail_on_5xx = false;
end

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
                report = {
                    uuid = "test";
                    ranges = "0,1s";

                    events = {
                        stats = "report";
                    };

                    errorlog = {
                        log = errorlog;
                        balancer2 = {
                            attempts_rate_limiter = {
                                coeff = coeff;
                                limit = limit;
                                max_budget = max_budget;
                            }; -- attempts_rate_limiter

                            attempts = attempts or 2;
                            fast_attempts = fast_attempts;
                            connection_attempts = connection_attempts;
                            hedged_delay = hedged_delay;
                            return_last_5xx = return_last_5xx;
                            status_code_blacklist = status_code_blacklist;

                            simple_policy = {};

                            rr = {
                                randomize_initial_state = false;
                                {
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "5s"; backend_timeout = backend_timeout or "5s";
                                        resolve_timeout = "1s";
                                        fail_on_5xx = fail_on_5xx;
                                    };
                                };
                            }; -- balancer2/rr
                        }; -- balancer2
                    }; -- errorlog
                }; -- report
            }; -- ipdispatch/default
        }; -- ipdispatch
    }; -- http
}; -- instance
