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
                balancer2 = {
                    backoff_policy = {
                        probabilities = {
                            prob1, prob2
                        };
                        retry_policy = {
                            unique_policy = {};
                        };
                    }; -- backoff_policy

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
            }; -- ipdispatch/defaul
        }; -- ipdispatch
    }; -- http
}; -- instance
